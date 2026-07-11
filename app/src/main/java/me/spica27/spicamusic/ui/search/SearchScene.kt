package me.spica27.spicamusic.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseOutCubic
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.AudioQualityBadges
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel

/** 首屏入场节拍（与刊头页共用的节奏） */
private const val ENTRANCE_STAGGER_MILLIS = 55L

/**
 * 搜索内容区的四种状态：
 * - [Idle] 空关键词，显示引导页
 * - [Loading] 结果尚未返回，显示呼吸骨架（关键：不显示"无结果"，修复空态闪现）
 * - [Empty] 已加载完成且确实没有匹配
 * - [Results] 正常结果列表
 */
private enum class SearchContentState { Idle, Loading, Empty, Results }

/**
 * 搜索页：胶囊搜索框 + 按首字母分组的结果列表，
 * 关键词在标题与副标题中高亮显示。
 *
 * 交互约定：
 * - 推场动画完成后才唤起键盘，避免与转场抢帧
 * - 滚动结果列表时自动收起键盘（iOS 式）
 * - 键盘 Search 键收起键盘
 */
class SearchScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val searchViewModel = koinViewModel<SearchViewModel>()
        val searchKey by searchViewModel.searchKeyword.collectAsStateWithLifecycle()
        val searchResult = searchViewModel.searchPagingResults.collectAsLazyPagingItems()
        val playerViewModel = LocalPlayerViewModel.current
        val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current
        val listState = rememberLazyListState()

        // 等推场动画完成后再唤起键盘，避免键盘上升与场景滑入互相抢帧
        LaunchedEffect(Unit) {
            enterAnimEnd.first { it }
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        // 用户开始滚动结果时自动收起键盘，把屏幕还给内容
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .filter { it }
                .collect {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
        }

        // 关键词变化时回到列表顶部
        LaunchedEffect(searchKey) {
            if (searchKey.isNotBlank()) {
                listState.scrollToItem(0)
            }
        }

        // 只有 refresh 完成且确实没有条目才算"无结果"，
        // 加载中（含防抖窗口，见 SearchViewModel 的停驻 Loading 处理）显示骨架
        val contentState by remember(searchResult) {
            derivedStateOf {
                when {
                    searchKey.isBlank() -> SearchContentState.Idle

                    searchResult.loadState.refresh is LoadState.Loading &&
                        searchResult.itemCount == 0 -> SearchContentState.Loading

                    searchResult.itemCount == 0 -> SearchContentState.Empty

                    else -> SearchContentState.Results
                }
            }
        }

        val headerEntrance = rememberEntrance(order = 0)
        val contentEntrance = rememberEntrance(order = 1)

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            SearchHeader(
                query = searchKey,
                onQueryChange = searchViewModel::updateSearchKeyword,
                onClear = searchViewModel::clearSearch,
                onBack = {
                    keyboardController?.hide()
                    path.popTop()
                },
                onImeSearch = { keyboardController?.hide() },
                focusRequester = focusRequester,
                listState = listState,
                modifier = Modifier.entranceGraphics(headerEntrance),
            )
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .imePadding()
                        .entranceGraphics(contentEntrance),
            ) {
                AnimatedContent(
                    targetState = contentState,
                    transitionSpec = {
                        (
                            fadeIn(tween(220, easing = EaseOutCubic)) +
                                slideInVertically(
                                    tween(
                                        220,
                                        easing = EaseOutCubic,
                                    ),
                                ) { it / 12 }
                        ).togetherWith(fadeOut(tween(120)))
                    },
                    label = "search_content",
                ) { state ->
                    when (state) {
                        SearchContentState.Idle ->
                            SearchIdleHint(modifier = Modifier.fillMaxSize())

                        SearchContentState.Loading ->
                            SearchSkeletonList(modifier = Modifier.fillMaxSize())

                        SearchContentState.Empty ->
                            SearchNoResultHint(
                                query = searchKey,
                                modifier = Modifier.fillMaxSize(),
                            )

                        SearchContentState.Results ->
                            SearchResultList(
                                listState = listState,
                                searchResult = searchResult,
                                keyword = searchKey,
                                playingMediaId = currentMediaItem?.mediaId,
                                onPlay = { song -> playerViewModel.playSong(song) },
                                onMore = { song -> path.push(SongMenuScene(song)) },
                                modifier = Modifier.fillMaxSize(),
                            )
                    }
                }
            }
        }
    }
}

