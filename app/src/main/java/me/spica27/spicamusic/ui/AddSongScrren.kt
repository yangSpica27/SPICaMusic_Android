package me.spica27.spicamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.viewModel.SelectSongViewModel
import me.spica27.spicamusic.widget.SelectableSongItem


/// 给歌单添加歌曲的页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongScreen(
  viewModel: SelectSongViewModel = hiltViewModel(), navController: AppComposeNavigator
) {
  val coroutineScope = rememberCoroutineScope()

  Scaffold(topBar = {
    TopAppBar(title = {
      Text(text = "选择新增歌曲")
    }, navigationIcon = {
      // 返回按钮
      IconButton(onClick = {
        navController.navigateUp()
      }) {
        Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
      }
    }, actions = {
      // 保存按钮
      TextButton(onClick = {
        coroutineScope.launch(Dispatchers.IO) {
          viewModel.addSongToPlaylist()
          withContext(Dispatchers.Main) {
            navController.navigateUp()
          }
        }
      }) {
        Text(
          "保存", style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.primary
          )
        )
      }
    })
  }, content = { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      // 歌曲列表


      val songs =
        combine(
          viewModel.getAllSongsNotInPlaylist(),
          viewModel.selectedSongsIds
        ) { allSongs, selectIds ->
          allSongs.map {
            Pair(it, selectIds.contains(it.songId))
          }
        }
          .collectAsState(initial = emptyList())


      if (songs.value.isEmpty()) {
        Text("没有更多歌曲了", modifier = Modifier.align(Alignment.Center))
      } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
          itemsIndexed(songs.value, key = { _, item ->
            item.first.songId.toString()
          }) { _, song ->
            // 歌曲条目
            SelectableSongItem(
              song = song.first,
              selected = song.second,
              onToggle = { viewModel.toggleSongSelection(song.first.songId) })
          }
        }
      }
    }
  })
}





