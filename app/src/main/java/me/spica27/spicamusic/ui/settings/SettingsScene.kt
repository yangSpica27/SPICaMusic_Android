package me.spica27.spicamusic.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.spica27.navkit.scene.StackScene

class SettingsScene : StackScene() {
    @Composable
    override fun Content() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Settings")
            }
        }
    }
}
