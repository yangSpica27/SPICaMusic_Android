package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import me.spica27.spicamusic.navigation.AppNavGraph
import me.spica27.spicamusic.navigation.LocalNavController
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.theme.SPICaMusicTheme
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 应用主框架
 * PlayerViewModel 在此处创建，作为 Activity 级别的单例共享
 */
@Composable
fun AppScaffold() {
    SPICaMusicTheme {
        val navController = rememberNavController()
        // Activity 级别的 PlayerViewModel，全局共享
        val playerViewModel: PlayerViewModel =
            koinActivityViewModel(
                key = "PlayerViewModel",
            )

        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalPlayerViewModel provides playerViewModel,
        ) {
            AppNavGraph(
                modifier = Modifier,
            )
        }
    }
}
