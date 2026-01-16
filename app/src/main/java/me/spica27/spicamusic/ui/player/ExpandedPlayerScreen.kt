package me.spica27.spicamusic.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.widget.FluidMusicBackground
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
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val duration by viewModel.currentDuration.collectAsStateWithLifecycle()

    // 当前播放位置（定时更新）
    var currentPosition by remember { mutableFloatStateOf(0f) }
    var userIsDragging by remember { mutableStateOf(false) }

    val trueTimePosition = viewModel.currentPosition.collectAsStateWithLifecycle()

    LaunchedEffect(trueTimePosition.value, userIsDragging) {
        if (!userIsDragging) {
            currentPosition = trueTimePosition.value.toFloat()
        }
    }

    // Pager 状态，默认显示播放器页面（index=1）
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier =
            modifier
                .background(MiuixTheme.colorScheme.background)
                .fillMaxSize()
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
        FluidMusicBackground(
            modifier = Modifier.fillMaxSize(),
            coverColor = MiuixTheme.colorScheme.primary,
            enabled = true,
            isDarkMode = isSystemInDarkTheme(),
        )

        // 内容层
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部工具栏（带页面指示器）
            TopBar(
                currentPage = pagerState.currentPage,
                onCollapse = onCollapse,
                onMoreClick = { /* TODO: 显示更多选项 */ },
            )

            // 水平 Pager 内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        // 歌曲详情页面
                        SongDetailPage(
                            currentMediaItem = currentMediaItem,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    1 -> {
                        // 播放器页面
                        PlayerPage(
                            currentMediaItem = currentMediaItem,
                            currentPosition = currentPosition,
                            duration = duration,
                            isPlaying = isPlaying,
                            playMode = playMode,
                            onValueChange = {
                                userIsDragging = true
                                currentPosition = it
                            },
                            onValueChangeFinished = {
                                userIsDragging = false
                                viewModel.seekTo(currentPosition.toLong())
                            },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onPreviousClick = { viewModel.skipToPrevious() },
                            onNextClick = { viewModel.skipToNext() },
                            onPlayModeClick = { viewModel.togglePlayMode() },
                            onFavoriteClick = { /* TODO: 收藏功能 */ },
                            progress = progress,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    2 -> {
                        // 全屏歌词页面（占位）
                        FullScreenLyricsPage(
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 顶部工具栏（带页面指示器）
 */
@Composable
private fun TopBar(
    currentPage: Int,
    onCollapse: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
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

            // 页面指示器
            PageIndicator(
                pageCount = 3,
                currentPage = currentPage,
            )

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
}

/**
 * 页面指示器
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width = animateDpAsState(if (isSelected) 20.dp else 6.dp).value
            Box(
                modifier =
                    Modifier
                        .size(
                            width = width,
                            height = 6.dp,
                        ).clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MiuixTheme.colorScheme.onSurface
                            } else {
                                MiuixTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            },
                        ),
            )
        }
    }
}

/**
 * 歌曲详情页面
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongDetailPage(
    currentMediaItem: androidx.media3.common.MediaItem?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .padding(horizontal = 15.dp, vertical = 24.dp)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部封面
        AsyncImage(
            model = currentMediaItem?.mediaMetadata?.artworkUri,
            contentDescription = "专辑封面",
            modifier =
                Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Gray.copy(alpha = 0.3f)),
            contentScale = ContentScale.Crop,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 歌曲名称
        Text(
            text = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 音频格式标签
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 从 extras 中获取音频信息（如果有）
            val sampleRate = currentMediaItem?.mediaMetadata?.extras?.getInt("sampleRate") ?: 0
            val bitRate = currentMediaItem?.mediaMetadata?.extras?.getInt("bitRate") ?: 0
            val mimeType = currentMediaItem?.mediaMetadata?.extras?.getString("mimeType") ?: ""

            // 采样率标签
            if (sampleRate > 0) {
                AudioTag(
                    text = "${sampleRate / 1000}kHz",
                    color = if (sampleRate >= 96000) Color(0xFF4CAF50) else MiuixTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 比特率标签
            if (bitRate > 0) {
                val bitRateKbps = bitRate / 1000
                AudioTag(
                    text = "${bitRateKbps}kbps",
                    color = if (bitRateKbps >= 320) Color(0xFF2196F3) else MiuixTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 无损标签
            val isLossless =
                mimeType.contains("flac", ignoreCase = true) ||
                    mimeType.contains("alac", ignoreCase = true) ||
                    mimeType.contains("wav", ignoreCase = true)
            if (isLossless) {
                AudioTag(
                    text = "无损",
                    color = Color(0xFFFF9800),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // 高码率标签
            if (bitRate >= 320000 && !isLossless) {
                AudioTag(
                    text = "高品质",
                    color = Color(0xFF9C27B0),
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 基础信息卡片
        InfoCard(title = "基础信息") {
            InfoRow(
                label = "文件名",
                value =
                    currentMediaItem?.mediaMetadata?.displayTitle?.toString()
                        ?: currentMediaItem?.mediaId
                        ?: "未知",
            )

            InfoRow(
                label = "URI",
                value = currentMediaItem?.requestMetadata?.mediaUri?.toString() ?: "N/A",
            )

            InfoRow(
                label = "歌曲名称",
                value = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲",
            )

            InfoRow(
                label = "艺术家",
                value = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
            )

            InfoRow(
                label = "专辑",
                value = currentMediaItem?.mediaMetadata?.albumTitle?.toString() ?: "未知专辑",
            )

            InfoRow(
                label = "时长",
                value = formatTime(currentMediaItem?.mediaMetadata?.extras?.getLong("duration") ?: 0),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 音频信息卡片
        InfoCard(title = "音频信息") {
            val sampleRate = currentMediaItem?.mediaMetadata?.extras?.getInt("sampleRate") ?: 0
            val bitRate = currentMediaItem?.mediaMetadata?.extras?.getInt("bitRate") ?: 0
            val channels = currentMediaItem?.mediaMetadata?.extras?.getInt("channels") ?: 0
            val mimeType = currentMediaItem?.mediaMetadata?.extras?.getString("mimeType") ?: "未知"

            InfoRow(
                label = "格式",
                value = mimeType,
            )

            if (sampleRate > 0) {
                InfoRow(
                    label = "采样率",
                    value = "${sampleRate}Hz (${sampleRate / 1000}kHz)",
                )
            }

            if (bitRate > 0) {
                InfoRow(
                    label = "比特率",
                    value = "${bitRate / 1000}kbps",
                )
            }

            if (channels > 0) {
                InfoRow(
                    label = "声道数",
                    value =
                        when (channels) {
                            1 -> "单声道"
                            2 -> "立体声"
                            else -> "$channels 声道"
                        },
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/**
 * 音频标签
 */
@Composable
private fun AudioTag(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 信息卡片
 */
@Composable
private fun InfoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = MiuixTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                ).padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

/**
 * 信息行
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 播放器页面（原有的播放器内容）
 */
@Composable
private fun PlayerPage(
    currentMediaItem: androidx.media3.common.MediaItem?,
    currentPosition: Float,
    duration: Long,
    isPlaying: Boolean,
    playMode: PlayMode,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onPlayModeClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.padding(
                vertical = 24.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 封面
        AlbumArtwork(
            artworkUri = currentMediaItem?.mediaMetadata?.artworkUri?.toString(),
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
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
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
            onPlayPauseClick = onPlayPauseClick,
            onPreviousClick = onPreviousClick,
            onNextClick = onNextClick,
            onPlayModeClick = onPlayModeClick,
            onFavoriteClick = onFavoriteClick,
        )
    }
}

/**
 * 全屏歌词页面（占位）
 */
@Composable
private fun FullScreenLyricsPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "🎵",
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "全屏歌词",
                style = MaterialTheme.typography.headlineMedium,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "（待实现）",
                style = MaterialTheme.typography.bodyMedium,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * 顶部工具栏
 */
@Composable
private fun TopBarOld(
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
    modifier: Modifier = Modifier,
) {
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
