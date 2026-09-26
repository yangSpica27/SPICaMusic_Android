package me.spica27.spicamusic.ui.player.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxDefaults
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.ui.navigation.ConfirmationDialogRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.TextInputDialogRoute
import me.spica27.spicamusic.ui.player.CurrentPlaylistPanelViewModel
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.player.PlayerViewModel
import me.spica27.spicamusic.ui.player.formatTime
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.theme.ScaleEnterFrom
import me.spica27.spicamusic.ui.theme.ScaleExitTo
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.ParticleDissolveDefaults
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.particleDissolve
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import me.spica27.spicamusic.utils.Nav3Transitions
import org.koin.compose.viewmodel.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.PI
import kotlin.math.sin

/** 顶栏高度（不含状态栏） */
private val TopBarHeight = 56.dp

/** 大标题完全收进顶栏所需的滚动距离 */
private val MastheadCollapseDistance = 140.dp

/** 列表头部的固定项：刊头 + 操作行 */
private const val QUEUE_HEADER_COUNT = 2

/** 首屏元素在编排中的槽位：刊头=0 操作行=1 歌曲行从 2 开始 */
private const val ENTRANCE_ROW_BASE = 2

/** 参与入场编排的最大歌曲行数（之后出现的行走 animateItem 淡入） */
private const val ENTRANCE_MAX_ROW = 8

/** 移除后可撤销的时间窗 */
private const val UNDO_WINDOW_MILLIS = 4_000L

/** 不可撤销提示的停留时间 */
private const val NOTICE_MILLIS = 2_500L

/** 已下发的操作等待播放器回传新列表的最长时间，超时后以播放器为准 */
private const val OPTIMISTIC_TIMEOUT_MILLIS = 1_500L

/** 多选栏还没测量出高度时的底部留白 */
private val SelectionBarFallbackHeight = 112.dp

/** 普通模式底部留白：最后一行不被撤销提示挡住 */
private val ListBottomSpace = 88.dp

/** 播放指示条暂停时的静止高度 */
private val RestBarHeights = floatArrayOf(0.45f, 0.8f, 0.6f)

private val ItemPlacementSpringSpec =
    spring<IntOffset>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

/**
 * 当前播放列表页面
 *
 * @param chromeColor 刊头收起后顶栏的填充色
 * @param contentWindowInsets 页面需要自行避让的系统栏（宿主已处理的传 0）
 */
