package me.spica27.spicamusic.ui.player.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import java.util.concurrent.TimeUnit

/**
 * 当前播放列表页面
 */
@Composable
fun CurrentPlaylistPage(
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

    // 追踪当前播放的索引并动画化
    val currentPlayingIndex =
        remember(currentPlaylist, currentMediaItem) {
            currentPlaylist.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }
        }
    val animatedPlayingIndex by animateFloatAsState(
        targetValue = currentPlayingIndex.toFloat(),
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow,
            ),
        label = "playingIndexAnimation",
    )

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

    val scrollState = rememberLazyListState()

    val coroutineScope = rememberCoroutineScope()

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
                Modifier,
            ) { isMultiSelectMode ->
                Text(
                    text = if (isMultiSelectMode) "已选择 $selectedCount 项" else "播放列表 (${currentPlaylist.size})",
                    style = MiuixTheme.textStyles.title3,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            AnimatedContent(isMultiSelectMode) { selectMode ->
                if (selectMode) {
                    TextButton(
                        text = "取消",
                        onClick = {
                            isMultiSelectMode = false
                            selectedMediaIds.clear()
                        },
                        insideMargin = PaddingValues(vertical = 4.dp, horizontal = 8.dp),
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    scrollState.animateScrollToItem(
                                        index = currentPlayingIndex,
                                        scrollOffset = -scrollState.layoutInfo.viewportSize.height / 2,
                                    )
                                }
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = "跳转到正在播放",
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.pause()
                                viewModel.updatePlaylist(emptyList())
                            },
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistRemove,
                                contentDescription = "清空播放列表",
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
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
            val selectItemBackgroundColor = MiuixTheme.colorScheme.primaryVariant.copy(alpha = 0.2f)

            LazyColumn(
                state = scrollState,
                modifier =
                    Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .overScrollVertical()
                        .weight(1f)
                        .drawBehind {
                            if (currentPlayingIndex >= 0) {
                                // 计算背景位置和尺寸
                                val itemHeight = 68.dp.toPx() // 44dp(封面) + 24dp(padding)
                                val spacing = 8.dp.toPx()
                                val topPadding = 8.dp.toPx()
                                val cornerRadius = 12.dp.toPx()

                                // 获取第一个可见项的偏移量
                                val firstVisibleItemIndex = scrollState.firstVisibleItemIndex
                                val firstVisibleItemScrollOffset =
                                    scrollState.firstVisibleItemScrollOffset.toFloat()

                                // 计算背景的实际位置（考虑滚动偏移）
                                val itemPositionInList =
                                    animatedPlayingIndex * (itemHeight + spacing)
                                val firstItemPosition =
                                    firstVisibleItemIndex * (itemHeight + spacing)
                                val backgroundTop =
                                    topPadding + itemPositionInList - firstItemPosition - firstVisibleItemScrollOffset

                                // 绘制圆角矩形背景
                                drawRoundRect(
                                    color = selectItemBackgroundColor,
                                    topLeft =
                                        Offset(0f, backgroundTop),
                                    size =
                                        Size(size.width, itemHeight),
                                    cornerRadius =
                                        CornerRadius(cornerRadius),
                                    style = Fill,
                                )
                            }
                        },
                contentPadding =
                    PaddingValues(
                        vertical = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    currentPlaylist,
                    key = { index, song -> song.mediaId },
                ) { index, item ->
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
                                viewModel.playByMediaStoreId(item.mediaId)
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
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "批量删除",
                        style = MiuixTheme.textStyles.body1,
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "创建歌单",
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "创建歌单", style = MiuixTheme.textStyles.body1)
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

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
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
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.size(6.dp))
            AudioCover(
                uri = artworkUri,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(Shapes.SmallCornerBasedShape)
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
                    text =
                        if (isPlaying) {
                            "正在播放"
                        } else {
                            formatTime(
                                item.mediaMetadata.durationMs ?: 0L,
                            )
                        },
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

/**
 * 格式化时间 (毫秒 -> mm:ss)
 */
private fun formatTime(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format(java.util.Locale.CHINESE, "%d:%02d", minutes, seconds)
}
