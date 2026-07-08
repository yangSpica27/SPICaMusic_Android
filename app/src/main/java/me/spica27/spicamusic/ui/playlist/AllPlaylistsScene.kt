package me.spica27.spicamusic.ui.playlist

import androidx.compose.runtime.Composable
import me.spica27.navkit.scene.StackScene

class AllPlaylistsScene : StackScene() {
    @Composable
    override fun Content() {
        AllPlaylistsScreen()
    }
}
