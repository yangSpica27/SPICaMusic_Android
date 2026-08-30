package me.spica27.spicamusic.ui.glass

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource

@Immutable
data class LiquidGlassConfig(
    val enabled: Boolean = false,
)

val LocalLiquidGlassConfig = staticCompositionLocalOf { LiquidGlassConfig() }

@Composable
fun Modifier.liquidGlassSource(hazeState: HazeState): Modifier =
    if (LocalLiquidGlassConfig.current.enabled) {
        hazeSource(hazeState)
    } else {
        this
    }

enum class LiquidGlassVariant {
    Navigation,
    PlayerBar,
    TopBar,
    PlayButton,
}

@Composable
fun Modifier.liquidGlass(
    hazeState: HazeState,
    variant: LiquidGlassVariant,
    shape: Shape,
    fallbackColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
): Modifier {
    val config = LocalLiquidGlassConfig.current
    if (!config.enabled) {
        return clip(shape).backgroundFallback(fallbackColor)
    }

    val style = liquidBlurStyle(variant, fallbackColor)
    return clip(shape).hazeBlur(
        input = HazeInput.Sources(hazeState),
        style = style,
        performanceMode =
            when (variant) {
                LiquidGlassVariant.TopBar -> HazePerformanceMode.Balanced
                LiquidGlassVariant.Navigation,
                LiquidGlassVariant.PlayerBar,
                LiquidGlassVariant.PlayButton,
                -> HazePerformanceMode.Performance
            },
        // The surfaces are clipped already; avoiding layer expansion keeps the blur bounds small.
        expandLayerBounds = false,
    )
}

@Composable
private fun liquidBlurStyle(
    variant: LiquidGlassVariant,
    fallbackColor: Color,
): HazeBlurStyle {
    val panelColor =
        when (variant) {
            LiquidGlassVariant.Navigation,
            LiquidGlassVariant.PlayerBar,
            -> MaterialTheme.colorScheme.surfaceContainerHigh

            LiquidGlassVariant.TopBar -> MaterialTheme.colorScheme.surfaceContainer
            LiquidGlassVariant.PlayButton -> MaterialTheme.colorScheme.primary
        }
    val blurRadius =
        when (variant) {
            LiquidGlassVariant.Navigation -> 20.dp
            LiquidGlassVariant.PlayerBar -> 28.dp
            LiquidGlassVariant.TopBar -> 18.dp
            LiquidGlassVariant.PlayButton -> 24.dp
        }
    val tintAlpha =
        when (variant) {
            LiquidGlassVariant.Navigation -> 0.38f
            LiquidGlassVariant.PlayerBar -> 0.46f
            LiquidGlassVariant.TopBar -> 0.32f
            LiquidGlassVariant.PlayButton -> 0.22f
        }
    val fallbackAlpha = if (variant == LiquidGlassVariant.PlayButton) 0.96f else 0.78f

    return remember(variant, panelColor, fallbackColor) {
        val saturation = 1.08f
        val inverseSaturation = 1f - saturation
        val luminanceRed = 0.213f * inverseSaturation
        val luminanceGreen = 0.715f * inverseSaturation
        val luminanceBlue = 0.072f * inverseSaturation
        val colorEnhancement =
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        luminanceRed + saturation,
                        luminanceGreen,
                        luminanceBlue,
                        0f,
                        0.015f,
                        luminanceRed,
                        luminanceGreen + saturation,
                        luminanceBlue,
                        0f,
                        0.015f,
                        luminanceRed,
                        luminanceGreen,
                        luminanceBlue + saturation,
                        0f,
                        0.015f,
                        0f,
                        0f,
                        0f,
                        1f,
                        0f,
                    ),
                ),
            )
        HazeBlurStyle {
            blurRadius(blurRadius)
            noiseFactor(0f)
            backgroundColor(panelColor.copy(alpha = 0.16f))
            colorEffects(
                listOf(
                    HazeColorEffect.colorFilter(colorEnhancement),
                    HazeColorEffect.tint(panelColor.copy(alpha = tintAlpha)),
                ),
            )
            fallbackColorEffect(HazeColorEffect.tint(fallbackColor.copy(alpha = fallbackAlpha)))
            alpha(1f)
        }
    }
}

private fun Modifier.backgroundFallback(color: Color): Modifier = background(color)
