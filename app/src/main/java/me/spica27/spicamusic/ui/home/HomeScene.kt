package me.spica27.spicamusic.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.artwork
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.glass.liquidGlassSource
import me.spica27.spicamusic.ui.home.page.FinderPage
import me.spica27.spicamusic.ui.home.page.LibraryPage
import me.spica27.spicamusic.ui.home.page.MusicPage
import me.spica27.spicamusic.ui.home.player_bar.HomeNavigationBar
import me.spica27.spicamusic.ui.home.player_bar.HomePlayerScaffold
import me.spica27.spicamusic.ui.home.player_bar.MiniPlayerBar
import me.spica27.spicamusic.ui.home.player_bar.PlayerSheetHandle
import me.spica27.spicamusic.ui.home.player_bar.rememberPlayerSheetState
import me.spica27.spicamusic.ui.navigation.HomeRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.player.DEFAULT_PAGE
import me.spica27.spicamusic.ui.player.ExpandedPlayerScreen
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.QUEUE_PAGE
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import org.koin.compose.viewmodel.koinActivityViewModel

@Composable
fun HomeScreen() {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current
    val backStack = LocalBackStack.current
    val active by remember(backStack) { derivedStateOf { backStack.lastScreenOrNull() is HomeRoute } }
    val currentPage by homeViewModel.currentPage.collectAsStateWithLifecycle()
    val mediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    // 保留歌曲信息，供退出动画显示。
    var lastMediaItem by remember { mutableStateOf(mediaItem) }
    if (mediaItem != null) SideEffect { lastMediaItem = mediaItem }
    val displayedMediaItem = mediaItem ?: lastMediaItem
    val glassEnabled = LocalLiquidGlassConfig.current.enabled
    val hazeState = rememberHazeState()
    val isPlaying by playerViewModel.isPlaying.collectAsStateWithLifecycle()
    val position = playerViewModel.currentPosition.collectAsStateWithLifecycle()
    val duration = playerViewModel.currentDuration.collectAsStateWithLifecycle()
    val reducedMotion = LocalReducedMotion.current
    val sheetState = rememberPlayerSheetState()
    var initialPlayerPage by rememberSaveable { mutableIntStateOf(DEFAULT_PAGE) }
    val pageStateHolder = rememberSaveableStateHolder()

    HomePlayerScaffold(
        playerAvailable = mediaItem != null,
        sheetState = sheetState,
        active = active,
        onMiniPlayerDragStart = { initialPlayerPage = DEFAULT_PAGE },
        navigationBar = { HomeNavigationBar(currentPage, homeViewModel::navigateToPage, glassEnabled = glassEnabled) },
        miniPlayer = { dragModifier ->
            val metadata = displayedMediaItem?.mediaMetadata
            MiniPlayerBar(
                title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song),
                artist = metadata?.artist?.toString() ?: stringResource(R.string.unknown_artist),
                artwork = metadata?.artwork(),
                isPlaying = isPlaying,
                onExpand = {
                    initialPlayerPage = DEFAULT_PAGE
                    sheetState.animateTo(true, reducedMotion)
                },
                onPlayPause = playerViewModel::togglePlayPause,
                onOpenQueue = {
                    initialPlayerPage = QUEUE_PAGE
                    sheetState.animateTo(true, reducedMotion)
                },
                modifier = dragModifier,
                progress = { if (duration.value > 0L) position.value.toFloat() / duration.value else 0f },
                hazeState = hazeState,
                glassEnabled = glassEnabled,
            )
        },
        fullScreenPlayer = { progress, dragModifier ->
            val collapse = { sheetState.animateTo(false, reducedMotion) }
            ExpandedPlayerScreen(
                onCollapse = collapse,
                progressProvider = progress,
                initialPage = initialPlayerPage,
                animationsEnabled = active,
                dragHandle = { PlayerSheetHandle(collapse, dragModifier) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().liquidGlassSource(hazeState)) {
            pageStateHolder.SaveableStateProvider(currentPage) {
                when (currentPage) {
                    HomePage.Finder -> FinderPage(bottomContentPadding = padding.calculateBottomPadding())
                    HomePage.Music -> MusicPage(bottomContentPadding = padding.calculateBottomPadding())
                    HomePage.Library -> LibraryPage(bottomContentPadding = padding.calculateBottomPadding())
                }
            }
        }
    }
}

@Immutable
enum class HomePage(
    @param:StringRes val titleRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Finder(R.string.nav_tab_finder, Icons.Outlined.Explore, Icons.Filled.Explore),
    Music(R.string.nav_tab_music, Icons.Outlined.MusicNote, Icons.Filled.MusicNote),
    Library(R.string.nav_tab_library, Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
}
