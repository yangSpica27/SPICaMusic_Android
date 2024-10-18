package me.spica27.spicamusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toCoilUri
import me.spica27.spicamusic.R
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlayingSongItem
import timber.log.Timber


@Composable
fun CurrentListPage(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val isPlaying = playBackViewModel.isPlaying.collectAsState(false)

  val currentIndex = playBackViewModel.playlistCurrentIndex.collectAsState(0)

  val playList = playBackViewModel.playList.collectAsState(emptyList())


  if (playList.value.isNotEmpty()) {
    Column(
      modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
    ) {
      Text(
        text = if (isPlaying.value) {
          "正在播放"
        } else {
          "未在播放"
        }, style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
          color = MaterialTheme.colorScheme.tertiary
        ), modifier = Modifier.padding(20.dp)
      )


      NowPlayIngSong(playBackViewModel, navigator)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp, horizontal = 20.dp)
      ) {
        Box {
          Text(
            text = "当前播放列表", style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
        }
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
          Text(
            text = "${currentIndex.value}/${playList.value.size}",
            style = MaterialTheme.typography.bodyMedium
          )
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
  } else {
    EmptyPlaceHolder()
  }
}

@Composable
private fun CurrentList(
  viewModel: PlayBackViewModel,
) {

  val currentSong = viewModel.currentSongFlow.collectAsState(null)

  val playlist = viewModel.playList.collectAsState(emptyList())

  LazyColumn {

    itemsIndexed(playlist.value, key = { _, song -> song.songId.toString() }) { index, song ->
      PlayingSongItem(isPlaying = currentSong.value?.songId == song.songId, song = song, onClick = {
        viewModel.play(playlist.value[index], playlist.value)
      })
    }

    item {
      Spacer(modifier = Modifier.size(60.dp))
    }
  }
}


/// 没有正在播放的音乐时候占位
@Composable
private fun EmptyPlaceHolder() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text(text = "没有正在播放的音乐", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.size(16.dp))
    OutlinedButton(
      onClick = { },
    ) {
      Text(text = "选取音乐")
    }
  }
}

@Composable
private fun NowPlayIngSong(viewModel: PlayBackViewModel, navigator: AppComposeNavigator? = null) {
  val song = viewModel.currentSongFlow.collectAsState(null)
  val positionMsState = viewModel.positionSec.collectAsState(0L)
  val isPlaying = viewModel.isPlaying.collectAsState(false)

  val coverPainter = rememberAsyncImagePainter(song.value?.getCoverUri()?.toCoilUri())
  val coverPainterState = coverPainter.state.collectAsState()



  Box(
    modifier = Modifier.fillMaxWidth()
  ) {
    Column {
      Spacer(modifier = Modifier.size(16.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically
      ) {
        // 封面
        Box(
          modifier = Modifier
            .clickable {
              navigator?.navigate(AppScreens.Player.route)
            }
            .size(64.dp),
          contentAlignment = Alignment.Center,
        ) {

          if (coverPainterState.value is AsyncImagePainter.State.Success) {
            Image(
              painter = coverPainter,
              contentDescription = "封面",
              modifier = Modifier.size(64.dp)
            )
          } else {
            Box(
              modifier = Modifier
                .size(64.dp)
                .background(
                  color = MaterialTheme.colorScheme.surfaceContainer,
                  shape = MaterialTheme.shapes.medium
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                modifier = Modifier
                  .fillMaxWidth()
                  .scale(1.5f),
                painter = painterResource(id = R.drawable.ic_dvd),
                contentDescription = "封面",
                tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
              )
            }
          }


        }
        // 歌曲信息
        Column(
          modifier = Modifier
            .padding(start = 16.dp)
            .weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = song.value?.displayName ?: "--",
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee(),
            fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
            maxLines = 1
          )
          Text(
            text = song.value?.artist ?: "--",
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
        }
        Row(
          horizontalArrangement = Arrangement.Center
        ) {
          IconButton(
            onClick = {
              viewModel.playPre()
            },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(painter = painterResource(id = R.drawable.ic_pre), contentDescription = "Previous")
          }
          Spacer(modifier = Modifier.size(16.dp))
          IconButton(
            onClick = {
              viewModel.togglePlaying()
            }, modifier = Modifier.size(48.dp), colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            )
          ) {
            Icon(
              painter = if (isPlaying.value) {
                painterResource(id = R.drawable.ic_pause)
              } else {
                painterResource(id = R.drawable.ic_play)
              }, contentDescription = "播放", tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          Spacer(modifier = Modifier.size(16.dp))
          IconButton(
            onClick = {
              viewModel.playNext()
            },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "next")
          }
        }
      }
      Spacer(modifier = Modifier.size(16.dp))
      // 时长信息
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
      ) {
        Box(
          modifier = Modifier.align(Alignment.CenterStart)
        ) {
          Text(
            text = positionMsState.value.formatDurationSecs(),
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier.align(Alignment.CenterEnd)
        ) {
          Text(
            text = song.value?.duration?.msToSecs()?.formatDurationSecs() ?: "--:--",
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
        }
      }

      // 播放进度条
      LinearProgressIndicator(
        progress = {
          positionMsState.value.toFloat() / (song.value?.duration?.msToSecs()?.toFloat() ?: 1f)
        }, modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
      )
    }
  }
}


@Preview
@Composable
fun Preview(modifier: Modifier = Modifier) {
  CurrentListPage(modifier = modifier)
}