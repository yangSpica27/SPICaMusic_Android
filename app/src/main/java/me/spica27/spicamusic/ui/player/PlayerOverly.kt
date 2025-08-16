package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.ui.main.player.PlayerScreen
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.PlayerBar
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerOverly(
  navigator: NavBackStack? = null,
) {

  val playbackViewModel: PlayBackViewModel = activityViewModel()

  val isPlaying = playbackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  val currentSong = playbackViewModel.currentSongFlow.collectAsStateWithLifecycle(null).value

  var overlyState by remember { mutableStateOf(PlayerOverlyState.HIDE) }


  BackHandler(overlyState == PlayerOverlyState.DETAIL) {
    overlyState = PlayerOverlyState.BOTTOM
  }

  BackHandler(overlyState == PlayerOverlyState.BOTTOM) {
    overlyState = PlayerOverlyState.MINI
  }

  LaunchedEffect(isPlaying) {
    if (isPlaying && overlyState == PlayerOverlyState.HIDE) {
      overlyState = PlayerOverlyState.MINI
    } else if (!isPlaying && overlyState == PlayerOverlyState.MINI) {
      overlyState = PlayerOverlyState.HIDE
    } else if (!isPlaying && overlyState == PlayerOverlyState.BOTTOM) {
      overlyState = PlayerOverlyState.MINI
    } else {
      Timber.tag("PlayerOverly").d("都不符合 isPlay =${isPlaying} overlyState = $overlyState")
    }
  }


  LaunchedEffect(overlyState) {
    Timber.tag("PlayerOverly").d("overlyState: $overlyState")
    if (overlyState == PlayerOverlyState.BOTTOM) {
      delay(5000)
      if (isPlaying) {
        overlyState = PlayerOverlyState.MINI
      }
    }
  }




  Box(
    modifier = Modifier
      .fillMaxSize()
  ) {
    SharedTransitionLayout(
      modifier = Modifier.fillMaxSize(),
    ) {
      val sharedContentState = rememberSharedContentState("player_widget")
      AnimatedContent(
        modifier = Modifier.fillMaxSize(),
        targetState = overlyState,
        label = "player_overly_state"
      ) { state ->
        when (state) {
          PlayerOverlyState.MINI -> {
            Box(
              modifier = Modifier.fillMaxSize()
            ) {
              Box(
                modifier = Modifier
                  .size(width = 64.dp, height = 64.dp)
                  .absoluteOffset(x = (32).dp)
                  .sharedBounds(
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedContentState = sharedContentState,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                  )
                  .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                  .clip(CircleShape)
                  .innerShadow(
                    shape = CircleShape,
                    Shadow(
                      radius = 6.dp,
                      color = MaterialTheme.colorScheme.onSurface,
                      alpha = .11f
                    )
                  )
                  .clickable {
                    overlyState = PlayerOverlyState.BOTTOM
                  }
                  .align(Alignment.CenterEnd)
              ) {
                currentSong?.let {
                  Mimi(
                    currentSong,
                  )
                }
              }
            }
          }

          PlayerOverlyState.HIDE -> {}
          PlayerOverlyState.BOTTOM -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  16.dp
                )
                .navigationBarsPadding()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .sharedBounds(
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedContentState = sharedContentState,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                  )
                  .align(Alignment.BottomCenter)
                  .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.shapes.medium
                  )
                  .clip(
                    MaterialTheme.shapes.medium
                  )
                  .clickable {
                    overlyState = PlayerOverlyState.DETAIL
                  }
              ) {
                currentSong?.let {
                  Bottom(currentSong)
                }
              }
            }
          }

          PlayerOverlyState.DETAIL -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                  animatedVisibilityScope = this@AnimatedContent,
                  sharedContentState = sharedContentState,
                  enter = scaleIn() + fadeIn(),
                  exit = scaleOut() + fadeOut(),
                )
            ) {
              PlayerScreen(
                navigator = navigator,
                onBackClick = {
                  overlyState = PlayerOverlyState.HIDE
                },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Bottom(song: Song) {
  PlayerBar()
}

@Composable
private fun Mimi(song: Song) {
  val infiniteTransition = rememberInfiniteTransition(label = "infinite")
  val rotateState = infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
      animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Restart
    ), label = ""
  )
  CoverWidget(
    modifier = Modifier
      .fillMaxSize()
      .padding(4.dp)
      .clip(CircleShape)
      .rotate(rotateState.value),
    song = song
  )
}


enum class PlayerOverlyState {
  MINI,
  HIDE,
  BOTTOM,
  DETAIL
}