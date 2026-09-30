package me.spica27.spicamusic.ui.widget

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/** 不透明文字的背景亮度约束，预留抖动与量化余量。 */
internal data class FluidLuminanceRange(
    val min: Float,
    val max: Float,
    val dark: Boolean,
)

internal fun fluidLuminanceRange(
    dark: Boolean,
    foreground: Color,
    secondaryForeground: Color,
): FluidLuminanceRange {
    val primary = foreground.luminance()
    val secondary = secondaryForeground.luminance()
    return if (dark) {
        val ceiling = ((minOf(primary, secondary) + 0.05f) / 4.5f - 0.05f - 0.006f).coerceIn(0f, 0.16f)
        FluidLuminanceRange(minOf(0.008f, ceiling * 0.25f), ceiling, dark = true)
    } else {
        val floor = ((maxOf(primary, secondary) + 0.05f) * 4.5f - 0.05f + 0.015f).coerceIn(0.52f, 1f)
        FluidLuminanceRange(floor, maxOf(0.96f, floor), dark = false)
    }
}

/** 按亮度分位数计算曝光基准，降低局部高光的影响。 */
internal fun fluidCoverLuminanceKey(pixels: IntArray): Float {
    if (pixels.isEmpty()) return 0.18f
    val luminances = FloatArray(pixels.size) { Color(pixels[it]).compositeOver(Color.Black).luminance() }
    luminances.sort()
    val shadow = luminances[(luminances.lastIndex * 0.1f).toInt()]
    val median = luminances[luminances.lastIndex / 2]
    val highlight = luminances[(luminances.lastIndex * 0.9f).toInt()]
    // 限制曝光基准，避免放大暗部噪声或过度压暗亮封面。
    return (shadow * 0.2f + median * 0.6f + highlight * 0.2f).coerceIn(0.004f, 0.4f)
}
