package me.spica27.spicamusic.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import me.spica27.spicamusic.player.api.PlayMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.concurrent.TimeUnit

/**
 * 全屏播放器页面
 */
@Composable
fun ExpandedPlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
    onCollapse: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (Float) -> Unit = {},
    progress: Float = 1f, // 展开进度，用于视觉效果
) {
    val fft by viewModel.fftBands.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val duration by viewModel.currentDuration.collectAsStateWithLifecycle()

    // 当前播放位置（定时更新）
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var userIsDragging by remember { mutableStateOf(false) }

    // 定时更新播放位置
    LaunchedEffect(isPlaying, userIsDragging) {
        while (isPlaying && !userIsDragging) {
            currentPosition = viewModel.currentPosition.toFloat()
            delay(100) // 每 100ms 更新一次
        }
    }

    Box(
        modifier =
            modifier
                .statusBarsPadding()
                .navigationBarsPadding()
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragCancel() },
                        onVerticalDrag = { _, dragAmount -> onDrag(dragAmount) },
                    )
                },
    ) {
        // 流动背景
//        FluidMusicBackground(
//            modifier = Modifier.fillMaxSize(),
//            fftBands = fft,
//            coverColor = MiuixTheme.colorScheme.primary,
//            enabled = isPlaying,
//            isDarkMode = isSystemInDarkTheme(),
//        )

        // 内容层
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部工具栏
            TopBar(
                onCollapse = onCollapse,
                onMoreClick = { /* TODO: 显示更多选项 */ },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 封面
            AlbumArtwork(
                artworkUri = currentMediaItem?.mediaMetadata?.artworkUri?.toString(),
                isPlaying = isPlaying,
                modifier =
                    Modifier
                        .graphicsLayer {
                            alpha =
                                if (progress < 0.5f) {
                                    0f
                                } else {
                                    (progress - 0.5f) * 2
                                }
                        }.weight(1f, fill = false),
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 歌曲信息
            SongInfo(
                title = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲",
                artist = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 进度条
            ProgressBar(
                modifier =
                    Modifier.graphicsLayer {
                        alpha =
                            if (progress < 0.5f) {
                                0f
                            } else {
                                (progress - 0.5f) * 2
                            }
                    },
                currentPosition = currentPosition,
                duration = duration.toFloat(),
                onValueChange = {
                    userIsDragging = true
                    currentPosition = it
                },
                onValueChangeFinished = {
                    userIsDragging = false
                    viewModel.seekTo(currentPosition.toLong())
                },
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 控制按钮
            PlayerControls(
                modifier =
                    Modifier.graphicsLayer {
                        alpha =
                            if (progress < 0.5f) {
                                0f
                            } else {
                                (progress - 0.5f) * 2
                            }
                    },
                isPlaying = isPlaying,
                playMode = playMode,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onPreviousClick = { viewModel.skipToPrevious() },
                onNextClick = { viewModel.skipToNext() },
                onPlayModeClick = { viewModel.togglePlayMode() },
                onFavoriteClick = { /* TODO: 收藏功能 */ },
            )

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * 顶部工具栏
 */
@Composable
private fun TopBar(
    onCollapse: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "收起",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }

        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "更多",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * 专辑封面
 */
@Composable
private fun AlbumArtwork(
    artworkUri: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    // 旋转动画
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(20000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "rotation_angle",
    )

    Box(
        modifier = modifier.fillMaxWidth(0.8f),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = artworkUri,
            contentDescription = "专辑封面",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .rotate(if (isPlaying) rotation else 0f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )
    }
}

/**
 * 歌曲信息
 */
@Composable
private fun SongInfo(
    title: String,
    artist: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist,
            style = MaterialTheme.typography.bodyLarge,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * 进度条
 */
@Composable
private fun ProgressBar(
    modifier: Modifier,
    currentPosition: Float,
    duration: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = currentPosition,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..duration.coerceAtLeast(1f),
            colors =
                SliderDefaults.colors(
                    thumbColor = MiuixTheme.colorScheme.onSurface,
                    activeTrackColor = MiuixTheme.colorScheme.onSurface,
                    inactiveTrackColor = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(currentPosition.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )

            Text(
                text = formatTime(duration.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * 播放控制按钮
 */
@Composable
private fun PlayerControls(
    modifier: Modifier,
    isPlaying: Boolean,
    playMode: PlayMode,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 主控制行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 上一首
            IconButton(
                onClick = onPreviousClick,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "上一首",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 播放/暂停
            IconButton(
                onClick = onPlayPauseClick,
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.9f)),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 下一首
            IconButton(
                onClick = onNextClick,
                modifier = Modifier.size(64.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "下一首",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 次要控制行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 播放模式
            IconButton(onClick = onPlayModeClick) {
                Icon(
                    imageVector =
                        when (playMode) {
                            PlayMode.LOOP -> Icons.Rounded.RepeatOne
                            PlayMode.LIST -> Icons.Rounded.Repeat
                            PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                        },
                    contentDescription = "播放模式",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }

            // 收藏
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = Icons.Rounded.FavoriteBorder, // TODO: 根据状态切换 Favorite
                    contentDescription = "收藏",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/**
 * 格式化时间 (毫秒 -> mm:ss)
 */
private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%d:%02d", minutes, seconds)
}
