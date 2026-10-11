@file:Suppress("ktlint:standard:package-name", "ktlint:standard:function-naming")

package me.spica27.spicamusic.ui.widget.audio_seekbar

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.isActive
import org.intellij.lang.annotations.Language
import timber.log.Timber
import kotlin.math.min

private const val GLOW_PERIOD_NANOS = 6_000_000_000L

@Language("AGSL")
internal const val GLOW_PARTICLE_SHADER_SOURCE = """
    uniform float2 head;
    uniform float density;
    uniform float phase;
    uniform float interaction;
    uniform float direction;
    uniform float playedLength;
    layout(color) uniform half4 glowColor;

    half4 main(float2 fragCoord) {
        float2 p = (fragCoord - head) / density;
        float pulse = 1.0 + 0.06 * sin(phase * 18.849556);
        float2 wide = p / float2(31.0, 11.0);
        float2 tight = p / float2(13.0, 4.5);
        float halo = 0.20 * exp(-dot(wide, wide) * 2.0);
        float core = 0.34 * exp(-dot(tight, tight) * 2.0);
        float behind = -p.x * direction;
        float tail = 0.10 * exp(-max(behind, 0.0) / 27.0 - p.y * p.y / 9.0);
        tail *= smoothstep(-2.0, 4.0, behind);
        tail *= 1.0 - smoothstep(playedLength, playedLength + 2.0, behind);
        float edge = 1.0 - smoothstep(19.0, 24.0, abs(p.y));
        float alpha = clamp((halo + core + tail) * pulse * (1.0 + interaction * 0.35), 0.0, 0.8);
        alpha *= edge * glowColor.a;
        half3 color = mix(glowColor.rgb, half3(1.0), half(core));
        // 输出预乘透明色。
        return half4(color * half(alpha), half(alpha));
    }
"""

