package me.spica27.spicamusic.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.materialkolor.DynamicMaterialTheme
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec

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
    val PlayerCollapsedHorizontalInset = 16.dp
    val PlayerCollapsedTopInset = 12.dp
    val PlayerCollapsedCornerRadius = 28.dp
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeColor: Color,
    content: @Composable () -> Unit,
) {
    DynamicMaterialTheme(
        seedColor = themeColor,
        isDark = darkTheme,
        animate = true,
        content = content,
        specVersion = ColorSpec.SpecVersion.SPEC_2025,
        style = PaletteStyle.TonalSpot,
    )
}
