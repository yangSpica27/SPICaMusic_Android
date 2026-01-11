package me.spica27.spicamusic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

// 主色调
private val Primary = Color(0xFF6750A4)
private val OnPrimary = Color(0xFFFFFFFF)
private val PrimaryContainer = Color(0xFFEADDFF)
private val OnPrimaryContainer = Color(0xFF21005D)

// 亮色主题
private val LightColorScheme =
    lightColorScheme()

// 暗色主题
private val DarkColorScheme =
    darkColorScheme(
        primary = Primary,
        onPrimary = OnPrimary,
        primaryContainer = PrimaryContainer,
        onPrimaryContainer = OnPrimaryContainer,
    )

@Composable
fun SPICaMusicTheme(
    darkTheme: Boolean = false, // TODO: 从设置中读取
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
