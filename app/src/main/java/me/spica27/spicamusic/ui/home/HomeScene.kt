package me.spica27.spicamusic.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInbox
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.home.page.FinderPage
import me.spica27.spicamusic.ui.home.page.LibraryPage
import me.spica27.spicamusic.ui.home.page.MusicPage
import me.spica27.spicamusic.ui.home.player_bar.BottomBarScrollConnection
import me.spica27.spicamusic.ui.home.player_bar.BottomMediaBarV2
import me.spica27.spicamusic.ui.home.player_bar.rememberBottomBarScrollConnection
import me.spica27.spicamusic.ui.widget.materialSharedAxisZIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
import org.koin.compose.viewmodel.koinActivityViewModel

class HomeScene : StackScene() {
    // 根页面在冷启动时应直接呈现；若沿用普通 StackScene 的整页滑入和缩放，
    // 会与首屏数据恢复、播放器重连叠加，造成明显的横移卡顿。
    override val transitionShadowEnabled: Boolean = false
    override val transitionScaleEnabled: Boolean = false
    override val transitionSlideEnabled: Boolean = false

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val homeViewModel: HomeViewModel = koinActivityViewModel()

        val currentPage by homeViewModel.currentPage.collectAsStateWithLifecycle()
        var hasLeftInitialFinder by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(currentPage) {
            if (currentPage != HomePage.Finder) {
                hasLeftInitialFinder = true
            }
        }

        val bottomBarScrollConnection = rememberBottomBarScrollConnection()
        val bottomScrimHeight by
            animateDpAsState(
                targetValue = if (bottomBarScrollConnection.isInline) 96.dp else 168.dp,
                animationSpec = tween(durationMillis = 220),
                label = "bottomControlsScrimHeight",
            )
        val pageBackground = MaterialTheme.colorScheme.background

        CompositionLocalProvider(
            LocalBottomBarScrollConnection provides bottomBarScrollConnection,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                AnimatedContent(
                    targetState = currentPage,
                    contentKey = {
                        it
                    },
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    transitionSpec = {
                        materialSharedAxisZIn(forward = true) togetherWith
                            materialSharedAxisZOut(
                                forward = true,
                            )
                    },
                ) {
                    when (it) {
                        HomePage.Finder ->
                            FinderPage(
                                playEntrance = hasLeftInitialFinder,
                            )
                        HomePage.Music -> MusicPage()
                        HomePage.Library -> LibraryPage()
                    }
                }
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(bottomScrimHeight)
                            .background(
                                Brush.verticalGradient(
                                    0f to pageBackground.copy(alpha = 0f),
                                    0.38f to pageBackground.copy(alpha = 0.92f),
                                    1f to pageBackground,
                                ),
                            ),
                )
                BottomMediaBarV2(bottomBarScrollConnection)
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
