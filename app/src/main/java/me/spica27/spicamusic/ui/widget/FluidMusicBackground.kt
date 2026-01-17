package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel

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
    val scope = rememberCoroutineScope()

    // 订阅和取消订阅 FFT 绘制数据
    LaunchedEffect(enabled) {
        if (enabled) {
            withContext(Dispatchers.Default) {
                playerViewModel.subscribeFFTDrawData()
            }
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

    // 收集插值后的 FFT 数据
    val fftDrawData by playerViewModel.fftDrawData.collectAsStateWithLifecycle()

    Canvas(
        modifier =
            modifier.hazeEffect {
                blurRadius = 50.dp
            },
    ) {
        if (!enabled) return@Canvas

        fftDrawData.forEachIndexed { index, f ->

            val bandHeight = size.height / 3 * 2 * f
            val bandWidth = size.width / fftDrawData.size

            // 根据封面颜色调整色相偏移和亮度
            val luminance = calculateLuminance(coverColor)
            val hueShift = if (luminance < 0.5f) 30f else -30f

            // 绘制矩形条
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                shiftHue(coverColor, hueShift).copy(alpha = 0.7f),
                                shiftHue(coverColor, hueShift * 2).copy(alpha = 0.3f),
                            ),
                    ),
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(x = index * bandWidth + bandWidth / 2, y = 0f),
                size =
                    androidx.compose.ui.geometry
                        .Size(width = bandWidth - 2f, height = bandHeight),
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
