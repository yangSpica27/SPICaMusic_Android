package me.spica27.spicamusic.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.utils.clickableNoRippleClickableWithVibration


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
    .clickableNoRippleClickableWithVibration{
      onClick()
    }
    .padding(horizontal = 16.dp, vertical = 6.dp)
    .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .width(50.dp)
        .height(50.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = (playlist.playlistName.firstOrNull() ?: "A").toString(),
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Normal
        ),
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      modifier = Modifier.weight(1f),
      text = playlist.playlistName, style = MaterialTheme.typography.titleMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
      ),
      maxLines = 1
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
    }else{
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "More",
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f)
      )
    }
  }
}

