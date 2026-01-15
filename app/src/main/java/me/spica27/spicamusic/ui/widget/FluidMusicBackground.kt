package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

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
    val o =
        remember { List(31) { 0f }.toMutableStateList() }

    val last = remember { List(31) { 0f }.toMutableStateList() }

    var lastTime by remember { mutableStateOf(0L) }

    var interval by remember { mutableStateOf(0L) }

    val drawData = remember { List(31) { 0f }.toMutableStateList() }

    val drawDataAnim = remember { List(31) { Animatable(0f) } }

    // 创建专用的计算线程池，并在组件销毁时自动关闭
    val computeContext =
        remember {
            Executors.newFixedThreadPool(1).asCoroutineDispatcher()
        }

    // 确保在组件销毁时关闭线程池，避免内存泄漏
    DisposableEffect(Unit) {
        onDispose {
            computeContext.close()
        }
    }

    val lock = remember { Any() }

    LaunchedEffect(fftBands) {
        launch(computeContext) {
            synchronized(lock) {
                fftBands.forEachIndexed { index, f ->
                    last[index] = o[index]
                    o[index] = f
                }
                val currentTime = System.currentTimeMillis()
                interval = currentTime - lastTime
                lastTime = currentTime
            }
        }
    }

    LaunchedEffect(Unit) {
        launch(computeContext) {
            while (isActive) {
                synchronized(lock) {
                    val currentTime = System.currentTimeMillis()
                    // 添加最小时间间隔保护，避免除零和进度突变
                    val safeInterval = interval.coerceAtLeast(16L)
                    val progress =
                        ((currentTime - lastTime) / safeInterval.toFloat()).coerceIn(0f, 1f)
                    drawData.forEachIndexed { index, f ->
                        // 从last线性插值到o，避免逆向插值导致的抖动
                        drawData[index] = last[index] + (o[index] - last[index]) * progress
                    }
                }
                delay(16)
            }
        }
    }

    Canvas(
        modifier =
        modifier,
    ) {
        drawData.forEachIndexed { index, f ->

            val bandHeight = size.height * f
            val bandWidth = size.width / fftBands.size

            // 根据封面颜色调整色相偏移和亮度
            val luminance = calculateLuminance(coverColor)
            val hueShift = if (luminance < 0.5f) 30f else -30f
            val adjustedColor = shiftHue(coverColor, hueShift)

            // 绘制矩形条
            drawRect(
                color = adjustedColor,
                topLeft =
                    androidx.compose.ui.geometry
                        .Offset(x = index * bandWidth, y = size.height - bandHeight),
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
