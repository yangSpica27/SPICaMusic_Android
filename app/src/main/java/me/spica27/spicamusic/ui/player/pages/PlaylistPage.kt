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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import me.spica27.spicamusic.ui.playlistdetail.RenameDialog
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.floor

/**
 * 当前播放列表页面
 */
@OptIn(ExperimentalMaterial3Api::class)
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
        modifier = modifier,
    ) {
        // 顶部标题和操作
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .animateContentSize()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
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
                            "${currentPlayingIndex + 1} / ${currentPlaylist.size}"
                        },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier =
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(50),
                            ).padding(
                                horizontal = 12.dp,
                                vertical = 6.dp,
                            ),
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
                        Text(stringResource(R.string.cancel))
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(
                                            topStartPercent = 50,
                                            bottomStartPercent = 50,
                                            topEndPercent = 15,
                                            bottomEndPercent = 15,
                                        ),
                                    ).clip(
                                        RoundedCornerShape(
                                            topStartPercent = 50,
                                            bottomStartPercent = 50,
                                            topEndPercent = 15,
                                            bottomEndPercent = 15,
                                        ),
                                    ).clickable {
                                        if (currentPlayingIndex >= 0) {
                                            coroutineScope.launch {
                                                scrollState.animateScrollToItem(
                                                    index = currentPlayingIndex,
                                                    scrollOffset = -scrollState.layoutInfo.viewportSize.height / 2,
                                                )
                                            }
                                        }
                                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationSearching,
                                contentDescription = stringResource(R.string.jump_to_playing),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Box(
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(15),
                                    ).clickable {
                                        isMultiSelectMode = true
                                    }.padding(horizontal = 8.dp, vertical = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = stringResource(R.string.clear_playlist),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier =
                                Modifier
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                        RoundedCornerShape(
                                            topStartPercent = 15,
                                            bottomStartPercent = 15,
                                            topEndPercent = 50,
                                            bottomEndPercent = 50,
                                        ),
                                    ).clip(
                                        RoundedCornerShape(
                                            topStartPercent = 15,
                                            bottomStartPercent = 15,
                                            topEndPercent = 50,
                                            bottomEndPercent = 50,
                                        ),
                                    ).clickable {
                                        viewModel.pause()
                                        viewModel.updatePlaylist(emptyList())
                                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = stringResource(R.string.clear_playlist),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp),
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

            // 稳定的 item key：以 mediaId 为主，重复歌曲追加出现序号去重；
            // 不含 index，删除/重排时其余 item 的 key 不变，animateItem 动画才能正常工作
            val itemKeys =
                remember(currentPlaylist) {
                    val seen = HashMap<String, Int>()
                    currentPlaylist.map { item ->
                        val n = seen.merge(item.mediaId, 1, Int::plus)!!
                        "${item.mediaId}#$n"
                    }
                }

            LazyColumn(
                state = scrollState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .drawBehind {
                            if (currentPlayingIndex < 0) return@drawBehind
                            val layoutInfo = scrollState.layoutInfo
                            val visible = layoutInfo.visibleItemsInfo
                            if (visible.isEmpty()) return@drawBehind

                            val spacing = 8.dp.toPx()
                            val cornerRadius = 12.dp.toPx()
                            val floorIndex = floor(animatedPlayingIndex).toInt()
                            val fraction = animatedPlayingIndex - floorIndex
                            val item0 = visible.firstOrNull { it.index == floorIndex }
                            val item1 = visible.firstOrNull { it.index == floorIndex + 1 }

                            // 基于真实布局偏移插值计算高亮背景位置，目标行不可见时不绘制
                            val topInViewport: Float
                            val itemHeight: Float
                            when {
                                item0 != null && item1 != null -> {
                                    topInViewport =
                                        item0.offset + (item1.offset - item0.offset) * fraction
                                    itemHeight = item0.size + (item1.size - item0.size) * fraction
                                }

                                item0 != null -> {
                                    topInViewport = item0.offset + (item0.size + spacing) * fraction
                                    itemHeight = item0.size.toFloat()
                                }

                                item1 != null -> {
                                    topInViewport =
                                        item1.offset - (item1.size + spacing) * (1f - fraction)
                                    itemHeight = item1.size.toFloat()
                                }

                                else -> return@drawBehind
                            }

                            val backgroundTop = topInViewport - layoutInfo.viewportStartOffset
                            if (backgroundTop + itemHeight < 0f || backgroundTop > size.height) return@drawBehind

                            drawRoundRect(
                                color = selectItemBackgroundColor,
                                topLeft = Offset(0f + 16.dp.toPx(), backgroundTop),
                                size = Size(size.width - 32.dp.toPx(), itemHeight),
                                cornerRadius = CornerRadius(cornerRadius),
                                style = Fill,
                            )
                        },
                contentPadding =
                    PaddingValues(
                        vertical = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(
                    currentPlaylist,
                    key = { index, song -> itemKeys.getOrElse(index) { song.mediaId } },
                ) { index, item ->
                    val isSelected = selectedMediaIds.contains(item.mediaId)
                    val isPlaying = currentMediaItem?.mediaId == item.mediaId
                    PlaylistItemRow(
                        index = index,
                        modifier = Modifier.animateItem(),
                        item = { item },
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
                        .padding(vertical = 12.dp, horizontal = 16.dp),
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
                    shape = RoundedCornerShape(15),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
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
                    shape = RoundedCornerShape(15),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = stringResource(R.string.create_playlist),
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

    if (showCreateDialog) {
        RenameDialog(
            title = stringResource(R.string.create_playlist),
            initialName = "",
            onDismiss = {
                showCreateDialog = false
            },
            onConfirm = {
                if (it.isNotBlank()) {
                    panelViewModel.createPlaylistWithMediaIds(
                        name = it,
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

/**
 * 播放列表项
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistItemRow(
    index: Int,
    item: () -> MediaItem,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    val metadata = item.invoke().mediaMetadata
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
                .padding(vertical = 8.dp, horizontal = 16.dp),
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
                    ShowOnIdleContent(true, delayMillis = 125) {
                        Image(painter, contentDescription = null, contentScale = ContentScale.Crop)
                    }
                },
                failure = {
                    Image(
                        painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
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
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    artist,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface,
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
                        stringResource(R.string.now_playing_indicator),
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
