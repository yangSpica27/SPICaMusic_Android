package me.spica27.spicamusic.ui.add_song

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.viewModel.SelectSongViewModel
import me.spica27.spicamusic.widget.SelectableSongItem
import org.koin.androidx.compose.koinViewModel

// / 给歌单添加歌曲的页面

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(
    navigator: NavController = LocalNavController.current,
    playlistId: Long,
) {
    val viewModel: SelectSongViewModel = koinViewModel()
    viewModel.setPlaylistId(playlistId)

    val coroutineScope = rememberCoroutineScope()

    val songs = viewModel.allSongsFlow.collectAsState(initial = emptyList()).value

    Scaffold(topBar = {
        TopAppBar(title = {
            Column {
                Text(text = "选择歌曲")
                Text(
                    text = "已选择${viewModel.selectedSongsIds.collectAsState(initial = emptyList()).value.size}首",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }, navigationIcon = {
            // 返回按钮
            IconButton(onClick = {
                navigator.popBackStack()
            }) {
                Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
            }
        }, actions = {
            TextButton(
                onClick = {
                    viewModel.selectSongs(songs)
                },
            ) {
                Text(
                    "全选",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
            }

            TextButton(
                onClick = {
                    viewModel.clearSelectedSongs()
                },
            ) {
                Text(
                    "清空选择",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
            }

            // 保存按钮
            TextButton(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        viewModel.addSongToPlaylist(playlistId)
                        withContext(Dispatchers.Main) {
                            navigator.popBackStack()
                        }
                    }
                },
            ) {
                Text(
                    "保存",
                    style =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                )
            }
        })
    }, content = { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // 歌曲列表
            val listDataState =
                combine(
                    viewModel.allSongsFlow,
                    viewModel.selectedSongsIds,
                ) { allSongs, selectIds ->
                    allSongs.map {
                        Pair(it, selectIds.contains(it.songId))
                    }
                }.collectAsState(initial = emptyList())

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextField(
                    value = viewModel.keyword.collectAsState("").value,
                    onValueChange = {
                        viewModel.setKeyword(it.trim())
                    },
                    placeholder = {
                        Text(text = "筛选关键词")
                    },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "搜索")
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors =
                        TextFieldDefaults.colors().copy(
                            disabledIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                )
                Spacer(
                    modifier =
                        Modifier
                            .height(12.dp),
                )
                HorizontalDivider(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(.5.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .11f),
                )
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                ) {
                    if (listDataState.value.isEmpty()) {
                        Text("没有更多歌曲了", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxSize(),
                        ) {
                            itemsIndexed(listDataState.value, key = { _, item ->
                                item.first.songId.toString()
                            }) { _, song ->
                                // 歌曲条目
                                SelectableSongItem(
                                    modifier = Modifier.animateItem(),
                                    song = song.first,
                                    selected = song.second,
                                    onToggle = { viewModel.toggleSongSelection(song.first.songId) },
                                )
                            }
                        }
                    }
                }
            }
        }
    })
}
