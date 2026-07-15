package me.spica27.spicamusic.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.spica27.spicamusic.common.entity.ThemeColorStyle
import me.spica27.spicamusic.ui.widget.rememberClickHighlightIndication

object Shapes {
    val ExtraLarge2CornerBasedShape = RoundedCornerShape(32.dp)
    val ExtraLarge1CornerBasedShape = RoundedCornerShape(24.dp)
    val ExtraLargeCornerBasedShape = RoundedCornerShape(20.dp)
    val LargeCornerBasedShape = RoundedCornerShape(16.dp)
    val MediumCornerBasedShape = RoundedCornerShape(12.dp)

    val SmallCornerBasedShape = RoundedCornerShape(8.dp)
    val ExtraSmallCornerBasedShape = RoundedCornerShape(4.dp)
}

object Spacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Huge = 32.dp
}

object LayoutTokens {
    val MusicHeaderHorizontalPadding = 20.dp
    val MusicHeaderTopPadding = 20.dp
    val MusicHeaderBottomPadding = 4.dp
    val MusicTabContainerPadding = 4.dp
    val MusicTabHeight = 64.dp
    val MusicActionRowMinHeight = 72.dp
    val PageHeaderFollowDistance = 240.dp
    val PageHeaderCollapsedTitleScale = 0.82f
    val PageHeaderCollapsedTabHeight = 52.dp
    val PlayerCollapsedHorizontalInset = 16.dp
    val PlayerCollapsedTopInset = 12.dp
    val PlayerCollapsedCornerRadius = 28.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvideAppInteractionIndication(content: @Composable () -> Unit) {
    val clickHighlightIndication =
        rememberClickHighlightIndication(
            color = MaterialTheme.colorScheme.onSurface,
        )
    CompositionLocalProvider(
        LocalIndication provides clickHighlightIndication,
        LocalRippleConfiguration provides null,
        content = content,
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: Color,
    themeColorStyle: ThemeColorStyle = ThemeColorStyle.Textured,
    content: @Composable () -> Unit,
) {
    when (themeColorStyle) {
        ThemeColorStyle.Textured ->
            DynamicMaterialTheme(
                seedColor = themeColor,
                isDark = darkTheme,
                animate = true,
                specVersion = ColorSpec.SpecVersion.SPEC_2021,
                style = PaletteStyle.TonalSpot,
            ) {
                ProvideAppInteractionIndication(content = content)
            }

        ThemeColorStyle.Flat -> {
            // 与质感化的 animate 行为对齐:对种子色做动画,派生色板随之平滑过渡
            val animatedSeedColor by animateColorAsState(
                targetValue = themeColor,
                animationSpec = tween(durationMillis = 500),
                label = "flat_theme_seed_color",
            )
            val colorScheme =
                remember(animatedSeedColor, darkTheme) {
                    antFlatColorScheme(
                        seedColor = animatedSeedColor,
                        darkTheme = darkTheme,
                    )
                }
            MaterialTheme(colorScheme = colorScheme) {
                ProvideAppInteractionIndication(content = content)
            }
        }
    }
}
