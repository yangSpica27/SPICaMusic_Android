package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * 风格的动态渐变背景
 * 适用于 BottomPlayerBar
 * @param modifier 修饰符
 * @param coverColor 封面主色调，用于生成渐变色系
 * @param enabled 是否启用动画效果
 * @param isDarkMode 暗色模式，null 时自动判断
 */
@Composable
fun CompactMusicBackground(
    modifier: Modifier = Modifier,
    coverColor: Color = MiuixTheme.colorScheme.primary,
    enabled: Boolean = true,
    isDarkMode: Boolean? = null,
) {
    // 自动判断亮暗模式
    val actualDarkMode =
        isDarkMode
            ?: remember(coverColor) {
                calculateLuminance(coverColor) < 0.5f
            }

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
            scope.launch(Dispatchers.Default) {
                playerViewModel.unsubscribeFFTDrawData()
            }
        }
    }

    // 收集插值后的 FFT 数据
    val fftDrawData by playerViewModel.fftDrawData.collectAsStateWithLifecycle()

    // 缓慢呼吸动画 - 模拟 Apple Music 的光晕效果
    val infiniteTransition = rememberInfiniteTransition(label = "glow_animation")

    // 主光晕呼吸（8秒周期）
    val breathingScale by
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.2f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(8000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "breathing",
        )

    // 光晕位置缓慢移动（12秒周期）
    val glowOffset1 by
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(12000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "glow1",
        )

    val glowOffset2 by
        infiniteTransition.animateFloat(
            initialValue = 180f,
            targetValue = 540f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(15000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "glow2",
        )

    // 生成渐变色系
    val gradientColors =
        remember(coverColor, actualDarkMode) {
            generateAppleMusicColors(coverColor, actualDarkMode)
        }

    Canvas(modifier = modifier.fillMaxWidth().clip(RectangleShape)) {
        if (!enabled) return@Canvas

        val width = size.width
        val height = size.height
        val centerX = width / 2
        val centerY = height / 2

        // 计算 FFT 能量影响亮度
        val avgEnergy = fftDrawData.average().toFloat().coerceIn(0f, 1f)
        val energyBoost = 1f + avgEnergy * 0.3f // 1.0 - 1.3

        // 背景基础渐变（从封面色的暗色到透明）
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            gradientColors[0].copy(alpha = 0.15f),
                            gradientColors[0].copy(alpha = 0.05f),
                            Color.Transparent,
                        ),
                ),
        )

        // 第一个光晕（主色调，左侧）
        val glow1X = centerX + cos(Math.toRadians(glowOffset1.toDouble())).toFloat() * width * 0.2f
        val glow1Y = centerY + sin(Math.toRadians(glowOffset1.toDouble())).toFloat() * height * 0.3f
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            gradientColors[1].copy(alpha = 0.4f * energyBoost),
                            gradientColors[1].copy(alpha = 0.2f * energyBoost),
                            Color.Transparent,
                        ),
                    center = Offset(glow1X, glow1Y),
                    radius = width * 0.5f * breathingScale,
                    tileMode = TileMode.Clamp,
                ),
            radius = width * 0.5f * breathingScale,
            center = Offset(glow1X, glow1Y),
        )

        // 第二个光晕（辅助色，右侧）
        val glow2X = centerX + cos(Math.toRadians(glowOffset2.toDouble())).toFloat() * width * 0.25f
        val glow2Y = centerY + sin(Math.toRadians(glowOffset2.toDouble())).toFloat() * height * 0.25f
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            gradientColors[2].copy(alpha = 0.35f * energyBoost),
                            gradientColors[2].copy(alpha = 0.15f * energyBoost),
                            Color.Transparent,
                        ),
                    center = Offset(glow2X, glow2Y),
                    radius = width * 0.45f * (2f - breathingScale),
                    tileMode = TileMode.Clamp,
                ),
            radius = width * 0.45f * (2f - breathingScale),
            center = Offset(glow2X, glow2Y),
        )

        // 第三个光晕（中央，较小）
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors =
                        listOf(
                            gradientColors[3].copy(alpha = 0.3f * energyBoost),
                            gradientColors[3].copy(alpha = 0.1f * energyBoost),
                            Color.Transparent,
                        ),
                    center = Offset(centerX, centerY),
                    radius = width * 0.3f,
                    tileMode = TileMode.Clamp,
                ),
            radius = width * 0.3f,
            center = Offset(centerX, centerY),
        )

        // 顶部柔光叠加层，模拟 Apple Music 的磨砂质感
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            Color.White.copy(alpha = if (actualDarkMode) 0.03f else 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = if (actualDarkMode) 0.05f else 0.02f),
                        ),
                ),
        )
    }
}

/**
 * 生成 Apple Music 风格的渐变色系
 * 从基础色提取多个互补色和近似色
 */
private fun generateAppleMusicColors(
    baseColor: Color,
    isDark: Boolean,
): List<Color> =
    listOf(
        // 背景基础色（较深）
        adjustBrightness(baseColor, if (isDark) 0.4f else 0.5f),
        // 主光晕色（原色调）
        baseColor,
        // 辅助光晕色（色相偏移）
        shiftHue(baseColor, 60f),
        // 中央光晕色（色相反向偏移）
        shiftHue(baseColor, -30f),
    )

/**
 * 调整颜色亮度
 */
private fun adjustBrightness(
    color: Color,
    factor: Float,
): Color =
    Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha,
    )

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
