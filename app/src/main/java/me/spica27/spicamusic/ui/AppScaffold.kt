package me.spica27.spicamusic.ui

import android.app.Activity
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import me.spica27.spicamusic.common.entity.ThemeColorStyle
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.ui.glass.LiquidGlassConfig
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.navigation.HomeRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.appEntryProvider
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.theme.SPICaMusicTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun AppScaffold() {
    val preferencesManager = koinInject<PreferencesManager>()

    val isDarkMode by
        preferencesManager
            .getBoolean(PreferencesManager.Keys.DARK_MODE)
            .collectAsStateWithLifecycle(false)

    val liquidGlassEnabled by
        preferencesManager
            .getBoolean(PreferencesManager.Keys.LIQUID_GLASS_ENABLED, false)
            .collectAsStateWithLifecycle(false)

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

    val view = LocalView.current
    LaunchedEffect(isDarkMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
    }

    SPICaMusicTheme(
        darkTheme = isDarkMode,
        themeColor = color,
        themeColorStyle = ThemeColorStyle.fromString(themeColorStyleValue),
    ) {
        val backStack = remember { mutableStateListOf<Any>(HomeRoute) }
        val dialogStrategy = remember { DialogSceneStrategy<Any>() }
        val entryProvider = appEntryProvider(backStack)

        CompositionLocalProvider(
            LocalLiquidGlassConfig provides LiquidGlassConfig(enabled = liquidGlassEnabled),
            LocalPlayerViewModel provides playerViewModel,
            LocalBackStack provides backStack,
        ) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(dialogStrategy),
                entryProvider = entryProvider,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 }
                },
                popTransitionSpec = {
                    slideInHorizontally { -it / 4 } togetherWith slideOutHorizontally { it }
                },
            )
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
