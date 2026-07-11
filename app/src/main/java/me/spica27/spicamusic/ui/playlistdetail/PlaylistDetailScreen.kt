package me.spica27.spicamusic.ui.playlistdetail

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.image.LandscapistImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.highlightKeyword
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.min

// ── 布局尺寸常量（全部 dp，px 一律在 draw/layout 阶段经 density 换算）──────────
private val HEADER_HEIGHT = 56.dp // 固定顶栏内容区高度（不含状态栏）
private val COVER_EXPANDED_MAX = 180.dp // 封面展开尺寸上限（矮窗口按可用高度 30% 钳制）
private val COVER_COLLAPSED = 38.dp // 封面折叠尺寸（顶栏内容区内垂直居中）
private val COVER_COLLAPSED_START = 56.dp // 封面折叠后距屏幕左缘距离（返回按钮之后）

// 折叠小标题距屏幕左缘 = 折叠封面终点 + 间隙，与封面严丝合缝
private val COLLAPSED_TITLE_START = COVER_COLLAPSED_START + COVER_COLLAPSED + Spacing.Medium

private val BOTTOM_PLAYER_RESERVED = 200.dp // 悬浮迷你播放器底部预留（全项目惯例值）
private val MULTI_SELECT_BAR_RESERVED = 72.dp // 多选底栏出现时的额外避让

/** 固定顶栏的三种形态：浏览 / 搜索 / 排序 */
private enum class TopBarState { Browse, Search, Sort }

/**
 * 搜索内容区的四种状态：
 * - [Idle] 空关键词，显示引导
 * - [Loading] 防抖窗口内或结果尚未返回，显示呼吸骨架（杜绝旧结果停留与空态闪现）
 * - [Empty] 已加载完成且确实没有匹配
 * - [Results] 正常结果列表
 */
private enum class SearchContentState { Idle, Loading, Empty, Results }

/**
 * 歌单详情页。
 *
 * 布局采用"浏览列表 + 搜索覆盖层"双层结构：
 * - 浏览列表的封面留白是列表自己的 item 0（高度 == 折叠量程），
 *   搜索覆盖层是一块不透明面板盖在其上 —— 顶部留白与滚动位置互不可见；
 * - 主列表与搜索结果是两条独立的 Paging 流（搜索流在 ViewModel 防抖 300ms），
 *   输入关键字不会触碰主列表的 Pager 与滚动状态。
 */
