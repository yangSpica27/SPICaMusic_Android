package me.spica27.spicamusic.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppScaffold() {
    val preferencesManager = koinInject<PreferencesManager>()

    val isDarkMode =
        preferencesManager
            .getBoolean(PreferencesManager.Keys.DARK_MODE)
            .collectAsState(false)

    val playerViewModel: PlayerViewModel = koinActivityViewModel()
    val color = playerViewModel.playerThemeColor.collectAsState().value
    val theColor = animateColorAsState(color, tween(550)).value
    SPICaMusicTheme(
        darkTheme = isDarkMode.value,
        themeColor = theColor,
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
