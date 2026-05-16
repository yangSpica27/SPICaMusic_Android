package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import me.spica27.navkit.stack.NavigationStack
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.ui.home.HomeScene
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
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

    val isDarkMode =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.DARK_MODE)
            .collectAsState(false)

    val playerViewModel: PlayerViewModel = koinActivityViewModel()
    val color = playerViewModel.playerThemeColor.collectAsState().value
    SPICaMusicTheme(
        darkTheme = isDarkMode.value,
        themeColor = color,
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
