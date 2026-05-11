package me.spica27.spicamusic.ui.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.spica27.navkit.scene.StackScene

class PlaylistCreatorScene : StackScene() {
    @Composable
    override fun Content() {
        Scaffold(modifier = Modifier.fillMaxWidth()) { paddingValues ->
            Box(
                modifier = Modifier.padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text("创建歌单界面")
            }
        }
    }
}
