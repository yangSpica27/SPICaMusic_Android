package me.spica27.spicamusic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.defaultTextStyles
import top.yukonga.miuix.kmp.theme.lightColorScheme

// 亮色主题
private val LightColorScheme = lightColorScheme()

// 暗色主题
private val DarkColorScheme = darkColorScheme()

object Shapes {
    val ExtraLarge2CornerBasedShape = RoundedCornerShape(32.dp)
    val ExtraLarge1CornerBasedShape = RoundedCornerShape(24.dp)
    val ExtraLargeCornerBasedShape = RoundedCornerShape(20.dp)
    val LargeCornerBasedShape = RoundedCornerShape(16.dp)
    val MediumCornerBasedShape = RoundedCornerShape(12.dp)

    val SmallCornerBasedShape = RoundedCornerShape(8.dp)
    val ExtraSmallCornerBasedShape = RoundedCornerShape(4.dp)
}

@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) {
            DarkColorScheme
        } else {
            LightColorScheme
        }
    MiuixTheme(
        colors = colorScheme,
        content = content,
        textStyles =
            defaultTextStyles(
                main =
                    TextStyle(
                        fontSize = 17.sp,
                        letterSpacing = 0.1.sp,
                    ),
                paragraph =
                    TextStyle(
                        fontSize = 17.sp,
                        lineHeight = 1.2f.em,
                    ),
                body1 =
                    TextStyle(
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp,
                    ),
                body2 =
                    TextStyle(
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp,
                    ),
                button =
                    TextStyle(
                        fontSize = 17.sp,
                        letterSpacing = 0.4.sp,
                    ),
                footnote1 =
                    TextStyle(
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                    ),
                footnote2 =
                    TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    ),
                headline1 =
                    TextStyle(
                        fontSize = 17.sp,
                    ),
                headline2 =
                    TextStyle(
                        fontSize = 16.sp,
                    ),
                subtitle =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                    ),
                title1 =
                    TextStyle(
                        fontSize = 32.sp,
                    ),
                title2 =
                    TextStyle(
                        fontSize = 24.sp,
                        letterSpacing = 0.1.sp,
                    ),
                title3 =
                    TextStyle(
                        fontSize = 20.sp,
                        letterSpacing = 0.15.sp,
                    ),
                title4 =
                    TextStyle(
                        fontSize = 18.sp,
                        letterSpacing = 0.1.sp,
                    ),
            ),
    )
}
