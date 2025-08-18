package me.spica27.spicamusic.ui.main.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.ToastUtils
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.InputTextDialog
import me.spica27.spicamusic.widget.PlayingSongItem
import me.spica27.spicamusic.wrapper.activityViewModel


/**
 * 当前播放列表
 */
@Composable
fun CurrentListPage(
  playBackViewModel: PlayBackViewModel = activityViewModel(),
  navigator: NavBackStack? = null
) {


  val playIndexState = playBackViewModel.playlistCurrentIndex.collectAsStateWithLifecycle()

  val playListSizeState =
    playBackViewModel.nowPlayingListSize.collectAsState()

  var showCreateDialog by remember { mutableStateOf(false) }

  val coroutineScope = rememberCoroutineScope()

  if (showCreateDialog) {
    InputTextDialog(
      onDismissRequest = {
        showCreateDialog = false
      },
      title = "创建为新歌单",
      placeholder = "新歌单名称",
      onConfirm = { txt ->
        if (txt.isEmpty()) {
          playBackViewModel.createPlaylistWithSongs("新建歌单", playBackViewModel.playList.value)
          showCreateDialog = false
        } else {
          playBackViewModel.createPlaylistWithSongs(txt, playBackViewModel.playList.value)
          showCreateDialog = false
        }
        ToastUtils.showToast("歌单创建完成")
      },
      onCancel = {
        showCreateDialog = false
      }
    )
  }

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
              playBackViewModel.clear()
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
              showCreateDialog = true
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
      val listState = rememberLazyListState()


      val showScrollToCurrent = remember {
        derivedStateOf {
          !listState.layoutInfo.visibleItemsInfo.any {
            it.index == playIndexState.value
          }
        }
      }

      CurrentList(playBackViewModel, listState)


      if (showScrollToCurrent.value) {
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .offset(
              x = (-64).dp,
              y = (-64).dp
            )
            .width(40.dp)
            .height(40.dp)
            .background(
              color = MaterialTheme.colorScheme.primaryContainer,
              shape = CircleShape
            )
            .clip(CircleShape)
            .clickableNoRippleWithVibration {
              coroutineScope.launch {
                listState.animateScrollToItem(playIndexState.value)
              }
            }
            .padding(8.dp),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(R.drawable.ic_radar),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer
          )
        }
      }

    }
  }

}

@Composable
private fun CurrentList(
  viewModel: PlayBackViewModel,
  listState: LazyListState
) {

  val playingSongState = viewModel.currentSongFlow.collectAsStateWithLifecycle()

  val listDataState = viewModel
    .playList
    .collectAsStateWithLifecycle(emptyList())

  LazyColumn(
    modifier = Modifier
      .fillMaxSize(),
    state = listState,
  ) {

    itemsIndexed(listDataState.value, key = { _, song -> song.songId.toString() }) { index, song ->
      PlayingSongItem(
        showRemove = true,
        onRemoveClick = {
          viewModel.removeSong(song)
        },
        modifier = Modifier
          .fillMaxWidth()
          .animateItem(),
        isPlaying = playingSongState.value?.songId == song.songId,
        song = song, onClick = {
          viewModel.play(listDataState.value[index])
        })
    }

    item {
      Spacer(modifier = Modifier.size(60.dp))
    }
  }
}





