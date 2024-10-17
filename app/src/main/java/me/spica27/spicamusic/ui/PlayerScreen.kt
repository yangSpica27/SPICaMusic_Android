package me.spica27.spicamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.formatDurationDs
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToDs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import timber.log.Timber

@Composable
fun PlayerScreen(
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  Scaffold(
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 10.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Top
      ) {
        AppBar(onSettings = {}, onBack = {})
        Spacer(modifier = Modifier.height(16.dp))
        Cover(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        SingInfo(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        PlayerControls(modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(30.dp))
      }

      // Player UI
    }
  }
}

@Composable
private fun SingInfo(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val song = playBackViewModel.currentSongFlow.collectAsState(null)

  LaunchedEffect(song) {
    Timber.tag("PlayerScreen").d("Song: $song")
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    // Title
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(),
      maxLines = 1,
      text = song.value?.displayName ?: "",
      style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W600)
    )
    Spacer(modifier = Modifier.height(4.dp))
    // Subtitle
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .basicMarquee(),
      maxLines = 1,
      text = song.value?.artist ?: "",
      style = MaterialTheme.typography.bodyMedium
    )
  }
}

// 播放控制面板
@Composable
private fun PlayerControls(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val isPlaying = playBackViewModel.isPlaying.collectAsState(false)

  val song = playBackViewModel.currentSongFlow.collectAsState(null)

  val positionSec = playBackViewModel.positionSec.collectAsState(0L)

  val isSeeking = remember { mutableStateOf(false) }

  val seekValue = remember { mutableFloatStateOf(0f) }

  LaunchedEffect(positionSec) {
    if (isSeeking.value) return@LaunchedEffect
    seekValue.floatValue = positionSec.value.secsToMs() * 1f
  }

  Box(
    modifier = modifier
      .padding(16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
      .padding(16.dp)
  ) {
    Column {
      // Progress
      Slider(
        value = seekValue.floatValue,
        valueRange = 0f..(song.value?.duration ?: 0).toFloat(),
        onValueChange = {
          seekValue.floatValue = it
          isSeeking.value = true
        },
        onValueChangeFinished = {
          playBackViewModel.seekTo(seekValue.floatValue.toLong())
          isSeeking.value = false
        },
        modifier = Modifier.fillMaxWidth()
      )
      // Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Current Time
        Text(
          modifier = Modifier
            .padding(vertical = 4.dp, horizontal = 8.dp),
          text = positionSec.value.formatDurationSecs(),
          style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 滑动到的地方
        AnimatedVisibility(
          visible = isSeeking.value,
        ) {
          Text(
            modifier = Modifier
              .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
              .padding(vertical = 4.dp, horizontal = 8.dp),
            text = seekValue.floatValue.toLong().msToSecs().formatDurationSecs(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Total Time
        Text(
          text = song.value?.duration?.msToDs()?.formatDurationDs() ?: "0:00",
          style = MaterialTheme.typography.bodyMedium
        )
      }
      // Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
      ) {
        // Previous
        IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
          Icon(painter = painterResource(id = R.drawable.ic_pre), contentDescription = "Previous")
        }
        // Play/Pause
        IconButton(
          modifier = Modifier.size(48.dp),
          onClick = {
            playBackViewModel.togglePlaying()
          }, colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
          )
        ) {
          Icon(
            painter = painterResource(id = if (isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = "Play/Pause",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        // Next
        IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
          Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "Next")
        }
      }
    }

  }
}

@Composable
private fun Cover(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val song = playBackViewModel.currentSongFlow.collectAsState(null)

  val painter = song.value?.getCoverUri()?.let { rememberAsyncImagePainter(it) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .background(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.shapes.medium
      ),
    contentAlignment = Alignment.Center
  ) {
    // Cover Image
    if (painter?.state is AsyncImagePainter.State.Success) {
      Image(
        modifier = Modifier.fillMaxSize(),
        painter = painter, contentDescription = "封面",
      )
    } else {
      Image(
        modifier = Modifier.fillMaxSize(),
        painter = painterResource(R.drawable.ic_dvd),
        contentDescription = "封面",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
      )
    }
  }
}

@Composable
private fun AppBar(
  modifier: Modifier = Modifier,
  onBack: () -> Unit,
  onSettings: () -> Unit,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val isPlaying = playBackViewModel.isPlaying.collectAsState(false)
  val song = playBackViewModel.currentSongFlow.collectAsState(null)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // AppBar UI
    IconButton(onClick = {}) {
      Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Back")
    }
    Column(modifier = Modifier.weight(1f)) {
      // Title
      Text(
        text = if (isPlaying.value) {
          "正在播放"
        } else {
          "未在播放"
        }, style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.W600
        )
      )
    }
    IconButton(onClick = {}) {
      Icon(imageVector = Icons.Default.MoreVert, contentDescription = "settings")
    }
  }
}

