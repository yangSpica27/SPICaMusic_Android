package me.spica27.spicamusic.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.stack.NavigationStack
import me.spica27.spicamusic.common.entity.ThemeColorStyle
import me.spica27.spicamusic.common.entity.ThemeMode
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.ui.home.HomeScene
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.theme.CircularRevealThemeHost
import me.spica27.spicamusic.ui.theme.SPICaMusicTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 应用主框架
 * PlayerViewModel 在此处创建，作为 Activity 级别的单例共享
 */
@Composable
fun AppScaffold() {
    val preferencesManager = koinInject<PreferencesManager>()
    val initialThemeMode =
        remember(preferencesManager) {
            preferencesManager.getInitialThemeMode()
        }

    val savedThemeMode by
        preferencesManager
            .getString(PreferencesManager.Keys.THEME_MODE, "")
            .collectAsStateWithLifecycle(initialThemeMode)
    val legacyDarkMode by
        preferencesManager
            .getBoolean(PreferencesManager.Keys.DARK_MODE)
            .collectAsStateWithLifecycle(false)
    val systemDarkMode = isSystemInDarkTheme()
    val themeMode =
        if (savedThemeMode.isBlank()) {
            if (legacyDarkMode) ThemeMode.DARK else ThemeMode.SYSTEM
        } else {
            ThemeMode.fromString(savedThemeMode)
        }
    val isDarkMode = themeMode.resolve(systemDarkMode)

    val themeColorStyleValue by
        preferencesManager
            .getString(PreferencesManager.Keys.THEME_COLOR_STYLE, ThemeColorStyle.Textured.value)
            .collectAsStateWithLifecycle(ThemeColorStyle.Textured.value)

    val playerViewModel: PlayerViewModel = koinActivityViewModel()
    val color by playerViewModel.playerThemeColor.collectAsStateWithLifecycle()
    val keepScreenOn by
        preferencesManager
            .getBoolean(PreferencesManager.Keys.KEEP_SCREEN_ON)
            .collectAsStateWithLifecycle(false)
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()

    KeepScreenOnEffect(enabled = keepScreenOn && isPlaying)

    CircularRevealThemeHost(
        targetDarkTheme = isDarkMode,
        targetThemeColor = color,
    ) { revealedDarkTheme, revealedThemeColor ->
        val themedView = LocalView.current
        LaunchedEffect(revealedDarkTheme) {
            val window = (themedView.context as Activity).window
            WindowCompat.getInsetsController(window, themedView).isAppearanceLightStatusBars =
                !revealedDarkTheme
        }
        SPICaMusicTheme(
            darkTheme = revealedDarkTheme,
            themeColor = revealedThemeColor,
            themeColorStyle = ThemeColorStyle.fromString(themeColorStyleValue),
            animateColors = false,
        ) {
            CompositionLocalProvider(LocalPlayerViewModel provides playerViewModel) {
                NavigationStack(
                    initialScene = {
                        HomeScene()
                    },
                    content = {
                    },
                )
            }
        }
    }
}

@Composable
private fun KeepScreenOnEffect(enabled: Boolean) {
    val view = LocalView.current

    DisposableEffect(view, enabled) {
        val previous = view.keepScreenOn
        view.keepScreenOn = enabled

        onDispose {
            view.keepScreenOn = previous
        }
    }
}
