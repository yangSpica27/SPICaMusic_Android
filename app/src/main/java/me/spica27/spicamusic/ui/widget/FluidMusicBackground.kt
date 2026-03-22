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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel
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

/** 线程安全数据持有者，供 TopGlowBackground 绘制线程读取 */
private class TopGlowHolder {
    @Volatile var fftData: FloatArray = FloatArray(0)

    @Volatile var colorA: Int = android.graphics.Color.BLUE

    @Volatile var colorB: Int = android.graphics.Color.TRANSPARENT

    // 预分配，避免绘制线程频繁 GC
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rectF = RectF()
}

@Composable
private fun TopGlowBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    val scope = rememberCoroutineScope()
    val holder = remember { TopGlowHolder() }

    // 仅在颜色变化时重算，写入 volatile 字段供绘制线程异步读取
    SideEffect {
        holder.fftData = fftDrawData
        val luminance = calculateLuminance(coverColor)
        val hueShift = if (luminance < 0.5f) 24f else -24f
        holder.colorA = shiftHue(coverColor, hueShift).copy(alpha = 0.85f).toArgb()
        holder.colorB = shiftHue(coverColor, hueShift * 1.6f).copy(alpha = 0.2f).toArgb()
    }

    AndroidView(
        modifier = modifier.hazeEffect { blurRadius = 72.dp },
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.isOpaque = false
                // API 31+ 使用 RenderEffect 模糊 TextureView 自身绘制内容
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                    val blurPx = with(density) { 72.dp.toPx() }
//                    tv.setRenderEffect(
//                        android.graphics.RenderEffect.createBlurEffect(
//                            blurPx,
//                            blurPx,
//                            Shader.TileMode.CLAMP,
//                        ),
//                    )
//                }
                tv.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        private var drawJob: Job? = null

                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            drawJob =
                                scope.launch(Dispatchers.Default) {
                                    while (isActive) {
                                        val canvas = tv.lockCanvas(null)
                                        if (canvas != null) {
                                            try {
                                                canvas.drawColor(
                                                    android.graphics.Color.TRANSPARENT,
                                                    PorterDuff.Mode.CLEAR,
                                                )
                                                val data = holder.fftData
                                                if (data.isNotEmpty()) {
                                                    val w = canvas.width.toFloat()
                                                    val h = canvas.height.toFloat()
                                                    val bandWidth = w / data.size
                                                    // 每帧创建一次渐变，横跨整个 canvas（与原 Brush.linearGradient 行为一致）
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
                                                        val barH = h * 0.8f * energy + h * 0.08f
                                                        val left = index * bandWidth
                                                        holder.rectF.set(
                                                            left,
                                                            0f,
                                                            left + max(1f, bandWidth * 0.9f),
                                                            barH,
                                                        )
                                                        canvas.drawRect(holder.rectF, holder.paint)
                                                    }
                                                }
                                            } finally {
                                                tv.unlockCanvasAndPost(canvas)
                                            }
                                        }
                                        delay(8L) // ~120 fps 上限，让调度器决定实际帧率
                                    }
                                }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            drawJob?.cancel()
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

    // 每层的颜色 A/B（ARGB Int），SideEffect 中更新
    val layerColorA = IntArray(3)
    val layerColorB = IntArray(3)

    // 预分配 Path 对象，每帧通过 reset() 复用
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
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val holder = remember { LiquidAuroraHolder() }

    SideEffect {
        holder.fftData = fftDrawData
        val alpha = if (isDarkMode == true) 0.9f else 0.75f
        for (layer in 0 until 3) {
            val colorA = shiftHue(coverColor, layer * 18f + 120f)
            val colorB = shiftHue(coverColor, layer * -14f - 116f)
            holder.layerColorA[layer] = colorA.copy(alpha = (alpha - layer * 0.2f)).toArgb()
            holder.layerColorB[layer] =
                colorB.copy(alpha = (alpha - layer * 0.3f).coerceAtLeast(0.1f)).toArgb()
        }
    }

    AndroidView(
        modifier = modifier.hazeEffect { blurRadius = 40.dp },
        factory = { ctx ->
            TextureView(ctx).also { tv ->
                tv.isOpaque = false
                tv.surfaceTextureListener =
                    object : TextureView.SurfaceTextureListener {
                        private var drawJob: Job? = null

                        override fun onSurfaceTextureAvailable(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {
                            drawJob =
                                scope.launch(Dispatchers.Default) {
                                    // 以启动时间为基准，20 秒一个相位周期（与原 tween(20000) 一致）
                                    val startTime = System.currentTimeMillis()
                                    val layers = 3

                                    while (isActive) {
                                        val canvas = tv.lockCanvas(null)
                                        if (canvas != null) {
                                            try {
                                                canvas.drawColor(
                                                    android.graphics.Color.TRANSPARENT,
                                                    PorterDuff.Mode.CLEAR,
                                                )
                                                val data = holder.fftData
                                                val w = canvas.width.toFloat()
                                                val h = canvas.height.toFloat()
                                                val elapsed =
                                                    (System.currentTimeMillis() - startTime) % 20_000L
                                                val phase = elapsed / 20_000f * 360f

                                                val chunkSize =
                                                    if (data.isNotEmpty()) {
                                                        (data.size / layers).coerceAtLeast(1)
                                                    } else {
                                                        1
                                                    }

                                                repeat(layers) { layer ->
                                                    val startIndex = layer * chunkSize
                                                    val endIndex =
                                                        min(data.size, startIndex + chunkSize)
                                                    if (data.isEmpty() || startIndex >= endIndex) return@repeat

                                                    val path = holder.paths[layer]
                                                    path.reset()
                                                    path.moveTo(0f, 0f)

                                                    val steps = endIndex - startIndex
                                                    val amplitude = h * (0.28f - layer * 0.05f)
                                                    val phaseShift =
                                                        (phase + layer * 45f) * (PI / 180.0)

                                                    for (i in 0 until steps) {
                                                        val progress =
                                                            if (steps == 1) 0f else i / (steps - 1f)
                                                        val energy =
                                                            data[startIndex + i].coerceIn(0f, 1f)
                                                        val wave =
                                                            sin(progress * 6f + phaseShift).toFloat()
                                                        val y =
                                                            h * 0.35f - amplitude * energy -
                                                                amplitude * 0.2f * wave
                                                        path.lineTo(progress * w, y)
                                                    }
                                                    path.lineTo(w, 0f)
                                                    path.close()

                                                    // 垂直渐变（与原 Brush.verticalGradient 一致）
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
                                            } finally {
                                                tv.unlockCanvasAndPost(canvas)
                                            }
                                        }
                                        delay(8L)
                                    }
                                }
                        }

                        override fun onSurfaceTextureSizeChanged(
                            surface: SurfaceTexture,
                            width: Int,
                            height: Int,
                        ) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            drawJob?.cancel()
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
