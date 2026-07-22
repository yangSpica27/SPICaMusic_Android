package me.spica27.spicamusic.ui.ignoredsongs

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
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
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
 * 已忽略歌曲管理
 */
class IgnoredSongsScene : StackScene() {
    @Composable
    override fun Content() {
        IgnoredSongsScreenContent()
    }
}

/** 首屏入场交错间隔 */
private const val ENTRANCE_STAGGER_MILLIS = 55L

/** 参与入场编排的最大歌曲行数（之后出现的行走 animateItem 淡入） */
private const val ENTRANCE_MAX_ROW = 8

/** 首屏元素在编排中的槽位：刊头=0 搜索=1 歌曲行从 2 开始（无操作行） */
private const val ENTRANCE_ORDER_ROW_BASE = 2

/** 大标题完全收进顶栏所需的滚动距离 */
private val MastheadCollapseDistance = 140.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IgnoredSongsScreenContent() {
    val path = LocalNavigationPath.current
    val viewModel: IgnoredSongsViewModel = koinViewModel()
    val songs by viewModel.ignoredSongs.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val selectedCount by viewModel.selectedCount.collectAsStateWithLifecycle()

    BackHandler {
        if (isMultiSelectMode) {
            viewModel.exitMultiSelectMode()
        } else {
            path.popTop()
        }
    }

    // 入场编排只在页面首次呈现时播放一次
    var entrancePlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        entrancePlayed = true
    }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 只在刊头完全滚出首位时翻转一次
    val mastheadGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    // 列表在加载中
    val songCount = songs?.size ?: 0
    val searching = searchKeyword.isNotBlank()

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
            item(key = "ignored_masthead") {
                val entrance = rememberEntrance(order = 0, play = !entrancePlayed)
                IgnoredSongsMasthead(
                    songCount = songCount,
                    searching = searching,
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

            item(key = "ignored_search") {
                val entrance = rememberEntrance(order = 1, play = !entrancePlayed)
                IgnoredSearchPill(
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

            val list = songs
            if (list == null) {
                item(key = "ignored_skeleton") {
                    val entrance =
                        rememberEntrance(order = ENTRANCE_ORDER_ROW_BASE, play = !entrancePlayed)
                    IgnoredSkeletonRows(
                        modifier =
                            Modifier
                                .padding(top = Spacing.Large)
                                .entranceGraphics(entrance),
                    )
                }
            } else if (list.isEmpty()) {
                item(key = "ignored_empty") {
                    val entrance =
                        rememberEntrance(order = ENTRANCE_ORDER_ROW_BASE, play = !entrancePlayed)
                    IgnoredEmptyState(
                        title =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.ignored_songs_empty_title)
                            } else {
                                stringResource(R.string.ignored_songs_search_empty)
                            },
                        subtitle =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.ignored_songs_empty_subtitle)
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
                itemsIndexed(
                    items = list,
                    key = { _, it -> it.mediaStoreId },
                    contentType = { _, _ -> "ignored_song" },
                ) { index, song ->
                    val entrance =
                        rememberEntrance(
                            order = index + ENTRANCE_ORDER_ROW_BASE,
                            play = !entrancePlayed && index < ENTRANCE_MAX_ROW,
                        )
                    val selected = selectedSongIds.contains(song.mediaStoreId)
                    IgnoredSongRow(
                        song = song,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = selected,
                        onClick = {
                            if (isMultiSelectMode) {
                                viewModel.toggleSongSelection(song.mediaStoreId)
                            } else {
                                viewModel.enterMultiSelectMode(song.mediaStoreId)
                            }
                        },
                        onLongClick = {
                            if (!isMultiSelectMode) {
                                viewModel.enterMultiSelectMode(song.mediaStoreId)
                            }
                        },
                        onRestore = { viewModel.unignoreSong(song) },
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
        }

        IgnoredSongsTopBar(
            title = stringResource(R.string.setting_ignore_music),
            listState = listState,
            solid = mastheadGone,
            onBack = { path.popTop() },
            modifier = Modifier.align(Alignment.TopStart),
        )

        AnimatedVisibility(
            visible = isMultiSelectMode,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(durationMillis = 200)),
            exit = slideOutVertically { it } + fadeOut(tween(durationMillis = 160)),
        ) {
            IgnoredMultiSelectBar(
                selectedCount = selectedCount,
                onClose = { viewModel.exitMultiSelectMode() },
                onSelectAll = { viewModel.selectAll() },
                onDeselectAll = { viewModel.deselectAll() },
                onRemove = {
                    viewModel.unignoreSelectedSongs()
                    viewModel.exitMultiSelectMode()
                },
            )
        }
    }
}

/** 大标题收缩进度：0f=完全展开 1f=完全收进顶栏 */
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

/** 固定顶栏：背景与标题透明度跟随刊头收缩进度，收起后出现分隔线 */
@Composable
private fun IgnoredSongsTopBar(
    title: String,
    listState: LazyListState,
    solid: Boolean,
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
        }
    }
}

