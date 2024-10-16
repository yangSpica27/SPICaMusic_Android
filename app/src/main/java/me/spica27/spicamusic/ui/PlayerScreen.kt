package me.spica27.spicamusic.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import me.spica27.spicamusic.R
import me.spica27.spicamusic.viewModel.MusicViewModel

@Composable
fun PlayerScreen() {
  Scaffold(
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
    ) {
      Column(
        modifier = Modifier.fillMaxSize(),
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
fun SingInfo(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
  ) {
    // Title
    Text(text = "歌曲名称", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W600))
    Spacer(modifier = Modifier.height(4.dp))
    // Subtitle
    Text(text = "作者", style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun PlayerControls(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .padding(16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
      .padding(16.dp)
  ) {
    Column {
      // Progress
      Slider(
        value = 0f,
        onValueChange = {},
        modifier = Modifier.fillMaxWidth()
      )
      // Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
      ) {
        // Current Time
        Text(text = "00:00", style = MaterialTheme.typography.bodyMedium)
        // Total Time
        Text(text = "00:00", style = MaterialTheme.typography.bodyMedium)
      }
      // Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
      ) {
        // Previous
        IconButton(onClick = {}) {
          Icon(painter = painterResource(id = R.drawable.ic_pre), contentDescription = "Previous")
        }
        // Play/Pause
        IconButton(
          onClick = {}, colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
          )
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_play),
            contentDescription = "Play/Pause",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        // Next
        IconButton(onClick = {}) {
          Icon(painter = painterResource(id = R.drawable.ic_next), contentDescription = "Next")
        }
      }
    }

  }
}

@Composable
private fun Cover(
  modifier: Modifier = Modifier,
  musicViewModel: MusicViewModel = hiltViewModel()
) {

  val song = musicViewModel.currentSongFlow.collectAsState(null)

  val painter = song.value?.getCoverUri()?.let { rememberAsyncImagePainter(it) }

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer),
    contentAlignment = androidx.compose.ui.Alignment.Center
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
        painter = painterResource(R.mipmap.default_cover),
        contentDescription = "封面",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop
      )
    }
  }
}

@Composable
private fun AppBar(
  modifier: Modifier = Modifier, onBack: () -> Unit, onSettings: () -> Unit, musicViewModel: MusicViewModel = hiltViewModel()
) {

  val isPlaying = musicViewModel.isPlaying.collectAsState(false)
  val song = musicViewModel.currentSongFlow.collectAsState(null)

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
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
      Spacer(modifier = Modifier.height(4.dp))
      // Subtitle
      Text(text = song.value?.displayName ?: "", style = MaterialTheme.typography.bodyMedium)
    }
    IconButton(onClick = {}) {
      Icon(imageVector = Icons.Default.MoreVert, contentDescription = "settings")
    }
  }
}

@Preview
@Composable
private fun Preview() {
  PlayerScreen()
}