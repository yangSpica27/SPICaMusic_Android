package me.spica27.spicamusic.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

/**
 * 扁平化主题色方案。
 *
 * 参考 Ant Design 色彩规范(https://ant.design/docs/spec/colors-cn):
 *
 */

private const val HUE_STEP = 2
private const val SATURATION_STEP = 0.16f
private const val SATURATION_STEP_2 = 0.05f
private const val BRIGHTNESS_STEP_1 = 0.05f
private const val BRIGHTNESS_STEP_2 = 0.15f
private const val LIGHT_COLOR_COUNT = 5
private const val DARK_COLOR_COUNT = 4

private val DARK_BACKGROUND = Color(0xFF141414)

// 官网预设品牌色(第 6 号色)的饱和度约在 0.88~0.91、明度约在 0.76~0.86,
// 种子色收敛到该区间以统一色彩深度;饱和度过低视为中性色,不做增饱和以免灰变红
private const val SEED_SATURATION_MIN = 0.88f
private const val SEED_SATURATION_MAX = 0.91f
private const val SEED_VALUE_MIN = 0.76f
private const val SEED_VALUE_MAX = 0.86f
private const val NEUTRAL_SATURATION_THRESHOLD = 0.02f

// 暗色色板混合表:十个档位分别取基础色板的某一色,按对应不透明度与背景色混合
private val DARK_COLOR_MAP =
    listOf(
        7 to 0.15f,
        6 to 0.25f,
        5 to 0.30f,
        5 to 0.45f,
        5 to 0.65f,
        5 to 0.85f,
        4 to 0.90f,
        3 to 0.95f,
        2 to 0.97f,
        1 to 0.98f,
    )

private fun getHue(
    baseHue: Float,
    i: Int,
    light: Boolean,
): Float {
    val rounded = baseHue.roundToInt()
    var hue =
        if (rounded in 60..240) {
            if (light) rounded - HUE_STEP * i else rounded + HUE_STEP * i
        } else {
            if (light) rounded + HUE_STEP * i else rounded - HUE_STEP * i
        }
    if (hue < 0) hue += 360
    if (hue >= 360) hue -= 360
    return hue.toFloat()
}

private fun getSaturation(
    baseHue: Float,
    baseSaturation: Float,
    i: Int,
    light: Boolean,
): Float {
    if (baseHue == 0f && baseSaturation == 0f) return baseSaturation
    var saturation =
        when {
            light -> baseSaturation - SATURATION_STEP * i
            i == DARK_COLOR_COUNT -> baseSaturation + SATURATION_STEP
            else -> baseSaturation + SATURATION_STEP_2 * i
        }
    saturation = saturation.coerceAtMost(1f)
    if (light && i == LIGHT_COLOR_COUNT && saturation > 0.1f) {
        saturation = 0.1f
    }
    return saturation.coerceAtLeast(0.06f)
}

private fun getValue(
    baseValue: Float,
    i: Int,
    light: Boolean,
): Float {
    val value =
        if (light) {
            baseValue + BRIGHTNESS_STEP_1 * i
        } else {
            baseValue - BRIGHTNESS_STEP_2 * i
        }
    return value.coerceIn(0f, 1f)
}

private fun mix(
    background: Color,
    overlay: Color,
    amount: Float,
): Color =
    Color(
        red = background.red + (overlay.red - background.red) * amount,
        green = background.green + (overlay.green - background.green) * amount,
        blue = background.blue + (overlay.blue - background.blue) * amount,
    )

/**
 * 由品牌色生成十色色板(下标 0..9 对应 Ant Design 的 1..10 号色)。
 */
private fun generateAntPalette(seed: Color): List<Color> {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    val (h, s, v) = hsv
    val palette = ArrayList<Color>(10)
    for (i in LIGHT_COLOR_COUNT downTo 1) {
        palette +=
            Color(
                AndroidColor.HSVToColor(
                    floatArrayOf(
                        getHue(h, i, light = true),
                        getSaturation(h, s, i, light = true),
                        getValue(v, i, light = true),
                    ),
                ),
            )
    }
    palette += seed
    for (i in 1..DARK_COLOR_COUNT) {
        palette +=
            Color(
                AndroidColor.HSVToColor(
                    floatArrayOf(
                        getHue(h, i, light = false),
                        getSaturation(h, s, i, light = false),
                        getValue(v, i, light = false),
                    ),
                ),
            )
    }
    return palette
}

/**
 * 由基础色板生成暗色色板。
 */
