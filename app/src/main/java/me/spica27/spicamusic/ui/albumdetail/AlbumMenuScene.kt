package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.ui.theme.Shapes

/**
 * 专辑设置对话框。
 */
class AlbumMenuScene(
    private val album: Album,
) : DialogScene() {
    @Composable
    override fun DialogContent() {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.ExtraLarge1CornerBasedShape,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Box(
                modifier = Modifier.padding(48.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center,
            ) {
                Text("还在开发中")
            }
        }
    }
}
