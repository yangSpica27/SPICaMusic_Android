package me.spica27.spicamusic.ui.player.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel

/**
 * 当前播放列表页面 - 优雅克制版本
 *
 * 设计理念:
 * 1. 微妙的渐变背景 - 更低的透明度,不喧宾夺主
 * 2. 扁平化列表设计 - 去除卡片阴影,使用细腻分隔
 * 3. 左侧指示条高亮 - 当前播放项用彩色条+微妙发光替代尺寸变化
 * 4. 统一视觉语言 - 向专辑详情页等其他页面看齐
 * 5. 精简动画效果 - 只保留必要的颜色和透明度过渡
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrPlaylistPage(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
) {
    val panelViewModel: CurrentPlaylistPanelViewModel = koinViewModel()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedMediaIds = remember { mutableStateListOf<String>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val selectedCount by remember { derivedStateOf { selectedMediaIds.size } }

    // 追踪当前播放的索引
    val currentPlayingIndex =
        remember(currentPlaylist, currentMediaItem) {
            currentPlaylist.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }
        }

    // 提取当前播放歌曲封面的主色调,用于微妙的动态背景
    val currentCoverUri = currentMediaItem?.mediaMetadata?.artworkUri
    val dominantColor =
        rememberDominantColorFromUri(
            uri = currentCoverUri,
            fallbackColor = MaterialTheme.colorScheme.primary,
        )
    val animatedDominantColor =
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec = spring(stiffness = 150f),
            label = "dominantColor",
        )

    // 微妙的呼吸灯效果,用于当前播放项的指示条
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breatheAlpha",
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

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        // === 微妙的渐变背景 - 更低透明度 ===
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush =
                                Brush.verticalGradient(
                                    0f to animatedDominantColor.value.copy(alpha = 0.08f),
                                    0.4f to animatedDominantColor.value.copy(alpha = 0.04f),
                                    0.7f to animatedDominantColor.value.copy(alpha = 0.02f),
                                    1f to Color.Transparent,
                                ),
                        )
                    },
        )

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            // === 顶部信息栏 ===
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
                tonalElevation = 3.dp,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // 左侧: 计数信息
                    AnimatedContent(
                        isMultiSelectMode,
                        modifier = Modifier.weight(1f),
                    ) { selectMode ->
                        if (selectMode) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier =
                                        Modifier
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                CircleShape,
                                            ).padding(6.dp),
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.multi_select_count_format, selectedCount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = stringResource(R.string.items_selected),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            Column {
                                Text(
                                    text = stringResource(R.string.now_playinglist),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "${currentPlayingIndex + 1} / ${currentPlaylist.size}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // 右侧: 操作按钮
                    AnimatedContent(isMultiSelectMode) { selectMode ->
                        if (selectMode) {
                            TextButton(
                                onClick = {
                                    isMultiSelectMode = false
                                    selectedMediaIds.clear()
                                },
                                colors =
                                    ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                // 定位到当前播放
                                Surface(
                                    onClick = {
                                        if (currentPlayingIndex >= 0) {
                                            coroutineScope.launch {
                                                scrollState.animateScrollToItem(
                                                    index = currentPlayingIndex,
                                                    scrollOffset = -scrollState.layoutInfo.viewportSize.height / 2,
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationSearching,
                                        contentDescription = stringResource(R.string.jump_to_playing),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier =
                                            Modifier
                                                .padding(10.dp)
                                                .size(22.dp),
                                    )
                                }

                                // 多选模式
                                Surface(
                                    onClick = { isMultiSelectMode = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Checklist,
                                        contentDescription = stringResource(R.string.multi_select),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier =
                                            Modifier
                                                .padding(10.dp)
                                                .size(22.dp),
                                    )
                                }

                                // 清空列表
                                Surface(
                                    onClick = { showClearConfirmDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ClearAll,
                                        contentDescription = stringResource(R.string.clear_playlist),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier =
                                            Modifier
                                                .padding(10.dp)
                                                .size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // === 主列表区域 ===
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
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(96.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainer,
                                        RoundedCornerShape(24.dp),
                                    ).padding(20.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Text(
                            text = stringResource(R.string.playlist_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "添加歌曲开始播放",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                // 列表内容
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
                            .weight(1f),
                    contentPadding =
                        PaddingValues(
                            top = 12.dp,
                            bottom = if (isMultiSelectMode) 80.dp else 16.dp,
                            start = 16.dp,
                            end = 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        currentPlaylist,
                        key = { index, song -> itemKeys.getOrElse(index) { song.mediaId } },
                    ) { index, item ->
                        val isSelected = selectedMediaIds.contains(item.mediaId)
                        val isPlaying = currentMediaItem?.mediaId == item.mediaId

                        EnhancedPlaylistItemRow(
                            index = index,
                            modifier = Modifier.animateItem(),
                            item = { item },
                            isPlaying = isPlaying,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            breatheAlpha = if (isPlaying) breatheAlpha else 1f,
                            accentColor = animatedDominantColor.value,
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
                }
            }

            // === 底部多选操作栏 ===
            AnimatedVisibility(visible = isMultiSelectMode) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { showDeleteConfirmDialog = true },
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.batch_delete),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        Button(
                            onClick = { showCreateDialog = true },
                            enabled = selectedCount > 0,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                ),
                            contentPadding = PaddingValues(vertical = 14.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.create_playlist),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }

    // === 对话框 ===
    if (showCreateDialog) {
        RenameDialog(
            title = stringResource(R.string.create_playlist),
            initialName = "",
            onDismiss = { showCreateDialog = false },
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

    if (showDeleteConfirmDialog) {
        CurrentPlaylistConfirmDialog(
            title = stringResource(R.string.delete_selected_title),
            message = stringResource(R.string.delete_selected_message, selectedCount),
            confirmText = stringResource(R.string.delete),
            onConfirm = {
                val toRemove = selectedMediaIds.toList()
                toRemove.forEach { mediaId ->
                    viewModel.removeFromPlaylist(mediaId)
                }
                selectedMediaIds.clear()
                isMultiSelectMode = false
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false },
        )
    }

    if (showClearConfirmDialog) {
        CurrentPlaylistConfirmDialog(
            title = stringResource(R.string.clear_current_playlist_title),
            message = stringResource(R.string.clear_current_playlist_message),
            confirmText = stringResource(R.string.clear_playlist),
            onConfirm = {
                viewModel.pause()
                viewModel.updatePlaylist(emptyList())
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false },
        )
    }
}

// === 确认对话框 ===
@Composable
private fun CurrentPlaylistConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(confirmText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

// === 优雅扁平化播放列表项 ===
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedPlaylistItemRow(
    index: Int,
    item: () -> MediaItem,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    breatheAlpha: Float,
    accentColor: Color,
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

    // 动态颜色
    val backgroundColor =
        when {
            isSelected -> MaterialTheme.colorScheme.surfaceContainerHighest
            isPlaying -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainer
        }

    val textColor =
        when {
            isPlaying -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface
        }

    val animatedBackgroundColor =
        animateColorAsState(
            targetValue = backgroundColor,
            animationSpec = spring(stiffness = 300f),
            label = "backgroundColor",
        )
    val animatedTextColor =
        animateColorAsState(
            targetValue = textColor,
            animationSpec = spring(stiffness = 300f),
            label = "textColor",
        )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(animatedBackgroundColor.value, RoundedCornerShape(12.dp))
                .combinedClickHighlight(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号或播放指示器
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = accentColor.copy(alpha = breatheAlpha),
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    "${index + 1}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = animatedTextColor.value.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 封面 - 固定尺寸,不再动画缩放
        LandscapistImage(
            imageModel = { artworkUri },
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            success = { state, painter ->
                ShowOnIdleContent(true, delayMillis = 125) {
                    Image(
                        painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                }
            },
            failure = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            },
        )

        Spacer(modifier = Modifier.width(14.dp))

        // 歌曲信息
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                color = animatedTextColor.value,
            )
            Text(
                artist,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                color = animatedTextColor.value.copy(alpha = 0.7f),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 右侧指示器
        AnimatedContent(
            targetState = Triple(isMultiSelectMode, isSelected, isPlaying),
            label = "rightIndicator",
        ) { (multiSelect, selected, playing) ->
            when {
                multiSelect -> {
                    Box(
                        modifier =
                            Modifier
                                .size(28.dp)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    },
                                    CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                playing -> {
                    Text(
                        stringResource(R.string.now_playing_indicator),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor.copy(alpha = breatheAlpha),
                    )
                }
                else -> {
                    Text(
                        formatTime(metadata.durationMs ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = animatedTextColor.value.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}
