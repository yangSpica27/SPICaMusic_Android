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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 流动融合动态背景组件
 * 响应音乐FFT数据和封面颜色
 * @param isDarkMode 暗色模式（true）或亮色模式（false），null时自动判断
 */
@Composable
fun FluidMusicBackground(
    modifier: Modifier = Modifier,
    fftBands: FloatArray = FloatArray(31),
    coverColor: Color = Color(0xFF2196F3),
    enabled: Boolean = true,
    isDarkMode: Boolean? = null,
) {
    // 自动判断亮暗模式（基于颜色亮度）
    val actualDarkMode =
        isDarkMode ?: remember(coverColor) {
            calculateLuminance(coverColor) < 0.5f
        }

    // 颜色动画过渡（避免切换时跳变）
    val animatedCoverColor by animateColorAsState(
        targetValue = coverColor,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "cover_color",
    )

    // 动画时间
    val infiniteTransition = rememberInfiniteTransition(label = "fluid_bg")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "progress",
    )

    // 从FFT数据计算平均响度（低频、中频、高频）- 增强响应系数
    val lowFreqAvg =
        remember(fftBands) {
            if (fftBands.isEmpty()) {
                0f
            } else {
                (fftBands.take(10).average().toFloat() * 1.8f).coerceAtMost(1f)
            }
        }
    val midFreqAvg =
        remember(fftBands) {
            if (fftBands.size < 20) {
                0f
            } else {
                (fftBands.slice(10..19).average().toFloat() * 1.5f).coerceAtMost(1f)
            }
        }
    val highFreqAvg =
        remember(fftBands) {
            if (fftBands.size < 31) {
                0f
            } else {
                (fftBands.slice(20..30).average().toFloat() * 1.3f).coerceAtMost(1f)
            }
        }

    // 从封面颜色派生柔和的辅助颜色（根据亮暗模式调整）
    val colors =
        remember(animatedCoverColor, actualDarkMode) {
            val baseAlpha = if (actualDarkMode) 0.4f else 0.6f
            listOf(
                animatedCoverColor.copy(alpha = baseAlpha),
                shiftHue(animatedCoverColor, 30f).copy(alpha = baseAlpha * 0.8f),
                shiftHue(animatedCoverColor, -30f).copy(alpha = baseAlpha * 0.9f),
                shiftHue(animatedCoverColor, 60f).copy(alpha = baseAlpha * 0.7f),
            )
        }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (!enabled) {
            // 禁用时显示静态渐变
            drawRect(
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF1A1A2E),
                                Color(0xFF16213E),
                            ),
                    ),
            )
            return@Canvas
        }

        // 绘制背景渐变（根据亮暗模式）
        val bgColors =
            if (actualDarkMode) {
                listOf(
                    Color(0xFF0F0F1E),
                    Color(0xFF000000),
                )
            } else {
                listOf(
                    Color(0xFFE8E8F0),
                    Color(0xFFD0D0E0),
                )
            }
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = bgColors,
                    center = center,
                ),
        )

        // 绘制多个流动的blob
        // 增强FFT响应的缩放系数
        drawFluidBlob(
            color = colors[0],
            progress = animationProgress,
            scale = 1f + lowFreqAvg * 1.2f,
            speed = 0.3f,
            offset = Offset(0f, 0f),
        )

        drawFluidBlob(
            color = colors[1],
            progress = animationProgress,
            scale = 1f + midFreqAvg * 1.0f,
            speed = 0.5f,
            offset = Offset(size.width * 0.3f, size.height * 0.2f),
        )

        drawFluidBlob(
            color = colors[2],
            progress = animationProgress,
            scale = 1f + highFreqAvg * 0.8f,
            speed = 0.7f,
            offset = Offset(size.width * 0.6f, size.height * 0.5f),
        )

        drawFluidBlob(
            color = colors[3],
            progress = animationProgress,
            scale = 1f + (lowFreqAvg + midFreqAvg) * 0.7f,
            speed = 0.4f,
            offset = Offset(size.width * 0.2f, size.height * 0.7f),
        )
    }
}

/**
 * 绘制单个流动blob
 */
private fun DrawScope.drawFluidBlob(
    color: Color,
    progress: Float,
    scale: Float,
    speed: Float,
    offset: Offset,
) {
    val angle = progress * 2 * PI.toFloat() * speed
    val radius = size.minDimension * 0.4f * scale

    // 计算blob中心位置（圆形路径运动）
    val orbitRadius = size.minDimension * 0.15f
    val centerX = center.x + offset.x + cos(angle) * orbitRadius
    val centerY = center.y + offset.y + sin(angle) * orbitRadius

    // 绘制渐变圆形blob
    drawCircle(
        brush =
            Brush.radialGradient(
                colors =
                    listOf(
                        color,
                        color.copy(alpha = color.alpha),
                        Color.Transparent,
                    ),
                center = Offset(centerX, centerY),
                radius = radius,
            ),
        radius = radius,
        center = Offset(centerX, centerY),
        blendMode = BlendMode.Screen, // 使用Screen混合模式产生柔和融合效果
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
