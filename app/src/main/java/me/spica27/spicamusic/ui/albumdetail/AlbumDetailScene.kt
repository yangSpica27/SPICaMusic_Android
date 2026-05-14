package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.runtime.Composable
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.Album

class AlbumDetailScene(
    private val album: Album,
) : StackScene() {
    @Composable
    override fun Content() {
        AlbumDetailScreen(album = album)
    }
}
