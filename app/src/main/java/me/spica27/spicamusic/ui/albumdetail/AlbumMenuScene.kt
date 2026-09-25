package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.ui.component.DialogContainer

@Composable
fun AlbumMenuDialogContent(album: Album) {
    DialogContainer {
        Box(
            modifier = Modifier.padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("还在开发中")
        }
    }
}
