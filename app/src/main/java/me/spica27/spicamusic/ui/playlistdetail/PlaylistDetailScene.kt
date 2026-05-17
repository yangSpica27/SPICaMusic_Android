package me.spica27.spicamusic.ui.playlistdetail

import androidx.compose.runtime.Composable
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.Playlist

class PlaylistDetailScene(
    private val playlist: Playlist,
) : StackScene() {
    @Composable
    override fun Content() {
        PlaylistDetailScreen(playlist = playlist)
    }
}
