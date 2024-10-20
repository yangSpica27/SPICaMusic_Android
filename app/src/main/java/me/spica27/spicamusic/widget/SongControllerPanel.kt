package me.spica27.spicamusic.widget

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.viewModel.SongViewModel


/// 歌曲信息面板
@Composable
fun SongControllerPanel(
  modifier: Modifier = Modifier,
  songId: Long,
  songViewModel: SongViewModel = hiltViewModel<SongViewModel>()
) {


  val song = songViewModel.getSongFlow(songId).collectAsState(null).value

  if (song == null) {
    Box(
      modifier = modifier
        .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
        .fillMaxWidth()
        .aspectRatio(1f),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
    return
  }

  val coverPainter = rememberAsyncImagePainter(
    model = song.getSongUri(),
  )
  val coverState = coverPainter.state.collectAsState()

  Box(
    modifier = modifier
      .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
  ) {
    Column {
      // 歌曲信息和封面
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
      ) {
        // 封面
        Box(
          modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .weight(1f)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.medium)
        ) {

          if (coverState.value is AsyncImagePainter.State.Success) {
            AsyncImage(
              model = coverPainter,
              contentDescription = null,
              modifier = Modifier.fillMaxSize()
            )
          } else {
            Box(
              modifier = Modifier
                .fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = song.displayName.first().toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  fontWeight = FontWeight.W600
                )
              )
            }
          }
        }
        // 歌曲信息
        Column(
          modifier = Modifier
            .weight(2f)
            .padding(start = 16.dp)
            .wrapContentHeight(
              Alignment.CenterVertically
            ),
        ) {
          Text(
            text = song.displayName,
            style = MaterialTheme.typography.titleMedium.copy(
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.W600
            ),
            maxLines = 1,
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
          Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.W500
            ),
            maxLines = 1
          )
          Text(
            text = song.duration.msToSecs().formatDurationSecs(),
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.6f
              ),
              fontWeight = FontWeight.W600
            ),
            maxLines = 1,
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
          Text(
            "播放次数:${song.playTimes}",
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = 0.6f
              ),
              fontWeight = FontWeight.W600
            ),
          )
        }
      }
      // 分割线
      HorizontalDivider(
        modifier = Modifier
          .fillMaxWidth(),
        thickness = 1.dp / 2
      )

      BottomButton(
        modifier = Modifier.fillMaxWidth(),
        onclick = { },
        icon = {
          Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null
          )
        },
        text = "添加到当前播放列表"
      )

      BottomButton(
        modifier = Modifier.fillMaxWidth(),
        onclick = {
          songViewModel.toggleFavorite(songId)
        },
        icon = {
          Icon(
            imageVector = if (song.like) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null
          )
        },
        text = if (song.like) "取消收藏"
        else "收藏"
      )


      Row(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        BottomButton(
          modifier = Modifier.weight(1f),
          onclick = { },
          icon = {
            Icon(
              imageVector = Icons.Outlined.PlayArrow,
              contentDescription = null
            )
          },
          text = "立即播放"
        )

        BottomButton(
          modifier = Modifier.weight(1f),
          onclick = { },
          icon = {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null
            )
          },
          text = "信息"
        )

      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
      ) {
        BottomButton(
          modifier = Modifier.weight(1f),
          onclick = { },
          icon = {
            Icon(
              imageVector = Icons.Outlined.AccountBox,
              contentDescription = null
            )
          },
          text = "封面"
        )

        BottomButton(
          modifier = Modifier.weight(1f),
          onclick = { },
          icon = {
            Icon(
              imageVector = Icons.Outlined.Delete,
              contentDescription = null
            )
          },
          text = "删除"
        )

      }

    }
  }
}

@Composable
private fun BottomButton(
  modifier: Modifier = Modifier,
  onclick: () -> Unit,
  icon: @Composable BoxScope.() -> Unit,
  text: String
) {

  Row(
    modifier = modifier
      .clickable {
        onclick()
      }
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Icon
    Box(
      modifier = Modifier
        .width(24.dp),
      contentAlignment = Alignment.Center
    ) {
      icon()
    }
    Spacer(modifier = Modifier.width(12.dp))
    // Text
    Crossfade(
      targetState = text, label = ""
    ) {
      Text(
        text = it,
        maxLines = 1,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface,
        ),
      )
    }
  }
}


