package me.spica27.spicamusic.ui.favorite

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HeartBroken
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel

/**
 * 我的收藏 独立页面（全屏）
 *
 * 「刊头收藏」排版：杂志刊头式大标题 + 双药丸操作行 + 搜索胶囊 + 通栏歌曲列表，
 * 纯色背景、零描边，计数全页唯一。
 */
class FavoriteScene : StackScene() {
    @Composable
    override fun Content() {
        FavoriteScreenContent()
    }
}

/** 首屏入场交错间隔 */
private const val ENTRANCE_STAGGER_MILLIS = 55L

/** 参与入场编排的最大歌曲行数（之后出现的行走 animateItem 淡入） */
private const val ENTRANCE_MAX_ROW = 8

/** 首屏元素在编排中的槽位：刊头=0 操作行=1 搜索=2 歌曲行从 3 开始 */
private const val ENTRANCE_ORDER_ROW_BASE = 3

/** 大标题完全收进顶栏所需的滚动距离 */
private val MastheadCollapseDistance = 140.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoriteScreenContent() {
    val path = LocalNavigationPath.current
    val viewModel: FavoriteViewModel = koinViewModel()
    val songs = viewModel.favoriteSongs.collectAsLazyPagingItems()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showSavePlaylistDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (isMultiSelectMode) {
            viewModel.exitMultiSelectMode()
        } else {
            path.popTop()
        }
    }

    LaunchedEffect(snackbarMessage) {
        val message = snackbarMessage ?: return@LaunchedEffect
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT).show()
        viewModel.clearSnackbar()
    }

    // 入场编排只在页面首次呈现时播放一次
    var entrancePlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(950)
        entrancePlayed = true
    }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 布尔量化的派生状态：只在刊头完全滚出首位时翻转一次
    val mastheadGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + 56.dp,
                    bottom = 200.dp,
                ),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "favorites_masthead") {
                val entrance = rememberEntrance(order = 0, play = !entrancePlayed)
                FavoriteMasthead(
                    songCount = songCount,
                    searching = searchKeyword.isNotBlank(),
                    showMultiSelect = songCount > 0,
                    isMultiSelectMode = isMultiSelectMode,
                    onToggleMultiSelect = {
                        if (isMultiSelectMode) {
                            viewModel.exitMultiSelectMode()
                        } else {
                            viewModel.enterMultiSelectMode()
                        }
                    },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .graphicsLayer {
                                // 跟手收缩：大标题缩小、上移、淡出，直接耦合滚动偏移
                                val t = mastheadCollapse(listState)
                                val enter = entrance.value
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = (1f - t) * enter
                                translationY = -t * 16.dp.toPx() + (1f - enter) * 28.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }

            item(key = "favorites_actions") {
                val entrance = rememberEntrance(order = 1, play = !entrancePlayed)
                FavoriteActionRow(
                    enabled = songCount > 0,
                    onPlayAll = { viewModel.playAllSongs() },
                    onShufflePlay = { viewModel.shufflePlayAllSongs() },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .entranceGraphics(entrance),
                )
            }

            item(key = "favorites_search") {
                val entrance = rememberEntrance(order = 2, play = !entrancePlayed)
                FavoriteSearchPill(
                    keyword = searchKeyword,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onClear = viewModel::clearSearch,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .entranceGraphics(entrance),
                )
            }

            if (songs.loadState.refresh is LoadState.Loading) {
                item(key = "favorites_skeleton") {
                    val entrance = rememberEntrance(order = ENTRANCE_ORDER_ROW_BASE, play = !entrancePlayed)
                    FavoriteSkeletonRows(
                        modifier =
                            Modifier
                                .padding(top = Spacing.Large)
                                .entranceGraphics(entrance),
                    )
                }
            } else if (songs.itemCount == 0) {
                item(key = "favorites_empty") {
                    val entrance = rememberEntrance(order = ENTRANCE_ORDER_ROW_BASE, play = !entrancePlayed)
                    FavoriteEmptyState(
                        title =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.favorites_empty_title)
                            } else {
                                stringResource(R.string.favorites_search_empty)
                            },
                        subtitle =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.favorites_empty_subtitle)
                            } else {
                                searchKeyword
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .animateItem()
                                .entranceGraphics(entrance),
                    )
                }
            } else {
                items(
                    count = songs.itemCount,
                    key = { index -> songs.peek(index)?.mediaStoreId ?: "favorite_placeholder_$index" },
                    contentType = { "favorite_song" },
                ) { index ->
                    val song = songs[index]
                    if (song != null) {
                        val entrance =
                            rememberEntrance(
                                order = index + ENTRANCE_ORDER_ROW_BASE,
                                play = !entrancePlayed && index < ENTRANCE_MAX_ROW,
                            )
                        val selected = selectedSongIds.contains(song.mediaStoreId)
                        FavoriteSongRow(
                            song = song,
                            isMultiSelectMode = isMultiSelectMode,
                            isSelected = selected,
                            onClick = {
                                if (isMultiSelectMode) {
                                    viewModel.toggleSongSelection(song.mediaStoreId)
                                } else {
                                    viewModel.playAllSongs(song.mediaStoreId)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    path.push(SongMenuScene(song))
                                }
                            },
                            onRemoveFavorite = { viewModel.toggleFavorite(song) },
                            modifier =
                                Modifier
                                    .animateItem(
                                        fadeInSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                                        placementSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioLowBouncy,
                                                stiffness = Spring.StiffnessMediumLow,
                                                visibilityThreshold = IntOffset.VisibilityThreshold,
                                            ),
                                        fadeOutSpec = tween(durationMillis = 160),
                                    ).entranceGraphics(entrance),
                        )
                    }
                }

                if (songs.loadState.append is LoadState.Loading) {
                    item(key = "favorites_append_loading") {
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

        FavoriteTopBar(
            title = stringResource(R.string.favorites_title),
            listState = listState,
            solid = mastheadGone,
            showPlayAll = mastheadGone && songCount > 0 && !isMultiSelectMode,
            onPlayAll = { viewModel.playAllSongs() },
            onBack = { path.popTop() },
            modifier = Modifier.align(Alignment.TopStart),
        )

        AnimatedVisibility(
            visible = isMultiSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(durationMillis = 200)),
            exit = slideOutVertically { it } + fadeOut(tween(durationMillis = 160)),
        ) {
            FavoriteMultiSelectBar(
                selectedCount = selectedCount,
                onClose = { viewModel.exitMultiSelectMode() },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onPlay = {
                    viewModel.playSelectedSongs()
                    viewModel.exitMultiSelectMode()
                },
                onSaveAsPlaylist = { showSavePlaylistDialog = true },
                onRemove = { viewModel.dislikeSelectedSongs() },
            )
        }
    }

    if (showSavePlaylistDialog) {
        SavePlaylistDialog(
            initialName = stringResource(R.string.finder_favorites_playlist_name),
            onConfirm = { name ->
                viewModel.createPlaylistFromSelected(name)
                viewModel.exitMultiSelectMode()
                showSavePlaylistDialog = false
            },
            onDismiss = { showSavePlaylistDialog = false },
        )
    }
}

