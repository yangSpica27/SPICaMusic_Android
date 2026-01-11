package me.spica27.spicamusic.ui.home.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.Collator
import java.util.*

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

    // 按首字母分组
    val groupedSongs by remember(searchResults) {
        derivedStateOf {
            searchResults
                .groupBy { song ->
                    getFirstLetter(song.displayName)
                }.toSortedMap()
        }
    }

    // 字母索引列表
    val letterIndexes by remember(groupedSongs) {
        derivedStateOf {
            groupedSongs.keys.toList()
        }
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
                    groupedSongs.forEach { (letter, songs) ->
                        // 字母分组标题
                        item(key = "header_$letter") {
                            Text(
                                text = letter,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            )
                        }

                        // 该字母下的歌曲列表
                        itemsIndexed(
                            items = songs,
                            key = { _, song -> "song_${song.songId}_${song.mediaStoreId}" },
                        ) { _, song ->
                            SongItemCard(
                                song = song,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            // A-Z 快速定位侧边栏
            if (letterIndexes.isNotEmpty()) {
                AlphabetScrollBar(
                    letters = letterIndexes,
                    onLetterSelected = { letter ->
                        coroutineScope.launch {
                            // 找到该字母在列表中的位置
                            val targetIndex = groupedSongs.keys.toList().indexOf(letter)
                            if (targetIndex >= 0) {
                                // 计算实际在LazyColumn中的位置（考虑header）
                                var itemIndex = 0
                                groupedSongs.keys.forEachIndexed { index, key ->
                                    if (index < targetIndex) {
                                        itemIndex += 1 + (groupedSongs[key]?.size ?: 0)
                                    }
                                }
                                listState.animateScrollToItem(itemIndex)
                            }
                        }
                    },
                    modifier =
                        Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp),
                )
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
            playerViewModel.playById(song.songId.toString())
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
 * A-Z 字母快速定位侧边栏
 */
@Composable
private fun AlphabetScrollBar(
    letters: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedLetter by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.6f)
                    .width(24.dp)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    .pointerInput(letters) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val index =
                                    (offset.y / (size.height / letters.size))
                                        .toInt()
                                        .coerceIn(0, letters.lastIndex)
                                val letter = letters[index]
                                selectedLetter = letter
                                onLetterSelected(letter)
                            },
                            onDrag = { change, _ ->
                                val index =
                                    (change.position.y / (size.height / letters.size))
                                        .toInt()
                                        .coerceIn(0, letters.lastIndex)
                                val letter = letters[index]
                                if (letter != selectedLetter) {
                                    selectedLetter = letter
                                    onLetterSelected(letter)
                                }
                            },
                            onDragEnd = {
                                selectedLetter = null
                            },
                        )
                    },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            letters.forEach { letter ->
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        fontSize = 10.sp,
                        color =
                            if (letter == selectedLetter) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurface
                            },
                        fontWeight =
                            if (letter == selectedLetter) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            },
                    )
                }
            }
        }

        // 显示当前选中字母的气泡提示
        if (selectedLetter != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .padding(end = 64.dp)
                        .size(56.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MiuixTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = selectedLetter!!,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * 获取字符串首字母（支持中文拼音）
 */
private fun getFirstLetter(text: String): String {
    if (text.isEmpty()) return "#"

    val firstChar = text.first().uppercaseChar()

    // 如果是英文字母，直接返回
    if (firstChar in 'A'..'Z') {
        return firstChar.toString()
    }

    // 中文字符，使用拼音
    val collator = Collator.getInstance(Locale.CHINA)
    return when {
        firstChar in '\u4e00'..'\u9fa5' -> {
            // 简单的拼音首字母判断（通过Unicode范围）
            val pinyin = getPinyinFirstLetter(firstChar)
            if (pinyin in 'A'..'Z') pinyin.toString() else "#"
        }

        else -> "#"
    }
}

/**
 * 获取汉字拼音首字母（简化版本）
 */
private fun getPinyinFirstLetter(char: Char): Char {
    val code = char.code
    return when (code) {
        in 0x4E00..0x9FA5 -> {
            // 简化的拼音首字母映射（基于Unicode区间）
            when {
                code >= 0x963F && code <= 0x9FFF -> 'A'
                code >= 0x5315 && code <= 0x531F -> 'B'
                code >= 0x5321 && code <= 0x5364 -> 'C'
                code >= 0x5366 && code <= 0x53BB -> 'D'
                code >= 0x53BF && code <= 0x54E9 -> 'E'
                code >= 0x53D1 && code <= 0x5525 -> 'F'
                code >= 0x5527 && code <= 0x564E -> 'G'
                code >= 0x54C8 && code <= 0x5653 -> 'H'
                code >= 0x4E0C && code <= 0x4E2C -> 'J'
                code >= 0x5580 && code <= 0x5653 -> 'K'
                code >= 0x5783 && code <= 0x57DF -> 'L'
                code >= 0x5988 && code <= 0x5A1C -> 'M'
                code >= 0x54EA && code <= 0x5544 -> 'N'
                code >= 0x5594 && code <= 0x5662 -> 'O'
                code >= 0x556A && code <= 0x5AB3 -> 'P'
                code >= 0x4E03 && code <= 0x4E54 -> 'Q'
                code >= 0x7136 && code <= 0x71DF -> 'R'
                code >= 0x4E09 && code <= 0x4E2A -> 'S'
                code >= 0x4ED6 && code <= 0x4F38 -> 'T'
                code >= 0x54C7 && code <= 0x6316 -> 'W'
                code >= 0x5915 && code <= 0x6C99 -> 'X'
                code >= 0x4E2B && code <= 0x5440 -> 'Y'
                code >= 0x5412 && code <= 0x5B6B -> 'Z'
                else -> '#'
            }
        }

        else -> '#'
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
