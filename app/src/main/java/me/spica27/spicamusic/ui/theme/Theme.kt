package me.spica27.spicamusic.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

object Shapes {
    val ExtraLarge2CornerBasedShape = RoundedCornerShape(32.dp)
    val ExtraLarge1CornerBasedShape = RoundedCornerShape(24.dp)
    val ExtraLargeCornerBasedShape = RoundedCornerShape(20.dp)
    val LargeCornerBasedShape = RoundedCornerShape(16.dp)
    val MediumCornerBasedShape = RoundedCornerShape(12.dp)

    val SmallCornerBasedShape = RoundedCornerShape(8.dp)
    val ExtraSmallCornerBasedShape = RoundedCornerShape(4.dp)
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        content = content,
        colorScheme =
            if (!darkTheme) {
                dynamicLightColorScheme(LocalContext.current)
            } else {
                dynamicDarkColorScheme(LocalContext.current)
            },
    )
}
