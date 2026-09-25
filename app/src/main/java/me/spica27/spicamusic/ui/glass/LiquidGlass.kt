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

// 对话框与页面共享模糊状态；未提供时使用不透明背景。
val LocalDialogHazeState = staticCompositionLocalOf<HazeState?> { null }

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
    Dialog,
    PopupMenu,
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
                LiquidGlassVariant.TopBar,
                LiquidGlassVariant.Dialog,
                LiquidGlassVariant.PopupMenu,
                -> HazePerformanceMode.Balanced
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
            LiquidGlassVariant.Dialog,
            LiquidGlassVariant.PopupMenu,
            -> MaterialTheme.colorScheme.surface
        }
    val blurRadius =
        when (variant) {
            LiquidGlassVariant.Navigation -> 20.dp
            LiquidGlassVariant.PlayerBar -> 28.dp
            LiquidGlassVariant.TopBar -> 18.dp
            LiquidGlassVariant.PlayButton -> 24.dp
            LiquidGlassVariant.Dialog -> 32.dp
            LiquidGlassVariant.PopupMenu -> 26.dp
        }
    val tintAlpha =
        when (variant) {
            LiquidGlassVariant.Navigation -> 0.38f
            LiquidGlassVariant.PlayerBar -> 0.46f
            LiquidGlassVariant.TopBar -> 0.32f
            LiquidGlassVariant.PlayButton -> 0.22f
            LiquidGlassVariant.Dialog -> 0.55f
            LiquidGlassVariant.PopupMenu -> 0.50f
        }
    val fallbackAlpha =
        when (variant) {
            LiquidGlassVariant.PlayButton -> 0.96f
            LiquidGlassVariant.Dialog,
            LiquidGlassVariant.PopupMenu,
            -> 1.0f
            else -> 0.78f
        }
    val saturation =
        when (variant) {
            LiquidGlassVariant.Dialog,
            LiquidGlassVariant.PopupMenu,
            -> 1.15f
            else -> 1.08f
        }

    return remember(variant, panelColor, fallbackColor, saturation) {
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