/** 顶部：返回按钮 + 胶囊搜索框，底部带滚动后浮现的发丝线 */
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onImeSearch: () -> Unit,
    focusRequester: FocusRequester,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val hairlineColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .statusBarsPadding(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.Small,
                        end = LayoutTokens.MusicHeaderHorizontalPadding,
                        top = Spacing.Small,
                        bottom = Spacing.Medium,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
            SearchInputField(
                query = query,
                onQueryChange = onQueryChange,
                onClear = onClear,
                onImeSearch = onImeSearch,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
        }
        // 列表滚动后浮现的发丝线（Draw 阶段读取滚动值，滚动零重组）
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .drawBehind {
                        val progress =
                            if (listState.firstVisibleItemIndex > 0) {
                                1f
                            } else {
                                (listState.firstVisibleItemScrollOffset / 24.dp.toPx())
                                    .coerceIn(0f, 1f)
                            }
                        drawRect(color = hairlineColor.copy(alpha = 0.14f * progress))
                    },
        )
    }
}

/** 胶囊形搜索输入框：52dp 高，Search 键收起键盘，清除按钮与占位等宽切换避免跳动 */
@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onImeSearch: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(52.dp)
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
            value = query,
            onValueChange = onQueryChange,
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
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_songs_albums_artists),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        // 清除按钮与等宽 Spacer 通过 Z 轴切换，输入首字时布局不跳动
        AnimatedContent(
            targetState = query.isNotEmpty(),
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "search_clear",
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
                            imageVector = Icons.Default.Close,
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

/** 结果列表：分组字母头 + 歌曲卡片，占位条目渲染骨架行避免列表长度抖动 */
@Composable
private fun SearchResultList(
    listState: LazyListState,
    searchResult: LazyPagingItems<SearchListItem>,
    keyword: String,
    playingMediaId: String?,
    onPlay: (Song) -> Unit,
    onMore: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding =
            PaddingValues(
                start = LayoutTokens.MusicHeaderHorizontalPadding,
                end = LayoutTokens.MusicHeaderHorizontalPadding,
                top = Spacing.Small,
                bottom = 120.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
    ) {
        items(
            count = searchResult.itemCount,
            key = { index ->
                when (val item = searchResult.peek(index)) {
                    is SearchListItem.Header -> "header_${item.title}"
                    is SearchListItem.SongItem -> item.song.mediaStoreId
                    null -> "placeholder_$index"
                }
            },
            contentType = { index ->
                if (searchResult.peek(index) is SearchListItem.Header) "header" else "song"
            },
        ) { index ->
            when (val item = searchResult[index]) {
                is SearchListItem.Header ->
                    SearchGroupHeader(
                        title = item.title,
                        modifier = Modifier.animateItem(),
                    )

                is SearchListItem.SongItem ->
                    SearchSongItem(
                        song = item.song,
                        keyword = keyword,
                        isPlaying = playingMediaId == item.song.mediaStoreId.toString(),
                        onPlay = { onPlay(item.song) },
                        onMore = { onMore(item.song) },
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec =
                                    tween(
                                        durationMillis = 240,
                                        easing = FastOutSlowInEasing,
                                    ),
                                placementSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMediumLow,
                                        visibilityThreshold = IntOffset.VisibilityThreshold,
                                    ),
                                fadeOutSpec = tween(durationMillis = 160),
                            ),
                    )

                null -> SearchPlaceholderRow(modifier = Modifier.animateItem())
            }
        }
        if (searchResult.loadState.append is LoadState.Loading) {
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

/** 分组字母头：主题色圆角小标签 + 发丝线 */
@Composable
private fun SearchGroupHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = Spacing.Medium, bottom = Spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(Shapes.SmallCornerBasedShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        )
    }
}

