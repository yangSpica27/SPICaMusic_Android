package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 紧凑型流动背景组件 - 适用于 BottomPlayerBar
 * 特点：
 * - 更小的波形和更快的动画速度
 * - 水平流动效果，适合横向布局
 * - 更柔和的颜色和透明度
 * - 响应音频 FFT 数据但不那么激烈
 *
 * @param fftBands FFT 频谱数据（31个频段）
 * @param coverColor 封面主色调
 * @param enabled 是否启用动画效果
 * @param isDarkMode 暗色模式，null 时自动判断
 */
@Composable
fun CompactMusicBackground(
    modifier: Modifier = Modifier,
    fftBands: FloatArray = FloatArray(31),
    coverColor: Color = Color(0xFF6200EE),
    enabled: Boolean = true,
    isDarkMode: Boolean? = null,
) {
    // 自动判断亮暗模式
    val actualDarkMode =
        isDarkMode ?: remember(coverColor) {
            calculateLuminance(coverColor) < 0.5f
        }

    // 颜色动画过渡
    val animatedCoverColor by animateColorAsState(
        targetValue = coverColor,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "compact_cover_color",
    )

    // 快速动画时间（适合紧凑组件）
    val infiniteTransition = rememberInfiniteTransition(label = "compact_bg")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(12000, easing = LinearEasing), // 更快的循环
                repeatMode = RepeatMode.Restart,
            ),
        label = "compact_progress",
    )

    // FFT 数据处理（提升响应强度使动画更明显）
    val lowFreqAvg =
        remember(fftBands) {
            if (fftBands.isEmpty()) {
                0f
            } else {
                (fftBands.take(10).average().toFloat() * 1.5f).coerceAtMost(1f)
            }
        }
    val midFreqAvg =
        remember(fftBands) {
            if (fftBands.size < 20) {
                0f
            } else {
                (fftBands.slice(10..19).average().toFloat() * 1.2f).coerceAtMost(1f)
            }
        }
    val highFreqAvg =
        remember(fftBands) {
            if (fftBands.size < 31) {
                0f
            } else {
                (fftBands.slice(20..30).average().toFloat() * 1.0f).coerceAtMost(1f)
            }
        }

    // 派生鲜明的颜色（提高饱和度和对比度）
    val colors =
        remember(animatedCoverColor, actualDarkMode) {
            val baseAlpha = if (actualDarkMode) 0.5f else 0.65f
            listOf(
                animatedCoverColor.copy(alpha = baseAlpha),
                shiftHue(animatedCoverColor, 60f).copy(alpha = baseAlpha * 0.9f),
                shiftHue(animatedCoverColor, 120f).copy(alpha = baseAlpha * 0.85f),
                shiftHue(animatedCoverColor, -45f).copy(alpha = baseAlpha * 0.8f),
            )
        }

    Canvas(modifier = modifier) {
        fftBands.forEachIndexed { index, value ->
            val progressOffset = (animationProgress + index * 0.03f) % 1f
            val x = size.width * progressOffset
            val lowHeight = size.height * lowFreqAvg * value
            val midHeight = size.height * midFreqAvg * value
            val highHeight = size.height * highFreqAvg * value

            // 绘制低频部分
            drawLine(
                color = colors[0],
                start =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 - lowHeight / 2),
                end =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 + lowHeight / 2),
                strokeWidth = 4f,
            )

            // 绘制中频部分
            drawLine(
                color = colors[1],
                start =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 - midHeight / 2),
                end =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 + midHeight / 2),
                strokeWidth = 3f,
            )

            // 绘制高频部分
            drawLine(
                color = colors[2],
                start =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 - highHeight / 2),
                end =
                    androidx.compose.ui.geometry
                        .Offset(x, size.height / 2 + highHeight / 2),
                strokeWidth = 2f,
            )
        }
    }
}

/**
 * 计算颜色亮度
 */
private fun calculateLuminance(color: Color): Float = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue

/**
 * 色相偏移
 */
private fun shiftHue(
    color: Color,
    degrees: Float,
): Color {
    val amount = degrees / 360f
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
