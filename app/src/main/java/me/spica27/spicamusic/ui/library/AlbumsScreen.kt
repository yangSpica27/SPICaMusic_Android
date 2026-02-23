package me.spica27.spicamusic.ui.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

/**
 * 专辑页面
 */
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = koinViewModel(),
) {
    val backStack = LocalNavBackStack.current

    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()

    val listState = rememberLazyGridState()

    val listHazeSource = rememberHazeState()

    val albums = viewModel.filteredAlbums.collectAsLazyPagingItems()

    val isEmpty =
        remember(albums.itemCount) {
            derivedStateOf {
                albums.itemCount == 0
            }
        }

    LaunchedEffect(searchKeyword) {
        listState.animateScrollToItem(0)
    }

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier =
                    Modifier
                        .hazeEffect(
                            listHazeSource,
                            HazeMaterials.ultraThick(MiuixTheme.colorScheme.surface),
                        ) {
                            progressive =
                                HazeProgressive.verticalGradient(
                                    startIntensity = 1f,
                                    endIntensity = 0f,
                                )
                        },
            ) {
                TopAppBar(
                    title = "专辑",
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "返回",
                                tint = MiuixTheme.colorScheme.onSurfaceContainer,
                            )
                        }
                    },
                )
                // 搜索框
                SearchBar(
                    keyword = searchKeyword,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onClear = viewModel::clearSearch,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .hazeSource(listHazeSource)
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                isEmpty.value,
                modifier = Modifier.fillMaxSize(),
            ) { isEmpty ->

                if (isEmpty) {
                    EmptyPage(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    paddingValues,
                                ),
                    )
                } else {
                    ListPage(
                        modifier =
                            Modifier
                                .fillMaxSize(),
                        items = albums,
                        listState = listState,
                        paddingValues = paddingValues,
                        scrollBehavior = scrollBehavior,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(top = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "没有找到相关专辑",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.paragraph,
        )
    }
}

@Composable
private fun ListPage(
    modifier: Modifier = Modifier,
    items: LazyPagingItems<Album>,
    listState: LazyGridState,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
) {
    LazyVerticalGrid(
        modifier =
            modifier
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        columns = GridCells.Fixed(2),
        state = listState,
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 12.dp,
            ),
        horizontalArrangement =
            Arrangement
                .spacedBy(12.dp),
        verticalArrangement =
            Arrangement
                .spacedBy(12.dp),
    ) {
        items(
            items.itemCount,
            span = { GridItemSpan(1) },
            key =
                items.itemKey {
                    it.id
                },
        ) {
            val album = items[it]
            if (album != null) {
                AlbumItem(album, modifier = Modifier.animateItem())
            }
        }
        item(
            span = { GridItemSpan(2) },
        ) {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }
}

@Composable
private fun AlbumItem(
    album: Album,
    modifier: Modifier,
) {
    val backStack = LocalNavBackStack.current
    Column(
        modifier =
            modifier
                .clip(me.spica27.spicamusic.ui.theme.Shapes.SmallCornerBasedShape)
                .clickable {
                    backStack.add(Screen.AlbumDetail(album))
                },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AudioCover(
            uri = album.artworkUri.toString().toUri(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(me.spica27.spicamusic.ui.theme.Shapes.SmallCornerBasedShape),
            placeHolder = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                MiuixTheme.colorScheme.surfaceContainer,
                            ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = album.title,
                        style =
                            MiuixTheme.textStyles.headline1,
                        maxLines = 1,
                        modifier =
                            Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MiuixTheme.colorScheme.onSurfaceContainer,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(
                        modifier = Modifier.height(4.dp),
                    )
                    Text(
                        fontWeight = FontWeight.Black,
                        text = album.title,
                        style =
                            MiuixTheme.textStyles.headline1.copy(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.2f),
                                                MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.4f),
                                                MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.5f),
                                            ),
                                    ),
                            ),
                        maxLines = 1,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .scale(1f, -1f),
                        textAlign = TextAlign.Center,
                    )
                }
            },
        )
        Text(
            text = album.title,
            style = MiuixTheme.textStyles.subtitle,
            color = MiuixTheme.colorScheme.onSurfaceContainer,
            maxLines = 1,
        )
        Text(
            text = album.artist,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            maxLines = 1,
        )
    }
}

/**
 * 搜索框组件
 */
@Composable
private fun SearchBar(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = keyword,
        cornerRadius = 12.dp,
        onValueChange = onKeywordChange,
        modifier = modifier,
        borderColor = MiuixTheme.colorScheme.surfaceContainer,
        backgroundColor =
            MiuixTheme.colorScheme.surfaceContainer.copy(
                alpha = 0.6f,
            ),
        insideMargin = DpSize(22.dp, 16.dp),
        label = "搜索歌曲或艺术家",
        maxLines = 1,
        useLabelAsPlaceholder = true,
        leadingIcon = {
            androidx.compose.material3.IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = MiuixTheme.colorScheme.onSurfaceContainer,
                )
            }
        },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空",
                        tint = MiuixTheme.colorScheme.onSurfaceContainer,
                    )
                }
            }
        },
        singleLine = true,
    )
}
