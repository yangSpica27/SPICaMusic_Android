package me.spica27.spicamusic.ui.home.pages

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import me.spica27.spicamusic.common.entity.SongGroup
import me.spica27.spicamusic.player.impl.utils.getCoverUri
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
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.random.Random

/**
 * 搜索页面
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
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
                if (searchResults.isEmpty()) {
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
                } else if (searchKeyword.isNotEmpty() && searchResults.isNotEmpty()) {
                    // 搜索结果列表
                    SearchResultHolder(
                        searchResults = searchResults,
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
    LazyVerticalGrid(
        modifier = modifier.overScrollVertical(),
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
            WelcomeItem(title = "最近播放", onClick = {}) {
            }
        }
        item {
            WelcomeItem(title = "我喜欢的音乐")
        }
        item {
            WelcomeItem(title = "本地音乐")
        }
        item {
            WelcomeItem(title = "歌单")
        }
    }
}

@Composable
fun WelcomeItem(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: @Composable BoxScope.() -> Unit = {},
) {
    // 入场动画
    val appearAnim =
        remember(title) {
            androidx.compose.animation.core
                .Animatable(0f)
        }
    val delayMillis = remember(title) { Random.nextInt(0, 200) }
    val durationMillis = remember(title) { Random.nextInt(260, 520) }

    LaunchedEffect(Unit) {
        androidx.compose.animation.core
            .tween<Float>(
                durationMillis = durationMillis,
                delayMillis = delayMillis,
            ).let { tween ->
                appearAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = tween,
                )
            }
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = appearAnim.value
                    scaleX = appearAnim.value
                    scaleY = appearAnim.value
                }.clip(shape = Shapes.SmallCornerBasedShape)
                .background(
                    MiuixTheme.colorScheme.surfaceContainer,
                ).clickable { onClick() }
                .aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp),
        )
        icon()
    }
}

private fun LazyListState.isSticking(index: Int): State<Boolean> =
    derivedStateOf {
        val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
        firstVisible?.index == index && firstVisible.offset == -layoutInfo.beforeContentPadding
    }

@Composable
private fun SearchResultHolder(
    searchResults: List<SongGroup>,
    listState: LazyListState,
    paddingValues: PaddingValues,
    scrollBehavior: ScrollBehavior,
    listHazeSource: HazeState,
) {
    // 歌曲列表
    LazyColumn(
        state = listState,
        contentPadding = paddingValues,
        modifier =
            Modifier
                .hazeSource(state = listHazeSource)
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .overScrollVertical(),
    ) {
        searchResults.forEach { (title, songs) ->
            stickyHeader(
                key = title.hashCode(),
            ) { headerIndex ->

                val isSticking by listState.isSticking(headerIndex)

                val titleTextColor =
                    animateColorAsState(
                        targetValue = if (isSticking) Color.Transparent else MiuixTheme.colorScheme.onSurface,
                    )

                SmallTitle(
                    text = title,
                    textColor = titleTextColor.value,
                )
            }
            items(songs, key = { it.songId ?: -1 }) {
                SongItemCard(
                    song = it,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .animateItem(),
                )
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
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val playerViewModel = LocalPlayerViewModel.current
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val currentPlaylist by playerViewModel.currentPlaylist.collectAsStateWithLifecycle()

    val showPopup = remember { mutableStateOf(false) }
    val items = listOf("立刻播放", "加入播放列表", "下一首播放", "全部播放", "查看详情")

    Box(
        modifier =
            modifier.clickable {
                showPopup.value = true
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
                                    val currentIds = currentPlaylist.map { it.mediaId }.toMutableList()
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
                                    // 全部播放：播放搜索结果中该歌曲所在分组的所有歌曲
                                    val songGroup =
                                        searchResults.find { group ->
                                            group.songs.any { it.songId == song.songId }
                                        }
                                    songGroup?.let { group ->
                                        playerViewModel.updatePlaylistWithSongs(
                                            songs = group.songs,
                                            startSong = song,
                                            autoStart = true,
                                        )
                                    }
                                }

                                4 -> {
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