private fun generateAntDarkPalette(palette: List<Color>): List<Color> =
    DARK_COLOR_MAP.map { (index, opacity) ->
        mix(DARK_BACKGROUND, palette[index], opacity)
    }

/**
 * 将种子色的色彩深度归一化到官网示例品牌色的水平。
 */
private fun normalizeSeed(seed: Color): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(seed.toArgb(), hsv)
    if (hsv[1] <= NEUTRAL_SATURATION_THRESHOLD) return seed
    hsv[1] = hsv[1].coerceIn(SEED_SATURATION_MIN, SEED_SATURATION_MAX)
    hsv[2] = hsv[2].coerceIn(SEED_VALUE_MIN, SEED_VALUE_MAX)
    return Color(AndroidColor.HSVToColor(hsv))
}

fun antFlatColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
): ColorScheme {
    val palette = generateAntPalette(normalizeSeed(seedColor))
    return if (darkTheme) {
        antFlatDarkColorScheme(generateAntDarkPalette(palette))
    } else {
        antFlatLightColorScheme(palette)
    }
}

private fun antFlatLightColorScheme(palette: List<Color>): ColorScheme =
    lightColorScheme(
        primary = palette[5],
        onPrimary = Color.White,
        primaryContainer = palette[0],
        onPrimaryContainer = palette[7],
        inversePrimary = palette[3],
        secondary = palette[6],
        onSecondary = Color.White,
        secondaryContainer = palette[0],
        onSecondaryContainer = palette[8],
        tertiary = palette[4],
        onTertiary = Color.White,
        tertiaryContainer = palette[1],
        onTertiaryContainer = palette[7],
        background = Color.White,
        onBackground = Color(0xFF1F1F1F),
        surface = Color.White,
        onSurface = Color(0xFF1F1F1F),
        surfaceVariant = Color(0xFFF5F5F5),
        onSurfaceVariant = Color(0xFF595959),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFFAFAFA),
        surfaceContainer = Color(0xFFF5F5F5),
        surfaceContainerHigh = Color(0xFFF0F0F0),
        surfaceContainerHighest = Color(0xFFEBEBEB),
        surfaceDim = Color(0xFFF0F0F0),
        surfaceBright = Color.White,
        inverseSurface = Color(0xFF1F1F1F),
        inverseOnSurface = Color(0xFFFAFAFA),
        outline = Color(0xFFD9D9D9),
        outlineVariant = Color(0xFFF0F0F0),
        error = Color(0xFFFF4D4F),
        onError = Color.White,
        errorContainer = Color(0xFFFFF1F0),
        onErrorContainer = Color(0xFFA8071A),
        scrim = Color.Black,
        surfaceTint = Color.Transparent,
    )

private fun antFlatDarkColorScheme(darkPalette: List<Color>): ColorScheme =
    darkColorScheme(
        primary = darkPalette[5],
        onPrimary = Color.White,
        primaryContainer = darkPalette[1],
        onPrimaryContainer = darkPalette[8],
        inversePrimary = darkPalette[6],
        secondary = darkPalette[6],
        onSecondary = Color.White,
        secondaryContainer = darkPalette[1],
        onSecondaryContainer = darkPalette[8],
        tertiary = darkPalette[4],
        onTertiary = Color.White,
        tertiaryContainer = darkPalette[2],
        onTertiaryContainer = darkPalette[8],
        background = DARK_BACKGROUND,
        onBackground = Color(0xFFDBDBDB),
        surface = DARK_BACKGROUND,
        onSurface = Color(0xFFDBDBDB),
        surfaceVariant = Color(0xFF1F1F1F),
        onSurfaceVariant = Color(0xFFADADAD),
        surfaceContainerLowest = Color(0xFF0F0F0F),
        surfaceContainerLow = Color(0xFF1A1A1A),
        surfaceContainer = Color(0xFF1F1F1F),
        surfaceContainerHigh = Color(0xFF262626),
        surfaceContainerHighest = Color(0xFF2E2E2E),
        surfaceDim = DARK_BACKGROUND,
        surfaceBright = Color(0xFF2E2E2E),
        inverseSurface = Color(0xFFF0F0F0),
        inverseOnSurface = Color(0xFF1F1F1F),
        outline = Color(0xFF424242),
        outlineVariant = Color(0xFF303030),
        error = Color(0xFFDC4446),
        onError = Color.White,
        errorContainer = Color(0xFF2A1215),
        onErrorContainer = Color(0xFFE84749),
        scrim = Color.Black,
        surfaceTint = Color.Transparent,
    )
