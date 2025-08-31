package me.spica27.spicamusic.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.wrapper.activityViewModel

/**
 * 底部播放条
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBar(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = activityViewModel(),
) {

  val songState = playBackViewModel.currentSongFlow.collectAsStateWithLifecycle().value

  val isPlaying = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  val positionSec = playBackViewModel.positionSec.collectAsStateWithLifecycle().value

  val isSeekingState = remember { mutableStateOf(false) }

  val seekValueState = remember { mutableFloatStateOf(0f) }



  LaunchedEffect(positionSec) {
    if (isSeekingState.value) return@LaunchedEffect
    seekValueState.floatValue = positionSec.secsToMs() * 1f
  }



  Box(
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.Center
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        AnimatedContent(
          songState,
          label = "cover",
          contentKey = {
            it?.songId ?: -1L
          },
          transitionSpec =
            {
              scaleIn(
                animationSpec = spring()
              )+ fadeIn() togetherWith scaleOut()+ fadeOut()
            },
        ) { songState ->
          songState?.let {
            CoverWidget(
              modifier = Modifier
                .size(48.dp)
                .background(
                  MaterialTheme.colorScheme.surfaceContainerHigh,
                  MaterialTheme.shapes.medium
                )
                .clip(MaterialTheme.shapes.medium),
              song = songState
            )
          }
        }

        Spacer(modifier = Modifier.width(12.dp))
        Column(
          modifier = Modifier.weight(1f)
        ) {
          AnimatedContent(
            songState,
            label = "songName",
            contentKey = {
              "${it?.displayName}"
            },
            transitionSpec =
              {
                slideInVertically(
                  initialOffsetY = { -it },
                  animationSpec = tween(delayMillis = 150)
                ) togetherWith slideOutVertically { it }
              }
          ) { songState ->
            Text(
              songState?.displayName ?: "UNKNOWN",
              maxLines = 1,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
              ),
              modifier = Modifier
                .fillMaxWidth()
                .basicMarquee()
            )
          }
          AnimatedContent(
            songState,
            label = "artist",
            contentKey = {
              it?.artist ?: "<unknown>"
            },
            transitionSpec =
              {
                slideInVertically { -it } togetherWith slideOutVertically { it }
              }
          ) { songState ->
            Text(
              songState?.artist ?: "<unknown>",
              maxLines = 1,
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
              ),
              modifier = Modifier
                .fillMaxWidth()
                .alpha(.5f),
            )
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(
          isPlaying,
          transitionSpec =
            {
              scaleIn() togetherWith scaleOut()
            }
        ) { isPlaying ->
          Box(
            modifier =
              Modifier
                .size(48.dp)
                .background(
                  MaterialTheme.colorScheme.primaryContainer,
                  CircleShape
                )
                .clip(CircleShape)
                .clickable {
                  playBackViewModel.togglePlaying()
                }
                .innerShadow(
                  shape = CircleShape, shadow = Shadow(
                    radius = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    alpha = .11f
                  )
                ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
              contentDescription = "Play/Pause",
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
    }
  }
}


