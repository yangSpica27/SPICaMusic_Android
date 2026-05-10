package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.highLightClickable

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
    val title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata?.artist?.toString() ?: stringResource(R.string.unknown_artist)
    val artworkUri = metadata?.artworkUri

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .highLightClickable { onExpand() },
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
                                    MaterialTheme.colorScheme.tertiaryContainer,
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
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
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
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                        contentDescription =
                            if (isPlaying) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(
                                    R.string.play,
                                )
                            },
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                // 播放列表按钮
                IconButton(
                    onClick = onExpandToPlaylist,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = stringResource(R.string.queue),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            val progress =
                remember(currentDuration, currentPosition) {
                    (currentPosition.toFloat() / currentDuration.coerceAtLeast(1)).coerceIn(
                        0f,
                        1f,
                    )
                }

            LinearProgressIndicator(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp),
            )
        }
    }
}
