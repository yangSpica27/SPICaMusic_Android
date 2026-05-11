package me.spica27.spicamusic.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.cloudy.rememberSky
import com.skydoves.cloudy.sky
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

        val sky = rememberSky()

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
                        .sky(sky)
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
            BottomMediaBar(sky = sky)
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
