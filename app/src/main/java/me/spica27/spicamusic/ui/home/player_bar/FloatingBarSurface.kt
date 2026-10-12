package me.spica27.spicamusic.ui.home.player_bar

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import me.spica27.spicamusic.ui.theme.Spacing

internal object FloatingBarDefaults {
    val HorizontalMargin = Spacing.Medium
    val BottomMargin = Spacing.Medium
    val Gap = Spacing.Small
    val CornerRadius = 36.dp
    val Shape = RoundedCornerShape(CornerRadius)
}

/** 底栏共享玻璃参数，形状及图层范围由布局决定，前景控件保持清晰。 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun Modifier.playerBarSurface(
    hazeState: HazeState?,
    containerColor: Color,
    glassEnabled: Boolean,
    shape: RoundedCornerShape,
    expandLayerBounds: Boolean,
): Modifier {
    val clipped = clip(shape)
    if (!glassEnabled || hazeState == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return clipped.background(containerColor)
    }

    val style =
        remember(containerColor, shape) {
            GlassStyle.clear.then {
                shape(shape)
                optics(
                    refractionStrength = 0.3f,
                    refractionHeightFraction = 0.35f,
                    refractionDisplacement = 4.dp,
                    refractionProfile = RefractionProfile.Edge(10.dp),
                    blurRadius = 18.dp,
                )
                backgroundColor(containerColor.copy(alpha = 0.12f))
                tint(containerColor.copy(alpha = if (containerColor.luminance() < 0.5f) 0.52f else 0.40f))
                specularIntensity(0.4f)
                edgeShadow(Color.Black.copy(alpha = 0.08f))
                lightPosition(Alignment.TopStart)
            }
        }
    return clipped.hazeGlass(
        input = HazeInput.Sources(hazeState),
        style = style,
        performanceMode = HazePerformanceMode.Balanced,
        expandLayerBounds = expandLayerBounds,
    )
}
