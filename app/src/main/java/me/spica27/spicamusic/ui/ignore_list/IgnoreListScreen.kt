package me.spica27.spicamusic.ui.ignore_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.SimpleTopBar
import me.spica27.spicamusic.wrapper.activityViewModel


@Composable
fun IgnoreListScreen(navigator: NavBackStack) {

  val listState = rememberLazyListState()

  val songViewModel = activityViewModel<SongViewModel>()

  val songs = songViewModel.ignoreSongs.collectAsState()

  Scaffold(
    topBar = {
      SimpleTopBar(
        title = "忽略的歌曲",
        onBack = {
          navigator.removeLastOrNull()
        },
        lazyListState = listState
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = songs.value,
        modifier = Modifier.fillMaxSize()
      ) {
        if (it.isEmpty()) {
          Empty()
        } else {
          List(listState, it)
        }
      }
    }
  }
}

@Composable
fun List(
  listState: LazyListState,
  songs: List<Song>
) {

  val songViewModel = activityViewModel<SongViewModel>()

  var showRemoveDialog by remember { mutableStateOf(false) }

  var currentSong by remember { mutableStateOf<Song?>(null) }

  if (showRemoveDialog){
    AlertDialog(
      shape = MaterialTheme.shapes.small,
      onDismissRequest = {
        showRemoveDialog = false
      }, title = {
        Text("移出忽略清单")
      }, text = {
        Text("确定从忽略清单中移除吗?")
      }, confirmButton = {
        TextButton(onClick = {
          // 确认删除
          songViewModel.ignore(currentSong?.songId?:-1,false)
          showRemoveDialog = false
        }) {
          Text("确定")
        }
      }, dismissButton = {
        TextButton(onClick = {
          // 取消删除
          showRemoveDialog = false
        }) {
          Text("取消")
        }
      })
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState
  ) {
    items(songs) { item ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            currentSong = item
            showRemoveDialog = true
          }
          .padding(vertical = 10.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CoverWidget(
          item,
          modifier = Modifier
            .width(50.dp)
            .height(50.dp)
            .background(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium)
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.displayName,
            style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.W600
            ),
            maxLines = 1
          )
          Text(
            text = item.artist,
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.W500
            ),
            maxLines = 1
          )
        }
        Text(
          text = "移出",
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }
  }
}

@Composable
private fun Empty() {
  Column(
    modifier = Modifier
      .fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)
    )
    AsyncImage(
      modifier = Modifier.height(130.dp),
      model = R.drawable.load_error,
      contentDescription = null
    )
    Spacer(
      modifier = Modifier.height(10.dp)
    )
    Text(
      "空空如也",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
  }
}
