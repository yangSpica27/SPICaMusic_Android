package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
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

        DynamicSpectrumBackground.EffectShader ->
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                EffectShaderBackground(
                    modifier = modifier,
                    coverColor = coverColor,
                    fftDrawData = fftSnapshot,
                    isDarkMode = isDarkMode,
                )
            } else {
                Box(modifier = modifier) // 不支持的系统版本回退
            }

        DynamicSpectrumBackground.OFF ->
            Box(modifier = modifier)
    }
}

@Composable
private fun TopGlowBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
) {
    // 预计算渐变画笔：仅依赖 coverColor，避免每帧为每个频段重复创建 Brush + shiftHue
    val barBrush =
        remember(coverColor) {
            val luminance = calculateLuminance(coverColor)
            val hueShift = if (luminance < 0.5f) 24f else -24f
            Brush.linearGradient(
                colors =
                    listOf(
                        shiftHue(coverColor, hueShift).copy(alpha = 0.85f),
                        shiftHue(coverColor, hueShift * 1.6f).copy(alpha = 0.2f),
                    ),
            )
        }

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 72.dp
            },
    ) {
        val bandWidth = if (fftDrawData.isNotEmpty()) size.width / fftDrawData.size else size.width
        fftDrawData.forEachIndexed { index, magnitude ->
            val energy = magnitude.coerceIn(0f, 1f)
            val barHeight = size.height * 0.8f * energy + size.height * 0.08f
            drawRect(
                brush = barBrush,
                topLeft = Offset(x = index * bandWidth, y = 0f),
                size = Size(width = max(1f, bandWidth * 0.9f), height = barHeight),
            )
        }
    }
}

@Composable
private fun LiquidAuroraBackground(
    modifier: Modifier,
    fftDrawData: FloatArray,
    coverColor: Color,
    isDarkMode: Boolean?,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aurora-phase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "aurora-anim",
    )

    // 预分配 3 个 Path 对象，每帧通过 reset() 复用，避免每帧 new Path() × 3
    val paths = remember { Array(3) { Path() } }

    // 预计算每层颜色列表：仅在 coverColor/isDarkMode 变化时重新计算
    // 避免每帧 6 次 shiftHue + 6 次 Color.copy
    val layerColorLists =
        remember(coverColor, isDarkMode) {
            val alpha = if (isDarkMode == true) 0.9f else 0.75f
            Array(3) { layer ->
                val colorA = shiftHue(coverColor, layer * 18f + 120f)
                val colorB = shiftHue(coverColor, layer * -14f - 116f)
                val alpha1 = alpha - layer * 0.2f
                val alpha2 = (alpha - layer * 0.3f).coerceAtLeast(0.1f)
                listOf(
                    colorA.copy(alpha = alpha1),
                    colorB.copy(alpha = alpha2),
                    colorB.copy(alpha = alpha2),
                )
            }
        }

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 40.dp
            },
    ) {
        val layers = 3
        val chunkSize = (fftDrawData.size / layers).coerceAtLeast(1)
        repeat(layers) { layer ->
            val startIndex = layer * chunkSize
            val endIndex = min(fftDrawData.size, startIndex + chunkSize)
            if (startIndex >= endIndex) return@repeat

            val path = paths[layer]
            path.reset()
            path.moveTo(0f, 0f)

            val steps = endIndex - startIndex
            val amplitude = size.height * (0.28f - layer * 0.05f)
            val phaseShift = (phase + layer * 45f) * (PI / 180f)
            for (i in 0 until steps) {
                val progress = if (steps == 1) 0f else i / (steps - 1f)
                val energy = fftDrawData[startIndex + i].coerceIn(0f, 1f)
                val wave = sin(progress * 6f + phaseShift).toFloat()
                val y = size.height * 0.35f - amplitude * energy - amplitude * 0.2f * wave
                val x = progress * size.width
                path.lineTo(x, y)
            }
            path.lineTo(size.width, 0f)
            path.close()

            drawPath(
                path = path,
                brush =
                    Brush.verticalGradient(
                        startY = 0f,
                        endY = size.height * 0.5f,
                        colors = layerColorLists[layer],
                    ),
            )
        }
    }
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
