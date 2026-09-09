package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** 粒子消散效果的默认参数。 */
object ParticleDissolveDefaults {
    const val DURATION_MILLIS = 600
    const val WAVE_DELAY_MILLIS = 80L
    const val MAX_PARTICLE_COUNT = 2_500

    val ParticleSize = 3.dp

    val AnimationSpec: AnimationSpec<Float> =
        tween(
            durationMillis = DURATION_MILLIS,
            easing = LinearEasing,
        )
}

/**
 * 将当前内容捕获为粒子并播放一次消散动画。
 */
@Stable
fun Modifier.particleDissolve(
    isDissolving: Boolean,
    particleSize: Dp = ParticleDissolveDefaults.ParticleSize,
    animationSpec: AnimationSpec<Float> = ParticleDissolveDefaults.AnimationSpec,
    onComplete: () -> Unit = {},
): Modifier =
    composed {
        require(particleSize.value.isFinite() && particleSize.value > 0f) {
            "particleSize must be finite and greater than zero"
        }
        val particleSizePx = with(LocalDensity.current) { particleSize.toPx() }
        val graphicsLayer = rememberGraphicsLayer()
        val progress = remember { Animatable(0f) }
        val currentOnComplete by rememberUpdatedState(onComplete)

        val captureSignal =
            remember(isDissolving, particleSizePx) {
                CompletableDeferred<Unit>()
            }
        var captureState by
            remember(isDissolving, particleSizePx) {
                mutableStateOf<ParticleCaptureState>(
                    if (isDissolving) {
                        ParticleCaptureState.AwaitingCapture
                    } else {
                        ParticleCaptureState.Inactive
                    },
                )
            }

        LaunchedEffect(isDissolving, particleSizePx, animationSpec) {
            progress.snapTo(0f)
            if (!isDissolving) return@LaunchedEffect

            captureSignal.await()
            // 避免 Deferred 在 draw 调用栈内同步恢复后立即执行 GPU 回读。
            yield()
            captureState = ParticleCaptureState.Capturing

            try {
                val imageBitmap = graphicsLayer.toImageBitmap()
                val particles =
                    try {
                        withContext(Dispatchers.Default) {
                            generateParticles(
                                imageBitmap = imageBitmap,
                                particleSize = particleSizePx,
                            )
                        }
                    } finally {
                        imageBitmap.asAndroidBitmap().recycle()
                    }
                captureState = ParticleCaptureState.Ready(particles)
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = animationSpec,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                captureState = ParticleCaptureState.Failed
            }

            currentOnComplete()
        }

        drawWithContent {
            if (!isDissolving) {
                drawContent()
                return@drawWithContent
            }

            when (val state = captureState) {
                ParticleCaptureState.Inactive -> drawContent()

                ParticleCaptureState.AwaitingCapture -> {
                    if (size.width > 0f && size.height > 0f) {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    } else {
                        drawContent()
                    }
                    captureSignal.complete(Unit)
                }

                ParticleCaptureState.Capturing -> drawLayer(graphicsLayer)

                ParticleCaptureState.Failed -> drawContent()

                is ParticleCaptureState.Ready -> {
                    val currentProgress = progress.value
                    state.particles.forEach { particle ->
                        val age =
                            ((currentProgress - particle.startTime) / (1f - particle.startTime))
                                .coerceIn(0f, 1f)
                        val x =
                            particle.startX +
                                particle.velocityX * age +
                                particle.accelerationX * age * age * 0.5f
                        val y =
                            particle.startY +
                                particle.velocityY * age +
                                particle.accelerationY * age * age * 0.5f
                        val alpha = (1f - age) * (1f - age)
                        val radius = particleSizePx * (1f + age * 0.2f)

                        if (
                            alpha > MIN_VISIBLE_ALPHA &&
                            x >= -PARTICLE_OVERFLOW_PX &&
                            x <= size.width + PARTICLE_OVERFLOW_PX &&
                            y >= -size.height &&
                            y <= size.height * 2
                        ) {
                            drawCircle(
                                color = particle.color.copy(alpha = alpha * particle.color.alpha),
                                radius = radius,
                                center = Offset(x, y),
                            )
                        }
                    }
                }
            }
        }
    }

private sealed interface ParticleCaptureState {
    data object Inactive : ParticleCaptureState

    data object AwaitingCapture : ParticleCaptureState

    data object Capturing : ParticleCaptureState

    data class Ready(
        val particles: List<Particle>,
    ) : ParticleCaptureState

    data object Failed : ParticleCaptureState
}

private data class Particle(
    val startX: Float,
    val startY: Float,
    val velocityX: Float,
    val velocityY: Float,
    val accelerationX: Float,
    val accelerationY: Float,
    val color: Color,
    val startTime: Float,
)

private fun generateParticles(
    imageBitmap: ImageBitmap,
    particleSize: Float,
): List<Particle> {
    val width = imageBitmap.width
    val height = imageBitmap.height
    if (width == 0 || height == 0) return emptyList()

    val pixels = IntArray(width * height)
    imageBitmap.readPixels(
        buffer = pixels,
        width = width,
        height = height,
    )

    val sizeBasedStep = (particleSize * 2).toInt().coerceAtLeast(MIN_SAMPLE_STEP_PX)
    val cappedStep =
        ceil(
            sqrt(
                width.toDouble() * height.toDouble() /
                    ParticleDissolveDefaults.MAX_PARTICLE_COUNT,
            ),
        ).toInt()
    val step = max(sizeBasedStep, cappedStep.coerceAtLeast(MIN_SAMPLE_STEP_PX))
    val estimatedCount =
        min(
            ((width + step - 1) / step) * ((height + step - 1) / step),
            ParticleDissolveDefaults.MAX_PARTICLE_COUNT,
        )
    val particles = ArrayList<Particle>(estimatedCount)

    particleLoop@ for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            if (particles.size >= ParticleDissolveDefaults.MAX_PARTICLE_COUNT) {
                break@particleLoop
            }
            val pixel = pixels[y * width + x]
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < MIN_PARTICLE_ALPHA) continue

            val angle = Random.nextFloat() * TWO_PI
            val speed = Random.nextFloat() * SPEED_RANGE + MIN_SPEED
            val velocityX = cos(angle) * speed
            val velocityY = sin(angle) * speed - Random.nextFloat() * UPWARD_VELOCITY

            particles +=
                Particle(
                    startX = x.toFloat(),
                    startY = y.toFloat(),
                    velocityX = velocityX,
                    velocityY = velocityY,
                    accelerationX = (Random.nextFloat() - 0.5f) * HORIZONTAL_ACCELERATION,
                    accelerationY = Random.nextFloat() * VERTICAL_ACCELERATION_RANGE + MIN_VERTICAL_ACCELERATION,
                    color = Color(pixel),
                    startTime = Random.nextFloat() * MAX_START_DELAY,
                )
        }
    }

    return particles
}

private const val MIN_SAMPLE_STEP_PX = 4
private const val MIN_PARTICLE_ALPHA = 30
private const val MIN_VISIBLE_ALPHA = 0.01f
private const val PARTICLE_OVERFLOW_PX = 100f
private const val TWO_PI = 2f * Math.PI.toFloat()
private const val MIN_SPEED = 80f
private const val SPEED_RANGE = 150f
private const val UPWARD_VELOCITY = 60f
private const val HORIZONTAL_ACCELERATION = 100f
private const val MIN_VERTICAL_ACCELERATION = 150f
private const val VERTICAL_ACCELERATION_RANGE = 200f
private const val MAX_START_DELAY = 0.2f
