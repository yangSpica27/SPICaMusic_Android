package me.spica27.spicamusic.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.navigation.AppNavGraph
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelHost
import me.spica27.spicamusic.ui.player.DraggablePlayerSheet
import me.spica27.spicamusic.ui.player.LocalBottomPaddingState
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.LocalPlaylistPanelController
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.player.PlaylistPanelController
import me.spica27.spicamusic.ui.theme.SPICaMusicTheme
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 应用主框架
 * PlayerViewModel 在此处创建，作为 Activity 级别的单例共享
 */
@Composable
fun AppScaffold() {
    SPICaMusicTheme {
        val backStack = remember { mutableStateListOf<Screen>(Screen.Home) }

        // Activity 级别的 PlayerViewModel，全局共享
        val playerViewModel: PlayerViewModel =
            koinActivityViewModel(
                key = "PlayerViewModel",
            )

        // 全局底部 padding 状态，供各页面动态设置
        val bottomPaddingState = remember { mutableFloatStateOf(0f) }

        val bottomPaddingAnimValue = animateFloatAsState(bottomPaddingState.floatValue).value

        val playlistPanelVisible = remember { mutableStateOf(false) }
        val playlistPanelController = remember { PlaylistPanelController(playlistPanelVisible) }

        val surfaceHazeState = rememberHazeState()

        CompositionLocalProvider(
            LocalNavBackStack provides backStack,
            LocalPlayerViewModel provides playerViewModel,
            LocalBottomPaddingState provides bottomPaddingState,
            LocalSurfaceHazeState provides surfaceHazeState,
            LocalPlaylistPanelController provides playlistPanelController,
        ) {
            CurrentPlaylistPanelHost(
                visible = playlistPanelVisible.value,
                onVisibleChange = { playlistPanelVisible.value = it },
            ) {
                // 全局播放器层包裹整个导航
                DraggablePlayerSheet(
                    bottomPadding = bottomPaddingAnimValue,
                ) {
                    AppNavGraph(
                        modifier = Modifier,
                    )
                }
            }
        }
    }
}

val LocalSurfaceHazeState =
    staticCompositionLocalOf<HazeState> {
        error("HazeState not provided")
    }