@Composable
fun CurrPlaylistPage(
    onNavigateBack: () -> Unit,
    navigationIcon: ImageVector,
    navigationContentDescription: String,
    chromeColor: Color,
    contentWindowInsets: WindowInsets,
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = LocalPlayerViewModel.current,
) {
    val backStack = LocalBackStack.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val reducedMotion = LocalReducedMotion.current
    val panelViewModel: CurrentPlaylistPanelViewModel = koinViewModel()

    val currentPlaylist by viewModel.currentPlaylist.collectAsStateWithLifecycle()
    val currentMediaItem by viewModel.currentMediaItem.collectAsStateWithLifecycle()
    val reportedIndex by viewModel.currentMediaItemIndex.collectAsStateWithLifecycle()
    val playMode by viewModel.playMode.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    val itemKeys = remember(currentPlaylist) { createPlaylistItemKeys(currentPlaylist) }
    val baseEntries =
        remember(currentPlaylist, itemKeys) {
            currentPlaylist.mapIndexed { index, item -> QueueEntry(itemKeys[index], item) }
        }
    val currentKey =
        itemKeys.getOrNull(
            resolveCurrentQueueIndex(currentPlaylist, reportedIndex, currentMediaItem?.mediaId),
        )
    val latestBase = rememberUpdatedState(baseEntries)
    val latestCurrentKey = rememberUpdatedState(currentKey)

    val scope = rememberCoroutineScope()
    val editor = remember(viewModel) { QueueEditor(viewModel, scope, latestBase) }
    val entries = editor.visibleEntries
    val isMultiSelectMode = editor.isMultiSelectMode
    val pendingDeletion = editor.pendingDeletion
    val shuffle = playMode == PlayMode.SHUFFLE

    // 打开时直接停在当前歌曲附近（保留上一首作为上下文），每次进入页面都会重新计算
    val initialCurrent = remember { baseEntries.indexOfFirst { it.key == currentKey } }
    val entranceAnchor = if (initialCurrent >= 3) initialCurrent - 1 else 0
    val listState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = if (entranceAnchor > 0) QUEUE_HEADER_COUNT + entranceAnchor else 0,
        )

    // 入场编排只在页面首次呈现时播放一次（宿主的 ShowOnIdleContent 已经延后了首帧）
    var entrancePlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        entrancePlayed = true
    }

    val insets = contentWindowInsets.asPaddingValues()
    val insetTop = insets.calculateTopPadding()
    val insetBottom = insets.calculateBottomPadding()
    val density = LocalDensity.current
    var selectionBarHeightPx by remember { mutableIntStateOf(0) }
    val listBottomPadding =
        if (isMultiSelectMode) {
            val barHeight = with(density) { selectionBarHeightPx.toDp() }
            (if (barHeight > 0.dp) barHeight else insetBottom + SelectionBarFallbackHeight) + Spacing.Large
        } else {
            insetBottom + ListBottomSpace
        }

    val reorderState =
        rememberReorderableLazyListState(
            lazyListState = listState,
            scrollThresholdPadding = PaddingValues(top = insetTop + TopBarHeight, bottom = insetBottom),
        ) { from, to ->
            if (editor.moveInDrag(from.key, to.key)) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }
        }
    val canReorder = !isMultiSelectMode && !shuffle && pendingDeletion == null && entries.size > 1

    // 当前歌曲不在可视区时，顶栏弹出「定位」胶囊
    val showLocate by remember(listState, editor, reorderState) {
        derivedStateOf {
            val key = latestCurrentKey.value ?: return@derivedStateOf false
            if (editor.isMultiSelectMode || reorderState.isAnyItemDragging) return@derivedStateOf false
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == key }
            item == null || item.offset < 0 || item.offset + item.size > info.viewportEndOffset - info.afterContentPadding
        }
    }

    // 播放器回传的列表与预期一致即放行；对不上时等一会儿，仍不一致就以播放器为准
    LaunchedEffect(currentPlaylist, editor.optimistic) {
        val expected = editor.optimistic ?: return@LaunchedEffect
        if (currentPlaylist.map { it.mediaId } != expected.expectedMediaIds) {
            delay(OPTIMISTIC_TIMEOUT_MILLIS)
        }
        if (editor.optimistic === expected) editor.optimistic = null
    }

    // 待撤销的歌曲被自动切到（播完上一首 / 通知栏切歌）时立刻提交移除
    LaunchedEffect(currentKey) {
        if (currentKey != null && currentKey in editor.hiddenKeys) {
            editor.commitPendingRemovals()
        }
    }

    LaunchedEffect(itemKeys) {
        if (editor.pendingDeletion == null) editor.retainSelection(itemKeys.toSet())
    }

    // 提示到时：可撤销的提示过期即提交
    val notice = editor.notice
    LaunchedEffect(notice?.id) {
        val shown = notice ?: return@LaunchedEffect
        delay(if (shown.undoKeys != null) UNDO_WINDOW_MILLIS else NOTICE_MILLIS)
        if (editor.notice?.id == shown.id) {
            if (shown.undoKeys != null) editor.commitPendingRemovals()
            editor.notice = null
        }
    }

    // 批量删除：可视行按波次粒子消散；兜底超时防止被回收的行让列表一直锁着
    LaunchedEffect(pendingDeletion) {
        val deletion = pendingDeletion ?: return@LaunchedEffect
        deletion.animatedItemKeys.forEachIndexed { index, key ->
            if (index > 0) delay(ParticleDissolveDefaults.WAVE_DELAY_MILLIS)
            editor.dissolvingKeys += key
        }
        delay(ParticleDissolveDefaults.DURATION_MILLIS + 600L)
        editor.finishDeletion(deletion)
    }
    LaunchedEffect(pendingDeletion, editor.completedDissolveKeys.size) {
        val deletion = pendingDeletion ?: return@LaunchedEffect
        if (deletion.animatedItemKeys.all(editor.completedDissolveKeys::contains)) {
            editor.finishDeletion(deletion)
        }
    }

    // 离开页面（Pager 翻回播放页即销毁）时把所有未提交的移除交给播放器
    DisposableEffect(editor) {
        onDispose { editor.flush() }
    }

    BackHandler(enabled = isMultiSelectMode && pendingDeletion == null) {
        editor.exitMultiSelect()
    }

    val locateCurrent: () -> Unit = {
        val index = editor.visibleEntries.indexOfFirst { it.key == latestCurrentKey.value }
        if (index >= 0) {
            scope.launch {
                val info = listState.layoutInfo
                val viewport = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
                listState.animateScrollToItem(QUEUE_HEADER_COUNT + index, -viewport / 3)
            }
        }
    }

    val saveTitle = stringResource(R.string.save_as_playlist)
    val playlistNameLabel = stringResource(R.string.playlist_name_label)
    val confirmLabel = stringResource(R.string.confirm)
    val cancelLabel = stringResource(R.string.cancel)
    val openSaveDialog: (fromSelection: Boolean) -> Unit = { fromSelection ->
        val mediaIds =
            editor.visibleEntries
                .filter { !fromSelection || it.key in editor.selectedKeys }
                .map { it.item.mediaId }
                .distinct()
        if (mediaIds.isNotEmpty()) {
            backStack.add(
                TextInputDialogRoute(
                    title = saveTitle,
                    initialValue = "",
                    label = playlistNameLabel,
                    confirmLabel = confirmLabel,
                    dismissLabel = cancelLabel,
                    onConfirm = { name, dismiss ->
                        if (name.isNotBlank()) {
                            panelViewModel.createPlaylistWithMediaIds(name = name, mediaIds = mediaIds) { success ->
                                if (success) {
                                    Toast
                                        .makeText(
                                            App.getInstance(),
                                            App.getInstance().getString(R.string.saved_as_playlist_format, name.trim()),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    if (fromSelection) editor.exitMultiSelect()
                                    dismiss()
                                }
                            }
                        }
                    },
                ),
            )
        }
    }

    val clearTitle = stringResource(R.string.clear_current_playlist_title)
    val clearMessage = stringResource(R.string.clear_current_playlist_message)
    val clearLabel = stringResource(R.string.clear_playlist)
    val openClearDialog: () -> Unit = {
        backStack.add(
            ConfirmationDialogRoute(
                title = clearTitle,
                message = clearMessage,
                confirmLabel = clearLabel,
                dismissLabel = cancelLabel,
                icon = Icons.Rounded.DeleteSweep,
                destructive = true,
                onConfirm = { dismiss ->
                    editor.reset()
                    viewModel.pause()
                    viewModel.updatePlaylist(emptyList())
                    dismiss()
                },
            ),
        )
    }

    val queueTitle = stringResource(R.string.queue_title)
    val currentPosition =
        if (!shuffle && currentKey != null) entries.indexOfFirst { it.key == currentKey } + 1 else 0
    val metaText =
        if (currentPosition > 0) {
            stringResource(R.string.queue_position_format, currentPosition, entries.size)
        } else {
            stringResource(R.string.songs_count_format, entries.size)
        }
    val swipeThreshold = SwipeToDismissBoxDefaults.positionalThreshold

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = pendingDeletion == null,
            contentPadding = PaddingValues(top = insetTop + TopBarHeight, bottom = listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "queue_masthead", contentType = "queue_masthead") {
                QueueMasthead(
                    title = queueTitle,
                    metaValue = if (currentPosition > 0) currentPosition else entries.size,
                    metaText = metaText,
                    showMultiSelect = entries.isNotEmpty(),
                    isMultiSelectMode = isMultiSelectMode,
                    enabled = pendingDeletion == null,
                    onToggleMultiSelect = {
                        if (editor.isMultiSelectMode) editor.exitMultiSelect() else editor.enterMultiSelect()
                    },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .entrance(order = 0, play = !entrancePlayed)
                            .graphicsLayer {
                                // 跟手收缩：大标题缩小、上移、淡出，直接耦合滚动偏移
                                val t = mastheadCollapse(listState)
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = 1f - t
                                translationY = -t * 16.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }

            if (entries.isEmpty()) {
                item(key = "queue_empty", contentType = "queue_empty") {
                    QueueEmptyState(
                        reducedMotion = reducedMotion,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .entrance(order = 1, play = !entrancePlayed),
                    )
                }
            } else {
                item(key = "queue_actions", contentType = "queue_actions") {
                    QueueActionRow(
                        playMode = playMode,
                        reducedMotion = reducedMotion,
                        onTogglePlayMode = viewModel::togglePlayMode,
                        onSave = { openSaveDialog(false) },
                        onClear = openClearDialog,
                        modifier =
                            Modifier
                                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                                .padding(top = Spacing.Large, bottom = Spacing.Small)
                                .entrance(order = 1, play = !entrancePlayed),
                    )
                }

                itemsIndexed(
                    items = entries,
                    key = { _, entry -> entry.key },
                    contentType = { _, _ -> "queue_row" },
                ) { rowIndex, entry ->
                    val isCurrent = entry.key == currentKey
                    val canSwipe =
                        !isMultiSelectMode &&
                            pendingDeletion == null &&
                            !reorderState.isAnyItemDragging &&
                            !isCurrent
                    val entranceSlot = rowIndex - entranceAnchor
                    ReorderableItem(
                        state = reorderState,
                        key = entry.key,
                        enabled = canReorder,
                        modifier =
                            Modifier.entrance(
                                order = ENTRANCE_ROW_BASE + entranceSlot,
                                play = !entrancePlayed && entranceSlot in 0 until ENTRANCE_MAX_ROW,
                            ),
                        animateItemModifier =
                            Modifier.animateItem(
                                fadeInSpec = ListItemFadeInSpec,
                                placementSpec = if (reducedMotion) null else ItemPlacementSpringSpec,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ),
                    ) { isDragging ->
                        // 不能用 rememberSaveable：撤销后回到列表的行必须是未滑动状态
                        val swipeState =
                            remember(entry.key) {
                                SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, swipeThreshold)
                            }
                        val title =
                            entry.item.mediaMetadata.title
                                ?.toString()
                        SwipeToDismissBox(
                            state = swipeState,
                            backgroundContent = { QueueSwipeBackground(swipeState, reducedMotion) },
                            enableDismissFromStartToEnd = false,
                            enableDismissFromEndToStart = canSwipe,
                            gesturesEnabled = canSwipe,
                            onDismiss = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    editor.hideWithUndo(setOf(entry.key), title)
                                }
                            },
                        ) {
                            QueueRow(
                                entry = entry,
                                isCurrent = isCurrent,
                                isPlaying = isPlaying,
                                isMultiSelectMode = isMultiSelectMode,
                                isSelected = entry.key in editor.selectedKeys,
                                isDragging = isDragging,
                                isDissolving = entry.key in editor.dissolvingKeys,
                                enabled = pendingDeletion == null && !reorderState.isAnyItemDragging,
                                reducedMotion = reducedMotion,
                                handleModifier =
                                    Modifier.longPressDraggableHandle(
                                        enabled = canReorder,
                                        onDragStarted = {
                                            editor.startDrag()
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDragStopped = {
                                            editor.endDrag(entry.key)
                                            haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                        },
                                    ),
                                canMoveUp = canReorder && rowIndex > 0,
                                canMoveDown = canReorder && rowIndex < entries.lastIndex,
                                canRemove = canSwipe,
                                onMoveUp = { editor.moveBy(entry.key, -1) },
                                onMoveDown = { editor.moveBy(entry.key, 1) },
                                onRemove = { editor.hideWithUndo(setOf(entry.key), title) },
                                onClick = {
                                    if (editor.isMultiSelectMode) {
                                        editor.toggleSelection(entry.key)
                                    } else {
                                        editor.play(entry, latestCurrentKey.value)
                                    }
                                },
                                onDissolveComplete = { editor.completedDissolveKeys += entry.key },
                            )
                        }
                    }
                }
            }
        }

        QueueTopBar(
            title = queueTitle,
            listState = listState,
            chromeColor = chromeColor,
            insetTop = insetTop,
            navigationIcon = navigationIcon,
            navigationContentDescription = navigationContentDescription,
            onNavigateBack = onNavigateBack,
            showLocate = showLocate,
            onLocate = locateCurrent,
            modifier = Modifier.align(Alignment.TopStart),
        )

        QueueNoticeHost(
            notice = notice,
            onUndo = editor::undo,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                    .padding(bottom = insetBottom + Spacing.Large),
        )

        AnimatedVisibility(
            visible = isMultiSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(durationMillis = 200)),
            exit = slideOutVertically { it } + fadeOut(tween(durationMillis = 160)),
        ) {
            QueueSelectionBar(
                selectedCount = editor.selectedKeys.size,
                actionsEnabled = pendingDeletion == null,
                contentWindowInsets = contentWindowInsets,
                onClose = editor::exitMultiSelect,
                onSelectAll = editor::selectAll,
                onDeselectAll = editor::deselectAll,
                onSave = { openSaveDialog(true) },
                onRemove = {
                    val selected = editor.selectedKeys.toSet()
                    val animatedKeys =
                        if (reducedMotion) {
                            emptyList()
                        } else {
                            listState.layoutInfo.visibleItemsInfo.mapNotNull { info ->
                                (info.key as? String)?.takeIf { it in selected }
                            }
                        }
                    editor.removeSelected(animatedKeys, latestCurrentKey.value)
                },
                modifier = Modifier.onSizeChanged { selectionBarHeightPx = it.height },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 编辑状态
// ──────────────────────────────────────────────────────────────────────────

@Immutable
private data class QueueEntry(
    val key: String,
    val item: MediaItem,
)

/** 已下发给播放器、但播放器还没回传的预期列表 */
@Immutable
private class OptimisticQueue(
    val entries: List<QueueEntry>,
) {
    val expectedMediaIds: List<String> = entries.map { it.item.mediaId }
}

/** 底部提示；[undoKeys] 为 null 表示不可撤销 */
@Immutable
private data class QueueNotice(
    val id: Long,
    val count: Int,
    val title: String?,
    val undoKeys: Set<String>?,
)

@Immutable
private data class PendingPlaylistDeletion(
    val keys: Set<String>,
    val animatedItemKeys: List<String>,
    val removesCurrent: Boolean,
)

/**
 * 播放列表页的编辑状态：待撤销的移除、下发后等待回传的预期列表、拖动中的顺序、多选与批量删除。
 *
 * 下发给播放器的索引一律取自 [sourceEntries]，即播放器执行完已下发操作后的列表（含待撤销的隐藏项）；
 * 播放器按下发顺序执行，所以索引始终对得上。
 */
@Stable
private class QueueEditor(
    private val viewModel: PlayerViewModel,
    private val scope: CoroutineScope,
    private val baseEntries: State<List<QueueEntry>>,
) {
    /** 已从界面移除、仍可撤销、还没交给播放器的条目 */
    val hiddenKeys = mutableStateSetOf<String>()
    var optimistic by mutableStateOf<OptimisticQueue?>(null)
    var notice by mutableStateOf<QueueNotice?>(null)
    private var noticeSeq = 0L
    private var dragOrder by mutableStateOf<SnapshotStateList<QueueEntry>?>(null)

    var isMultiSelectMode by mutableStateOf(false)
        private set
    val selectedKeys = mutableStateSetOf<String>()

    var pendingDeletion by mutableStateOf<PendingPlaylistDeletion?>(null)
        private set
    val dissolvingKeys = mutableStateSetOf<String>()
    val completedDissolveKeys = mutableStateSetOf<String>()

    private val sourceEntries: List<QueueEntry>
        get() = optimistic?.entries ?: baseEntries.value

    val visibleEntries: List<QueueEntry> by derivedStateOf {
        dragOrder?.toList() ?: sourceEntries.filterNot { it.key in hiddenKeys }
    }

    fun play(
        entry: QueueEntry,
        currentKey: String?,
    ) {
        if (entry.key == currentKey) {
            viewModel.togglePlayPause()
            return
        }
        val index = sourceEntries.indexOfFirst { it.key == entry.key }
        if (index >= 0) viewModel.playQueueIndex(index)
    }

    /** 把待撤销的移除交给播放器，返回提交后的预期列表 */
    fun commitPendingRemovals(): List<QueueEntry> {
        val source = sourceEntries
        if (hiddenKeys.isEmpty()) return source
        val hidden = hiddenKeys.toSet()
        viewModel.removeFromPlaylist(selectedPlaylistIndices(source.map { it.key }, hidden))
        val remaining = source.filterNot { it.key in hidden }
        hiddenKeys.clear()
        if (notice?.undoKeys != null) notice = null
        optimistic = OptimisticQueue(remaining)
        return remaining
    }

    /** 从界面移除并给出撤销提示；同一时间只保留一组可撤销项 */
    fun hideWithUndo(
        keys: Set<String>,
        title: String?,
    ) {
        if (hiddenKeys.isNotEmpty()) commitPendingRemovals()
        hiddenKeys += keys
        notice = QueueNotice(++noticeSeq, keys.size, title, undoKeys = keys)
    }

    fun undo(target: QueueNotice) {
        val keys = target.undoKeys ?: return
        hiddenKeys.removeAll(keys)
        dissolvingKeys.removeAll(keys)
        completedDissolveKeys.removeAll(keys)
        if (notice?.id == target.id) notice = null
    }

    fun startDrag() {
        dragOrder = visibleEntries.toMutableStateList()
    }

    fun moveInDrag(
        fromKey: Any,
        toKey: Any,
    ): Boolean {
        val order = dragOrder ?: return false
        val from = order.indexOfFirst { it.key == fromKey }
        val to = order.indexOfFirst { it.key == toKey }
        if (from < 0 || to < 0 || from == to) return false
        order.add(to, order.removeAt(from))
        return true
    }

    fun endDrag(draggedKey: String) {
        val order = dragOrder ?: return
        // 与 optimistic 在同一次事件里切换，旧顺序不会闪回
        dragOrder = null
        applyVisibleOrder(order.map { it.key }, draggedKey)
    }

    /** 无障碍「上移 / 下移」 */
    fun moveBy(
        key: String,
        delta: Int,
    ) {
        val keys = visibleEntries.mapTo(mutableListOf()) { it.key }
        val from = keys.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in keys.indices) return
        keys.add(to, keys.removeAt(from))
        applyVisibleOrder(keys, key)
    }

    private fun applyVisibleOrder(
        reorderedVisibleKeys: List<String>,
        movedKey: String,
    ) {
        val source = sourceEntries
        val (from, to) = resolveQueueMove(source.map { it.key }, reorderedVisibleKeys, movedKey) ?: return
        viewModel.moveQueueItem(from, to)
        optimistic = OptimisticQueue(source.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun enterMultiSelect(initialKey: String? = null) {
        isMultiSelectMode = true
        if (initialKey != null) selectedKeys += initialKey
    }

    fun exitMultiSelect() {
        isMultiSelectMode = false
        selectedKeys.clear()
    }

    fun toggleSelection(key: String) {
        if (!selectedKeys.remove(key)) selectedKeys += key
    }

    fun selectAll() {
        selectedKeys += visibleEntries.map { it.key }
    }

    fun deselectAll() {
        selectedKeys.clear()
    }

    fun retainSelection(validKeys: Set<String>) {
        selectedKeys.retainAll(validKeys)
        if (validKeys.isEmpty()) isMultiSelectMode = false
    }

    fun removeSelected(
        animatedKeys: List<String>,
        currentKey: String?,
    ) {
        val keys = selectedKeys.toSet()
        if (keys.isEmpty() || pendingDeletion != null) return
        dissolvingKeys.clear()
        completedDissolveKeys.clear()
        pendingDeletion =
            PendingPlaylistDeletion(
                keys = keys,
                animatedItemKeys = animatedKeys,
                removesCurrent = currentKey != null && currentKey in keys,
            )
    }

    fun finishDeletion(deletion: PendingPlaylistDeletion) {
        if (pendingDeletion !== deletion) return
        pendingDeletion = null
        exitMultiSelect()
        if (deletion.removesCurrent) {
            // 正在播放的歌曲留在播放器里会继续响，不能延后：立即提交，不提供撤销
            if (hiddenKeys.isNotEmpty()) commitPendingRemovals()
            hiddenKeys += deletion.keys
            commitPendingRemovals()
            notice = QueueNotice(++noticeSeq, deletion.keys.size, null, undoKeys = null)
        } else {
            val title =
                deletion.keys.singleOrNull()?.let { key ->
                    sourceEntries
                        .firstOrNull { it.key == key }
                        ?.item
                        ?.mediaMetadata
                        ?.title
                        ?.toString()
                }
            hideWithUndo(deletion.keys, title)
        }
        // 等行的淡出结束再撤掉消散标记，否则最后一帧会把原内容画回来
        scope.launch {
            delay(400)
            dissolvingKeys.removeAll(deletion.keys)
            completedDissolveKeys.removeAll(deletion.keys)
        }
    }

    fun flush() {
        pendingDeletion?.let { deletion ->
            hiddenKeys += deletion.keys
            pendingDeletion = null
        }
        commitPendingRemovals()
    }

    fun reset() {
        hiddenKeys.clear()
        notice = null
        optimistic = null
        dragOrder = null
        pendingDeletion = null
        dissolvingKeys.clear()
        completedDissolveKeys.clear()
        exitMultiSelect()
    }
}

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

/** 当前歌曲的列表索引：优先播放器上报的索引（能区分重复歌曲），对不上时退回首个同 ID 项 */
internal fun resolveCurrentQueueIndex(
    items: List<MediaItem>,
    reportedIndex: Int,
    currentMediaId: String?,
): Int {
    if (currentMediaId == null) return -1
    if (reportedIndex in items.indices && items[reportedIndex].mediaId == currentMediaId) return reportedIndex
    return items.indexOfFirst { it.mediaId == currentMediaId }
}

/**
 * 把「可见列表里的新顺序」换算成播放器列表上的一次移动。
 *
 * [sourceKeys] 可能夹着待撤销的隐藏项，所以按移动项在新顺序里的邻居定位：
 * 放到后一个可见邻居之前；它已是最后一个时放到前一个邻居之后。
 * @return (fromIndex, toIndex)，toIndex 是移动完成后的位置；无需移动时为 null
 */
internal fun resolveQueueMove(
    sourceKeys: List<String>,
    reorderedVisibleKeys: List<String>,
    movedKey: String,
): Pair<Int, Int>? {
    val from = sourceKeys.indexOf(movedKey)
    val position = reorderedVisibleKeys.indexOf(movedKey)
    if (from < 0 || position < 0) return null
    val remaining = sourceKeys.toMutableList().apply { removeAt(from) }
    val to =
        when {
            position + 1 < reorderedVisibleKeys.size ->
                remaining.indexOf(reorderedVisibleKeys[position + 1]).takeIf { it >= 0 } ?: return null

            position > 0 ->
                (remaining.indexOf(reorderedVisibleKeys[position - 1]).takeIf { it >= 0 } ?: return null) + 1

            else -> return null
        }
    return if (to == from) null else from to to
}

// ──────────────────────────────────────────────────────────────────────────
// 顶栏 / 刊头（与 FavoriteScene 同一套手感）
// ──────────────────────────────────────────────────────────────────────────

/** 大标题收缩进度：0f=完全展开 1f=完全收进顶栏（在 Draw 阶段读取，滚动零重组） */
private fun Density.mastheadCollapse(listState: LazyListState): Float =
    if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / MastheadCollapseDistance.toPx()).coerceIn(0f, 1f)
    }

/** 一次“扑通-扑通”双搏脉冲（点击切换播放模式时的反馈） */
private suspend fun heartThump(pulse: Animatable<Float, AnimationVector1D>) {
    pulse.animateTo(
        targetValue = 1f,
        animationSpec =
            keyframes {
                durationMillis = 640
                1f at 0
                1.07f at 120 using FastOutSlowInEasing
                1f at 280 using FastOutSlowInEasing
                1.045f at 420 using FastOutSlowInEasing
                1f at 640 using FastOutSlowInEasing
            },
    )
}

@Composable
private fun QueueTopBar(
    title: String,
    listState: LazyListState,
    chromeColor: Color,
    insetTop: Dp,
    navigationIcon: ImageVector,
    navigationContentDescription: String,
    onNavigateBack: () -> Unit,
    showLocate: Boolean,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val solid by remember(listState) { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(insetTop + TopBarHeight)
                .drawBehind {
                    // 背景不透明度在 Draw 阶段跟随滚动，避免每帧重组
                    drawRect(color = chromeColor.copy(alpha = chromeColor.alpha * mastheadCollapse(listState) * 0.5f))
                },
    ) {
        // 全页唯一分隔线：顶栏收起后出现
        if (solid) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = insetTop)
                    .padding(horizontal = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = navigationIcon,
                    contentDescription = navigationContentDescription,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = mastheadCollapse(listState) },
                color = MaterialTheme.colorScheme.onSurface,
            )
            // 当前歌曲滚出可视区时弹出的「定位」胶囊
            AnimatedVisibility(
                visible = showLocate,
                enter =
                    scaleIn(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        initialScale = ScaleEnterFrom,
                    ) + fadeIn(tween(durationMillis = 160)),
                exit =
                    scaleOut(
                        animationSpec = tween(durationMillis = 140),
                        targetScale = ScaleExitTo,
                    ) + fadeOut(tween(durationMillis = 140)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(end = Spacing.Small)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickHighlight(onClick = onLocate)
                            .padding(horizontal = Spacing.Medium, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.locate_playing),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 刊头：大标题 + meta 行（播放位置 / 数量 + 多选入口） */
@Composable
private fun QueueMasthead(
    title: String,
    metaValue: Int,
    metaText: String,
    showMultiSelect: Boolean,
    isMultiSelectMode: Boolean,
    enabled: Boolean,
    onToggleMultiSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            QueueRollingText(
                value = metaValue,
                text = metaText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (showMultiSelect) {
                Row(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickHighlight(enabled = enabled, onClick = onToggleMultiSelect)
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedContent(
                        targetState = isMultiSelectMode,
                        transitionSpec = { Nav3Transitions.cube() },
                        label = "queueMultiSelectPill",
                    ) { active ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Rounded.Close else Icons.Rounded.Checklist,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text =
                                    if (active) {
                                        stringResource(R.string.exit_multiselect_cd)
                                    } else {
                                        stringResource(R.string.multi_select)
                                    },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 数字变化时上下滚动切换（增大向上、减小向下） */
@Composable
private fun QueueRollingText(
    value: Int,
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = value to text,
        transitionSpec = {
            val direction = if (targetState.first >= initialState.first) 1 else -1
            (
                slideInVertically { height -> direction * height / 2 } +
                    fadeIn(tween(durationMillis = 240))
            ) togetherWith
                (
                    slideOutVertically { height -> -direction * height / 2 } +
                        fadeOut(tween(durationMillis = 160))
                ) using SizeTransform(clip = false)
        },
        modifier = modifier,
        label = "queueRollingText",
    ) { (_, shownText) ->
        Text(
            text = shownText,
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

/** 操作行：播放模式（tonal）+ 存为歌单 + 清空（仅图标，错误色图标降低存在感） */
@Composable
private fun QueueActionRow(
    playMode: PlayMode,
    reducedMotion: Boolean,
    onTogglePlayMode: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            QueueCommandPill(
                text =
                    when (playMode) {
                        PlayMode.LOOP -> stringResource(R.string.play_mode_loop)
                        PlayMode.LIST -> stringResource(R.string.play_mode_repeat_one)
                        PlayMode.SHUFFLE -> stringResource(R.string.shuffle_play)
                    },
                icon =
                    when (playMode) {
                        PlayMode.LOOP -> Icons.Rounded.Repeat
                        PlayMode.LIST -> Icons.Rounded.RepeatOne
                        PlayMode.SHUFFLE -> Icons.Rounded.Shuffle
                    },
                container = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                pulseOnClick = !reducedMotion,
                reducedMotion = reducedMotion,
                onClickLabel = stringResource(R.string.play_mode),
                onClick = onTogglePlayMode,
                modifier = Modifier.weight(1f),
            )
            QueueCommandPill(
                text = stringResource(R.string.queue_save_short),
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                pulseOnClick = false,
                reducedMotion = reducedMotion,
                onClickLabel = stringResource(R.string.save_as_playlist),
                onClick = onSave,
                modifier = Modifier.weight(1f),
            )
            QueueIconPill(
                icon = Icons.Rounded.DeleteSweep,
                contentDescription = stringResource(R.string.clear_playlist),
                tint = MaterialTheme.colorScheme.error,
                reducedMotion = reducedMotion,
                onClick = onClear,
            )
        }
        AnimatedVisibility(
            visible = playMode == PlayMode.SHUFFLE,
            enter = expandVertically() + fadeIn(tween(durationMillis = 200)),
            exit = shrinkVertically() + fadeOut(tween(durationMillis = 160)),
        ) {
            Row(
                modifier = Modifier.padding(top = Spacing.Medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.queue_shuffle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 52dp 主操作胶囊：按压回弹，可选点击脉冲，文案/图标切换走 shared axis */
@Composable
private fun QueueCommandPill(
    text: String,
    icon: ImageVector,
    container: Color,
    contentColor: Color,
    pulseOnClick: Boolean,
    reducedMotion: Boolean,
    onClickLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "queueCommandPillPressScale",
    )
    val pulse = remember { Animatable(1f) }
    Box(
        modifier =
            modifier
                .height(52.dp)
                .graphicsLayer {
                    val scale = pressScale * pulse.value
                    scaleX = scale
                    scaleY = scale
                }.clip(CircleShape)
                .background(container)
                .clickHighlight(onClickLabel = onClickLabel, interactionSource = interactionSource) {
                    if (pulseOnClick && !pulse.isRunning) {
                        scope.launch { heartThump(pulse) }
                    }
                    onClick()
                }.padding(horizontal = Spacing.Medium),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = text to icon,
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "queueCommandPillContent",
        ) { (shownText, shownIcon) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Icon(
                    imageVector = shownIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = shownText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun QueueIconPill(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && !reducedMotion) 0.92f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "queueIconPillPressScale",
    )
    Box(
        modifier =
            modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }.clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickHighlight(onClickLabel = contentDescription, interactionSource = interactionSource, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 歌曲行
// ──────────────────────────────────────────────────────────────────────────

/** 通栏歌曲行：封面 + 标题/歌手 + 时长 + 拖动手柄；多选时左侧滑出勾选框 */
@Composable
private fun QueueRow(
    entry: QueueEntry,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    isDissolving: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    handleModifier: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    canRemove: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    onDissolveComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val metadata = entry.item.mediaMetadata
    val title = metadata.title?.toString() ?: stringResource(R.string.unknown_song)
    val artist = metadata.artist?.toString() ?: stringResource(R.string.unknown_artist)

    // 拖起：轻微放大 + 不透明底色盖住下方内容
    val fillColor by animateColorAsState(
        targetValue =
            when {
                isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
            },
        animationSpec = tween(durationMillis = 160, easing = EaseOutEmphasized),
        label = "queueRowFill",
    )
    val liftScale by animateFloatAsState(
        targetValue = if (isDragging && !reducedMotion) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "queueRowLiftScale",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "queueRowTitleColor",
    )

    val nowPlayingLabel = stringResource(R.string.now_playing_indicator)
    val moveUpLabel = stringResource(R.string.move_up)
    val moveDownLabel = stringResource(R.string.move_down)
    val removeLabel = stringResource(R.string.remove_from_queue)
    val clickLabel = stringResource(if (isCurrent && isPlaying) R.string.pause else R.string.play)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .particleDissolve(isDissolving = isDissolving, onComplete = onDissolveComplete)
                .padding(horizontal = Spacing.Small)
                .graphicsLayer {
                    shape = Shapes.MediumCornerBasedShape
                    clip = true
                    scaleX = liftScale
                    scaleY = liftScale
                }.background(fillColor)
                .semantics(mergeDescendants = true) {
                    if (isMultiSelectMode) selected = isSelected
                    if (isCurrent) stateDescription = nowPlayingLabel
                    customActions =
                        buildList {
                            if (canMoveUp) {
                                add(
                                    CustomAccessibilityAction(moveUpLabel) {
                                        onMoveUp()
                                        true
                                    },
                                )
                            }
                            if (canMoveDown) {
                                add(
                                    CustomAccessibilityAction(moveDownLabel) {
                                        onMoveDown()
                                        true
                                    },
                                )
                            }
                            if (canRemove) {
                                add(
                                    CustomAccessibilityAction(removeLabel) {
                                        onRemove()
                                        true
                                    },
                                )
                            }
                        }
                }.then(handleModifier)
                .combinedClickHighlight(
                    enabled = enabled,
                    onClickLabel = if (isMultiSelectMode) null else clickLabel,
                    onClick = onClick,
                ).padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = isMultiSelectMode,
                enter =
                    expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) +
                        fadeIn(tween(durationMillis = 180)),
                exit = shrinkHorizontally(tween(durationMillis = 150)) + fadeOut(tween(durationMillis = 120)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        enabled = enabled,
                    )
                    Spacer(modifier = Modifier.width(Spacing.Medium))
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(Shapes.MediumCornerBasedShape),
            ) {
                AudioCover(
                    uri = metadata.artworkUri,
                    fallbackUri = entry.item.albumCoverUri(),
                    modifier = Modifier.fillMaxSize(),
                )
                NowPlayingCoverOverlay(
                    visible = isCurrent,
                    isPlaying = isPlaying,
                    reducedMotion = reducedMotion,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = formatTime(metadata.durationMs ?: 0L),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 36.dp),
        )
    }
}

/** 歌曲本体无内嵌封面时回退专辑图（与 Song.getAlbumCoverUri 同规则） */
private fun MediaItem.albumCoverUri(): Uri? =
    mediaMetadata.extras
        ?.getLong("albumId")
        ?.takeIf { it > 0 }
        ?.let { "content://media/external/audio/albumart/$it".toUri() }

/** 左滑露出的移除底：只画已露出的部分（行本身透明，不能整块铺底） */
@Composable
private fun QueueSwipeBackground(
    state: SwipeToDismissBoxState,
    reducedMotion: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    val armed = state.targetValue == SwipeToDismissBoxValue.EndToStart
    val iconScale by animateFloatAsState(
        targetValue = if (armed || reducedMotion) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "queueSwipeIconScale",
    )
    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }
            .drop(1)
            .collect { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                }
            }
    }
    val fill = MaterialTheme.colorScheme.errorContainer
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.Small)
                .clip(Shapes.MediumCornerBasedShape)
                .drawBehind {
                    val revealed = state.revealedWidth()
                    if (revealed > 0f) {
                        drawRect(
                            color = fill,
                            topLeft = Offset(size.width - revealed, 0f),
                            size = Size(revealed, size.height),
                        )
                    }
                },
    ) {
        Icon(
            imageVector = Icons.Rounded.PlaylistRemove,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = Spacing.ExtraLarge)
                    .size(24.dp)
                    .graphicsLayer {
                        alpha = (state.revealedWidth() / 72.dp.toPx()).coerceIn(0f, 1f)
                        scaleX = iconScale
                        scaleY = iconScale
                    },
        )
    }
}

/** 左滑已露出的宽度（像素），未布局时为 0 */
private fun SwipeToDismissBoxState.revealedWidth(): Float = (-runCatching { requireOffset() }.getOrDefault(0f)).coerceAtLeast(0f)

/** 当前歌曲封面上的律动条：播放时跳动，暂停时收拢成静止高度 */
@Composable
private fun NowPlayingCoverOverlay(
    visible: Boolean,
    isPlaying: Boolean,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 160)) + scaleIn(initialScale = ScaleEnterFrom),
        exit = fadeOut(tween(durationMillis = 120)),
    ) {
        val amplitude =
            animateFloatAsState(
                targetValue = if (isPlaying && !reducedMotion) 1f else 0f,
                animationSpec = tween(durationMillis = 240, easing = EaseOutEmphasized),
                label = "queueBarsAmplitude",
            )
        val moving by remember { derivedStateOf { amplitude.value > 0.001f } }
        // 只有跳动时才跑无限动画；相位只在 Canvas 绘制阶段读取
        val phase =
            if (moving) {
                rememberInfiniteTransition(label = "queueBars").animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(1_400, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                    label = "queueBarsPhase",
                )
            } else {
                null
            }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.38f)),
            contentAlignment = Alignment.Center,
        ) {
            PlayingBars(
                color = Color.White,
                phaseProvider = { phase?.value ?: 0f },
                amplitudeProvider = { amplitude.value },
                modifier = Modifier.size(width = 18.dp, height = 16.dp),
            )
        }
    }
}

