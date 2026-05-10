package me.spica27.spicamusic.ui.widget

import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * 流动融合动态背景组件
 * 响应音乐FFT数据和封面颜色
 *
 * @param modifier 修饰符
 * @param coverColor 封面主色，用于色彩调整
 * @param enabled 是否启用动画
 * @param isDarkMode 暗色模式（true）或亮色模式（false），null时自动判断
 */
@Composable
fun FluidMusicBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = Color(0xFF2196F3),
    enabled: Boolean = true,
    isDarkMode: Boolean? = null,
) {
    val playerViewModel = LocalPlayerViewModel.current
    val settingsViewModel: SettingsViewModel = koinViewModel()

    val scope = rememberCoroutineScope()

    LaunchedEffect(enabled) {
        if (enabled) {
            playerViewModel.subscribeFFTDrawData()
        } else {
            playerViewModel.unsubscribeFFTDrawData()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // 组件销毁时取消订阅
            scope.launch(Dispatchers.Default) {
                playerViewModel.unsubscribeFFTDrawData()
            }
        }
    }

    val fftDrawData by playerViewModel.fftDrawData.collectAsStateWithLifecycle()
    val modeValue by settingsViewModel.dynamicSpectrumBackground.collectAsStateWithLifecycle()
    val backgroundMode = remember(modeValue) { DynamicSpectrumBackground.fromString(modeValue) }
    val fftSnapshot = if (enabled) fftDrawData else FloatArray(fftDrawData.size)

    when (backgroundMode) {
        DynamicSpectrumBackground.TopGlow ->
            TopGlowBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
            )

        DynamicSpectrumBackground.LiquidAurora ->
            LiquidAuroraBackground(
                modifier = modifier,
                fftDrawData = fftSnapshot,
                coverColor = coverColor,
                isDarkMode = isDarkMode,
            )

        DynamicSpectrumBackground.EffectShader -> {
            EffectShaderBackground(
                modifier = modifier,
                coverColor = coverColor,
                fftDrawData = fftSnapshot,
                isDarkMode = isDarkMode,
            )
        }

        DynamicSpectrumBackground.OFF ->
            Box(modifier = modifier)
    }
}

private const val RENDER_FRAME_DELAY_MS = 8L

private class TextureViewRenderLoop(
    threadName: String,
) {
    private val surfaceActive = AtomicBoolean(false)
    private val generation = AtomicInteger(0)
    private val stateLock = Any()
    private val renderDispatcher: ExecutorCoroutineDispatcher =
        Executors
            .newSingleThreadExecutor { runnable ->
                Thread(runnable, threadName).apply {
                    priority = Thread.MIN_PRIORITY
                }
            }.asCoroutineDispatcher()
    private val renderScope = CoroutineScope(renderDispatcher + SupervisorJob())
    private var drawJob: Job? = null

    fun start(
        textureView: TextureView,
        drawFrame: (android.graphics.Canvas) -> Unit,
    ) {
        stopAndAwait()
        surfaceActive.set(true)
        val token = generation.incrementAndGet()

        synchronized(stateLock) {
            drawJob =
                renderScope.launch {
                    while (isActive && surfaceActive.get() && generation.get() == token) {
                        val canvas =
                            try {
                                textureView.lockCanvas(null)
                            } catch (e: IllegalStateException) {
                                Timber
                                    .tag("FluidMusicBackground")
                                    .w(e, "TextureView lockCanvas failed, stopping render loop")
                                break
                            }

                        if (canvas == null) {
                            delay(RENDER_FRAME_DELAY_MS)
                            continue
                        }

                        var shouldContinue = true
                        try {
                            if (!surfaceActive.get() || generation.get() != token) {
                                shouldContinue = false
                            } else {
                                drawFrame(canvas)
                            }
                        } finally {
                            try {
                                textureView.unlockCanvasAndPost(canvas)
                            } catch (e: IllegalStateException) {
                                Timber
                                    .tag("FluidMusicBackground")
                                    .w(e, "TextureView unlockCanvasAndPost failed, stopping render loop")
                                shouldContinue = false
                            }
                        }

                        if (!shouldContinue) {
                            break
                        }

                        delay(RENDER_FRAME_DELAY_MS)
                    }
                }
        }
    }

    fun stopAndAwait() {
        surfaceActive.set(false)
        generation.incrementAndGet()
        val job =
            synchronized(stateLock) {
                drawJob.also { drawJob = null }
            }

        job?.cancel()
        if (job != null) {
            runBlocking {
                job.join()
            }
        }
    }

    fun release() {
        stopAndAwait()
        renderScope.coroutineContext.cancel()
        renderDispatcher.close()
    }
}

/** 线程安全数据持有者，供 TopGlowBackground 绘制线程读取 */
private class TopGlowHolder {
    @Volatile var fftData: FloatArray = FloatArray(0)

    @Volatile var colorA: Int = android.graphics.Color.BLUE

    @Volatile var colorB: Int = android.graphics.Color.TRANSPARENT

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rectF = RectF()
}