/** 单曲条目：音乐页同款圆角卡片 + 封面 + 关键词高亮，长按也可打开菜单 */
@Composable
private fun SearchSongItem(
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
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ).combinedClickHighlight(onLongClick = onMore, onClick = onPlay)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            uri = song.getCoverUri(),
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(Shapes.LargeCornerBasedShape),
            placeHolder = { SearchCoverPlaceholder() },
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = highlightKeyword(song.displayName, keyword),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                AudioQualityBadges(song = song)
                Text(
                    text =
                        highlightKeyword(
                            buildString {
                                if (song.artist.isNotBlank()) append(song.artist)
                                if (song.artist.isNotBlank() && song.album.isNotBlank()) append(" · ")
                                if (song.album.isNotBlank()) append(song.album)
                            },
                            keyword,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
        }
        IconButton(onClick = onMore) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 封面占位：surfaceContainerHigh 底 + 音符图标（全局占位约定） */
@Composable
private fun SearchCoverPlaceholder() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 把命中关键词的部分用主题色加粗高亮 */
@Composable
private fun highlightKeyword(
    text: String,
    keyword: String,
) = buildAnnotatedString {
    val trimmed = keyword.trim()
    if (trimmed.isEmpty()) {
        append(text)
        return@buildAnnotatedString
    }
    val highlightStyle =
        SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
    var start = 0
    while (start < text.length) {
        val hit = text.indexOf(trimmed, startIndex = start, ignoreCase = true)
        if (hit < 0) {
            append(text.substring(start))
            break
        }
        append(text.substring(start, hit))
        withStyle(highlightStyle) {
            append(text.substring(hit, hit + trimmed.length))
        }
        start = hit + trimmed.length
    }
}

/** 空关键词引导：开放排版无卡片，图标轻盈浮动（收藏页空态同款） */
@Composable
private fun SearchIdleHint(modifier: Modifier = Modifier) {
    val floatTransition = rememberInfiniteTransition(label = "searchIdleFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "searchIdleBob",
    )
    Column(
        modifier =
            modifier
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = 64.dp),
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
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.search_your_music_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.search_your_music_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 无结果：与引导页同款开放排版，仅在结果确实为空时出现 */
@Composable
private fun SearchNoResultHint(
    query: String,
    modifier: Modifier = Modifier,
) {
    val floatTransition = rememberInfiniteTransition(label = "searchEmptyFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "searchEmptyBob",
    )
    Column(
        modifier =
            modifier
                .padding(horizontal = Spacing.ExtraLarge)
                .padding(top = 64.dp),
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
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
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

/** 加载骨架：与结果卡片同几何的呼吸占位，保住版面节奏（收藏页同款节拍） */
@Composable
private fun SearchSkeletonList(
    modifier: Modifier = Modifier,
    rowCount: Int = 8,
) {
    val transition = rememberInfiniteTransition(label = "searchSkeleton")
    val breath by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "searchSkeletonBreath",
    )
    Column(
        modifier =
            modifier
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .padding(top = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        repeat(rowCount) {
            SearchSkeletonRow(alphaProvider = { breath })
        }
    }
}

/** Paging 占位条目（enablePlaceholders=true 时的 null item）：渲染骨架行避免列表长度抖动 */
@Composable
private fun SearchPlaceholderRow(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "searchPlaceholder")
    val breath by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "searchPlaceholderBreath",
    )
    SearchSkeletonRow(alphaProvider = { breath }, modifier = modifier)
}

/** 骨架行：与 [SearchSongItem] 同几何（alpha 在 Draw 阶段读取） */
@Composable
private fun SearchSkeletonRow(
    alphaProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val bone = MaterialTheme.colorScheme.surfaceContainerHigh
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = alphaProvider() }
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(Shapes.LargeCornerBasedShape)
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
                    .width(24.dp)
                    .height(10.dp)
                    .clip(Shapes.SmallCornerBasedShape)
                    .background(bone),
        )
    }
}

/** 首屏入场：延迟 [order] 个节拍后弹入（刊头页同款节奏） */
@Composable
private fun rememberEntrance(order: Int): Animatable<Float, AnimationVector1D> {
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
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
    return entrance
}

/** 入场位移+淡入，动画值全部在 Draw 阶段读取 */
private fun Modifier.entranceGraphics(entrance: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        val enter = entrance.value
        alpha = enter
        translationY = (1f - enter) * 28.dp.toPx()
    }
