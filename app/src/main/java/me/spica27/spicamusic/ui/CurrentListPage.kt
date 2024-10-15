package me.spica27.spicamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.MusicViewModel


@Composable
fun CurrentListPage(
  modifier: Modifier = Modifier,
  musicViewModel: MusicViewModel = hiltViewModel()
) {

  val isPlaying = musicViewModel.isPlaying.collectAsState(false)

  val currentSong = musicViewModel.currentSongFlow.collectAsState(null)

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
    NowPlayIngSong(song = currentSong.value)
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
          text = "0/100", style = MaterialTheme.typography.bodyMedium
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
      CurrentList(currentSong.value, playList.value, musicViewModel)
    }
  }
}

@Composable
private fun CurrentList(
  current: Song? = null,
  playlist: List<Song> = emptyList(),
  viewModel: MusicViewModel
) {
  LazyColumn {
    items(count = playlist.size, key = { playlist[it].songId ?: -1 }) {
      SongItem(isPlaying = current?.songId == playlist[it].songId, song = playlist[it],
        onClick = {
          viewModel.play(playlist[it], playlist)
        })
    }
    item {
      Spacer(modifier = Modifier.size(60.dp))
    }
  }
}

@Composable
private fun SongItem(isPlaying: Boolean = false, song: Song, onClick: () -> Unit = { }) {
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
        AsyncImage(
          model = song.getCoverUri(),
          contentDescription = "封面", modifier = Modifier.size(48.dp),
        )
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
fun NowPlayIngSong(song: Song?, isPlaying: Boolean = false) {

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
            .size(64.dp)
            .background(
              color = MaterialTheme.colorScheme.surfaceContainer, shape = androidx.compose.foundation.shape.CircleShape
            ),
          contentAlignment = Alignment.Center,
        ) {
          AsyncImage(
            model = song?.getCoverUri(),
            contentDescription = "封面", modifier = Modifier.size(64.dp)
          )
        }
        // 歌曲信息
        Column(
          modifier = Modifier
            .padding(start = 16.dp)
            .weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = song?.displayName ?: "--", modifier = Modifier.fillMaxWidth(), fontWeight = androidx.compose.ui.text.font.FontWeight.W600, maxLines = 1
          )
          Text(
            text = song?.artist ?: "--", maxLines = 1, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
        }
        Row(
          horizontalArrangement = Arrangement.Center
        ) {
          IconButton(
            onClick = { },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "播放", tint = MaterialTheme.colorScheme.onSurface
            )
          }
          Spacer(modifier = Modifier.size(16.dp))
          IconButton(
            onClick = { }, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            )
          ) {
            Icon(
              imageVector = if (isPlaying) {
                Icons.Default.Close
              } else {
                Icons.Default.PlayArrow
              }, contentDescription = "播放",
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
          Spacer(modifier = Modifier.size(16.dp))
          IconButton(
            onClick = { },
            modifier = Modifier.size(48.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Default.ArrowForward, contentDescription = "播放", tint = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
      Spacer(modifier = Modifier.size(16.dp))
      // 时长信息
      Text(
        text = "--:-- / --:--", modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp), style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ), textAlign = androidx.compose.ui.text.style.TextAlign.End
      )
      // 播放进度条
      Slider(
        value = 0f, onValueChange = { }, modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      )
    }
  }
}


@Preview
@Composable
fun Preview(modifier: Modifier = Modifier) {
  CurrentListPage(modifier = modifier)
}