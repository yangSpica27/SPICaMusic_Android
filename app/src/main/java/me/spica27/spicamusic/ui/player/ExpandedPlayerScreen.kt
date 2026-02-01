package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import com.linc.amplituda.Amplituda
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.App
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.FluidMusicBackground
import me.spica27.spicamusic.ui.widget.LyricsUI
import me.spica27.spicamusic.ui.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.ui.widget.materialSharedAxisYIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisYOut
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.util.*
import java.util.concurrent.TimeUnit

// ============================================
// 常量定义
// ============================================

// 展开动画透明度阈值常量
private const val COVER_FADE_THRESHOLD = 0.8f
private const val CONTROLS_FADE_THRESHOLD = 0.5f
private const val PAGE_COUNT = 3
public const val DEFAULT_PAGE = 1

// ============================================
// 主屏幕组件
// ============================================

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
    initialPage: Int = DEFAULT_PAGE, // 初始页面索引
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val duration by viewModel.currentDuration.collectAsStateWithLifecycle()

    // 当前播放位置（定时更新）
    var seekValueState by remember { mutableFloatStateOf(0f) }
    var isSeekingState by remember { mutableStateOf(false) }

    val trueTimePosition = viewModel.currentPosition.collectAsStateWithLifecycle()

    LaunchedEffect(trueTimePosition.value) {
        if (!isSeekingState) {
            seekValueState = trueTimePosition.value.toFloat()
        }
    }

    // Pager 状态，使用传入的初始页面
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { PAGE_COUNT })

    // 从封面提取主色调
    val coverColor =
        rememberDominantColorFromUri(
            uri = currentMediaItem?.mediaMetadata?.artworkUri,
            fallbackColor = MiuixTheme.colorScheme.primary,
        )

    val backgroundState = rememberHazeState()

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
            modifier =
                Modifier
                    .hazeSource(backgroundState)
                    .fillMaxSize(),
            coverColor = coverColor,
            enabled = true,
            isDarkMode = MiuixTheme.colorScheme.surface.luminance() < 0.5f,
        )

        // 内容层
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 顶部工具栏（带页面指示器）
            TopBar(
                currentPage = pagerState.currentPage,
                onCollapse = onCollapse,
            )

            // 水平 Pager 内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                key = { it },
            ) { page ->
                when (page) {
                    0 -> {
                        // 当前播放列表页面
                        CurrentPlaylistPage(
                            modifier = Modifier.fillMaxSize(),
                            backgroundState = backgroundState,
                        )
                    }

                    1 -> {
                        // 播放器页面
                        PlayerPage(
                            isSeekingState = isSeekingState,
                            currentMediaItem = currentMediaItem,
                            realPosition = trueTimePosition.value.toFloat(),
                            seekPosition = seekValueState,
                            duration = duration,
                            isPlaying = isPlaying,
                            playMode = playMode,
                            onValueChange = {
                                isSeekingState = true
                                seekValueState = it * duration
                            },
                            onValueChangeFinished = {
                                viewModel.seekTo(seekValueState.toLong())
                                isSeekingState = false
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

// ============================================
// 导航栏组件
// ============================================

/**
 * 顶部工具栏（带页面指示器）
 */
@Composable
private fun TopBar(
    currentPage: Int,
    onCollapse: () -> Unit,
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

            // 占位符，保持布局对称
            Spacer(modifier = Modifier.size(48.dp))
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
            val width by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 6.dp,
                label = "indicatorWidth",
            )
            val alpha = if (isSelected) 1f else 0.3f

            Box(
                modifier =
                    Modifier
                        .size(width = width, height = 6.dp)
                        .clip(CircleShape)
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = alpha)),
            )
        }
    }
}

// ============================================
// 页面内容组件
// ============================================

// ---------- 播放列表页面 ----------

/**
 * 当前播放列表页面
 */
