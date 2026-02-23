package me.spica27.spicamusic.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.rememberNavBackStack
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.navigation.AppNavGraph
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelHost
import me.spica27.spicamusic.ui.player.DraggablePlayerSheet
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.LocalPlaylistPanelController
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.player.PlaylistPanelController
import me.spica27.spicamusic.ui.theme.SPICaMusicTheme
import me.spica27.spicamusic.ui.widget.FloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.widget.rememberFloatingTabBarScrollConnection
import me.spica27.spicamusic.utils.PreferencesManager
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
            .collectAsStateWithLifecycle(false)

    SPICaMusicTheme(
        darkTheme = isDarkMode.value,
    ) {
        val backStack = rememberNavBackStack(Screen.Library)

        // Activity 级别的 PlayerViewModel，全局共享
        val playerViewModel: PlayerViewModel = koinActivityViewModel(key = "PlayerViewModel")

        val playlistPanelVisible = remember { mutableStateOf(false) }
        val playlistPanelController = remember { PlaylistPanelController(playlistPanelVisible) }

        val surfaceHazeState = rememberHazeState()

        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavBackStack provides backStack,
                LocalPlayerViewModel provides playerViewModel,
                LocalSurfaceHazeState provides surfaceHazeState,
                LocalPlaylistPanelController provides playlistPanelController,
                LocalNavSharedTransitionScope provides this@SharedTransitionLayout,
                LocalFloatingTabBarScrollConnection provides rememberFloatingTabBarScrollConnection(),
            ) {
                CurrentPlaylistPanelHost(
                    visible = playlistPanelVisible.value,
                    onVisibleChange = { playlistPanelVisible.value = it },
                ) {
                    // 全局播放器层包裹整个导航
                    DraggablePlayerSheet {
                        AppNavGraph(
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

val LocalSurfaceHazeState = staticCompositionLocalOf<HazeState> { error("HazeState not provided") }

val LocalFloatingTabBarScrollConnection =
    staticCompositionLocalOf<FloatingTabBarScrollConnection> {
        error("FloatingTabBarScrollConnection not provided")
    }

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalNavSharedTransitionScope: ProvidableCompositionLocal<SharedTransitionScope> =
    compositionLocalOf {
        throw IllegalStateException(
            "Unexpected access to LocalNavSharedTransitionScope. You must provide a " +
                "SharedTransitionScope from a call to SharedTransitionLayout() or " +
                "SharedTransitionScope()",
        )
    }