@Composable
fun PlaylistDetailScreen(playlist: Playlist) {
    val path = LocalNavigationPath.current
    val viewModel =
        koinViewModel<PlaylistDetailViewModel>(
            key = "PlaylistDetailViewModel_${playlist.playlistId}",
        ) { parametersOf(playlist.playlistId) }

    // ── State collection ───────────────────────────────────────────────────
    val currentPlaylist by viewModel.playlist.collectAsStateWithLifecycle()
    val browseSongs = viewModel.browseSongs.collectAsLazyPagingItems()
    val coverAlbumIds by viewModel.coverAlbumIds.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val isSearchMode by viewModel.isSearchMode.collectAsStateWithLifecycle()
    val isSortMode by viewModel.isSortMode.collectAsStateWithLifecycle()
    val sortModeSongs by viewModel.sortModeSongs.collectAsStateWithLifecycle()
    val sortModeLimitExceeded by viewModel.sortModeLimitExceeded.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongs by viewModel.selectedSongs.collectAsStateWithLifecycle()
    val showRenameDialog by viewModel.showRenameDialog.collectAsStateWithLifecycle()
    val showDeleteConfirmDialog by viewModel.showDeleteConfirmDialog.collectAsStateWithLifecycle()
    val showAddSongsSheet by viewModel.showAddSongsSheet.collectAsStateWithLifecycle()
    val showMoreOptionsMenu by viewModel.showMoreOptionsMenu.collectAsStateWithLifecycle()
    val playlistDeleted by viewModel.playlistDeleted.collectAsStateWithLifecycle()

    val playerViewModel = LocalPlayerViewModel.current
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val playingMediaId = currentMediaItem?.mediaId

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val haptics = LocalHapticFeedback.current

    val displayName = currentPlaylist?.playlistName ?: playlist.playlistName

    // 歌单确实为空（区别于"首屏还没加载完"）：空态提示与搜索入口都以它为准
    val isPlaylistEmpty =
        browseSongs.itemCount == 0 && browseSongs.loadState.refresh is LoadState.NotLoading

    // 歌单被删除时返回上一页
    LaunchedEffect(playlistDeleted) {
        if (playlistDeleted) path.popTop()
    }

    // ── 搜索覆盖层过渡（hoisted：键盘时序与关键字清理都依赖它）─────────────────
    val searchTransition = remember { MutableTransitionState(false) }
    searchTransition.targetState = isSearchMode

    // 覆盖层完全退场后再清空关键字：退场动画期间仍渲染旧结果，避免闪现 Idle
    LaunchedEffect(searchTransition) {
        snapshotFlow { searchTransition.isIdle && !searchTransition.currentState }
            .collect { fullyHidden ->
                if (fullyHidden) viewModel.clearSearchKeyword()
            }
    }

    val searchFocusRequester = remember { FocusRequester() }

    // 键盘与覆盖层过渡确定性错峰：面板完全展开后再唤起键盘
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            snapshotFlow { searchTransition.isIdle && searchTransition.currentState }.first { it }
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val exitSearch = {
        keyboardController?.hide()
        focusManager.clearFocus()
        viewModel.exitSearchMode()
    }

    // 三模式在 ViewModel 层互斥，同一时刻至多一个 enabled
    BackHandler(isMultiSelectMode) { viewModel.toggleMultiSelectMode() }
    BackHandler(isSortMode) { viewModel.cancelSortMode() }
    BackHandler(isSearchMode) { exitSearch() }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

        // 展开封面：横屏/分屏矮窗口时按可用高度 30% 钳制
        val coverExpanded = COVER_EXPANDED_MAX.coerceAtMost(maxHeight * 0.3f)

        // 封面占位块高度 == 折叠量程：滚过 item 0 时 progress 恰好到 1，边界零断层
        val coverBlock = Spacing.Small + coverExpanded + Spacing.Medium
        val coverStartExpanded = (maxWidth - coverExpanded) / 2

        val browseListState = rememberLazyListState()
        val headerEntrance = rememberEntrance(order = 2)
        val actionRowEntrance = rememberEntrance(order = 3)
        var listEntrancePlay by remember { mutableStateOf(true) }
        LaunchedEffect(listEntrancePlay) {
            if (listEntrancePlay) {
                delay(55)
                listEntrancePlay = false
            }
        }
        // 折叠进度：只在 draw/graphicsLayer 阶段调用 → 滚动全程零重组
        val collapseProgress: Density.() -> Float =
            remember(browseListState, coverBlock) {
                {
                    if (browseListState.firstVisibleItemIndex > 0) {
                        1f
                    } else {
                        (browseListState.firstVisibleItemScrollOffset / coverBlock.toPx())
                            .coerceIn(0f, 1f)
                    }
                }
            }

        val reorderableLazyListState =
            rememberReorderableLazyListState(browseListState) { from, to ->
                val fromMediaId = from.key as? Long ?: return@rememberReorderableLazyListState
                val toMediaId = to.key as? Long ?: return@rememberReorderableLazyListState
                viewModel.moveSortModeSong(
                    fromMediaId = fromMediaId,
                    toMediaId = toMediaId,
                    insertAfterTarget = from.index < to.index,
                )
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            }

        // ── [L0] 渐变背景（随折叠淡出）──────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT + coverExpanded + 120.dp)
                .graphicsLayer { alpha = 1f - collapseProgress() }
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                        1f to MaterialTheme.colorScheme.background.copy(alpha = 0f),
                    ),
                ),
        )

        // ── [L1] 浏览态列表（搜索期间滚动位置与 Pager 全程冻结）────────────────────
        LazyColumn(
            state = browseListState,
            modifier = Modifier.fillMaxSize(),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + HEADER_HEIGHT,
                    bottom =
                        BOTTOM_PLAYER_RESERVED +
                            if (isMultiSelectMode) MULTI_SELECT_BAR_RESERVED else 0.dp,
                ),
        ) {
            // 封面占位：留白属于浏览列表自身，搜索覆盖层根本看不到它
            item(key = "cover_space", contentType = "cover_space") {
                Spacer(Modifier.height(coverBlock))
            }

            // 歌单名称 + 歌曲数（随滚动淡出）
            item(key = "playlist_header", contentType = "header") {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                        .graphicsLayer {
                            alpha = (1f - collapseProgress() * 2.5f).coerceIn(0f, 1f)
                        }.entranceGraphics(headerEntrance),
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.ExtraSmall))
                    Text(
                        text = stringResource(R.string.songs_count, songCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 播放 / 添加按钮行（排序中就地换成提示胶囊）
            item(key = "action_row", contentType = "actions") {
                ActionRow(
                    isSortMode = isSortMode,
                    playEnabled = !isPlaylistEmpty,
                    onPlayAll = viewModel::playAll,
                    onAddSongs = viewModel::showAddSongsSheet,
                    modifier = Modifier.entranceGraphics(actionRowEntrance),
                )
            }

            // 空歌单引导
            if (isPlaylistEmpty && !isSortMode) {
                item(key = "empty_state", contentType = "empty") {
                    EmptyPlaylistHint(onAddSongs = viewModel::showAddSongsSheet)
                }
            }

            if (isSortMode) {
                items(
                    count = sortModeSongs.size,
                    key = { index -> sortModeSongs[index].mediaStoreId },
                    contentType = { "song" },
                ) { index ->
                    val song = sortModeSongs[index]
                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = song.mediaStoreId,
                    ) { isDragging ->
                        PlaylistSongRow(
                            song = song,
                            isPlaying = playingMediaId == song.mediaStoreId.toString(),
                            isMultiSelectMode = false,
                            isSelected = false,
                            isDragging = isDragging,
                            isReorderEnabled = true,
                            dragHandleModifier =
                                Modifier.draggableHandle(
                                    onDragStarted = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDragStopped = {
                                        haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    },
                                ),
                            onClick = {},
                            onLongClick = {},
                            onMore = {},
                        )
                    }
                }
            } else {
                items(
                    count = browseSongs.itemCount,
                    key = { index ->
                        browseSongs.peek(index)?.mediaStoreId ?: "song_placeholder_$index"
                    },
                    contentType = { "song" },
                ) { index ->
                    val entrance = rememberEntrance(min(4 + index, 8), needAnim = listEntrancePlay)
                    val song = browseSongs[index]
                    if (song == null) {
                        SongSkeletonRow(modifier = Modifier.animateItem())
                    } else {
                        PlaylistSongRow(
                            song = song,
                            isPlaying = playingMediaId == song.mediaStoreId.toString(),
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selectedSongs.contains(song.mediaStoreId),
                            isDragging = false,
                            isReorderEnabled = false,
                            dragHandleModifier = Modifier,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSongSelection(song.mediaStoreId)
                                } else {
                                    viewModel.playSongInList(song)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.toggleMultiSelectMode()
                                }
                                viewModel.toggleSongSelection(song.mediaStoreId)
                            },
                            onMore = { path.push(SongMenuScene(song)) },
                            modifier =
                                Modifier
                                    .animateItem(
                                        fadeInSpec = tween(240, easing = FastOutSlowInEasing),
                                        placementSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                                visibilityThreshold = IntOffset.VisibilityThreshold,
                                            ),
                                        fadeOutSpec = tween(160),
                                    ).entranceGraphics(entrance),
                        )
                    }
                }
            }
        }

        // ── [L2] 搜索覆盖层（不透明面板，独立列表状态，退出即销毁）────────────────
        AnimatedVisibility(
            visibleState = searchTransition,
            modifier = Modifier.fillMaxSize(),
            enter =
                fadeIn(tween(220, easing = EaseOutCubic)) +
                    slideInVertically(tween(220, easing = EaseOutCubic)) { -it / 24 },
            exit =
                fadeOut(tween(150)) +
                    slideOutVertically(tween(150)) { -it / 24 },
        ) {
            SearchOverlay(
                viewModel = viewModel,
                topPadding = statusBarTop + HEADER_HEIGHT,
                playingMediaId = playingMediaId,
                onPlay = viewModel::playSongInList,
                onMore = { song -> path.push(SongMenuScene(song)) },
            )
        }

        // ── [L3] 浮动封面（滚动直接映射，全部运动收敛在一个 graphicsLayer）─────────
        val collapsedScale = COVER_COLLAPSED / coverExpanded
        val coverAlpha by animateFloatAsState(
            targetValue = if (isSearchMode) 0f else 1f,
            animationSpec = tween(150),
            label = "coverAlpha",
        )
        Box(
            Modifier
                .padding(
                    start = coverStartExpanded,
                    top = statusBarTop + HEADER_HEIGHT + Spacing.Small,
                ).size(coverExpanded)
                .graphicsLayer {
                    val p = collapseProgress()
                    val s = lerp(1f, collapsedScale, p)
                    transformOrigin = TransformOrigin(0f, 0f)
                    scaleX = s
                    scaleY = s
                    translationX = lerp(0f, (COVER_COLLAPSED_START - coverStartExpanded).toPx(), p)
                    // 折叠终点在顶栏内容区内垂直居中（状态栏高度在展开/折叠位中相消）
                    translationY =
                        lerp(
                            0f,
                            ((HEADER_HEIGHT - COVER_COLLAPSED) / 2 - HEADER_HEIGHT - Spacing.Small).toPx(),
                            p,
                        )
                    alpha = coverAlpha
                    clip = true
                    // 视觉圆角全程线性 16dp → 8dp（除以缩放补偿 graphicsLayer 的整体缩放）
                    shape = RoundedCornerShape(lerp(16.dp.toPx(), 8.dp.toPx(), p) / s)
                },
        ) {
            PlaylistCoverView(
                albumIds = coverAlbumIds,
                modifier = Modifier.fillMaxSize(),
                iconSize = coverExpanded * 0.35f,
            )
        }

        // ── [L4] 固定顶栏（背景在 draw 阶段合成滚动/模式两路 alpha）────────────────
        val topBarState =
            when {
                isSearchMode -> TopBarState.Search
                isSortMode -> TopBarState.Sort
                else -> TopBarState.Browse
            }
        // 模式态（搜索/排序）强制不透明；离散切换走补间，避免退出模式时背景跳变
        val modeBarAlpha by animateFloatAsState(
            targetValue = if (topBarState != TopBarState.Browse) 1f else 0f,
            animationSpec = tween(200),
            label = "modeBarAlpha",
        )
        val topBarBg = MaterialTheme.colorScheme.background
        val hairlineColor = MaterialTheme.colorScheme.outlineVariant
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + HEADER_HEIGHT)
                .align(Alignment.TopStart)
                .drawBehind {
                    val scrollAlpha = ((collapseProgress() - 0.55f) / 0.45f).coerceIn(0f, 1f)
                    val bgAlpha = maxOf(scrollAlpha, modeBarAlpha)
                    drawRect(topBarBg.copy(alpha = bgAlpha))
                    // 折叠完成后浮现的底部发丝线（搜索态由覆盖层自己绘制）
                    if (!isSearchMode) {
                        drawRect(
                            color = hairlineColor.copy(alpha = 0.14f * bgAlpha),
                            topLeft =
                                androidx.compose.ui.geometry
                                    .Offset(0f, size.height - 1.dp.toPx()),
                            size = Size(size.width, 1.dp.toPx()),
                        )
                    }
                },
        ) {
            AnimatedContent(
                targetState = topBarState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(HEADER_HEIGHT)
                        .align(Alignment.BottomCenter),
                transitionSpec = { materialSharedAxisZ(forward = true) },
                label = "playlistTopBar",
            ) { state ->
                when (state) {
                    TopBarState.Browse ->
                        BrowseTopBar(
                            title = displayName,
                            collapseProgress = collapseProgress,
                            showSearchAction = !isMultiSelectMode && !isPlaylistEmpty,
                            isMultiSelectMode = isMultiSelectMode,
                            isPlaylistEmpty = isPlaylistEmpty,
                            showMoreOptionsMenu = showMoreOptionsMenu,
                            onBack = { path.popTop() },
                            viewModel = viewModel,
                        )

                    TopBarState.Search ->
                        SearchTopBar(
                            keyword = viewModel.searchKeyword.collectAsStateWithLifecycle().value,
                            focusRequester = searchFocusRequester,
                            onKeywordChange = viewModel::updateSearchKeyword,
                            onClear = { viewModel.updateSearchKeyword("") },
                            onBack = { exitSearch() },
                            onImeSearch = { keyboardController?.hide() },
                        )

                    TopBarState.Sort ->
                        SortTopBar(
                            onCancel = viewModel::cancelSortMode,
                            onDone = viewModel::finishSortMode,
                        )
                }
            }
        }

        // ── [L5] 多选底部操作栏 ────────────────────────────────────────────────
        AnimatedVisibility(
            visible = isMultiSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter =
                slideInVertically(
                    spring(
                        dampingRatio = 0.9f,
                        stiffness = Spring.StiffnessMediumLow,
                        visibilityThreshold = IntOffset.VisibilityThreshold,
                    ),
                ) { it } + fadeIn(tween(150)),
            exit =
                slideOutVertically(tween(180, easing = EaseOutCubic)) { it } +
                    fadeOut(tween(120)),
        ) {
            MultiSelectBar(
                selectedCount = selectedSongs.size,
                onClose = viewModel::toggleMultiSelectMode,
                onSelectAll = viewModel::selectAll,
                onDeselectAll = viewModel::deselectAll,
                onRemove = viewModel::removeSelectedSongs,
            )
        }
    }

    // ── 重命名对话框 ───────────────────────────────────────────────────────────
    if (showRenameDialog) {
        RenameDialog(
            initialName = displayName,
            onConfirm = viewModel::renamePlaylist,
            onDismiss = viewModel::hideRenameDialog,
            title = stringResource(R.string.rename_playlist_dialog_title),
        )
    }

    // ── 删除确认对话框 ─────────────────────────────────────────────────────────
    if (showDeleteConfirmDialog) {
        DeleteConfirmDialog(
            playlistName = displayName,
            onConfirm = viewModel::deletePlaylist,
            onDismiss = viewModel::hideDeleteConfirmDialog,
        )
    }

    // ── 添加歌曲底部弹窗 ───────────────────────────────────────────────────────
    if (showAddSongsSheet) {
        SongPickerBottomSheet(
            viewModel = viewModel,
            onDismiss = viewModel::hideAddSongsSheet,
        )
    }

    if (sortModeLimitExceeded) {
        AlertDialog(
            onDismissRequest = viewModel::hideSortModeLimitExceeded,
            title = { Text(stringResource(R.string.sort_mode_limit_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.sort_mode_limit_message,
                        PlaylistDetailViewModel.SORT_MODE_FULL_LIST_LIMIT,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::hideSortModeLimitExceeded) {
                    Text(stringResource(R.string.confirm))
                }
            },
        )
    }
}

