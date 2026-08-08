package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.common.entity.Album

/**
 * 专辑设置底部面板
 */
class AlbumMenuScene(
    private val album: Album,
) : DialogScene() {
    @Composable
    override fun DialogContent() {
        Surface {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text("还在开发中")
            }
        }
    }
}
