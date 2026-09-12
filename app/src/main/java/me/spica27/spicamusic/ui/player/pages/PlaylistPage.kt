package me.spica27.spicamusic.ui.player.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import com.skydoves.landscapist.image.LandscapistImage
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.glass.LiquidGlassVariant
import me.spica27.spicamusic.ui.glass.liquidGlass
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.player.formatTime
import me.spica27.spicamusic.ui.playlistdetail.RenameDialog
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.DefaultMusicCover
import me.spica27.spicamusic.ui.widget.ParticleDissolveDefaults
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.particleDissolve
import me.spica27.spicamusic.utils.rememberDominantColorFromUri
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.PI
import kotlin.math.sin

/**
 * 当前播放列表页面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CurrPlaylistPage(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
) {
    val panelViewModel: CurrentPlaylistPanelViewModel = koinViewModel()
    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val itemKeys = remember(currentPlaylist) { createPlaylistItemKeys(currentPlaylist) }

    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedItemKeys = remember { mutableStateListOf<String>() }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    val pendingDeletionState = remember { mutableStateOf<PendingPlaylistDeletion?>(null) }
    val removalDispatchedState = remember { mutableStateOf(false) }
    val pendingDeletion = pendingDeletionState.value
    val removalDispatched = removalDispatchedState.value
    val dissolvingItemKeys = remember { mutableStateListOf<String>() }
    val completedDissolveItemKeys = remember { mutableStateListOf<String>() }
    val isDeletionPending = pendingDeletion != null

    val selectedCount by remember { derivedStateOf { selectedItemKeys.size } }
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = LocalReducedMotion.current

    val currentPlayingIndex =
        remember(currentPlaylist, currentMediaItem) {
            currentPlaylist.indexOfFirst { it.mediaId == currentMediaItem?.mediaId }
        }

    val currentCoverUri = currentMediaItem?.mediaMetadata?.artworkUri
    val dominantColor =
        rememberDominantColorFromUri(
            uri = currentCoverUri,
            fallbackColor = MaterialTheme.colorScheme.primary,
        )
    val animatedDominantColor =
        animateColorAsState(
            targetValue = dominantColor,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            label = "playlistAmbientColor",
        )

    // 相位值只在 Canvas 绘制阶段读取，播放动画不会带着整个列表每帧重组。
    val playbackPhase =
        if (reducedMotion || currentPlayingIndex < 0) {
            null
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "playlistPlayingBars")
            infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(1_400, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "playlistPlayingBarsPhase",
            )
        }
    val playbackPhaseProvider = remember(playbackPhase) { { playbackPhase?.value ?: 0.18f } }

    BackHandler(enabled = isMultiSelectMode && !isDeletionPending) {
        isMultiSelectMode = false
        selectedItemKeys.clear()
    }

    LaunchedEffect(itemKeys) {
        if (pendingDeletionState.value != null) return@LaunchedEffect

        val validKeys = itemKeys.toSet()
        selectedItemKeys.removeAll { it !in validKeys }
        if (selectedItemKeys.isEmpty()) {
            isMultiSelectMode = false
        }
    }

    LaunchedEffect(pendingDeletion) {
        val deletion = pendingDeletion ?: return@LaunchedEffect
        if (deletion.animatedItemKeys.isEmpty()) {
            viewModel.removeFromPlaylist(deletion.indices)
            removalDispatchedState.value = true
            return@LaunchedEffect
        }

        deletion.animatedItemKeys.forEachIndexed { index, itemKey ->
            if (index > 0) {
                delay(ParticleDissolveDefaults.WAVE_DELAY_MILLIS)
            }
            dissolvingItemKeys.add(itemKey)
        }
    }

    LaunchedEffect(pendingDeletion, completedDissolveItemKeys.size, removalDispatched) {
        val deletion = pendingDeletion ?: return@LaunchedEffect
        if (
            deletion.animatedItemKeys.isNotEmpty() &&
            !removalDispatched &&
            deletion.animatedItemKeys.all(completedDissolveItemKeys::contains)
        ) {
            viewModel.removeFromPlaylist(deletion.indices)
            removalDispatchedState.value = true
        }
    }

    LaunchedEffect(currentPlaylist.size, pendingDeletion, removalDispatched) {
        val deletion = pendingDeletion ?: return@LaunchedEffect
        if (removalDispatched && currentPlaylist.size <= deletion.expectedPlaylistSize) {
            pendingDeletionState.value = null
            removalDispatchedState.value = false
            dissolvingItemKeys.clear()
            completedDissolveItemKeys.clear()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            pendingDeletionState.value?.takeIf { !removalDispatchedState.value }?.let { deletion ->
                viewModel.removeFromPlaylist(deletion.indices)
            }
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.surface

    var headerSize: IntSize by remember { mutableStateOf(IntSize.Zero) }

    val density = LocalDensity.current

    val hazeState = rememberHazeState()

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(backgroundColor),
    ) {
        if (currentPlaylist.isEmpty()) {
            QueueEmptyState(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(Spacing.ExtraLarge),
            )
        } else {
            val allItemsSelected = selectedCount == itemKeys.size
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = scrollState,
                    userScrollEnabled = !isDeletionPending,
                    modifier =
                        Modifier
                            .hazeSource(hazeState)
                            .fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            top =
                                Spacing.ExtraSmall +
                                    with(density) {
                                        headerSize.height.toDp()
                                    },
                            bottom = if (isMultiSelectMode) 104.dp else Spacing.ExtraLarge,
                            start = Spacing.Medium,
                            end = Spacing.Medium,
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    itemsIndexed(
                        currentPlaylist,
                        key = { index, _ -> itemKeys[index] },
                        contentType = { _, _ -> "playlist_item" },
                    ) { index, item ->
                        val itemKey = itemKeys[index]
                        val isSelected = selectedItemKeys.contains(itemKey)
                        val isPlaying = index == currentPlayingIndex
                        val isDissolving = dissolvingItemKeys.contains(itemKey)

                        EnhancedPlaylistItemRow(
                            index = index,
                            modifier =
                                Modifier.animateItem(
                                    fadeInSpec = ListItemFadeInSpec,
                                    placementSpec =
                                        if (reducedMotion) {
                                            snap()
                                        } else {
                                            spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                            )
                                        },
                                    fadeOutSpec = ListItemFadeOutSpec,
                                ),
                            item = { item },
                            isPlaying = isPlaying,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = isSelected,
                            isDissolving = isDissolving,
                            enabled = !isDeletionPending,
                            reducedMotion = reducedMotion,
                            playbackPhaseProvider = playbackPhaseProvider,
                            accentColor = dominantColor,
                            onClick = {
                                if (isMultiSelectMode) {
                                    if (isSelected) {
                                        selectedItemKeys.remove(itemKey)
                                    } else {
                                        selectedItemKeys.add(itemKey)
                                    }
                                } else {
                                    viewModel.playByMediaStoreId(item.mediaId)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    isMultiSelectMode = true
                                }
                                if (selectedItemKeys.contains(itemKey)) {
                                    selectedItemKeys.remove(itemKey)
                                } else {
                                    selectedItemKeys.add(itemKey)
                                }
                            },
                            onDissolveComplete = {
                                if (!completedDissolveItemKeys.contains(itemKey)) {
                                    completedDissolveItemKeys.add(itemKey)
                                }
                            },
                        )
                    }
                }
                QueueOverviewHeader(
                    currentItem = currentMediaItem,
                    currentPlayingIndex = currentPlayingIndex,
                    itemCount = currentPlaylist.size,
                    selectedCount = selectedCount,
                    allItemsSelected = allItemsSelected,
                    isMultiSelectMode = isMultiSelectMode,
                    enabled = !isDeletionPending,
                    reducedMotion = reducedMotion,
                    playbackPhaseProvider = playbackPhaseProvider,
                    onJumpToPlaying = {
                        if (currentPlayingIndex >= 0) {
                            coroutineScope.launch {
                                scrollState.animateScrollToItem(
                                    index = currentPlayingIndex,
                                    scrollOffset = -scrollState.layoutInfo.viewportSize.height / 2,
                                )
                            }
                        }
                    },
                    onEnterMultiSelect = { isMultiSelectMode = true },
                    onToggleSelectAll = {
                        selectedItemKeys.clear()
                        if (!allItemsSelected) {
                            selectedItemKeys.addAll(itemKeys)
                        }
                    },
                    onCancelMultiSelect = {
                        isMultiSelectMode = false
                        selectedItemKeys.clear()
                    },
                    onClearPlaylist = { showClearConfirmDialog = true },
                    modifier =
                        Modifier
                            .onGloballyPositioned {
                                headerSize = it.size
                            }.liquidGlass(
                                hazeState,
                                variant = LiquidGlassVariant.TopBar,
                                shape = RectangleShape,
                            ).padding(
                                horizontal = Spacing.Large,
                                vertical = Spacing.Medium,
                            ),
                )
            }

            PlaylistSelectionBar(
                visible = isMultiSelectMode,
                selectedCount = selectedCount,
                reducedMotion = reducedMotion,
                onDelete = { showDeleteConfirmDialog = true },
                onCreatePlaylist = { showCreateDialog = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            )
        }
    }

    if (showCreateDialog) {
        RenameDialog(
            title = stringResource(R.string.create_playlist),
            initialName = "",
            onDismiss = { showCreateDialog = false },
            onConfirm = {
                if (it.isNotBlank()) {
                    panelViewModel.createPlaylistWithMediaIds(
                        name = it,
                        mediaIds =
                            itemKeys.mapIndexedNotNull { index, itemKey ->
                                currentPlaylist[index].mediaId.takeIf {
                                    selectedItemKeys.contains(itemKey)
                                }
                            },
                    ) { success ->
                        if (success) {
                            selectedItemKeys.clear()
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
            icon = Icons.Default.Delete,
            onConfirm = {
                val selectedKeySet = selectedItemKeys.toSet()
                val indices = selectedPlaylistIndices(itemKeys, selectedKeySet)
                val animatedItemKeys =
                    if (reducedMotion) {
                        emptyList()
                    } else {
                        scrollState.layoutInfo.visibleItemsInfo.mapNotNull { itemInfo ->
                            itemKeys.getOrNull(itemInfo.index)?.takeIf { it in selectedKeySet }
                        }
                    }

                if (indices.isNotEmpty()) {
                    dissolvingItemKeys.clear()
                    completedDissolveItemKeys.clear()
                    removalDispatchedState.value = false
                    pendingDeletionState.value =
                        PendingPlaylistDeletion(
                            indices = indices,
                            animatedItemKeys = animatedItemKeys,
                            expectedPlaylistSize = currentPlaylist.size - indices.size,
                        )
                }

                selectedItemKeys.clear()
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
            icon = Icons.Default.DeleteSweep,
            onConfirm = {
                viewModel.pause()
                viewModel.updatePlaylist(emptyList())
                showClearConfirmDialog = false
            },
            onDismiss = { showClearConfirmDialog = false },
        )
    }
}

@Immutable
private data class PendingPlaylistDeletion(
    val indices: List<Int>,
    val animatedItemKeys: List<String>,
    val expectedPlaylistSize: Int,
)

internal fun createPlaylistItemKeys(items: List<MediaItem>): List<String> {
    val occurrences = mutableMapOf<String, Int>()
    return items.map { item ->
        val occurrence = occurrences.getOrDefault(item.mediaId, 0) + 1
        occurrences[item.mediaId] = occurrence
        "${item.mediaId}#$occurrence"
    }
}

internal fun selectedPlaylistIndices(
    itemKeys: List<String>,
    selectedItemKeys: Set<String>,
): List<Int> = itemKeys.indices.filter { itemKeys[it] in selectedItemKeys }

@Composable
private fun QueueOverviewHeader(
    currentItem: MediaItem?,
    currentPlayingIndex: Int,
    itemCount: Int,
    selectedCount: Int,
    allItemsSelected: Boolean,
    isMultiSelectMode: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    playbackPhaseProvider: () -> Float,
    onJumpToPlaying: () -> Unit,
    onEnterMultiSelect: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onCancelMultiSelect: () -> Unit,
    onClearPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isMultiSelectMode,
        transitionSpec = {
            if (reducedMotion) {
                fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized)) togetherWith
                    fadeOut(tween(durationMillis = 110, easing = EaseOutEmphasized))
            } else {
                (
                    fadeIn(tween(durationMillis = 200, easing = EaseOutEmphasized)) +
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 240, easing = EaseOutEmphasized),
                            initialOffsetX = { it / 10 },
                        )
                ) togetherWith
                    (
                        fadeOut(tween(durationMillis = 120, easing = EaseOutEmphasized)) +
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 160, easing = EaseOutEmphasized),
                                targetOffsetX = { -it / 12 },
                            )
                    )
            }
        },
        modifier =
            modifier
                .fillMaxWidth(),
        label = "playlistHeaderMode",
    ) { selectMode ->
        if (selectMode) {
            SelectionOverviewCard(
                selectedCount = selectedCount,
                itemCount = itemCount,
                allItemsSelected = allItemsSelected,
                enabled = enabled,
                reducedMotion = reducedMotion,
                onToggleSelectAll = onToggleSelectAll,
                onCancel = onCancelMultiSelect,
            )
        } else {
            NowPlayingOverviewCard(
                currentItem = currentItem,
                currentPlayingIndex = currentPlayingIndex,
                itemCount = itemCount,
                enabled = enabled,
                reducedMotion = reducedMotion,
                playbackPhaseProvider = playbackPhaseProvider,
                onJumpToPlaying = onJumpToPlaying,
                onEnterMultiSelect = onEnterMultiSelect,
                onClearPlaylist = onClearPlaylist,
            )
        }
    }
}

@Composable
private fun NowPlayingOverviewCard(
    currentItem: MediaItem?,
    currentPlayingIndex: Int,
    itemCount: Int,
    enabled: Boolean,
    reducedMotion: Boolean,
    playbackPhaseProvider: () -> Float,
    onJumpToPlaying: () -> Unit,
    onEnterMultiSelect: () -> Unit,
    onClearPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metadata = currentItem?.mediaMetadata
    val title = metadata?.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata?.artist?.toString() ?: stringResource(R.string.unknown_artist)
    val positionText =
        if (currentPlayingIndex >= 0) {
            "${currentPlayingIndex + 1} / $itemCount"
        } else {
            stringResource(R.string.songs_count_format, itemCount)
        }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            QueueArtwork(
                item = currentItem,
                playbackPhaseProvider = playbackPhaseProvider,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Text(
                        text = stringResource(R.string.now_playing_indicator),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.82f),
                    ) {
                        Text(
                            text = positionText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            QueueActionPill(
                text = stringResource(R.string.locate_playing),
                icon = Icons.Default.LocationSearching,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                enabled = enabled && currentPlayingIndex >= 0,
                reducedMotion = reducedMotion,
                onClick = onJumpToPlaying,
                onClickLabel = stringResource(R.string.jump_to_playing),
                modifier = Modifier.weight(1.15f),
            )
            QueueActionPill(
                text = stringResource(R.string.multi_select),
                icon = Icons.Default.Checklist,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                enabled = enabled,
                reducedMotion = reducedMotion,
                onClick = onEnterMultiSelect,
                modifier = Modifier.weight(0.9f),
            )
            QueueIconAction(
                label = stringResource(R.string.clear_playlist),
                icon = Icons.Default.DeleteSweep,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                enabled = enabled,
                reducedMotion = reducedMotion,
                onClick = onClearPlaylist,
            )
        }
    }
}

@Composable
private fun SelectionOverviewCard(
    selectedCount: Int,
    itemCount: Int,
    allItemsSelected: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    onToggleSelectAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedCount,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized)) togetherWith
                            fadeOut(tween(durationMillis = 100, easing = EaseOutEmphasized))
                    },
                    label = "playlistSelectedCount",
                ) { count ->
                    Text(
                        text = stringResource(R.string.multi_select_count_format, count),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "$selectedCount / $itemCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            QueueActionPill(
                text =
                    stringResource(
                        if (allItemsSelected) R.string.deselect_all else R.string.select_all,
                    ),
                icon = Icons.Default.SelectAll,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                enabled = enabled,
                reducedMotion = reducedMotion,
                onClick = onToggleSelectAll,
                modifier = Modifier.weight(1f),
            )
            QueueActionPill(
                text = stringResource(R.string.cancel),
                icon = Icons.Default.Close,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                enabled = enabled,
                reducedMotion = reducedMotion,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QueueActionPill(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    onClickLabel: String = text,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.96f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1_100f,
            ),
        label = "queueActionPressScale",
    )
    Row(
        modifier =
            modifier
                .height(48.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    alpha = if (enabled) 1f else 0.38f
                }.clip(CircleShape)
                .background(containerColor)
                .clickHighlight(
                    enabled = enabled,
                    onClickLabel = onClickLabel,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    onClick = onClick,
                ).padding(horizontal = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun QueueIconAction(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 1_100f),
        label = "queueIconActionPressScale",
    )
    Box(
        modifier =
            modifier
                .size(48.dp)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    alpha = if (enabled) 1f else 0.38f
                }.clip(CircleShape)
                .background(containerColor)
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }.clickHighlight(
                    enabled = enabled,
                    onClickLabel = label,
                    role = Role.Button,
                    interactionSource = interactionSource,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun QueueArtwork(
    item: MediaItem?,
    playbackPhaseProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val artworkUri = item?.mediaMetadata?.artworkUri
    val coverShape = Shapes.LargeCornerBasedShape
    val surfaceColor = MaterialTheme.colorScheme.surface
    Box(
        modifier =
            modifier
                .size(72.dp),
    ) {
        LandscapistImage(
            imageModel = { artworkUri },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(coverShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = coverShape,
                    ),
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            failure = { DefaultMusicCover() },
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(27.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, surfaceColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PlayingBars(
                color = MaterialTheme.colorScheme.onPrimary,
                progressProvider = playbackPhaseProvider,
                modifier = Modifier.size(width = 13.dp, height = 14.dp),
            )
        }
    }
}

@Composable
private fun QueueEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .size(128.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = Shapes.ExtraLarge1CornerBasedShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border =
                    BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                    ),
                tonalElevation = 3.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.Medium))
        Text(
            text = stringResource(R.string.playlist_empty),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.ExtraSmall))
        Text(
            text = stringResource(R.string.playlist_empty_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlaylistSelectionBar(
    visible: Boolean,
    selectedCount: Int,
    reducedMotion: Boolean,
    onDelete: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enter =
        if (reducedMotion) {
            fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized))
        } else {
            fadeIn(tween(durationMillis = 180, easing = EaseOutEmphasized)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 240, easing = EaseOutEmphasized),
                    initialOffsetY = { it / 2 },
                )
        }
    val exit =
        if (reducedMotion) {
            fadeOut(tween(durationMillis = 110, easing = EaseOutEmphasized))
        } else {
            fadeOut(tween(durationMillis = 120, easing = EaseOutEmphasized)) +
                slideOutVertically(
                    animationSpec = tween(durationMillis = 160, easing = EaseOutEmphasized),
                    targetOffsetY = { it / 3 },
                )
        }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = enter,
        exit = exit,
        label = "playlistSelectionActions",
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.ExtraLarge1CornerBasedShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.98f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border =
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f),
                ),
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
        ) {
            Row(
                modifier = Modifier.padding(Spacing.Small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Button(
                    onClick = onDelete,
                    enabled = selectedCount > 0,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                    shape = CircleShape,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                            disabledContentColor = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f),
                        ),
                    contentPadding = PaddingValues(horizontal = Spacing.Medium),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(
                        text = stringResource(R.string.batch_delete),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onCreatePlaylist,
                    enabled = selectedCount > 0,
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(52.dp),
                    shape = CircleShape,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    contentPadding = PaddingValues(horizontal = Spacing.Medium),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
                    Text(
                        text = stringResource(R.string.create_playlist),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayingBars(
    color: Color,
    progressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val progress = progressProvider().coerceIn(0f, 1f)
        val barWidth = size.width * 0.18f
        val gap = (size.width - barWidth * 3f) / 2f
        repeat(3) { index ->
            val phase = progress + index * 0.23f
            val wave = ((sin(phase * PI * 2.0) + 1.0) * 0.5).toFloat()
            val barHeight = size.height * (0.34f + wave * 0.58f)
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(left, (size.height - barHeight) / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

// === 确认对话框 ===
@Composable
private fun CurrentPlaylistConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
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
        shape = Shapes.ExtraLarge1CornerBasedShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

@Composable
private fun EnhancedPlaylistItemRow(
    index: Int,
    item: () -> MediaItem,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    isDissolving: Boolean = false,
    enabled: Boolean = true,
    reducedMotion: Boolean,
    playbackPhaseProvider: () -> Float,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDissolveComplete: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val metadata = item.invoke().mediaMetadata
    val title = metadata.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata.artist?.toString() ?: stringResource(R.string.unknown_artist)
    val artworkUri = metadata.artworkUri
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = Shapes.LargeCornerBasedShape
    val primaryColor = MaterialTheme.colorScheme.primary

    val backgroundColor =
        when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
            isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }

    val animatedBackgroundColor =
        animateColorAsState(
            targetValue = backgroundColor,
            animationSpec =
                tween(
                    durationMillis = if (reducedMotion) 0 else 220,
                    easing = EaseOutEmphasized,
                ),
            label = "playlistItemBackground",
        )
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !reducedMotion) 0.985f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1_200f,
            ),
        label = "playlistItemPressScale",
    )
    val multiSelectLabel = stringResource(R.string.multi_select)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                    alpha = if (enabled) 1f else 0.68f
                }.particleDissolve(
                    isDissolving = isDissolving,
                    onComplete = onDissolveComplete,
                ).clip(shape)
                .background(animatedBackgroundColor.value)
                .drawBehind {
                    if (isPlaying && !isSelected) {
                        drawRect(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            primaryColor.copy(alpha = 0.16f),
                                            accentColor.copy(alpha = 0.04f),
                                            Color.Transparent,
                                        ),
                                ),
                        )
                    }
                }.semantics(mergeDescendants = true) {
                    role = Role.Button
                    if (isMultiSelectMode) {
                        selected = isSelected
                    }
                }.combinedClickHighlight(
                    enabled = enabled,
                    role = Role.Button,
                    onLongClickLabel = multiSelectLabel,
                    interactionSource = interactionSource,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = Spacing.Medium, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (isPlaying) {
                PlayingBars(
                    color = MaterialTheme.colorScheme.primary,
                    progressProvider = playbackPhaseProvider,
                    modifier = Modifier.size(width = 20.dp, height = 18.dp),
                )
            } else {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        LandscapistImage(
            imageModel = { artworkUri },
            modifier =
                Modifier
                    .size(58.dp)
                    .clip(Shapes.MediumCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (isPlaying) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                shape = Shapes.MediumCornerBasedShape,
                            )
                        } else {
                            Modifier
                        },
                    ),
            success = { _, painter ->
                ShowOnIdleContent(true, delayMillis = 125) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
            failure = { DefaultMusicCover() },
        )

        Spacer(modifier = Modifier.width(Spacing.Medium))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying || isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.Small))

        AnimatedContent(
            targetState = Triple(isMultiSelectMode, isSelected, isPlaying),
            transitionSpec = {
                fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized)) togetherWith
                    fadeOut(tween(durationMillis = 100, easing = EaseOutEmphasized))
            },
            label = "playlistItemTrailingState",
        ) { (multiSelect, selected, playing) ->
            when {
                multiSelect -> {
                    val checkScale by animateFloatAsState(
                        targetValue = if (selected) 1f else 0.82f,
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = 1_000f,
                            ),
                        label = "playlistSelectionCheckScale",
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(32.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }.background(
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            Color.Transparent
                                        },
                                    shape = CircleShape,
                                ).border(
                                    width = 1.5.dp,
                                    color =
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.62f)
                                        },
                                    shape = CircleShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                playing -> {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = stringResource(R.string.now_playing_indicator),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            modifier =
                                Modifier.padding(
                                    horizontal = Spacing.Small,
                                    vertical = Spacing.ExtraSmall,
                                ),
                        )
                    }
                }

                else -> {
                    Text(
                        text = formatTime(metadata.durationMs ?: 0L),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