// ── 顶栏：浏览态 ──────────────────────────────────────────────────────────────

@Composable
private fun BrowseTopBar(
    title: String,
    collapseProgress: Density.() -> Float,
    showSearchAction: Boolean,
    isMultiSelectMode: Boolean,
    isPlaylistEmpty: Boolean,
    showMoreOptionsMenu: Boolean,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(end = Spacing.ExtraSmall)
                    .graphicsLayer {
                        val a = ((collapseProgress() - 0.6f) / 0.3f).coerceIn(0f, 1f)
                        alpha = a
                        translationY = (1f - a) * 4.dp.toPx()
                    },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 搜索入口：多选中 / 空歌单时用等宽占位换出，布局不跳动
        AnimatedContent(
            targetState = showSearchAction,
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "searchAction",
        ) { show ->
            if (show) {
                IconButton(onClick = viewModel::enterSearchMode) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
        }
        Box {
            IconButton(onClick = viewModel::showMoreOptionsMenu) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            DropdownMenu(
                expanded = showMoreOptionsMenu,
                onDismissRequest = viewModel::hideMoreOptionsMenu,
                shape = RoundedCornerShape(22.dp),
                offset = DpOffset(x = (-12).dp, y = 0.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
            ) {
                PlaylistDropdownMenuItem(
                    text = stringResource(R.string.add_songs),
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    onClick = {
                        viewModel.hideMoreOptionsMenu()
                        viewModel.showAddSongsSheet()
                    },
                )
                if (isMultiSelectMode) {
                    PlaylistDropdownMenuItem(
                        text = stringResource(R.string.select_all),
                        icon = Icons.Default.CheckBox,
                        onClick = {
                            viewModel.hideMoreOptionsMenu()
                            viewModel.selectAll()
                        },
                    )
                    PlaylistDropdownMenuItem(
                        text = stringResource(R.string.deselect_all),
                        icon = Icons.Default.CheckBoxOutlineBlank,
                        onClick = {
                            viewModel.hideMoreOptionsMenu()
                            viewModel.deselectAll()
                        },
                    )
                } else if (!isPlaylistEmpty) {
                    PlaylistDropdownMenuItem(
                        text = stringResource(R.string.sort_songs),
                        icon = Icons.Default.DragIndicator,
                        onClick = viewModel::enterSortMode,
                    )
                    PlaylistDropdownMenuItem(
                        text = stringResource(R.string.multi_select),
                        icon = Icons.Default.CheckBoxOutlineBlank,
                        onClick = {
                            viewModel.hideMoreOptionsMenu()
                            viewModel.toggleMultiSelectMode()
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                )
                PlaylistDropdownMenuItem(
                    text = stringResource(R.string.rename),
                    icon = Icons.Default.Edit,
                    onClick = {
                        viewModel.hideMoreOptionsMenu()
                        viewModel.showRenameDialog()
                    },
                )
                PlaylistDropdownMenuItem(
                    text = stringResource(R.string.delete_playlist_title),
                    icon = Icons.Default.Delete,
                    destructive = true,
                    onClick = {
                        viewModel.hideMoreOptionsMenu()
                        viewModel.showDeleteConfirmDialog()
                    },
                )
            }
        }
    }
}

// ── 顶栏：搜索态（胶囊输入框，与全局搜索页同款）─────────────────────────────────

@Composable
private fun SearchTopBar(
    keyword: String,
    focusRequester: FocusRequester,
    onKeywordChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onImeSearch: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(start = Spacing.ExtraSmall, end = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBackIosNew,
                contentDescription = stringResource(R.string.close_search_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Row(
            modifier =
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(start = Spacing.Large, end = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (keyword.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_in_playlist_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            // 清除按钮与等宽占位 Z 轴切换，输入首字时布局不跳动
            AnimatedContent(
                targetState = keyword.isNotEmpty(),
                transitionSpec = { materialSharedAxisZ(forward = true) },
                label = "searchClear",
            ) { showClear ->
                if (showClear) {
                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickHighlight(onClick = onClear),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                                    ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(R.string.clear_input),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.size(40.dp))
                }
            }
        }
    }
}

// ── 顶栏：排序态 ──────────────────────────────────────────────────────────────

@Composable
private fun SortTopBar(
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(end = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.sort_songs),
            modifier =
                Modifier
                    .weight(1f)
                    .padding(start = Spacing.Small),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        // 模式唯一出口，实底权重
        Button(
            onClick = onDone,
            modifier = Modifier.height(40.dp),
            contentPadding = PaddingValues(horizontal = Spacing.Large),
        ) {
            Text(stringResource(R.string.done_sorting))
        }
    }
}

// ── 更多菜单条目 ──────────────────────────────────────────────────────────────

@Composable
private fun PlaylistDropdownMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val contentColor =
        if (destructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    val iconContainerColor =
        if (destructive) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        }
    val iconColor =
        if (destructive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }

    DropdownMenuItem(
        modifier =
            Modifier
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .width(220.dp)
                .clip(Shapes.LargeCornerBasedShape),
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = contentColor,
            )
        },
        leadingIcon = {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(11.dp),
                color = iconContainerColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        colors =
            MenuDefaults.itemColors(
                textColor = contentColor,
                leadingIconColor = iconColor,
            ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        onClick = onClick,
    )
}

// ── 操作行：播放 / 添加（排序中就地换成提示胶囊）────────────────────────────────

@Composable
private fun ActionRow(
    isSortMode: Boolean,
    playEnabled: Boolean,
    onPlayAll: () -> Unit,
    onAddSongs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = isSortMode,
        modifier = modifier.fillMaxWidth(),
        transitionSpec = { materialSharedAxisZ(forward = true) },
        label = "actionRow",
    ) { sorting ->
        if (sorting) {
            SortHintCapsule(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
            )
        } else {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                // 页面主行动点用实底按钮，添加为次级
                Button(
                    onClick = onPlayAll,
                    enabled = playEnabled,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    Text(stringResource(R.string.play_all_songs))
                }
                FilledTonalButton(
                    onClick = onAddSongs,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.ExtraSmall))
                    Text(stringResource(R.string.add_songs))
                }
            }
        }
    }
}

