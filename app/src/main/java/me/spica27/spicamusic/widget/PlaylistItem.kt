package me.spica27.spicamusic.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song


/// 歌单条目
@Composable
fun PlaylistItem(
  modifier: Modifier = Modifier,
  playlist: Playlist,
  onClick: () -> Unit = {},
  onClickMenu: () -> Unit = {},
  showMenu: Boolean = false
) {
  Row(modifier = modifier
    .clickable {
      onClick()
    }
    .padding(horizontal = 16.dp, vertical = 6.dp)
    .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .width(50.dp)
        .height(50.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = (playlist.playlistName.firstOrNull() ?: "A").toString(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      modifier = Modifier.weight(1f),
      text = playlist.playlistName, style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
      )
    )
    if (showMenu) {
      IconButton(
        onClick = onClickMenu,
      ) {
        Icon(
          imageVector = Icons.Default.MoreVert,
          contentDescription = "More",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
fun PlayingSongItem(isPlaying: Boolean = false, song: Song, onClick: () -> Unit = { }) {
  val painter = rememberAsyncImagePainter(song.getCoverUri())
  Row(Modifier
    .background(
      color = if (isPlaying) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
      else MaterialTheme.colorScheme.surface,
    )
    .clickable {
      onClick()
    }
    .padding(vertical = 12.dp, horizontal = 16.dp)
    .fillMaxWidth()) {
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
              color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f),
              shape = MaterialTheme.shapes.medium
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
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
              ),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = (song.displayName.firstOrNull() ?: "S").toString(),
              style = MaterialTheme.typography.bodyMedium.copy(
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
        text = song.displayName,
        modifier = if (isPlaying) Modifier
          .fillMaxWidth()
          .basicMarquee() else Modifier.fillMaxWidth(),
        fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
        maxLines = 1
      )
      Text(
        text = song.artist,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth(),
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
        imageVector = Icons.Default.MoreVert,
        contentDescription = "播放",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }
  }
}


@Composable
fun SelectableSongItem(
  modifier: Modifier = Modifier,
  song: Song,
  selected: Boolean = false,
  playing: Boolean = false,
  onToggle: () -> Unit = { },
) {
  Row(modifier = modifier
    .clickable {
      onToggle()
    }
    .padding(horizontal = 16.dp, vertical = 6.dp)
    .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {

    // 选中图标
    Box(
      modifier = Modifier
        .padding(end = 16.dp)
        .width(24.dp)
        .height(24.dp)
        .background(
          if (selected) {
            MaterialTheme.colorScheme.primaryContainer
          } else {
            MaterialTheme.colorScheme.surface
          }, CircleShape
        )
        .border(
          width = 1.dp,
          color = if (selected) Color.Transparent
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          shape = CircleShape
        )
        .padding(2.dp),
      contentAlignment = Alignment.Center
    ) {
      if (selected) {
        Icon(
          modifier = Modifier.fillMaxWidth(),
          imageVector = Icons.Default.Check,
          contentDescription = "Selected",
          tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
      }
    }

    Column {
      Text(
        text = song.displayName, style = MaterialTheme.typography.bodyLarge.copy(
          color = if (selected) {
            MaterialTheme.colorScheme.secondary
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          fontWeight = FontWeight.W600
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = song.artist, style = MaterialTheme.typography.bodyMedium.copy(
          color = if (selected) {
            MaterialTheme.colorScheme.secondary
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          },
          fontWeight = FontWeight.W500
        )
      )
    }

  }
}