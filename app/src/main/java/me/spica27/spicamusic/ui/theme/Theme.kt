package me.spica27.spicamusic.ui.theme

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme


// 亮色主题
private val LightColorScheme = lightColorScheme()

// 暗色主题
private val DarkColorScheme = darkColorScheme()

@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = false,
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
    )
}
