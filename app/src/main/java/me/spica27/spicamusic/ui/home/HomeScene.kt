package me.spica27.spicamusic.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.home.page.FinderPage
import me.spica27.spicamusic.ui.home.page.LibraryPage
import me.spica27.spicamusic.ui.home.page.MusicPage
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

        AnimatedContent(
            targetState = currentPage,
            contentKey = {
                it
            },
            modifier = Modifier.fillMaxSize(),
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
    }
}

@Immutable
enum class HomePage(
    val title: String,
) {
    Finder("发现"),
    Music("音乐"),
    Library("资料库"),
}
