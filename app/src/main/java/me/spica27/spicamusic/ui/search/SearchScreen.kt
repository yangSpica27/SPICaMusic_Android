package me.spica27.spicamusic.ui.search

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.extra.LocalWindowListPopupState
import top.yukonga.miuix.kmp.extra.WindowListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import kotlin.random.Random

/**
 * 搜索页面
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val searchPagingItems = viewModel.searchPagingResults.collectAsLazyPagingItems()
    val searchResultCount by viewModel.searchResultCount.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchKeyword) {
        listState.animateScrollToItem(0)
    }

    val scrollBehavior = MiuixScrollBehavior()

    val listHazeSource = rememberHazeState()

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
                    title = "搜索",
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent,
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
        AnimatedContent(
            searchKeyword.isEmpty(),
        ) { isEmptyKeyword ->
            if (isEmptyKeyword) {
                // 欢迎提示
                WelcomeHolder(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    paddingValues = paddingValues,
                )
            } else {
                if (searchResultCount == 0) {
                    // 空提示
                    EmptyHolder(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .padding(top = 22.dp)
                                .padding(horizontal = 16.dp),
                        inputText = searchKeyword,
                    )
                } else if (searchKeyword.isNotEmpty() && searchResultCount > 0) {
                    // 搜索结果列表
                    SearchResultHolder(
                        searchPagingItems = searchPagingItems,
                        listState = listState,
                        paddingValues = paddingValues,
                        scrollBehavior = scrollBehavior,
                        listHazeSource = listHazeSource,
                    )
                }
            }
        }
    }
}

/**
 * 空提示组件
 */
@Composable
private fun EmptyHolder(
    modifier: Modifier = Modifier,
    inputText: String,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text =
                buildAnnotatedString {
                    append("未找到与 ")
                    withStyle(
                        style =
                            MiuixTheme.textStyles.body1
                                .copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary,
                                ).toSpanStyle(),
                    ) {
                        append("\"$inputText\"")
                    }
                    append(" 相关的歌曲或艺术家")
                },
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/**
 * 欢迎提示组件
 */
@Composable
private fun WelcomeHolder(
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues,
) {
    val backStack = LocalNavBackStack.current

    LazyVerticalGrid(
        modifier =
            modifier
                .overScrollVertical(),
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding(),
            ),
    ) {
        item {
            WelcomeItem(title = "经常播放", onClick = {
                backStack.add(Screen.MostPlayed)
            }) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        item {
            WelcomeItem(
                title = "所有歌曲",
                onClick = {
                    backStack.add(Screen.AllSongs)
                },
            ) {
                // 喜欢插画：多个心形叠加
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.15f),
                        modifier =
                            Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    translationX = (-8).dp.toPx()
                                    translationY = 6.dp.toPx()
                                },
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(38.dp),
                    )
                }
            }
        }
        item {
            WelcomeItem(title = "我的歌单", onClick = {
                backStack.add(Screen.Playlists)
            }) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
        item {
            WelcomeItem(title = "喜欢的音乐", onClick = {}) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeItem(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    bottom: @Composable BoxScope.() -> Unit = {},
) {
    // 入场动画
    val appearAnim =
        remember(title) {
            Animatable(0f)
        }
    val delayMillis = remember(title) { Random.nextInt(0, 200) }
    val durationMillis = remember(title) { Random.nextInt(260, 520) }

    LaunchedEffect(Unit) {
        tween<Float>(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
        ).let { tween ->
            appearAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween,
            )
        }
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape = Shapes.SmallCornerBasedShape)
                .pressable(interactionSource = interactionSource, indication = SinkFeedback())
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ).graphicsLayer {
                    alpha = appearAnim.value
                    scaleX = appearAnim.value
                    scaleY = appearAnim.value
                }.background(
                    MiuixTheme.colorScheme.surfaceContainer,
                ).aspectRatio(2.15f),
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(.72f)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        rotationZ = 45f
                        translationY = 16.dp.toPx()
                        translationX = 12.dp.toPx()
                    }.background(
                        MiuixTheme.colorScheme.tertiaryContainerVariant,
                        shape = Shapes.SmallCornerBasedShape,
                    ).padding(
                        12.dp,
                    ),
            contentAlignment = Alignment.BottomEnd,
        ) {
            bottom()
        }
        // 标题文字
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .padding(16.dp),
        )
    }
}

