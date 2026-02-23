package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.LocalSurfaceHazeState
import me.spica27.spicamusic.ui.widget.AudioCover
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.ProgressIndicatorDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底部迷你播放条
 * 显示当前播放歌曲信息和基本控制按钮
 */
@Composable
fun LargeBottomPlayerBar(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
    onExpand: () -> Unit,
    onExpandToPlaylist: () -> Unit = {}, // 展开到播放列表页面
) {
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition = viewModel.currentPosition.collectAsStateWithLifecycle().value
    val currentDuration by viewModel.currentDuration.collectAsStateWithLifecycle()

    val metadata = currentMediaItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: "未知歌曲"
    val artist = metadata?.artist?.toString() ?: "未知艺术家"
    val artworkUri = metadata?.artworkUri

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .hazeEffect(
                    LocalSurfaceHazeState.current,
                    HazeMaterials.ultraThin(
                        MiuixTheme.colorScheme.primaryContainer,
                    ),
                ).fillMaxWidth()
                .clickable { onExpand() },
    ) {
        Column {
            // 进度条
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 封面

                AnimatedContent(artworkUri) { artworkUri ->
                    AudioCover(
                        uri = artworkUri,
                        modifier =
                            Modifier
                                .size(48.dp)
                                .clip(me.spica27.spicamusic.ui.theme.Shapes.LargeCornerBasedShape)
                                .background(
                                    MiuixTheme.colorScheme.tertiaryContainer,
                                ),
                        placeHolder = {
                            Box(
                                modifier =
                                    Modifier
                                        .size(48.dp),
                            ) {
                                Text(
                                    "🎵",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 歌曲信息
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = title,
                        style = MiuixTheme.textStyles.body1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = artist,
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 播放/暂停按钮
                IconButton(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        tint = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // 播放列表按钮
                IconButton(
                    onClick = onExpandToPlaylist,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = "播放列表",
                        tint = MiuixTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            LinearProgressIndicator(
                colors =
                    ProgressIndicatorDefaults.progressIndicatorColors().copy(
                        backgroundColor = Color.Transparent,
                        foregroundColor = MiuixTheme.colorScheme.primary,
                    ),
                progress =
                    (currentPosition.toFloat() / currentDuration.coerceAtLeast(1)).coerceIn(
                        0f,
                        1f,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
            )
        }
    }
}

@Composable
fun SmallBottomPlayerBar(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
) {
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPosition = viewModel.currentPosition.collectAsStateWithLifecycle().value
    val currentDuration by viewModel.currentDuration.collectAsStateWithLifecycle()

    val metadata = currentMediaItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: "未知歌曲"
    val artist = metadata?.artist?.toString() ?: "未知艺术家"
    val artworkUri = metadata?.artworkUri

    val localFloatingTabBarScrollConnection = LocalFloatingTabBarScrollConnection.current

    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .hazeEffect(
                    LocalSurfaceHazeState.current,
                    HazeMaterials.ultraThin(
                        MiuixTheme.colorScheme.primaryContainer,
                    ),
                ).fillMaxWidth()
                .clickable {
                    localFloatingTabBarScrollConnection.expand()
                }.padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedContent(
            artworkUri,
            transitionSpec = {
                slideIntoContainer(
                    SlideDirection.Left,
                ) togetherWith
                    slideOutOfContainer(
                        SlideDirection.Left,
                    )
            },
        ) { artworkUri ->
            AudioCover(
                uri = artworkUri,
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(me.spica27.spicamusic.ui.theme.Shapes.SmallCornerBasedShape)
                        .background(
                            MiuixTheme.colorScheme.tertiaryContainer,
                        ),
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    ) {
                        Text(
                            "🎵",
                            modifier = Modifier.align(Alignment.Center),
                            color = MiuixTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                },
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(
            targetState = title,
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
            transitionSpec = {
                slideIntoContainer(
                    SlideDirection.Up,
                ) togetherWith
                    slideOutOfContainer(
                        SlideDirection.Down,
                    )
            },
        ) { title ->
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
        )
    }
}
