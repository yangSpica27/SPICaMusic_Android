package me.spica27.spicamusic.ui.artistdetail

import androidx.compose.runtime.Composable
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.Artist

class ArtistDetailScene(
    private val artist: Artist,
) : StackScene() {
    @Composable
    override fun Content() {
        ArtistDetailScreen(artist = artist)
    }
}
