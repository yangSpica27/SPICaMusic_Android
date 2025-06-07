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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import me.spica27.spicamusic.R
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlayingSongItem


@Composable
fun CurrentListPage(
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  navigator: NavBackStack? = null
) {

  val playState = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false)

  val playIndexState = playBackViewModel.playlistCurrentIndex.collectAsStateWithLifecycle()

  val playListSizeState = playBackViewModel.nowPlayingListSize.collectAsStateWithLifecycle()


  Column(
    modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
  ) {
    Text(
      "向下轻扫回到播放页面",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
      ),
      modifier = Modifier
        .padding(vertical = 12.dp, horizontal = 20.dp)
        .fillMaxWidth(),
      textAlign = TextAlign.Center
    )
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

  val playingSongState = viewModel.currentSongFlow.collectAsStateWithLifecycle()

  val listDataState = viewModel
    .playList
    .collectAsStateWithLifecycle()

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





