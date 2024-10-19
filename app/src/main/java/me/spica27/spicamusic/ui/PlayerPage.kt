package me.spica27.spicamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.utils.formatDurationDs
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToDs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.VisualizerSurfaceView
import me.spica27.spicamusic.widget.audio_seekbar.AudioWaveform
import timber.log.Timber


@Composable
fun PlayerPage(
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  // 当前播放的歌曲
  val currentPlayingSong = playBackViewModel.currentSongFlow.collectAsState(null)

  // 快速傅里叶变换后的振幅
  val amp = playBackViewModel.playingSongAmplitudes.collectAsState(emptyList())

  Box(
    modifier = Modifier
      .fillMaxSize(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize(),
    ) {
      Spacer(modifier = Modifier.height(15.dp))
      //  标题
      Title(
        modifier = Modifier.padding(vertical = 10.dp, horizontal = 20.dp),
      )
      Spacer(modifier = Modifier.height(15.dp))
      // 封面
      Cover(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
          .weight(1f),
        songState = currentPlayingSong
      )
      Spacer(modifier = Modifier.height(20.dp))
      // 歌名和歌手
      SongInfo(
        song = currentPlayingSong.value,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp)
      )
      ControlPanel(
        modifier = Modifier.padding(vertical = 15.dp, horizontal = 20.dp),
        ampState = amp
      )
      Text(
        modifier = Modifier
          .padding(10.dp)
          .fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        text = "向上滑动查看播放列表",
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      )
    }
  }
}

@Composable
private fun Title(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val indexState = playBackViewModel.playlistCurrentIndex.collectAsState(0)

  val playlistSizeState = playBackViewModel.nowPlayingListSize.collectAsState(0)

  Row(
    modifier = modifier,
  ) {
    Text(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
        .padding(vertical = 4.dp, horizontal = 8.dp),
      text = "循环播放",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
        .padding(vertical = 4.dp, horizontal = 8.dp),
      text = "第 ${indexState.value + 1} / ${playlistSizeState.value} 首",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
    )
  }
}

/// 封面
@Composable
private fun Cover(
  modifier: Modifier = Modifier,
  songState: State<Song?>,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val context = LocalContext.current

  val coverPainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(context)
      .data(songState.value?.getCoverUri())
      .build(),
  )


  val coverPainterState = coverPainter.state.collectAsState()

  val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface

  Box(
    modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainer),
    contentAlignment = Alignment.Center,
  ) {


    AndroidView(
      factory = { context ->
        VisualizerSurfaceView(context).apply {
          setBgColor(backgroundColor.toArgb())
          setColor(onSurfaceColor.toArgb())
        }
      },
      update = { view ->
        view.setBgColor(backgroundColor.toArgb())
        view.setColor(onSurfaceColor.toArgb())
      },
      modifier = Modifier
        .fillMaxSize()
        .aspectRatio(1f)
    )

//    if (coverPainterState.value is AsyncImagePainter.State.Success) {
//      Image(
//        painter = coverPainter,
//        contentDescription = "Cover",
//        modifier = Modifier
//          .fillMaxSize(),
//        contentScale = ContentScale.Crop
//      )
//    } else {
//      Text(
//        modifier = Modifier.rotate(45f),
//        text = songState.value?.displayName ?: "Unknown",
//        style = MaterialTheme.typography.headlineLarge.copy(
//          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
//          fontWeight = FontWeight.W900
//        )
//      )
//    }
  }
}

/// 控制面板
@Composable
private fun ControlPanel(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  ampState: State<List<Int>>
) {


  val isPlayingState = playBackViewModel.isPlaying.collectAsState(false)

  val songState = playBackViewModel.currentSongFlow.collectAsState(null)

  val positionSecState = playBackViewModel.positionSec.collectAsState(0L)

  val isSeekingState = remember { mutableStateOf(false) }

  val seekValueState = remember { mutableFloatStateOf(0f) }



  LaunchedEffect(positionSecState.value) {
    if (isSeekingState.value) return@LaunchedEffect
    seekValueState.floatValue = positionSecState.value.secsToMs() * 1f
  }

  Column(
    modifier = modifier,
  ) {

    // 振幅 进度条
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(80.dp),
      contentAlignment = Alignment.Center
    ) {
      AudioWaveform(
        amplitudes = ampState.value,
        waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceVariant),
        progressBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier
          .fillMaxWidth(),
        progress = seekValueState.floatValue / (songState.value?.duration ?: 1).toFloat(),
        onProgressChangeFinished = {
          playBackViewModel.seekTo(seekValueState.floatValue.toLong())
          isSeekingState.value = false
        },
        onProgressChange = {
          Timber.d("Seeking to $it")
          seekValueState.floatValue = it * (songState.value?.duration ?: 1).toFloat()
          isSeekingState.value = true
        }
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Current Time
      Text(
        modifier = Modifier
          .padding(vertical = 4.dp, horizontal = 8.dp),
        text = positionSecState.value.formatDurationSecs(),
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.width(8.dp))

      // 滑动到的地方
      AnimatedVisibility(
        visible = isSeekingState.value,
      ) {
        Text(
          modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
            .padding(vertical = 4.dp, horizontal = 8.dp),
          text = seekValueState.floatValue.toLong().msToSecs().formatDurationSecs(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Total Time
      Text(
        text = songState.value?.duration?.msToDs()?.formatDurationDs() ?: "0:00",
        style = MaterialTheme.typography.bodyMedium
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Spacer(modifier = Modifier.weight(2f))
      // Previous
      IconButton(onClick = {
        playBackViewModel.playPre()
      }, modifier = Modifier.size(60.dp)) {
        Icon(painter = painterResource(id = R.drawable.ic_pre), contentDescription = "Previous")
      }
      Spacer(modifier = Modifier.weight(1f))
      // Play/Pause
      IconButton(
        modifier = Modifier.size(60.dp),
        onClick = {
          playBackViewModel.togglePlaying()
        }, colors = IconButtonDefaults.iconButtonColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
      ) {
        Icon(
          painter = painterResource(id = if (isPlayingState.value) R.drawable.ic_pause else R.drawable.ic_play),
          contentDescription = "Play/Pause",
          tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      // Next
      IconButton(onClick = {
        playBackViewModel.playNext()
      }, modifier = Modifier.size(60.dp)) {
        Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "Next")
      }
      Spacer(modifier = Modifier.weight(2f))
    }

  }


}

// 歌名和歌手
@Composable
private fun SongInfo(song: Song?, modifier: Modifier = Modifier) {
  if (song == null) {
    return
  }
  Row(
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.weight(2f),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        maxLines = 1,
        text = song.displayName,
        style = MaterialTheme.typography.titleLarge.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
          fontWeight = androidx.compose.ui.text.font.FontWeight.W600
        ),
        modifier = Modifier.basicMarquee(),
      )
      Spacer(modifier = Modifier.height(5.dp))
      Text(
        modifier = Modifier.basicMarquee(),
        maxLines = 1,
        text = song.artist,
        style = MaterialTheme.typography.titleMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    IconButton(
      onClick = { /*TODO*/ },
    ) {
      if (song.like) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = "More",
          tint = Color(0xFFF44336)
        )
      } else {
        Icon(
          imageVector = Icons.Default.FavoriteBorder,
          contentDescription = "More",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }
    }
    Spacer(Modifier.width(10.dp))
    IconButton(
      onClick = { /*TODO*/ },
    ) {
      Icon(
        Icons.Default.MoreVert,
        contentDescription = "More",
      )
    }
  }
}