/** 刊头：大标题 + meta 行（唯一计数 + 多选入口） */
@Composable
private fun IgnoredSongsMasthead(
    songCount: Int,
    searching: Boolean,
    showMultiSelect: Boolean,
    isMultiSelectMode: Boolean,
    onToggleMultiSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.setting_ignore_music),
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
                        label = "ignoredMultiSelectPill",
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

/** 已忽略数量：数字变化时上下滚动切换，搜索时文案切「找到 N 首」即实时反馈 */
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
        label = "ignoredCountRoll",
    ) { (count, isSearching) ->
        Text(
            text =
                if (isSearching) {
                    stringResource(R.string.ignored_songs_count_found, count)
                } else {
                    stringResource(R.string.ignored_songs_count_total, count)
                },
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

/** 搜索胶囊：44dp 高、不透明、无描边 */
@Composable
private fun IgnoredSearchPill(
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
                    text = stringResource(R.string.ignored_songs_search_hint),
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
            label = "ignoredSearchClear",
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

/** 通栏歌曲行：无卡片无分隔线，多选时左侧滑出勾选框，行尾「恢复」图标取消忽略 */
@Composable
private fun IgnoredSongRow(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    // 取消忽略：图标先蓄力放大、再收缩消失，随后条目在列表中弹性退场
    val restoreScale = remember(song.mediaStoreId) { Animatable(1f) }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Small)
                .clip(Shapes.MediumCornerBasedShape)
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        Color.Transparent
                    },
                ).combinedClickHighlight(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = isMultiSelectMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = Spacing.Small),
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
        Column(modifier = Modifier.weight(1f).padding(horizontal = Spacing.Medium)) {
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
            modifier = Modifier.widthIn(min = 36.dp).padding(end = Spacing.Medium),
        )
        AnimatedVisibility(visible = !isMultiSelectMode) {
            IconButton(
                onClick = {
                    if (restoreScale.isRunning) return@IconButton
                    // 先提交取消忽略（unignoreSong 内部走 viewModelScope，不受行回收影响），
                    // 缩放动画仅作视觉反馈；避免行在动画期间被回收导致 scope 取消、动作丢失。
                    onRestore()
                    scope.launch {
                        restoreScale.animateTo(
                            targetValue = 1.28f,
                            animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
                        )
                        restoreScale.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing),
                        )
                    }
                },
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        .graphicsLayer {
                            scaleX = restoreScale.value
                            scaleY = restoreScale.value
                        },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.title_remove_from_ignore_list),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** 加载骨架：与歌曲行同几何的呼吸占位，保住版面节奏 */
@Composable
private fun IgnoredSkeletonRows(
    modifier: Modifier = Modifier,
    rowCount: Int = 6,
) {
    val transition = rememberInfiniteTransition(label = "ignoredSkeleton")
    val breath by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "ignoredSkeletonBreath",
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
private fun IgnoredEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val floatTransition = rememberInfiniteTransition(label = "ignoredEmptyFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "ignoredEmptyBob",
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
private fun IgnoredMultiSelectBar(
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
                    text = stringResource(R.string.title_remove_from_ignore_list),
                    icon = Icons.Default.MusicNote,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
