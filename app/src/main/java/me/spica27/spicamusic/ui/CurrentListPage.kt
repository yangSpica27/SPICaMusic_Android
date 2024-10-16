package me.spica27.spicamusic.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.viewModel.MusicViewModel


@Composable
fun CurrentListPage(
  modifier: Modifier = Modifier,
  musicViewModel: MusicViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val isPlaying = musicViewModel.isPlaying.collectAsState(false)

  val currentIndex = musicViewModel.playlistCurrentIndex.collectAsState(0)

  val playList = musicViewModel.playList.collectAsState(emptyList())

  Column(
    modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
  ) {
    Text(
      text = if (isPlaying.value) {
        "正在播放"
      } else {
        "未在播放"
      }, style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.W600
      ), modifier = Modifier.padding(20.dp)
    )
    NowPlayIngSong(musicViewModel, navigator)
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
          text = "${currentIndex.value}/${playList.value.size}", style = MaterialTheme.typography.bodyMedium
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
      CurrentList(musicViewModel)
    }
  }
}

@Composable
private fun CurrentList(
  viewModel: MusicViewModel,
) {

  val currentSong = viewModel.currentSongFlow.collectAsState(null)

  val playlist = viewModel.playList.collectAsState(emptyList())

  LazyColumn {

    itemsIndexed(playlist.value, key = { _, song -> song.songId.toString() }) { index, song ->
      SongItem(
        isPlaying = currentSong.value?.songId == song.songId, song = song,
        onClick = {
          viewModel.play(playlist.value[index], playlist.value)
        })
    }

    item {
      Spacer(modifier = Modifier.size(60.dp))
    }
  }
}

@Composable
private fun SongItem(isPlaying: Boolean = false, song: Song, onClick: () -> Unit = { }) {
  val painter = rememberAsyncImagePainter(song.getCoverUri())
  Row(
    Modifier
      .background(
        color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface,
      )
      .clickable {
        onClick()
      }
      .padding(vertical = 12.dp, horizontal = 16.dp)
      .fillMaxWidth()
  ) {
    // 封面
    Box(
      modifier = Modifier
        .size(48.dp)
        .fillMaxWidth()
        .background(
          color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium
        ),
      contentAlignment = Alignment.Center,
    ) {
      if (isPlaying) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f), shape = MaterialTheme.shapes.medium
            ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "播放中", style = MaterialTheme.typography.bodySmall.copy(
              color = MaterialTheme.colorScheme.background
            )
          )
        }
      } else {

        if (painter.state is AsyncImagePainter.State.Success) {
          Image(
            painter = painter, contentDescription = "封面", modifier = Modifier.size(48.dp)
          )
        } else {
          Box(
            modifier = Modifier
              .size(48.dp)
              .background(
                color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = song.displayName.first().toString(), style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
              )
            )
          }
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
        text = song.displayName, modifier = Modifier.fillMaxWidth(), fontWeight = androidx.compose.ui.text.font.FontWeight.W600, maxLines = 1
      )
      Text(
        text = song.artist, maxLines = 1, modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      )
    }
    // 菜单
    IconButton(
      onClick = { },
      modifier = Modifier.size(48.dp),
    ) {
      Icon(
        imageVector = Icons.Default.MoreVert, contentDescription = "播放", tint = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}

@Composable
fun NowPlayIngSong(viewModel: MusicViewModel, navigator: AppComposeNavigator? = null) {
  val song = viewModel.currentSongFlow.collectAsState(null)
  val positionMsState = viewModel.positionDs.collectAsState(0L)
  val isPlaying = viewModel.isPlaying.collectAsState(false)
  val painter = rememberAsyncImagePainter(song.value?.getCoverUri())
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

          if (painter.state is AsyncImagePainter.State.Success) {
            Image(
              painter = painter, contentDescription = "封面", modifier = Modifier.size(64.dp)
            )
          } else {
            Box(
              modifier = Modifier
                .size(64.dp)
                .background(
                  color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium
                ),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = song.value?.displayName?.first().toString(), style = MaterialTheme.typography.bodyMedium.copy(
                  color = MaterialTheme.colorScheme.onSurface
                )
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
            text = song.value?.displayName ?: "--", modifier = Modifier.fillMaxWidth(), fontWeight = androidx.compose.ui.text.font.FontWeight.W600, maxLines = 1
          )
          Text(
            text = song.value?.artist ?: "--", maxLines = 1, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium.copy(
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
            }, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            )
          ) {
            Icon(
              painter = if (isPlaying.value) {
                painterResource(id = R.drawable.ic_pause)
              } else {
                painterResource(id = R.drawable.ic_play)
              }, contentDescription = "播放",
              tint = MaterialTheme.colorScheme.onPrimaryContainer
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
          modifier = Modifier
            .align(Alignment.CenterStart)
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
          modifier = Modifier
            .align(Alignment.CenterEnd)
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
        },
        modifier = Modifier
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