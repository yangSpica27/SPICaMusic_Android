package me.spica27.spicamusic.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.home.page.FinderPage
import me.spica27.spicamusic.ui.home.page.LibraryPage
import me.spica27.spicamusic.ui.home.page.MusicPage
import me.spica27.spicamusic.ui.home.player_bar.BottomBarScrollConnection
import me.spica27.spicamusic.ui.home.player_bar.BottomMediaBar
import me.spica27.spicamusic.ui.home.player_bar.rememberBottomBarScrollConnection
import me.spica27.spicamusic.ui.widget.materialSharedAxisZIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
import org.koin.compose.viewmodel.koinActivityViewModel

class HomeScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigationPath = LocalNavigationPath.current

        val homeViewModel: HomeViewModel = koinActivityViewModel()

        val currentPage = homeViewModel.currentPage.collectAsStateWithLifecycle().value

        val bottomBarScrollConnection = rememberBottomBarScrollConnection()

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
                        HomePage.Finder -> FinderPage()
                        HomePage.Music -> MusicPage()
                        HomePage.Library -> LibraryPage()
                    }
                }
                BottomMediaBar(bottomBarScrollConnection)
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