@Composable
private fun SearchResultHolder(
    searchPagingItems: LazyPagingItems<SearchListItem>,
    listState: LazyListState,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    listHazeSource: HazeState,
) {
    val playerViewModel = LocalPlayerViewModel.current
    val currentPlaylist by playerViewModel.currentPlaylist.collectAsStateWithLifecycle()

    // 歌曲列表
    LazyColumn(
        state = listState,
        contentPadding = paddingValues,
        modifier =
            Modifier
                .hazeSource(state = listHazeSource)
                .fillMaxSize()
                .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical(),
    ) {
        items(
            count = searchPagingItems.itemCount,
            key =
                searchPagingItems.itemKey { item ->
                    when (item) {
                        is SearchListItem.Header -> "header_${item.title}"
                        is SearchListItem.SongItem -> item.song.songId ?: item.song.mediaStoreId
                    }
                },
        ) { index ->
            when (val item = searchPagingItems[index]) {
                is SearchListItem.Header -> {
                    SmallTitle(
                        text = item.title,
                        textColor = MiuixTheme.colorScheme.onSurface,
                    )
                }

                is SearchListItem.SongItem -> {
                    SongItemCard(
                        song = item.song,
                        currentPlaylist = currentPlaylist,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .animateItem(),
                    )
                }

                null -> { // placeholder
                }
            }
        }
        item {
            Spacer(modifier = Modifier.size(150.dp))
        }
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
            IconButton(onClick = {}) {
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

/**
 * 歌曲列表项卡片
 */
@Composable
private fun SongItemCard(
    song: Song,
    currentPlaylist: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    val playerViewModel = LocalPlayerViewModel.current

    val showPopup = remember { mutableStateOf(false) }
    val items = listOf("立刻播放", "加入播放列表", "下一首播放", "查看详情")

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier =
            modifier.clickable {
                showPopup.value = true
                keyboardController?.hide()
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AudioCover(
                modifier =
                    Modifier
                        .size(48.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = ContinuousRoundedRectangle(10.dp),
                            clip = false,
                            ambientColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            spotColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        ).clip(
                            ContinuousRoundedRectangle(10.dp),
                        ),
                uri = song.getCoverUri(),
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(
                                    MiuixTheme.colorScheme.surfaceContainerHigh,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_dvd),
                            contentDescription = "默认封面",
                            tint = MiuixTheme.colorScheme.onSurfaceContainerVariant.copy(alpha = .3f),
                            modifier = Modifier.size(33.dp),
                        )
                    }
                },
            )
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    maxLines = 1,
                    style = MiuixTheme.textStyles.body1,
                    letterSpacing = 1.002.sp,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    overflow = TextOverflow.Ellipsis,
                    letterSpacing = 1.009.sp,
                )
            }

            // 时长
            Text(
                text = formatDuration(song.duration),
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        WindowListPopup(
            show = showPopup,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showPopup.value = false },
        ) {
            val dismiss = LocalWindowListPopupState.current
            ListPopupColumn {
                items.forEachIndexed { index, string ->
                    DropdownImpl(
                        text = string,
                        optionSize = items.size,
                        isSelected = false,
                        onSelectedIndexChange = {
                            when (index) {
                                0 -> {
                                    // 立刻播放
                                    playerViewModel.playSong(song)
                                }

                                1 -> {
                                    // 加入播放列表：添加到队列末尾
                                    val currentIds =
                                        currentPlaylist.map { it.mediaId }.toMutableList()
                                    val mediaId = song.mediaStoreId.toString()
                                    if (!currentIds.contains(mediaId)) {
                                        currentIds.add(mediaId)
                                        playerViewModel.updatePlaylist(
                                            mediaIds = currentIds,
                                            startMediaId = null,
                                            autoStart = false,
                                        )
                                    }
                                }

                                2 -> {
                                    // 下一首播放
                                    playerViewModel.addSongToNext(song)
                                }

                                3 -> {
                                    // 查看详情
                                }
                            }
                            dismiss()
                        },
                        index = index,
                    )
                }
            }
        }
    }
}

/**
 * 格式化时长（毫秒 -> mm:ss）
 */
@SuppressLint("DefaultLocale")
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
