package me.spica27.spicamusic.ui.home.pages

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 搜索页面
 */
@Composable
fun SearchPage(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val searchKeyword by viewModel.searchKeyword.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(searchKeyword) {
        listState.animateScrollToItem(0)
    }

    fun LazyListState.isSticking(index: Int): State<Boolean> =
        derivedStateOf {
            val firstVisible = layoutInfo.visibleItemsInfo.firstOrNull()
            firstVisible?.index == index && firstVisible.offset == -layoutInfo.beforeContentPadding
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 搜索框
                SearchBar(
                    keyword = searchKeyword,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onClear = viewModel::clearSearch,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                )

                // 歌曲列表
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
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
                                        .animateItem()
                                        .padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }
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
        insideMargin = DpSize(22.dp, 16.dp),
        label = "搜索歌曲或艺术家",
        maxLines = 1,
        useLabelAsPlaceholder = true,
        leadingIcon = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                )
            }
        },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "清空",
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
) {
    val playerViewModel = LocalPlayerViewModel.current

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            // 使用 mediaStoreId 而不是 songId (MediaLibrary 使用 mediaStoreId 查找歌曲)
            timber.log.Timber.d("SearchPage: Clicking song: ${song.displayName}, mediaStoreId=${song.mediaStoreId}, songId=${song.songId}")
            playerViewModel.playSong(song)
        },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 4.dp),
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
    }
}

/**
 * 格式化时长（毫秒 -> mm:ss）
 */
private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%02d:%02d", minutes, remainingSeconds)
}