/** 大标题收缩进度：0f=完全展开 1f=完全收进顶栏（在 Draw 阶段读取，滚动零重组） */
private fun Density.mastheadCollapse(listState: LazyListState): Float =
    if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / MastheadCollapseDistance.toPx()).coerceIn(0f, 1f)
    }

/** 首屏入场：延迟 [order] 个节拍后弹入，[play] 为 false 时直接呈现 */
@Composable
private fun rememberEntrance(
    order: Int,
    play: Boolean,
): Animatable<Float, AnimationVector1D> {
    val entrance = remember { Animatable(if (play) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (entrance.value < 1f) {
            delay(order * ENTRANCE_STAGGER_MILLIS)
            entrance.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 380f,
                    ),
            )
        }
    }
    return entrance
}

/** 入场位移+淡入，全部在 Draw 阶段读取动画值 */
private fun Modifier.entranceGraphics(entrance: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        val enter = entrance.value
        alpha = enter
        translationY = (1f - enter) * 28.dp.toPx()
    }

/** 一次“扑通-扑通”双搏脉冲（点击播放时的心跳签名） */
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
private fun FavoriteTopBar(
    title: String,
    listState: LazyListState,
    solid: Boolean,
    showPlayAll: Boolean,
    onPlayAll: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 56.dp)
                .drawBehind {
                    // 背景不透明度在 Draw 阶段跟随滚动，避免每帧重组
                    drawRect(color = backgroundColor.copy(alpha = mastheadCollapse(listState)))
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
                    .padding(top = statusBarTop)
                    .padding(horizontal = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
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
            )
            // 顶栏收起后弹出的迷你“播放全部”胶囊
            AnimatedVisibility(
                visible = showPlayAll,
                enter =
                    scaleIn(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        initialScale = 0.6f,
                    ) + fadeIn(tween(durationMillis = 160)),
                exit =
                    scaleOut(
                        animationSpec = tween(durationMillis = 140),
                        targetScale = 0.8f,
                    ) + fadeOut(tween(durationMillis = 140)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(end = Spacing.Small)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickHighlight(onClick = onPlayAll)
                            .padding(horizontal = Spacing.Medium, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.favorites_play_all),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

/** 刊头：大标题 + meta 行（唯一计数 + 多选入口） */
@Composable
private fun FavoriteMasthead(
    songCount: Int,
    searching: Boolean,
    showMultiSelect: Boolean,
    isMultiSelectMode: Boolean,
    onToggleMultiSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RollingCountText(
                songCount = songCount,
                searching = searching,
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
                            .clickHighlight(onClick = onToggleMultiSelect)
                            .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    AnimatedContent(
                        targetState = isMultiSelectMode,
                        transitionSpec = { materialSharedAxisZ(forward = true) },
                        label = "favoriteMultiSelectPill",
                    ) { active ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                        ) {
                            Icon(
                                imageVector = if (active) Icons.Default.Close else Icons.Default.Checklist,
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

/** 收藏数量：数字变化时上下滚动切换，搜索时文案切「找到 N 首」即实时反馈 */
@Composable
private fun RollingCountText(
    songCount: Int,
    searching: Boolean,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = songCount to searching,
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
        label = "favoriteCountRoll",
    ) { (count, isSearching) ->
        Text(
            text =
                if (isSearching) {
                    stringResource(R.string.favorites_count_found, count)
                } else {
                    stringResource(R.string.favorites_count_total, count)
                },
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

/** 操作行：播放全部（实心）+ 随机播放（tonal）双药丸 */
@Composable
private fun FavoriteActionRow(
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        HeroCommandPill(
            text = stringResource(R.string.favorites_play_all),
            icon = Icons.Default.PlayArrow,
            container = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            enabled = enabled,
            pulseOnClick = true,
            onClick = onPlayAll,
            modifier = Modifier.weight(1f),
        )
        HeroCommandPill(
            text = stringResource(R.string.shuffle_play),
            icon = Icons.Default.Shuffle,
            container = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            enabled = enabled,
            pulseOnClick = false,
            onClick = onShufflePlay,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HeroCommandPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    container: Color,
    contentColor: Color,
    enabled: Boolean,
    pulseOnClick: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // 按压回弹：手指按下轻微缩小，抬起弹性回复
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "commandPillPressScale",
    )
    // 点击瞬间的“扑通-扑通”双搏脉冲
    val pulse = remember { Animatable(1f) }
    val contentAlpha = if (enabled) 1f else 0.42f
    Box(
        modifier =
            modifier
                .height(52.dp)
                .graphicsLayer {
                    val scale = pressScale * pulse.value
                    scaleX = scale
                    scaleY = scale
                }.clip(CircleShape)
                .background(if (enabled) container else container.copy(alpha = 0.45f))
                .clickHighlight(enabled = enabled) {
                    if (pulseOnClick && !pulse.isRunning) {
                        scope.launch { heartThump(pulse) }
                    }
                    onClick()
                },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = contentAlpha),
            )
        }
    }
}

/** 搜索胶囊：44dp 高、不透明、无描边 */
@Composable
private fun FavoriteSearchPill(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = Spacing.Large, end = Spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Spacing.Small))
        Box(modifier = Modifier.weight(1f)) {
            if (keyword.isEmpty()) {
                Text(
                    text = stringResource(R.string.favorites_search_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = keyword,
                onValueChange = onKeywordChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
            )
        }
        AnimatedContent(
            targetState = keyword.isNotBlank(),
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "favoriteSearchClear",
        ) { hasKeyword ->
            if (hasKeyword) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

/** 通栏歌曲行：无卡片无分隔线，多选时左侧滑出勾选框 */
@Composable
private fun FavoriteSongRow(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // 取消收藏：心形先蓄力放大、再收缩消失，随后条目在列表中弹性退场
    val heartScale = remember(song.mediaStoreId) { Animatable(1f) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        Color.Transparent
                    },
                ).combinedClickHighlight(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AnimatedVisibility(visible = isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
            )
        }
        AudioCover(
            uri = song.getCoverUri(),
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(Shapes.MediumCornerBasedShape),
            placeHolder = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(R.string.cover_placeholder),
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
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
        Text(
            text = song.getFormattedDuration(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.widthIn(min = 36.dp),
        )
        AnimatedVisibility(visible = !isMultiSelectMode) {
            IconButton(
                onClick = {
                    if (heartScale.isRunning) return@IconButton
                    scope.launch {
                        heartScale.animateTo(
                            targetValue = 1.28f,
                            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                        )
                        heartScale.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                        )
                        onRemoveFavorite()
                    }
                },
                modifier =
                    Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            scaleX = heartScale.value
                            scaleY = heartScale.value
                        },
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.remove_from_favorites),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 加载骨架：与歌曲行同几何的呼吸占位，保住版面节奏 */
@Composable
private fun FavoriteSkeletonRows(
    modifier: Modifier = Modifier,
    rowCount: Int = 6,
) {
    val transition = rememberInfiniteTransition(label = "favoriteSkeleton")
    val breath by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "favoriteSkeletonBreath",
    )
    val bone = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = modifier.fillMaxWidth()) {
        repeat(rowCount) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Small)
                        .graphicsLayer { alpha = breath },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(Shapes.MediumCornerBasedShape)
                            .background(bone),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.55f)
                                .height(14.dp)
                                .clip(Shapes.SmallCornerBasedShape)
                                .background(bone),
                    )
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth(0.32f)
                                .height(10.dp)
                                .clip(Shapes.SmallCornerBasedShape)
                                .background(bone),
                    )
                }
                Box(
                    modifier =
                        Modifier
                            .width(36.dp)
                            .height(10.dp)
                            .clip(Shapes.SmallCornerBasedShape)
                            .background(bone),
                )
            }
        }
    }
}

