package me.spica27.spicamusic.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toCoilUri
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import me.spica27.spicamusic.utils.getColorFromMimeTypeString
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.utils.pressable


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


  val itemCoordinates = remember { mutableStateOf(Offset.Zero) }
  val coverPainter = rememberAsyncImagePainter(song.getCoverUri().toCoilUri())
  val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .pressable()
      .clickableNoRippleWithVibration { onClick() }
      .padding(horizontal = 16.dp, vertical = 6.dp),
  ) {
    Box(
      modifier = Modifier
        .width(coverSize)
        .height(coverSize)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
        .clip(MaterialTheme.shapes.medium), contentAlignment = Alignment.Center
    ) {

      if (coverPainterState.value is AsyncImagePainter.State.Success) {
        Image(
          painter = coverPainter,
          contentDescription = "封面",
          modifier = Modifier.size(66.dp),
          contentScale = ContentScale.Crop
        )
      } else {
        Box(
          Modifier
            .fillMaxWidth()
            .padding(8.dp),
        ) {
          Icon(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(id = R.drawable.ic_dvd),
            contentDescription = "封面",
            tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
          )
        }
      }

    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = song.displayName,
        maxLines = 1,
        style = MaterialTheme.typography.bodyLarge.copy(
          color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
          modifier = Modifier
            .padding(end = 8.dp)
            .background(
              color = Color(song.mimeType.getColorFromMimeTypeString()),
              shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp),
          text = song.getFormatMimeType().uppercase().replace("AUDIO/", ""),
          style = MaterialTheme.typography.bodyMedium.copy(
            color = Color.Black,
            fontWeight = FontWeight.W500,
            letterSpacing = 0.5.sp
          ),
          maxLines = 1
        )
        Text(
          text = song.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1
        )
      }
    }


    if (showPlus) {
      IconButton(onClick = {
        onPlusClick()
      }) {
        Icon(
          imageVector = Icons.Default.Add, contentDescription = "更多"
        )
      }
    }

    if (showLike) {
      IconButton(onClick = {
        onLikeClick()
      }) {
        Icon(
          tint = if (song.like) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.6f
          ),
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

@Composable
fun PlayingSongItem(
  modifier: Modifier = Modifier,
  showRemove: Boolean = false,
  onRemoveClick: () -> Unit = { },
  isPlaying: Boolean = false,
  song: Song,
  onClick: () -> Unit = { }
) {

  val coverPainter = rememberAsyncImagePainter(song.getCoverUri().toCoilUri())
  val coverPainterState = coverPainter.state.collectAsState()

  // 左指示器宽度
  val padding = animateFloatAsState(
    targetValue = if (isPlaying) 20.dp.value else 0f, label = "leftStrokeWidth",
    animationSpec = tween(225, easing = LinearEasing),
  )

  // 背景颜色
  val backgroundColor = animateColorAsState(
    targetValue = if (isPlaying) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.surface,
    label = "backgroundColor",
    animationSpec = tween(225, easing = LinearEasing),
  )

  val borderColor = animateColorAsState(
    targetValue = if (isPlaying) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    else Color.Transparent,
    label = "backgroundColor",
    animationSpec = tween(225, easing = LinearEasing),
  )

  Row(
    modifier = modifier
      .drawBehind {
        drawRoundRect(
          brush = SolidColor(borderColor.value),
          size = Size(size.width - padding.value * 2, size.height),
          topLeft = Offset(padding.value, 0f),
          cornerRadius = CornerRadius(12.dp.value),
          style = Stroke(
            width = 2.dp.value
          )
        )
        drawRoundRect(
          brush = SolidColor(backgroundColor.value),
          size = Size(size.width - padding.value * 2, size.height),
          topLeft = Offset(padding.value, 0f),
          cornerRadius = CornerRadius(12.dp.value)
        )
      }
      .noRippleClickable {
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
        )
        .clip(MaterialTheme.shapes.medium),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(48.dp)
          .background(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            shape = MaterialTheme.shapes.medium
          ),
        contentAlignment = Alignment.Center,
      ) {

        if (coverPainterState.value is AsyncImagePainter.State.Success) {
          Image(
            painter = coverPainter,
            contentDescription = "封面",
            modifier = Modifier.size(66.dp),
            contentScale = ContentScale.Crop
          )
        } else {
          Icon(
            modifier = Modifier
              .fillMaxWidth()
              .scale(0.8f),
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
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = song.displayName,
        modifier = if (isPlaying) Modifier
          .fillMaxWidth()
          .basicMarquee() else Modifier.fillMaxWidth(),
        fontWeight = FontWeight.W600,
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

    if (showRemove) {
      IconButton(
        onClick = {
          onRemoveClick()
        },
        modifier = Modifier.size(48.dp),
      ) {
        Icon(
          painter = painterResource(id = R.drawable.ic_remove),
          contentDescription = "delete from list",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
      }
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
  Row(
    modifier = modifier
      .clickable {
        onToggle()
      }
      .padding(horizontal = 16.dp, vertical = 6.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically) {

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
          width = 1.dp, color = if (selected) Color.Transparent
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), shape = CircleShape
        )
        .padding(2.dp), contentAlignment = Alignment.Center
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
          }, fontWeight = FontWeight.W600
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = song.artist, style = MaterialTheme.typography.bodyMedium.copy(
          color = if (selected) {
            MaterialTheme.colorScheme.secondary
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
          }, fontWeight = FontWeight.W500
        )
      )
    }

  }
}

