package me.spica27.spicamusic.ui.player.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.navkit.scene.SceneStage
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.player.pages.CurrPlaylistPage
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent

class CurrentListScene : DialogScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current

        val contentReady =
            scene.stage.value == SceneStage.Appeared ||
                scene.stage.value == SceneStage.Disappearing

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        scrollBehavior = scrollBehavior,
                        colors =
                            TopAppBarDefaults.topAppBarColors().copy(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                titleContentColor = MaterialTheme.colorScheme.onSurface,
                                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                subtitleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    path.popTop()
                                },
                                colors =
                                    IconButtonDefaults.iconButtonColors().copy(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBackIosNew,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        },
                        title = {
                            Text(stringResource(R.string.now_playinglist))
                        },
                    )
                },
            ) {
                ShowOnIdleContent(contentReady) {
                    Box(
                        modifier = Modifier.padding(it),
                    ) {
                        CurrPlaylistPage(
                            modifier = Modifier.fillMaxSize(),
//                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        }
    }
}
