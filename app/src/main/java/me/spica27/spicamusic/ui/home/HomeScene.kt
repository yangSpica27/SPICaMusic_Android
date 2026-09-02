package me.spica27.spicamusic.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.glass.liquidGlassSource
import me.spica27.spicamusic.ui.home.page.FinderPage
import me.spica27.spicamusic.ui.home.page.LibraryPage
import me.spica27.spicamusic.ui.home.page.MusicPage
import me.spica27.spicamusic.ui.home.player_bar.BottomBarScrollConnection
import me.spica27.spicamusic.ui.home.player_bar.BottomMediaBarV2
import me.spica27.spicamusic.ui.home.player_bar.rememberBottomBarScrollConnection
import org.koin.compose.viewmodel.koinActivityViewModel

class HomeScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val homeViewModel: HomeViewModel = koinActivityViewModel()
        val navigationPath = LocalNavigationPath.current
        val scene = LocalScene.current

        // NavigationStack 会保留底层场景以完成退场/压缩动画，因此 HomeScene 在歌词页
        // 覆盖期间仍处于组合树中。只在自己是栈顶场景时启用高开销的播放器动效。
        // scenes 是 SnapshotStateList，这个读取只会在 push/pop 时失效，不会随播放进度变化。
        val isSceneVisible by remember(navigationPath, scene) {
            derivedStateOf {
                navigationPath.scenes.lastOrNull() === scene
            }
        }

        val currentPage = homeViewModel.currentPage.collectAsStateWithLifecycle().value

        val bottomBarScrollConnection = rememberBottomBarScrollConnection()
        // One source for the home content lets the persistent bottom surfaces share one capture.
        val hazeState = rememberHazeState()

        CompositionLocalProvider(
            LocalBottomBarScrollConnection provides bottomBarScrollConnection,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // 底栏切页是每天上百次的动作 —— 不做转场动画：任何转场都会
                // 让最高频的操作显得迟滞。切页即时生效，感知延迟为零。
                //
                // SaveableStateHolder 让离开的页面保留可保存状态（列表滚动位置、
                // 入场动画已播标记等），切回时不重建、不重播入场 stagger。
                val pageStateHolder = rememberSaveableStateHolder()
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .liquidGlassSource(hazeState),
                ) {
                    pageStateHolder.SaveableStateProvider(key = currentPage) {
                        when (currentPage) {
                            HomePage.Finder -> FinderPage()
                            HomePage.Music -> MusicPage()
                            HomePage.Library -> LibraryPage()
                        }
                    }
                }
                BottomMediaBarV2(
                    bottomBarScrollConnection = bottomBarScrollConnection,
                    hazeState = hazeState,
                    animationsEnabled = isSceneVisible,
                )
            }
        }
    }
}

@Immutable
enum class HomePage(
    @StringRes val titleRes: Int,
    val icon: ImageVector,
) {
    Finder(R.string.nav_tab_finder, Icons.Default.AllInbox),
    Music(R.string.nav_tab_music, Icons.Default.MusicNote),
    Library(R.string.nav_tab_library, Icons.Default.LibraryMusic),
}

val LocalBottomBarScrollConnection =
    compositionLocalOf<BottomBarScrollConnection> {
        error("No BottomBarScrollConnection provided. This composable must be called inside a Scene's content lambda.")
    }
