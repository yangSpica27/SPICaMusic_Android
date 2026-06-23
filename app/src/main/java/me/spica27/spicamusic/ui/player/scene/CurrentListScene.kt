package me.spica27.spicamusic.ui.player.scene

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.player.pages.CurrentPlaylistPage
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent

class CurrentListScene : DialogScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        BackHandler(true) {
            path.popTop()
        }

        val enterAnimEnd = enterAnimEnd.collectAsState()

        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
            ShowOnIdleContent(enterAnimEnd.value) {
                Box(
                    modifier = Modifier.padding(it),
                ) {
                    CurrentPlaylistPage(
                        modifier = Modifier.fillMaxSize(),
//                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        }
    }

    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val density = LocalDensity.current

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize(),
        ) {
            // ── 半透明遮罩：随进度渐显，点击关闭 ──
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = enterProgress.value }
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { path.pop(scene) },
            )

            // ── 卡片：从底部上滑 + 淡入/淡出 ──
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            translationY = size.height - enterProgress.value * size.height
                            if (enterProgress.isRunning) {
                                shape =
                                    RoundedCornerShape(
                                        topStart =
                                            lerp(
                                                36.dp.toPx(),
                                                0f,
                                                enterProgress.value,
                                            ),
                                        topEnd =
                                            lerp(
                                                36.dp.toPx(),
                                                0f,
                                                enterProgress.value,
                                            ),
                                    )
                            }
                            clip = true
                        },
            ) {
                DialogContent()
            }
        }
    }
}
