package me.spica27.spicamusic.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song


/// 带封面的歌曲列表项
@Composable
fun SongItemWithCover(
  modifier: Modifier = Modifier,
  song: Song,
  onClick: () -> Unit = {},
  onMenuClick: (
    Offset
  ) -> Unit = {},
  onLikeClick: () -> Unit = {},
  onPlusClick: () -> Unit = {},
  showMenu: Boolean = true,
  showLike: Boolean = false,
  showPlus: Boolean = false,
  coverSize: Dp = 55.dp
) {

  val painter = rememberAsyncImagePainter(song.getCoverUri())

  val itemCoordinates = remember { mutableStateOf(Offset.Zero) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Box(
      modifier = Modifier
        .width(coverSize)
        .height(coverSize)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
        .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {

      if (painter.state is AsyncImagePainter.State.Success) {
        Image(
          painter = painter, contentDescription = "封面", modifier = Modifier.size(66.dp)
        )
      } else {
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
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.displayName, maxLines = 1, style = MaterialTheme.typography.bodyLarge.copy(
          color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1
      )
    }


    if (showPlus) {
      IconButton(onClick = {
        onPlusClick()
      }) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "更多"
        )
      }
    }

    if (showLike) {
      IconButton(onClick = {
        onLikeClick()
      }) {
        Icon(
          tint = if (song.like) Color.Red else
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          imageVector = if (song.like) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
          contentDescription = "更多"
        )
      }
    }



    if (showMenu) {
      IconButton(modifier = Modifier.onGloballyPositioned {
        itemCoordinates.value = it.localToWindow(Offset.Zero)
      }, onClick = {
        onMenuClick(itemCoordinates.value)
      }) {
        Icon(
          imageVector = Icons.Default.MoreVert, contentDescription = "更多"
        )
      }
    }
  }
}