/** 排序模式的就地提示：出现在按钮行原位，提示出现在视线所在处 */
@Composable
private fun SortHintCapsule(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .clip(Shapes.LargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Icon(
            Icons.Default.DragIndicator,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.sort_mode_hint),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ── 空歌单引导 ────────────────────────────────────────────────────────────────

@Composable
private fun EmptyPlaylistHint(
    onAddSongs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        FloatingHintIcon(icon = Icons.Default.MusicNote)
        Text(
            text = stringResource(R.string.playlist_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.playlist_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        FilledTonalButton(onClick = onAddSongs) {
            Icon(
                Icons.AutoMirrored.Filled.PlaylistAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.ExtraSmall))
            Text(stringResource(R.string.add_songs))
        }
    }
}

/** 空态/引导共用的浮动图标（±5dp 上下浮动 + ±6° 摆动，全局空态同款节奏） */
@Composable
private fun FloatingHintIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val floatTransition = rememberInfiniteTransition(label = "hintFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "hintBob",
    )
    Box(
        modifier =
            modifier
                .size(56.dp)
                .graphicsLayer {
                    translationY = bob * 5.dp.toPx()
                    rotationZ = bob * 6f
                }.clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

// ── 歌曲列表行（浏览 / 多选 / 排序共用一个家族）─────────────────────────────────

@Composable
private fun PlaylistSongRow(
    song: Song,
    isPlaying: Boolean,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    isDragging: Boolean,
    isReorderEnabled: Boolean,
    @SuppressLint("ModifierParameter")
    dragHandleModifier: Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground =
        when {
            isDragging -> MaterialTheme.colorScheme.surfaceContainerHighest
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            isPlaying -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
            else -> Color.Transparent
        }
    val dragElevation by animateDpAsState(
        targetValue = if (isDragging) 8.dp else 0.dp,
        label = "dragElevation",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium)
                .shadow(dragElevation, Shapes.MediumCornerBasedShape)
                .clip(Shapes.MediumCornerBasedShape)
                .background(rowBackground)
                .combinedClickHighlight(
                    enabled = !isReorderEnabled,
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter =
                expandHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) +
                    fadeIn(tween(180)),
            exit = shrinkHorizontally(tween(150)) + fadeOut(tween(120)),
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
            )
        }
        SongCoverImage(song = song)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color =
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isMultiSelectMode) {
            if (isReorderEnabled) {
                Icon(
                    Icons.Default.DragIndicator,
                    contentDescription = stringResource(R.string.drag_to_reorder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        dragHandleModifier
                            .clip(CircleShape)
                            .padding(Spacing.Medium)
                            .size(22.dp),
                )
            } else {
                IconButton(onClick = onMore) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 歌曲封面（48dp，失败时渲染专辑占位） */
@Composable
private fun SongCoverImage(song: Song) {
    LandscapistImage(
        imageModel = { song.getCoverUri() },
        modifier =
            Modifier
                .size(48.dp)
                .clip(Shapes.SmallCornerBasedShape),
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Album,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        },
    )
}

/** 骨架行：与歌曲行同几何的呼吸占位（alpha 在 Draw 阶段读取） */
@Composable
private fun SongSkeletonRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "songSkeleton")
    val breath by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "songSkeletonBreath",
    )
    val bone = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium)
                .graphicsLayer { alpha = breath }
                .clip(Shapes.MediumCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(Shapes.SmallCornerBasedShape)
                .background(bone),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .clip(Shapes.SmallCornerBasedShape)
                    .background(bone),
            )
            Box(
                Modifier
                    .fillMaxWidth(0.32f)
                    .height(10.dp)
                    .clip(Shapes.SmallCornerBasedShape)
                    .background(bone),
            )
        }
    }
}

