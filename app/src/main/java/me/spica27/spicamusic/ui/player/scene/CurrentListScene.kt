package me.spica27.spicamusic.ui.player.scene

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.player.pages.CurrentPlaylistPage

class CurrentListScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        BackHandler(true) {
            path.popTop()
        }

        Scaffold {
            Box(
                modifier = Modifier.padding(it),
            ) {
                CurrentPlaylistPage()
            }
        }
    }
}
