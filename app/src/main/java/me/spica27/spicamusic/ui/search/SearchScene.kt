package me.spica27.spicamusic.ui.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.image.LandscapistImage
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
import me.spica27.spicamusic.ui.widget.AudioQualityBadges
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisXIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel

/**
 * 搜索页：大标题 + 圆角胶囊搜索框，
 * 结果列表按首字母分组，关键词在结果中高亮显示。
 */
class SearchScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val searchViewModel = koinViewModel<SearchViewModel>()
        val searchKey by searchViewModel.searchKeyword.collectAsState()
        val searchResult = searchViewModel.searchPagingResults.collectAsLazyPagingItems()
        val playerViewModel = LocalPlayerViewModel.current

        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Scaffold(
            topBar = {
                SearchHeader(
                    query = searchKey,
                    onQueryChange = searchViewModel::updateSearchKeyword,
                    onClear = searchViewModel::clearSearch,
                    onBack = { path.popTop() },
                    focusRequester = focusRequester,
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
            ) {
                AnimatedContent(
                    targetState = searchKey.isEmpty(),
                    transitionSpec = {
                        (fadeIn(tween(220, easing = EaseOutCubic)) + slideInVertically(tween(220, easing = EaseOutCubic)) { it / 12 })
                            .togetherWith(fadeOut(tween(120)))
                    },
                    label = "search_content",
                ) { isEmptyQuery ->
                    if (isEmptyQuery) {
                        SearchHint()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
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
                            if (searchResult.itemCount == 0) {
                                item(key = "no_result") {
                                    NoResultHint(query = searchKey)
                                }
                            }
                            items(searchResult.itemCount) { index ->
                                when (val item = searchResult[index] ?: return@items) {
                                    is SearchListItem.Header ->
                                        SearchGroupHeader(
                                            title = item.title,
                                            modifier = Modifier.animateItem(),
                                        )

                                    is SearchListItem.SongItem ->
                                        SearchSongItem(
                                            song = item.song,
                                            keyword = searchKey,
                                            onPlay = { playerViewModel.playSong(item.song) },
                                            onMore = { path.push(SongMenuScene(item.song)) },
                                            modifier = Modifier.animateItem(),
                                        )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 顶部：返回按钮 + 胶囊搜索框 */
@Composable
private fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .padding(
                    start = Spacing.Small,
                    end = LayoutTokens.MusicHeaderHorizontalPadding,
                    top = Spacing.Small,
                    bottom = Spacing.Medium,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 胶囊形搜索输入框 */
@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_songs_albums_artists),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                textStyle =
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
            )
        }
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = materialSharedAxisXIn(true),
            exit = materialSharedAxisZOut(true),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
                        .clickHighlight(onClick = onClear),
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
    }
}

/** 分组字母头：主题色圆角小标签 */
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
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        )
    }
}

/** 单曲条目：圆角卡片 + 封面 + 关键词高亮 */
@Composable
private fun SearchSongItem(
    song: Song,
    keyword: String,
    onPlay: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.LargeCornerBasedShape)
                .clickHighlight(onClick = onPlay)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(Shapes.MediumCornerBasedShape),
        ) {
            LandscapistImage(
                imageModel = { song.getCoverUri() },
                modifier = Modifier.fillMaxSize(),
                imageOptions = ImageOptions(contentScale = ContentScale.Crop),
                failure = {
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
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = highlightKeyword(song.displayName, keyword),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                AudioQualityBadges(song = song)
                Text(
                    text =
                        buildString {
                            if (song.artist.isNotBlank()) append(song.artist)
                            if (song.artist.isNotBlank() && song.album.isNotBlank()) append(" · ")
                            if (song.album.isNotBlank()) append(song.album)
                        },
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

/** 空关键词时的引导页：呼吸动画图标 + 文案 */
@Composable
private fun SearchHint(modifier: Modifier = Modifier) {
    val breath = rememberInfiniteTransition(label = "search_hint_breath")
    val scale by breath.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scale",
    )
    val glowAlpha by breath.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = EaseOutCubic),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glow",
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 64.dp),
            contentAlignment = Alignment.Center,
        ) {
            // 外圈光晕
            Box(
                modifier =
                    Modifier
                        .size(112.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = glowAlpha }
                        .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Box(
                modifier =
                    Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(Modifier.height(Spacing.ExtraLarge))
        Text(
            text = stringResource(R.string.search_your_music_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.Small))
        Text(
            text = stringResource(R.string.search_your_music_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 无结果时的提示 */
@Composable
private fun NoResultHint(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = 64.dp, bottom = Spacing.ExtraLarge)
                .padding(horizontal = Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(Spacing.Small))
        Text(
            text = stringResource(R.string.search_no_results_format, query),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.search_try_different_keywords),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