/** 带辉光与独立粒子的进度条。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlowParticleSeekBar(
    progress: Float,
    onProgressChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onProgressChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    animationsEnabled: Boolean = true,
    glowColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val dragged by interactionSource.collectIsDraggedAsState()
    val pressed by interactionSource.collectIsPressedAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    val interaction =
        animateFloatAsState(
            targetValue = if (enabled && (dragged || pressed || focused)) 1f else 0f,
            animationSpec = tween(if (animationsEnabled) 150 else 0),
            label = "glow_seek_interaction",
        )
    val value = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
    val drawnProgress =
        animateFloatAsState(
            targetValue = value,
            animationSpec = tween(if (!animationsEnabled || dragged || pressed) 0 else 120, easing = LinearEasing),
            label = "glow_seek_progress",
        )
    val elapsedTime = rememberGlowTime(animationsEnabled && enabled)
    val particles = remember { GlowParticleSystem() }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val shader =
        remember {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                runCatching { GlowShader() }
                    .onFailure { Timber.w(it, "进度条辉光着色器编译失败") }
                    .getOrNull()
            } else {
                null
            }
        }
    val haloBrush =
        remember(glowColor) {
            Brush.radialGradient(
                0f to lerp(glowColor, Color.White, 0.35f).copy(alpha = glowColor.alpha * 0.45f),
                0.35f to glowColor.copy(alpha = glowColor.alpha * 0.24f),
                1f to glowColor.copy(alpha = 0f),
                center = Offset.Zero,
                radius = 1f,
            )
        }

    // 保留原生拖动与无障碍交互。
    Slider(
        value = value,
        onValueChange = onProgressChange,
        onValueChangeFinished = onProgressChangeFinished,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.heightIn(min = 48.dp),
        thumb = {},
        track = {
            Canvas(Modifier.fillMaxWidth().height(64.dp)) {
                if (size.width <= 0f || size.height <= 0f) return@Canvas
                val fraction = drawnProgress.value
                val direction = if (isRtl) -1f else 1f
                val start = Offset(if (isRtl) size.width else 0f, center.y)
                val end = Offset(if (isRtl) 0f else size.width, center.y)
                val head = Offset(start.x + direction * fraction * size.width, center.y)
                val playedLength = fraction * size.width
                val emphasis = interaction.value
                val opacity = if (enabled) 1f else 0.38f
                val color = glowColor.copy(alpha = glowColor.alpha * opacity)
                val stroke = (3f + emphasis * 1.2f).dp.toPx()
                val timeNanos = elapsedTime.value
                val animationPhase = (timeNanos % GLOW_PERIOD_NANOS).toFloat() / GLOW_PERIOD_NANOS
                particles.update(
                    timeNanos = timeNanos,
                    emitterX = head.x / density,
                    trackWidth = size.width / density,
                    emissionDirection = direction,
                    emitting = animationsEnabled && enabled,
                )

                drawLine(trackColor, start, end, stroke, StrokeCap.Round, alpha = opacity)
                if (playedLength > 0f) {
                    drawLine(
                        brush =
                            Brush.linearGradient(
                                listOf(color, lerp(color, Color.White.copy(alpha = opacity), 0.6f)),
                                start,
                                head,
                            ),
                        start = start,
                        end = head,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    shader != null &&
                    drawContext.canvas.nativeCanvas.isHardwareAccelerated
                ) {
                    shader.draw(this, head, color, animationPhase, emphasis, direction, playedLength)
                } else {
                    drawGlowFallback(head, haloBrush, emphasis, opacity)
                }
                drawParticles(particles, color)

                val highlightLength = min(14.dp.toPx(), playedLength)
                if (highlightLength > 0f) {
                    val highlightStart = head.copy(x = head.x - direction * highlightLength)
                    drawLine(
                        brush =
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = opacity)),
                                highlightStart,
                                head,
                            ),
                        start = highlightStart,
                        end = head,
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                drawCircle(Color.White, radius = stroke * 0.48f, center = head, alpha = opacity)
            }
        },
    )
}

@Composable
private fun rememberGlowTime(animate: Boolean): State<Long> {
    val elapsedTime = remember { mutableLongStateOf(0L) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(animate, lifecycleOwner) {
        if (!animate) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var previousFrame = 0L
            while (isActive) {
                withFrameNanos { frame ->
                    if (previousFrame != 0L) {
                        elapsedTime.longValue += frame - previousFrame
                    }
                    previousFrame = frame
                }
            }
        }
    }
    // 动画只更新绘制。
    return elapsedTime
}

internal fun DrawScope.drawGlowFallback(
    head: Offset,
    brush: Brush,
    interaction: Float,
    opacity: Float,
) {
    val radiusX = 36.dp.toPx()
    val radiusY = 12.dp.toPx()
    withTransform({
        translate(head.x, head.y)
        scale(radiusX, radiusY, pivot = Offset.Zero)
    }) {
        drawCircle(brush, radius = 1f, center = Offset.Zero, alpha = opacity * (0.8f + interaction * 0.2f))
    }
}

private fun DrawScope.drawParticles(
    system: GlowParticleSystem,
    color: Color,
) {
    system.particles.forEach { particle ->
        if (!particle.alive) return@forEach
        val point = Offset(particle.x * density, center.y + particle.y * density)
        val radius = particle.radius * density
        drawCircle(color, radius * 2.2f, point, alpha = particle.opacity * 0.14f)
        drawCircle(lerp(color, Color.White.copy(alpha = color.alpha), 0.75f), radius, point, alpha = particle.opacity * 0.7f)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlowShader {
    private val shader = RuntimeShader(GLOW_PARTICLE_SHADER_SOURCE)
    private val brush = ShaderBrush(shader)

    fun draw(
        scope: DrawScope,
        head: Offset,
        color: Color,
        phase: Float,
        interaction: Float,
        direction: Float,
        playedLength: Float,
    ) = with(scope) {
        // 仅绘制端点附近。
        val headX = (if (direction > 0f) 100.dp else 40.dp).toPx()
        val headY = 24.dp.toPx()
        shader.setFloatUniform("head", headX, headY)
        shader.setFloatUniform("density", density)
        shader.setFloatUniform("phase", phase)
        shader.setFloatUniform("interaction", interaction)
        shader.setFloatUniform("direction", direction)
        shader.setFloatUniform("playedLength", playedLength / density)
        shader.setColorUniform("glowColor", color.toArgb())
        translate(head.x - headX, head.y - headY) {
            drawRect(brush, size = Size(140.dp.toPx(), 48.dp.toPx()))
        }
    }
}

@Preview(name = "辉光粒子", widthDp = 360, heightDp = 96)
@Composable
private fun GlowParticleSeekBarPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            GlowParticleSeekBar(
                progress = 0.72f,
                onProgressChange = {},
                animationsEnabled = false,
                modifier = Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 24.dp),
            )
        }
    }
}