/** 空态：开放排版无卡片，音符轻盈浮动 */
@Composable
private fun FavoriteEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val floatTransition = rememberInfiniteTransition(label = "favoriteEmptyFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "favoriteEmptyBob",
    )
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
                        translationY = bob * 5.dp.toPx()
                        rotationZ = bob * 6f
                    }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** 多选底部操作栏 */
@Composable
private fun FavoriteMultiSelectBar(
    selectedCount: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onPlay: () -> Unit,
    onSaveAsPlaylist: () -> Unit,
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
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.exit_multiselect_cd),
                    )
                }
                Text(
                    text = stringResource(R.string.selected_songs_count_format, selectedCount),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onSelectAll) { Text(stringResource(R.string.select_all)) }
                TextButton(onClick = onDeselectAll) { Text(stringResource(R.string.deselect_all)) }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                MultiSelectActionPill(
                    text = stringResource(R.string.play),
                    icon = Icons.Default.PlayArrow,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = selectedCount > 0,
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                )
                MultiSelectActionPill(
                    text = stringResource(R.string.save_as_playlist),
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    enabled = selectedCount > 0,
                    onClick = onSaveAsPlaylist,
                    modifier = Modifier.weight(1f),
                )
                MultiSelectActionPill(
                    text = stringResource(R.string.remove_from_favorites),
                    icon = Icons.Default.HeartBroken,
                    container = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    enabled = selectedCount > 0,
                    onClick = onRemove,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MultiSelectActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
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
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = contentColor.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 输入歌单名称的确认弹窗 */
@Composable
private fun SavePlaylistDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_playlist)) },
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