// ── 搜索覆盖层 ────────────────────────────────────────────────────────────────

/**
 * 不透明搜索面板：盖在浏览列表之上，持有自己的 LazyListState，
 * 退出搜索随覆盖层一起销毁 —— 主列表的滚动位置零污染。
 */
@Composable
private fun SearchOverlay(
    viewModel: PlaylistDetailViewModel,
    topPadding: Dp,
    playingMediaId: String?,
    onPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val searchInput by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val debouncedKeyword by viewModel.debouncedKeyword.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant

    // 只有 refresh 完成且确实没有条目才算"无结果"；
    // 防抖窗口内（输入 != 防抖值）显示骨架，杜绝旧结果停留与空态闪现
    val contentState by remember(searchResults) {
        derivedStateOf {
            when {
                searchInput.isBlank() -> SearchContentState.Idle

                searchInput.trim() != debouncedKeyword -> SearchContentState.Loading

                searchResults.loadState.refresh is LoadState.Loading &&
                    searchResults.itemCount == 0 -> SearchContentState.Loading

                searchResults.itemCount == 0 -> SearchContentState.Empty

                else -> SearchContentState.Results
            }
        }
    }

    // 滚动结果列表时收起键盘，把屏幕还给内容
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
    }

    // 新一轮查询生效时回到列表顶部（内容整体经 AnimatedContent 淡入，无感知）
    LaunchedEffect(debouncedKeyword) {
        if (debouncedKeyword.isNotBlank()) {
            listState.scrollToItem(0)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = topPadding)
                .background(MaterialTheme.colorScheme.background)
                // 兜底消费点击，防手势穿透到下层浏览列表
                .pointerInput(Unit) { detectTapGestures { } }
                .drawBehind {
                    // 顶部发丝线：结果列表滚动后浮现（与顶栏底边同位置）
                    val progress =
                        if (listState.firstVisibleItemIndex > 0) {
                            1f
                        } else {
                            (listState.firstVisibleItemScrollOffset / 24.dp.toPx())
                                .coerceIn(0f, 1f)
                        }
                    drawRect(
                        color = hairlineColor.copy(alpha = 0.14f * progress),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                },
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .imePadding(),
        ) {
            AnimatedContent(
                targetState = contentState,
                transitionSpec = {
                    (
                        fadeIn(tween(220, easing = EaseOutCubic)) +
                            slideInVertically(tween(220, easing = EaseOutCubic)) { it / 12 }
                    ).togetherWith(fadeOut(tween(120)))
                },
                label = "searchContent",
            ) { state ->
                when (state) {
                    SearchContentState.Idle ->
                        SearchIdleHint(
                            songCount = songCount,
                            modifier = Modifier.fillMaxSize(),
                        )

                    SearchContentState.Loading ->
                        SearchSkeletonList(modifier = Modifier.fillMaxSize())

                    SearchContentState.Empty ->
                        SearchNoResultHint(
                            query = debouncedKeyword,
                            modifier = Modifier.fillMaxSize(),
                        )

                    SearchContentState.Results ->
                        SearchResultList(
                            listState = listState,
                            searchResults = searchResults,
                            keyword = debouncedKeyword,
                            playingMediaId = playingMediaId,
                            onPlay = onPlay,
                            onMore = onMore,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
            }
        }
    }
}

@Composable
private fun SearchResultList(
    listState: LazyListState,
    searchResults: LazyPagingItems<Song>,
    keyword: String,
    playingMediaId: String?,
    onPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val needAnim = remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(55)
        needAnim.value = false
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        contentPadding =
            PaddingValues(
                start = Spacing.Large,
                end = Spacing.Large,
                top = Spacing.Small,
                bottom = 120.dp,
            ),
    ) {
        items(
            count = searchResults.itemCount,
            key = { index ->
                searchResults.peek(index)?.mediaStoreId ?: "search_placeholder_$index"
            },
            contentType = { "song" },
        ) { index ->
            val song = searchResults[index]
            val entrance = rememberEntrance(min(index, 8), needAnim = needAnim.value)
            if (song == null) {
                SongSkeletonRow(
                    modifier =
                        Modifier
                            .animateItem()
                            .entranceGraphics(entrance),
                )
            } else {
                SearchResultRow(
                    song = song,
                    keyword = keyword,
                    isPlaying = playingMediaId == song.mediaStoreId.toString(),
                    onPlay = { onPlay(song) },
                    onMore = { onMore(song) },
                    modifier =
                        Modifier
                            .animateItem()
                            .entranceGraphics(entrance),
                )
            }
        }
        if (searchResults.loadState.append is LoadState.Loading) {
            item(key = "append_loading", contentType = "append_loading") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.Large),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** 搜索结果行：圆角卡片 + 关键词高亮，长按也可打开菜单（与全局搜索页心智一致） */
@Composable
private fun SearchResultRow(
    song: Song,
    keyword: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.MediumCornerBasedShape)
                .background(
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ).combinedClickHighlight(onLongClick = onMore, onClick = onPlay)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        SongCoverImage(song = song)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlightKeyword(song.displayName, keyword),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.W500,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = highlightKeyword(song.artist, keyword),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onMore) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 空关键词引导：浮动图标 + 提示 + 歌曲数副行 */
@Composable
private fun SearchIdleHint(
    songCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        FloatingHintIcon(icon = Icons.Default.Search)
        Text(
            text = stringResource(R.string.search_in_playlist_hint),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.songs_count, songCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 无结果：仅在结果确实为空时出现 */
@Composable
private fun SearchNoResultHint(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        FloatingHintIcon(icon = Icons.Default.SearchOff)
        Text(
            text = stringResource(R.string.search_no_results_format, query),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.search_try_different_keywords),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 搜索加载骨架：与结果行同几何的呼吸占位 */
@Composable
private fun SearchSkeletonList(
    modifier: Modifier = Modifier,
    rowCount: Int = 8,
) {
    Column(
        modifier =
            modifier
                .padding(horizontal = Spacing.ExtraSmall)
                .padding(top = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        repeat(rowCount) {
            SongSkeletonRow()
        }
    }
}

// ── 多选底部操作栏 ─────────────────────────────────────────────────────────────

@Composable
private fun MultiSelectBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.exit_multiselect_cd),
                )
            }
            Text(
                text = stringResource(R.string.selected_songs_count_format, selectedCount),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onSelectAll) { Text(stringResource(R.string.select_all)) }
            TextButton(onClick = onDeselectAll) { Text(stringResource(R.string.cancel)) }
            Button(
                onClick = onRemove,
                enabled = selectedCount > 0,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.ExtraSmall))
                Text(stringResource(R.string.remove))
            }
        }
    }
}