@Composable
private fun TopGlowBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    val holder = remember { TopGlowHolder() }
    val renderLoop = remember { TextureViewRenderLoop("TopGlow-Renderer") }

    SideEffect {
        holder.fftData = fftDrawData
        val luminance = calculateLuminance(coverColor)
        val hueShift = if (luminance < 0.5f) 24f else -24f
        holder.colorA = shiftHue(coverColor, hueShift).copy(alpha = 0.85f).toArgb()
        holder.colorB = shiftHue(coverColor, hueShift * 1.6f).copy(alpha = 0.2f).toArgb()
    }

    DisposableEffect(renderLoop) {
        onDispose {
            renderLoop.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.isOpaque = false
                tv.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            renderLoop.start(tv) { canvas ->
                                canvas.drawColor(
                                    android.graphics.Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR,
                                )
                                val data = holder.fftData
                                if (data.isEmpty()) return@start

                                val w = canvas.width.toFloat()
                                val h = canvas.height.toFloat()
                                val bandWidth = w / data.size
                                holder.paint.shader =
                                    LinearGradient(
                                        0f,
                                        0f,
                                        w,
                                        h,
                                        holder.colorA,
                                        holder.colorB,
                                        Shader.TileMode.CLAMP,
                                    )

                                data.forEachIndexed { index, magnitude ->
                                    val energy = magnitude.coerceIn(0f, 1f)
                                    val barHeight = h * 0.8f * energy + h * 0.08f
                                    val left = index * bandWidth
                                    holder.rectF.set(
                                        left,
                                        0f,
                                        left + max(1f, bandWidth * 0.9f),
                                        barHeight,
                                    )
                                    canvas.drawRect(holder.rectF, holder.paint)
                                }
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            renderLoop.stopAndAwait()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
            }
        },
    )
}

/** 线程安全数据持有者，供 LiquidAuroraBackground 绘制线程读取 */
private class LiquidAuroraHolder {
    @Volatile var fftData: FloatArray = FloatArray(0)

    @Volatile var phase: Float = 0f

    val layerColorA = IntArray(3)
    val layerColorB = IntArray(3)
    val paths = Array(3) { android.graphics.Path() }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
}

@Composable
private fun LiquidAuroraBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val holder = remember { LiquidAuroraHolder() }
    val renderLoop = remember { TextureViewRenderLoop("LiquidAurora-Renderer") }

    SideEffect {
        holder.fftData = fftDrawData
        val elapsed = System.currentTimeMillis() % 20_000L
        holder.phase = elapsed / 20_000f * 360f

        val alpha = if (isDarkMode == true) 0.9f else 0.75f
        for (layer in 0 until 3) {
            val colorA = shiftHue(coverColor, layer * 18f + 120f)
            val colorB = shiftHue(coverColor, layer * -14f - 116f)
            holder.layerColorA[layer] = colorA.copy(alpha = (alpha - layer * 0.2f)).toArgb()
            holder.layerColorB[layer] =
                colorB.copy(alpha = (alpha - layer * 0.3f).coerceAtLeast(0.1f)).toArgb()
        }
    }

    DisposableEffect(renderLoop) {
        onDispose {
            renderLoop.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.isOpaque = false
                tv.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            renderLoop.start(tv) { canvas ->
                                canvas.drawColor(
                                    android.graphics.Color.TRANSPARENT,
                                    PorterDuff.Mode.CLEAR,
                                )
                                val data = holder.fftData
                                if (data.isEmpty()) return@start

                                val w = canvas.width.toFloat()
                                val h = canvas.height.toFloat()
                                val layers = 3
                                val chunkSize = (data.size / layers).coerceAtLeast(1)

                                repeat(layers) { layer ->
                                    val startIndex = layer * chunkSize
                                    val endIndex = min(data.size, startIndex + chunkSize)
                                    if (startIndex >= endIndex) return@repeat

                                    val path = holder.paths[layer]
                                    path.reset()
                                    path.moveTo(0f, 0f)

                                    val steps = endIndex - startIndex
                                    val amplitude = h * (0.28f - layer * 0.05f)
                                    val phaseShift =
                                        (holder.phase + layer * 45f) * (PI / 180.0)

                                    for (index in 0 until steps) {
                                        val progress =
                                            if (steps == 1) {
                                                0f
                                            } else {
                                                index / (steps - 1f)
                                            }
                                        val energy = data[startIndex + index].coerceIn(0f, 1f)
                                        val wave =
                                            sin(progress * 6f + phaseShift).toFloat()
                                        val y =
                                            h * 0.35f -
                                                amplitude * energy -
                                                amplitude * 0.2f * wave
                                        path.lineTo(progress * w, y)
                                    }

                                    path.lineTo(w, 0f)
                                    path.close()

                                    holder.paint.shader =
                                        LinearGradient(
                                            0f,
                                            0f,
                                            0f,
                                            h * 0.5f,
                                            intArrayOf(
                                                holder.layerColorA[layer],
                                                holder.layerColorB[layer],
                                                holder.layerColorB[layer],
                                            ),
                                            null,
                                            Shader.TileMode.CLAMP,
                                        )
                                    canvas.drawPath(path, holder.paint)
                                }
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            renderLoop.stopAndAwait()
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
            }
        },
    )
}

/**
 * 计算颜色亮度（感知亮度）
 */
private fun calculateLuminance(color: Color): Float = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

/**
 * 色相偏移辅助函数
 * 简化版本：通过RGB分量旋转实现
 */
private fun shiftHue(
    color: Color,
    degrees: Float,
): Color {
    val amount = degrees / 360f

    // 简单的颜色偏移算法
    val r = color.red
    val g = color.green
    val b = color.blue

    return when {
        amount > 0 ->
            Color(
                red = (r + amount * (1 - r)).coerceIn(0f, 1f),
                green = (g - amount * g * 0.5f).coerceIn(0f, 1f),
                blue = (b + amount * (1 - b) * 0.5f).coerceIn(0f, 1f),
                alpha = color.alpha,
            )

        else ->
            Color(
                red = (r + amount * r * 0.5f).coerceIn(0f, 1f),
                green = (g - amount * (1 - g)).coerceIn(0f, 1f),
                blue = (b + amount * (1 - b)).coerceIn(0f, 1f),
                alpha = color.alpha,
            )
    }
}