@Composable
private fun PlayingBars(
    color: Color,
    phaseProvider: () -> Float,
    amplitudeProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val phase = phaseProvider()
        val amplitude = amplitudeProvider().coerceIn(0f, 1f)
        val barWidth = size.width * 0.18f
        val gap = (size.width - barWidth * 3f) / 2f
        repeat(3) { index ->
            val wave = ((sin((phase + index * 0.23f) * PI * 2.0) + 1.0) * 0.5).toFloat()
            val rest = RestBarHeights[index]
            val barHeight = size.height * (rest + (0.34f + wave * 0.58f - rest) * amplitude)
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

// ──────────────────────────────────────────────────────────────────────────
// 空态 / 提示 / 多选栏
// ──────────────────────────────────────────────────────────────────────────

/** 空态：开放排版无卡片，图标轻盈浮动 */
@Composable
private fun QueueEmptyState(
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val bob =
        if (reducedMotion) {
            null
        } else {
            rememberInfiniteTransition(label = "queueEmptyFloat").animateFloat(
                initialValue = -1f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "queueEmptyBob",
            )
        }
    Column(
        modifier = modifier.padding(top = 64.dp, bottom = Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        val offset = bob?.value ?: 0f
                        translationY = offset * 5.dp.toPx()
                        rotationZ = offset * 6f
                    }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.queue_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.queue_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        )
    }
}

/** 退场动画期间 notice 已为 null，普通字段记住最后一条用于绘制 */
private class NoticeMemory {
    var last: QueueNotice? = null
}

@Composable
private fun QueueNoticeHost(
    notice: QueueNotice?,
    onUndo: (QueueNotice) -> Unit,
    modifier: Modifier = Modifier,
) {
    val memory = remember { NoticeMemory() }
    if (notice != null) memory.last = notice
    AnimatedVisibility(
        visible = notice != null,
        modifier = modifier,
        enter = slideInVertically { it / 2 } + fadeIn(tween(durationMillis = 200)),
        exit = slideOutVertically { it / 2 } + fadeOut(tween(durationMillis = 160)),
    ) {
        val shown = notice ?: memory.last ?: return@AnimatedVisibility
        AnimatedContent(
            targetState = shown,
            contentKey = { it.id },
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "queueNotice",
        ) { current ->
            QueueNoticePill(notice = current, onUndo = { onUndo(current) })
        }
    }
}

@Composable
private fun QueueNoticePill(
    notice: QueueNotice,
    onUndo: () -> Unit,
) {
    val undoable = notice.undoKeys != null
    Surface(
        modifier =
            Modifier
                .widthIn(max = 480.dp)
                .height(48.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = if (undoable) Spacing.ExtraSmall else 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    if (notice.count == 1 && notice.title != null) {
                        stringResource(R.string.queue_removed_one_format, notice.title)
                    } else {
                        stringResource(R.string.queue_removed_many_format, notice.count)
                    },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (undoable) {
                TextButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary),
                ) {
                    Text(
                        text = stringResource(R.string.undo),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** 多选底部操作栏（与 FavoriteMultiSelectBar 同构） */
@Composable
private fun QueueSelectionBar(
    selectedCount: Int,
    actionsEnabled: Boolean,
    contentWindowInsets: WindowInsets,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(contentWindowInsets.only(WindowInsetsSides.Bottom))
                    .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose, enabled = actionsEnabled) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.exit_multiselect_cd),
                    )
                }
                QueueRollingText(
                    value = selectedCount,
                    text = stringResource(R.string.selected_songs_count_format, selectedCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSelectAll, enabled = actionsEnabled) {
                    Text(stringResource(R.string.select_all))
                }
                TextButton(onClick = onDeselectAll, enabled = actionsEnabled) {
                    Text(stringResource(R.string.deselect_all))
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                QueueSelectionActionPill(
                    text = stringResource(R.string.save_as_playlist),
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    enabled = actionsEnabled && selectedCount > 0,
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                )
                QueueSelectionActionPill(
                    text = stringResource(R.string.remove_from_queue),
                    icon = Icons.Rounded.PlaylistRemove,
                    container = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    enabled = actionsEnabled && selectedCount > 0,
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun QueueSelectionActionPill(
    text: String,
    icon: ImageVector,
    container: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (enabled) 1f else 0.42f
    Row(
        modifier =
            modifier
                .clip(Shapes.MediumCornerBasedShape)
                .background(if (enabled) container else container.copy(alpha = 0.45f))
                .clickHighlight(enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor.copy(alpha = contentAlpha),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Spacing.ExtraSmall))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