// ── 重命名对话框 ───────────────────────────────────────────────────────────────

@Composable
fun RenameDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                colors =
                    TextFieldDefaults.colors().copy(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ── 删除确认对话框 ─────────────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    playlistName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_playlist_title)) },
        text = { Text(stringResource(R.string.confirm_delete_playlist_full, playlistName)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ── 添加歌曲选择器底部弹窗 ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerBottomSheet(
    viewModel: PlaylistDetailViewModel,
    onDismiss: () -> Unit,
) {
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var pickerKeyword by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(pickerKeyword) {
        viewModel.updatePickerKeyword(pickerKeyword)
    }

    val pickerSongs = viewModel.pickerSongsPaging.collectAsLazyPagingItems()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, top = 20.dp, end = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = stringResource(R.string.add_songs),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text =
                            if (selectedIds.isEmpty()) {
                                stringResource(R.string.add_songs_from_library_hint)
                            } else {
                                stringResource(R.string.songs_count_to_add_format, selectedIds.size)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                }
            }

            TextField(
                value = pickerKeyword,
                colors =
                    TextFieldDefaults.colors().copy(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                onValueChange = { pickerKeyword = it },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                placeholder = { Text(stringResource(R.string.search_songs_artists_albums)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon =
                    if (pickerKeyword.isNotEmpty()) {
                        {
                            IconButton(onClick = { pickerKeyword = "" }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.clear_input),
                                )
                            }
                        }
                    } else {
                        null
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {}),
                shape = RoundedCornerShape(24.dp),
            )

            AnimatedContent(selectedIds.isNotEmpty()) {
                if (it) {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Default.CheckBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text =
                                    stringResource(
                                        R.string.will_add_songs_format,
                                        selectedIds.size,
                                    ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { selectedIds = emptySet() }) {
                                Text(stringResource(R.string.deselect_all))
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 112.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(count = pickerSongs.itemCount, key = { index ->
                        pickerSongs[index]?.mediaStoreId ?: index
                    }) { index ->
                        val song = pickerSongs[index] ?: return@items
                        val isSelected = selectedIds.contains(song.mediaStoreId)
                        PickerSongRow(
                            modifier = Modifier.animateItem(),
                            song = song,
                            isSelected = isSelected,
                            onToggle = {
                                selectedIds =
                                    if (isSelected) {
                                        selectedIds - song.mediaStoreId
                                    } else {
                                        selectedIds + song.mediaStoreId
                                    }
                            },
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            // 键盘弹出时操作条上浮，"添加 N 首"不被遮挡
                            .imePadding()
                            .navigationBarsPadding(),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { viewModel.addSongsToPlaylist(selectedIds.toList()) },
                            enabled = selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1.5f),
                        ) {
                            Text(
                                if (selectedIds.isEmpty()) {
                                    stringResource(
                                        R.string.select_songs_button,
                                    )
                                } else {
                                    stringResource(R.string.add_n_songs_format, selectedIds.size)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── 选择器歌曲行 ───────────────────────────────────────────────────────────────

@Composable
private fun PickerSongRow(
    song: Song,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = rowBackground,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .clickHighlight(onClick = onToggle)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LandscapistImage(
                imageModel = { song.getCoverUri() },
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                success = { _, painter ->
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                failure = {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

/** 首屏入场：延迟 [order] 个节拍后弹入*/
@Composable
private fun rememberEntrance(
    order: Int,
    needAnim: Boolean = true,
): Animatable<Float, AnimationVector1D> {
    val entrance = remember { Animatable(if (needAnim) 0f else 1f) }
    LaunchedEffect(Unit) {
        delay(order * 55L)
        entrance.animateTo(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = 380f,
                ),
        )
    }
    return entrance
}

/** 入场位移+淡入，动画值全部在 Draw 阶段读取 */
private fun Modifier.entranceGraphics(entrance: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        val enter = entrance.value
        alpha = enter
        translationY = (1f - enter) * 28.dp.toPx()
    }.blur(radius = ((1f - entrance.value) * 4).dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
