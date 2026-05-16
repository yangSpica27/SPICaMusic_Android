package me.spica27.spicamusic.ui.player.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.PlaylistRemove
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.player.formatTime
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinViewModel
import java.util.concurrent.TimeUnit

/**
 * 当前播放列表页面
 */
@Composable
fun CurrentPlaylistPage(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
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
        modifier = modifier.padding(horizontal = 16.dp),
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
                modifier = Modifier,
            ) { isMultiSelectMode ->
                Text(
                    text =
                        if (isMultiSelectMode) {
                            stringResource(
                                R.string.multi_select_count_format,
                                selectedCount,
                            )
                        } else {
                            stringResource(R.string.playlist_count_format, currentPlaylist.size)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            AnimatedContent(isMultiSelectMode) { selectMode ->
                if (selectMode) {
                    TextButton(
                        onClick = {
                            isMultiSelectMode = false
                            selectedMediaIds.clear()
                        },
                    ) {
                        stringResource(R.string.cancel)
                    }
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
                                contentDescription = stringResource(R.string.jump_to_playing),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                contentDescription = stringResource(R.string.clear_playlist),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = MaterialTheme.colorScheme.inversePrimary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.playlist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.inversePrimary,
                    )
                }
            }
        } else {
            val selectItemBackgroundColor =
                MaterialTheme.colorScheme.inversePrimary.copy(alpha = 0.2f)

            LazyColumn(
                state = scrollState,
                modifier =
                    Modifier
                        .animateContentSize()
                        .fillMaxWidth()
                        .weight(1f)
                        .drawBehind {
                            if (currentPlayingIndex >= 0) {
                                // 计算背景位置和尺寸
                                val itemHeight = 80.dp.toPx() // 64dp(封面) + 24dp(padding)
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
                                    topLeft = Offset(0f, backgroundTop),
                                    size = Size(size.width, itemHeight),
                                    cornerRadius = CornerRadius(cornerRadius),
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
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.batch_delete),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Button(
                    onClick = { showCreateDialog = true },
                    enabled = selectedCount > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = stringResource(R.string.create_playlist),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.create_playlist),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    var playlistName by remember { mutableStateOf("") }

//    OverlayDialog(
//        title = stringResource(R.string.create_playlist),
//        onDismissRequest = {
//            showCreateDialog = false
//            playlistName = ""
//        },
//        show = showCreateDialog,
//    ) {
//        Column(
//            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
//        ) {
//            TextField(
//                value = playlistName,
//                onValueChange = { playlistName = it },
//                label = stringResource(R.string.hint_input_playlist_name),
//                modifier = Modifier.fillMaxWidth(),
//                useLabelAsPlaceholder = true,
//            )
//            Spacer(modifier = Modifier.height(24.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.End,
//            ) {
//                TextButton(
//                    text = stringResource(R.string.cancel),
//                    onClick = { showCreateDialog = false },
//                )
//                Spacer(modifier = Modifier.size(12.dp))
//                TextButton(
//                    text = stringResource(R.string.create),
//                    onClick = {
//                        if (playlistName.isNotBlank()) {
//                            panelViewModel.createPlaylistWithMediaIds(
//                                name = playlistName,
//                                mediaIds = selectedMediaIds.toList(),
//                            ) { success ->
//                                if (success) {
//                                    selectedMediaIds.clear()
//                                    isMultiSelectMode = false
//                                    showCreateDialog = false
//                                }
//                            }
//                        }
//                    },
//                )
//            }
//        }
//    }
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
) {
    val metadata = item.mediaMetadata
    val title =
        remember(metadata) {
            metadata.title?.toString() ?: App.getInstance().getString(R.string.unknown_song)
        }
    val artist =
        remember(metadata) {
            metadata.artist?.toString() ?: App.getInstance().getString(R.string.unknown_artist)
        }
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
                ).height(80.dp)
                .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${index + 1}",
                Modifier.width(44.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
            )
            LandscapistImage(
                imageModel = {
                    artworkUri
                },
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surface),
                success = { state, painter ->
                    ShowOnIdleContent(true) {
                        Image(painter, contentDescription = null, contentScale = ContentScale.Crop)
                    }
                },
                failure = {
                    ShowOnIdleContent(true) {
                        Image(
                            painterResource(R.drawable.default_cover),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                        )
                    }
                },
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 12.dp,
                    ),
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W600,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    artist,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                )
            }
            AnimatedContent(isPlaying) { isPlaying ->
                if (isMultiSelectMode) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint =
                            animateColorAsState(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    Color.Transparent
                                },
                            ).value,
                    )
                } else if (isPlaying) {
                    Text(
                        "正在播放",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        formatTime(metadata.durationMs ?: 0L),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/**
 * 基于 Morph 进度的封面裁剪 Shape，每次 createOutline 时使用当时的 progress。
 * 注意：matrix 在每次调用时重置，避免累积变换。
 */
private class MorphClipShape(
    private val morph: Morph,
    private val progress: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val matrix = Matrix()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        val path = morph.toPath(progress = progress).asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
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
