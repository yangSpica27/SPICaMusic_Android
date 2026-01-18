package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.compose.viewmodel.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

class PlaylistPanelController internal constructor(
    private val visibleState: MutableState<Boolean>,
) {
    val isVisible: Boolean
        get() = visibleState.value

    fun show() {
        visibleState.value = true
    }

    fun dismiss() {
        visibleState.value = false
    }

    fun toggle() {
        visibleState.value = !visibleState.value
    }
}

val LocalPlaylistPanelController =
    staticCompositionLocalOf<PlaylistPanelController> {
        error("PlaylistPanelController not provided")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentPlaylistPanelHost(
    visible: Boolean,
    @OptIn(ExperimentalMaterial3Api::class)
    onVisibleChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keepSheet by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            keepSheet = true
            sheetState.show()
        } else if (keepSheet) {
            sheetState.hide()
            keepSheet = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        content()
    }

    if (keepSheet) {
        ModalBottomSheet(
            onDismissRequest = { onVisibleChange(false) },
            sheetState = sheetState,
            sheetGesturesEnabled = true,
            containerColor = MiuixTheme.colorScheme.surface,
        ) {
            CurrentPlaylistPanelContent(
                onDismiss = { onVisibleChange(false) },
            )
        }
    }
}

@Composable
private fun CurrentPlaylistPanelContent(
    onDismiss: () -> Unit,
    playerViewModel: PlayerViewModel = LocalPlayerViewModel.current,
) {
    val panelViewModel: CurrentPlaylistPanelViewModel = koinViewModel()
    val currentPlaylist by playerViewModel.currentPlaylist.collectAsStateWithLifecycle()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

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
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
    ) {
        // 顶部标题栏
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isMultiSelectMode) "已选择 $selectedCount 项" else "当前播放列表",
                style = MiuixTheme.textStyles.title3,
                modifier = Modifier.weight(1f),
            )

            if (isMultiSelectMode) {
                TextButton(
                    text = "取消",
                    onClick = {
                        isMultiSelectMode = false
                        selectedMediaIds.clear()
                    },
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "关闭",
                )
            }
        }

        if (currentPlaylist.isEmpty()) {
            EmptyPlaylistState(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
            )
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(currentPlaylist, key = { it.mediaId }) { item ->
                    val isSelected = selectedMediaIds.contains(item.mediaId)
                    val isPlaying = currentMediaItem?.mediaId == item.mediaId
                    PlaylistMediaItemRow(
                        item = item,
                        isPlaying = isPlaying,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = isSelected,
                        onClick = {
                            if (isMultiSelectMode) {
                                toggleSelection(selectedMediaIds, item.mediaId)
                            } else {
                                playerViewModel.playById(item.mediaId)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                isMultiSelectMode = true
                            }
                            toggleSelection(selectedMediaIds, item.mediaId)
                        },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(if (isMultiSelectMode) 72.dp else 16.dp))
                }
            }
        }

        AnimatedVisibility(visible = isMultiSelectMode) {
            MultiSelectActionBar(
                enabled = selectedCount > 0,
                onDelete = {
                    val toRemove = selectedMediaIds.toList()
                    toRemove.forEach { mediaId ->
                        playerViewModel.removeFromPlaylist(mediaId)
                    }
                    selectedMediaIds.clear()
                    isMultiSelectMode = false
                },
                onCreatePlaylist = { showCreateDialog = true },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                panelViewModel.createPlaylistWithMediaIds(
                    name = name,
                    mediaIds = selectedMediaIds.toList(),
                ) { success ->
                    if (success) {
                        selectedMediaIds.clear()
                        isMultiSelectMode = false
                        showCreateDialog = false
                    }
                }
            },
        )
    }
}

@Composable
private fun EmptyPlaylistState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "播放列表为空",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistMediaItemRow(
    item: MediaItem,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val metadata = item.mediaMetadata
    val title = metadata.title?.toString() ?: "未知歌曲"
    val artist = metadata.artist?.toString() ?: "未知艺术家"
    val artworkUri = metadata.artworkUri

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isPlaying) {
                        MiuixTheme.colorScheme.secondaryContainer
                    } else {
                        MiuixTheme.colorScheme.surfaceContainer
                    },
                ).combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AudioCover(
                uri = artworkUri,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh),
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

@Composable
private fun MultiSelectActionBar(
    enabled: Boolean,
    onDelete: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onDelete,
            enabled = enabled,
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
            onClick = onCreatePlaylist,
            enabled = enabled,
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

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }
    val showState = remember { mutableStateOf(true) }

    WindowDialog(
        title = "创建歌单",
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = "请输入歌单名称",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "取消",
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.size(12.dp))
                TextButton(
                    text = "创建",
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreate(playlistName)
                            playlistName = ""
                        }
                    },
                )
            }
        }
    }
}

private fun toggleSelection(
    selectedMediaIds: MutableList<String>,
    mediaId: String,
) {
    if (selectedMediaIds.contains(mediaId)) {
        selectedMediaIds.remove(mediaId)
    } else {
        selectedMediaIds.add(mediaId)
    }
}
