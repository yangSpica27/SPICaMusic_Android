package me.spica27.spicamusic.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.SliderDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.viewModel.PlayBackViewModel

/**
 * 底部播放条
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val songState = playBackViewModel.currentSongFlow.collectAsStateWithLifecycle().value

  val context = LocalContext.current

  val coverPainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(context).data(songState?.getCoverUri()).build(),
  )

  val isPlaying = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()

  val positionSec = playBackViewModel.positionSec.collectAsStateWithLifecycle().value

  val isSeekingState = remember { mutableStateOf(false) }

  val seekValueState = remember { mutableFloatStateOf(0f) }



  LaunchedEffect(positionSec) {
    if (isSeekingState.value) return@LaunchedEffect
    seekValueState.floatValue = positionSec.secsToMs() * 1f
  }



  Box(
    modifier = modifier
      .padding(horizontal = 16.dp)
      .background(
        MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium
      )

  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .width(45.dp)
            .height(45.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .clip(MaterialTheme.shapes.medium)
            .border(
              2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
              MaterialTheme.shapes.medium
            ), contentAlignment = Alignment.Center
        ) {
          if (coverPainterState.value is AsyncImagePainter.State.Success) {
            Image(
              painter = coverPainter,
              contentDescription = "Cover",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Text(
              modifier = Modifier.rotate(45f),
              text = songState?.displayName ?: "Unknown",
              style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.W900
              )
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
          modifier = Modifier.weight(1f)
        ) {
          Text(
            songState?.displayName ?: "UNKNOWN",
            maxLines = 1,
            style = MaterialTheme.typography.titleMedium.copy(
              fontWeight = FontWeight.ExtraBold
            ),
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
          Text(
            songState?.artist ?: "<unknown>",
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium.copy(),
            modifier = Modifier
              .fillMaxWidth()
              .alpha(.5f),
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        IconButton(
          modifier = Modifier.size(60.dp), onClick = {
            playBackViewModel.togglePlaying()
          }, colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
          )
        ) {
          Icon(
            painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = "Play/Pause",
            tint = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }
      WavySlider(
        modifier = Modifier
          .fillMaxWidth(),
        value = (seekValueState.floatValue / (songState?.duration ?: 1)).coerceIn(0f, 1f),
        onValueChange = {
          seekValueState.floatValue = it * (songState?.duration ?: 1).toFloat()
          isSeekingState.value = true
        },
        onValueChangeFinished = {
          playBackViewModel.seekTo(seekValueState.floatValue.toLong())
          isSeekingState.value = false
        },
        colors = SliderDefaults.colors(
          thumbColor = MaterialTheme.colorScheme.tertiary,
          activeTrackColor = MaterialTheme.colorScheme.tertiary,
          disabledThumbColor = MaterialTheme.colorScheme.tertiaryContainer,
        )
      )
    }
  }
}