@Composable
private fun CurrentPlaylistPage(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
    backgroundState: HazeState,
) {
    val panelViewModel: CurrentPlaylistPanelViewModel = koinViewModel()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedMediaIds = remember { mutableStateListOf<String>() }
    var showCreateDialog by remember { mutableStateOf(false) }

    val selectedCount by remember { derivedStateOf { selectedMediaIds.size } }

    BackHandler(enabled = isMultiSelectMode) {
        isMultiSelectMode = false
        selectedMediaIds.clear()
    }

    LaunchedEffect(currentPlaylist) {
        val validIds = currentPlaylist.map { it.mediaId }.toSet()
        selectedMediaIds.removeAll { it !in validIds }
        if (selectedMediaIds.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp),
    ) {
        // 顶部标题和操作
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AnimatedContent(
                isMultiSelectMode,
                modifier =
                    Modifier
                        .animateContentSize(),
            ) { isMultiSelectMode ->
                Text(
                    text = if (isMultiSelectMode) "已选择 $selectedCount 项" else "播放列表 (${currentPlaylist.size})",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            AnimatedVisibility(isMultiSelectMode) {
                TextButton(
                    text = "取消",
                    onClick = {
                        isMultiSelectMode = false
                        selectedMediaIds.clear()
                    },
                    insideMargin = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                )
            }
        }

        if (currentPlaylist.isEmpty()) {
            // 空状态
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "播放列表为空",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .overScrollVertical()
                        .weight(1f),
                contentPadding =
                    PaddingValues(
                        vertical = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(currentPlaylist, key = { index, song -> song.mediaId }) { index, item ->
                    val isSelected = selectedMediaIds.contains(item.mediaId)
                    val isPlaying = currentMediaItem?.mediaId == item.mediaId
                    PlaylistItemRow(
                        index = index,
                        modifier = Modifier.animateItem(),
                        item = item,
                        isPlaying = isPlaying,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isMultiSelectMode) {
                                if (isSelected) {
                                    selectedMediaIds.remove(item.mediaId)
                                } else {
                                    selectedMediaIds.add(item.mediaId)
                                }
                            } else {
                                viewModel.playById(item.mediaId)
                            }
                        },
                        backgroundState = backgroundState,
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                            }
                            if (selectedMediaIds.contains(item.mediaId)) {
                                selectedMediaIds.remove(item.mediaId)
                            } else {
                                selectedMediaIds.add(item.mediaId)
                            }
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(if (isMultiSelectMode) 72.dp else 16.dp))
                }
            }
        }

        AnimatedVisibility(visible = isMultiSelectMode) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        val toRemove = selectedMediaIds.toList()
                        toRemove.forEach { mediaId ->
                            viewModel.removeFromPlaylist(mediaId)
                        }
                        selectedMediaIds.clear()
                        isMultiSelectMode = false
                    },
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "批量删除")
                }

                Button(
                    onClick = { showCreateDialog = true },
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "创建歌单",
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "创建歌单")
                }
            }
        }
    }

    if (showCreateDialog) {
        var playlistName by remember { mutableStateOf("") }
        val showState = remember { mutableStateOf(true) }

        WindowDialog(
            title = "创建歌单",
            onDismissRequest = { showCreateDialog = false },
            show = showState,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                TextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = "请输入歌单名称",
                    modifier = Modifier.fillMaxWidth(),
                    useLabelAsPlaceholder = true,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = "取消",
                        onClick = { showCreateDialog = false },
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    TextButton(
                        text = "创建",
                        onClick = {
                            if (playlistName.isNotBlank()) {
                                panelViewModel.createPlaylistWithMediaIds(
                                    name = playlistName,
                                    mediaIds = selectedMediaIds.toList(),
                                ) { success ->
                                    if (success) {
                                        selectedMediaIds.clear()
                                        isMultiSelectMode = false
                                        showCreateDialog = false
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * 播放列表项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistItemRow(
    index: Int,
    item: MediaItem,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
    backgroundState: HazeState,
) {
    val metadata = item.mediaMetadata
    val title = metadata.title?.toString() ?: "未知歌曲"
    val artist = metadata.artist?.toString() ?: "未知艺术家"
    val artworkUri = metadata.artworkUri

    val modifier1 =
        if (isPlaying) {
            modifier
                .clip(
                    RoundedCornerShape(12.dp),
                ).hazeEffect(
                    state = backgroundState,
                    HazeMaterials.ultraThin(
                        MiuixTheme.colorScheme.tertiaryContainer,
                    ),
                )
        } else {
            modifier.clip(
                RoundedCornerShape(12.dp),
            )
        }

    Box(
        modifier =
            modifier1
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(12.dp))
            AudioCover(
                uri = artworkUri,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MiuixTheme.colorScheme.tertiaryContainer,
                                        MiuixTheme.colorScheme.surfaceContainerHigh,
                                    ),
                            ),
                        ),
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "封面占位符",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .align(
                                        Alignment.Center,
                                    ),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            AnimatedContent(isPlaying) { isPlaying ->
                Text(
                    text = if (isPlaying) "正在播放" else formatTime(item.mediaMetadata.durationMs ?: 0L),
                    style = MiuixTheme.textStyles.body2,
                    color =
                        if (
                            isPlaying
                        ) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceVariantSummary
                        },
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            if (isMultiSelectMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

// ---------- 歌曲详情页面 ----------

/**
 * 歌曲详情页面
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongDetailPage(
    currentMediaItem: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                .overScrollVertical()
                .padding(horizontal = 15.dp, vertical = 24.dp)
                .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部封面
        val artworkUri = currentMediaItem?.mediaMetadata?.artworkUri
        val songTitle = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲"

        AudioCover(
            uri = artworkUri,
            modifier =
                Modifier
                    .height(200.dp)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 8.dp,
                        shape = ContinuousRoundedRectangle(8.dp),
                        clip = false,
                    ).clip(ContinuousRoundedRectangle(8.dp)),
            placeHolder = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(ContinuousRoundedRectangle(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = "封面占位符",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier =
                            Modifier
                                .size(64.dp)
                                .align(
                                    Alignment.Center,
                                ),
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 歌曲名称
        Text(
            text = songTitle,
            style = MiuixTheme.textStyles.headline1,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 音频格式标签
        AudioQualityTags(
            currentMediaItem = currentMediaItem,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 基础信息卡片
        InfoCard(title = "基础信息") {
            InfoRow(
                label = "文件名",
                value =
                    currentMediaItem?.mediaMetadata?.displayTitle?.toString()
                        ?: currentMediaItem?.mediaId ?: "未知",
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
                value = formatTime(currentMediaItem?.mediaMetadata?.durationMs ?: 0L),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 音频信息卡片
        AudioInfoCard(currentMediaItem = currentMediaItem)

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ---------- 音频信息组件 ----------

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
        shape = ContinuousRoundedRectangle(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MiuixTheme.textStyles.subtitle,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/**
 * 音频质量标签组
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioQualityTags(
    currentMediaItem: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val extras = currentMediaItem?.mediaMetadata?.extras
    val sampleRate = extras?.getInt("sampleRate") ?: 0
    val bitRate = extras?.getInt("bitRate") ?: 0
    val mimeType = extras?.getString("mimeType") ?: ""
    val isLossless =
        mimeType.contains("flac", ignoreCase = true) ||
            mimeType.contains(
                "alac",
                ignoreCase = true,
            ) ||
            mimeType.contains("wav", ignoreCase = true)

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        if (isLossless) {
            AudioTag(
                text = "无损",
                color = Color(0xFFFF9800),
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 高品质标签
        if (bitRate >= 320000 && !isLossless) {
            AudioTag(
                text = "高品质",
                color = Color(0xFF9C27B0),
            )
        }
    }
}

/**
 * 音频信息卡片
 */
@Composable
private fun AudioInfoCard(
    currentMediaItem: MediaItem?,
    modifier: Modifier = Modifier,
) {
    val extras = currentMediaItem?.mediaMetadata?.extras
    val sampleRate = extras?.getInt("sampleRate") ?: 0
    val bitRate = extras?.getInt("bitRate") ?: 0
    val channels = extras?.getInt("channels") ?: 0
    val mimeType = currentMediaItem?.localConfiguration?.mimeType ?: "未知格式"

    InfoCard(title = "音频信息", modifier = modifier) {
        InfoRow(label = "格式", value = mimeType)

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
            val channelName =
                when (channels) {
                    1 -> "单声道"
                    2 -> "立体声"
                    else -> "$channels 声道"
                }
            InfoRow(label = "声道数", value = channelName)
        }
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
                .clip(ContinuousRoundedRectangle(16.dp))
                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
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
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------- 播放器页面 ----------

/**
 * 播放器页面（原有的播放器内容）
 */
@Composable
private fun PlayerPage(
    currentMediaItem: MediaItem?,
    seekPosition: Float,
    realPosition: Float,
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
    isSeekingState: Boolean = false,
) {
    Column(
        modifier =
            modifier.padding(
                vertical = 24.dp,
                horizontal = 16.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 封面
        AnimatedContent(
            currentMediaItem,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            modifier =
                Modifier
                    .graphicsLayer {
                        alpha = calculateFadeAlpha(progress, COVER_FADE_THRESHOLD)
                    }.weight(1f, fill = false)
                    .aspectRatio(1f)
                    .shadow(
                        elevation = 8.dp,
                        shape = ContinuousRoundedRectangle(8.dp),
                        clip = false,
                    ).clip(ContinuousRoundedRectangle(8.dp)),
        ) { currentMediaItem ->
            AudioCover(
                uri = currentMediaItem?.mediaMetadata?.artworkUri,
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(ContinuousRoundedRectangle(8.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = "封面占位符",
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier =
                                Modifier
                                    .size(64.dp)
                                    .align(
                                        Alignment.Center,
                                    ),
                        )
                    }
                },
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(ContinuousRoundedRectangle(8.dp)),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 歌曲信息
        SongInfo(
            title = currentMediaItem?.mediaMetadata?.title?.toString() ?: "未知歌曲",
            artist = currentMediaItem?.mediaMetadata?.artist?.toString() ?: "未知艺术家",
        )

        Spacer(modifier = Modifier.height(24.dp))

        val amplituda: Amplituda = koinInject<Amplituda>()

        // 优化：使用缓存机制避免重复加载波形数据
        val amplitudeCache = remember { mutableMapOf<String, List<Int>>() }
        var ampState by remember { mutableStateOf(listOf<Int>()) }

        // 音频波形数据
        LaunchedEffect(currentMediaItem?.mediaId) {
            val mediaId = currentMediaItem?.mediaId ?: return@LaunchedEffect

            // 检查缓存
            if (amplitudeCache.containsKey(mediaId)) {
                ampState = amplitudeCache[mediaId] ?: emptyList()
                return@LaunchedEffect
            }

            launch(Dispatchers.IO) {
                val data = loadAmplitudeData(currentMediaItem, amplituda)

                // 保存到缓存，最多保留3首歌曲的数据
                if (amplitudeCache.size >= 3) {
                    // 移除最旧的项
                    amplitudeCache.remove(amplitudeCache.keys.first())
                }
                amplitudeCache[mediaId] = data
                ampState = data
            }
        }

        // 进度条
        AudioWaveSlider(
            progress = if (duration > 0) seekPosition / duration else 0f,
            amplitudes = ampState,
            onProgressChange = {
                onValueChange.invoke(it)
            },
            onProgressChangeFinished = {
                onValueChangeFinished.invoke()
            },
            waveformBrush = SolidColor(MiuixTheme.colorScheme.onSurfaceContainerVariant),
            progressBrush = SolidColor(MiuixTheme.colorScheme.onSurface),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .graphicsLayer {
                        alpha = calculateFadeAlpha(progress, CONTROLS_FADE_THRESHOLD)
                    },
        )
        // 当前位置 和 总时长
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatTime(realPosition.toLong()),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 滑动到的地方
            AnimatedVisibility(
                visible = isSeekingState,
            ) {
                Text(
                    modifier =
                        Modifier
                            .background(
                                MiuixTheme.colorScheme.primaryVariant,
                                shape = ContinuousRoundedRectangle(8.dp),
                            ).padding(vertical = 4.dp, horizontal = 8.dp),
                    text = formatTime(seekPosition.toLong()),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onPrimaryVariant,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = formatTime(duration),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        // 控制按钮
        PlayerControls(
            modifier =
                Modifier.graphicsLayer {
                    alpha = calculateFadeAlpha(progress, CONTROLS_FADE_THRESHOLD)
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

// ---------- 歌词页面 ----------

/**
 * 全屏歌词页面（占位）
 */
@Composable
private fun FullScreenLyricsPage(modifier: Modifier = Modifier) {
    val playerViewModel = koinActivityViewModel<PlayerViewModel>()
    val apiClient: ApiClient = koinInject()

    // 状态管理
    var currentTime by remember { mutableLongStateOf(0L) }
    var lyric by remember { mutableStateOf<List<LyricItem>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 观察当前歌曲变化
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

    // 歌曲变化时自动搜索歌词
    LaunchedEffect(currentMediaItem?.mediaId) {
        val mediaItem = currentMediaItem
        if (mediaItem == null) {
            lyric = null
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            // 从 MediaMetadata 提取歌曲信息
            val title = mediaItem.mediaMetadata.title?.toString() ?: ""
            val artist = mediaItem.mediaMetadata.artist?.toString() ?: ""

            if (title.isBlank()) {
                errorMessage = "歌曲信息缺失"
                lyric = null
                return@LaunchedEffect
            }

            // 调用 EAPI 获取歌词（带自动 YRC/LRC 回退）
            val extraInfo = apiClient.fetchExtInfo(title, artist)

            Timber.d("获取到的歌词信息: extraInfo=$extraInfo")
            Timber.d("歌词内容长度: ${extraInfo?.lyrics?.length}")
            Timber.d("歌词前100字符: ${extraInfo?.lyrics?.take(100)}")

            if (extraInfo?.lyrics.isNullOrBlank()) {
                errorMessage = "暂无歌词"
                lyric = null
            } else {
                val lyricsText = extraInfo!!.lyrics!!

                Timber.d("歌词全文: $lyricsText")

                // 检测 YRC 格式（包含字级时间戳）
                val isYrcFormat =
                    lyricsText.contains("](") &&
                        lyricsText.contains("[") &&
                        lyricsText.matches(Regex(".*\\[\\d+.*\\]\\(\\d+.*\\).*"))

                Timber.d("检测到歌词格式: ${if (isYrcFormat) "YRC" else "LRC"}")

                lyric =
                    if (isYrcFormat) {
                        // YRC 格式 - 使用新解析器转换为 LRC
                        try {
                            val yrcLines = YrcParser.parse(lyricsText)
                            LrcParser.parse(YrcParser.toLrc(yrcLines))
                        } catch (e: Exception) {
                            // YRC 解析失败，回退到标准 LRC
                            Timber.w(e, "YRC parse failed, fallback to LRC")
                            LrcParser.parse(lyricsText)
                        }
                    } else {
                        // 标准 LRC 格式
                        LrcParser.parse(lyricsText)
                    }

                Timber.d("解析后的歌词条数: ${lyric?.size}")

                if (lyric.isNullOrEmpty()) {
                    errorMessage = "歌词解析失败"
                    lyric = null
                } else {
                    errorMessage = null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch lyrics")
            errorMessage = "加载歌词失败: ${e.message ?: "未知错误"}"
            lyric = null
        } finally {
            isLoading = false
        }
    }

    // 持续更新播放时间
    LaunchedEffect(Unit) {
        while (true) {
            awaitFrame()
            currentTime = playerViewModel.getCurrentPositionMs()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        when {
            isLoading -> {
                // 加载状态
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MiuixTheme.colorScheme.primary,
                )
            }
            errorMessage != null -> {
                // 错误或无歌词状态
                Text(
                    text = errorMessage!!,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
            lyric != null -> {
                // 歌词显示
                LyricsUI(
                    modifier = Modifier.fillMaxSize(),
                    lyric = lyric!!,
                    currentTime = currentTime,
                    onSeekToTime = {
                        playerViewModel.seekTo(it)
                    },
                )
            }
            else -> {
                // 初始状态
                Text(
                    text = "等待播放",
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ============================================
// UI 子组件
// ============================================

// ---------- 播放器控制组件 ----------

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
        AnimatedContent(
            title,
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
            contentKey = { it },
        ) { title ->
            Text(
                text = title,
                style = MiuixTheme.textStyles.title1,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            artist,
            contentKey = { it },
            transitionSpec = {
                materialSharedAxisYIn(true) togetherWith materialSharedAxisYOut(true)
            },
        ) { artist ->
            Text(
                text = artist,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
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
                        .clip(ContinuousRoundedRectangle(40.dp))
                        .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.9f)),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    tint = MiuixTheme.colorScheme.onPrimary,
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
                            PlayMode.LOOP -> Icons.Rounded.Repeat
                            PlayMode.LIST -> Icons.Rounded.RepeatOne
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

// ============================================
// 工具函数
// ============================================

/**
 * 格式化时间 (毫秒 -> mm:ss)
 */
private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(Locale.CHINESE, "%d:%02d", minutes, seconds)
}

/**
 * 计算淡入透明度
 * @param progress 当前进度 (0-1)
 * @param threshold 开始淡入的阈值
 * @return 透明度值 (0-1)
 */
private fun calculateFadeAlpha(
    progress: Float,
    threshold: Float,
): Float =
    if (progress < threshold) {
        0f
    } else {
        ((progress - threshold) / (1f - threshold)).coerceIn(0f, 1f)
    }

/**
 * 加载音频波形数据
 */
private suspend fun loadAmplitudeData(
    mediaItem: MediaItem?,
    amplituda: Amplituda,
): List<Int> =
    withContext(Dispatchers.IO) {
        val config = mediaItem?.localConfiguration ?: return@withContext emptyList()

        // ALAC 和 MP4 格式不支持波形提取
        if (config.mimeType == MimeTypes.AUDIO_ALAC || config.mimeType == MimeTypes.AUDIO_MP4) {
            return@withContext emptyList()
        }

        return@withContext try {
            App.getInstance().contentResolver.openInputStream(config.uri)?.use { inputStream ->
                var result = emptyList<Int>()
                amplituda.processAudio(inputStream).get(
                    { result = it.amplitudesAsList() },
                    { result = emptyList() },
                )
                result
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
