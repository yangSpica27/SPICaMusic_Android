package me.spica27.spicamusic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlayingSongItem


@Composable
fun CurrentListPage(
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val playState = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false)

  val playIndexState = playBackViewModel.playlistCurrentIndex.collectAsStateWithLifecycle(0)

  val playListSizeState = playBackViewModel.nowPlayingListSize.collectAsStateWithLifecycle(0)


  Column(
    modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (playState.value) {
          "正在播放"
        } else {
          "未在播放"
        }, style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
          color = MaterialTheme.colorScheme.tertiary
        ), modifier = Modifier.padding(20.dp)
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        "向下轻扫回到播放页面", style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ), modifier = Modifier.padding(20.dp)
      )
    }
    Spacer(modifier = Modifier.size(4.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp, horizontal = 20.dp)
    ) {
      Box {
        Column {
          Text(
            text = "当前播放列表",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
          )
          Text(
            text = "${playIndexState.value + 1}/${playListSizeState.value}",
            style = MaterialTheme.typography.bodyMedium
          )
        }
      }
      Box(modifier = Modifier.align(Alignment.BottomEnd)) {
        Row {
          IconButton(
            onClick = {
              // 清空播放列表
            }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_playlist_remove),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          }
          IconButton(
            onClick = {
              // 保存为新歌单

            }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_new),
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          }
        }
      }
    }
    HorizontalDivider(
      thickness = 1.dp / 2, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    )
    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
    ) {
      CurrentList(playBackViewModel)
    }
  }
}

@Composable
private fun CurrentList(
  viewModel: PlayBackViewModel,
) {

  val playingSongState = viewModel.currentSongFlow.collectAsStateWithLifecycle(null)

  val listDataState = viewModel.playList.collectAsStateWithLifecycle(emptyList())

  val listState = rememberLazyListState()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize(),
    state = listState,
  ) {

    itemsIndexed(listDataState.value, key = { _, song -> song.songId.toString() }) { index, song ->
      PlayingSongItem(
        showRemove = true,
        onRemoveClick = {
          viewModel.removeSong(index)
        },
        modifier = Modifier
          .fillMaxWidth()
          .animateItem(),
        isPlaying = playingSongState.value?.songId == song.songId,
        song = song, onClick = {
          viewModel.play(listDataState.value[index], listDataState.value)
        })
    }

    item {
      Spacer(modifier = Modifier.size(60.dp))
    }
  }
}





