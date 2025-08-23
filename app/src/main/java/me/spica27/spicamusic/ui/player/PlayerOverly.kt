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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
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
import me.spica27.spicamusic.widget.LyricsView
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


  val overlyState = LocalPlayerWidgetState.current


  BackHandler(overlyState.value == PlayerOverlyState.PLAYER) {
    overlyState.value = PlayerOverlyState.BOTTOM
  }

  BackHandler(overlyState.value == PlayerOverlyState.BOTTOM) {
    overlyState.value = PlayerOverlyState.MINI
  }

  BackHandler(
    overlyState.value == PlayerOverlyState.FULLSCREEN_LRC
  ) {
    overlyState.value = PlayerOverlyState.PLAYER
  }

  LaunchedEffect(isPlaying) {
    if (isPlaying && overlyState.value == PlayerOverlyState.HIDE) {
      overlyState.value = PlayerOverlyState.MINI
    } else if (!isPlaying && overlyState.value == PlayerOverlyState.MINI) {
      overlyState.value = PlayerOverlyState.HIDE
    } else if (!isPlaying && overlyState.value == PlayerOverlyState.BOTTOM) {
      overlyState.value = PlayerOverlyState.MINI
    } else {
      Timber.tag("PlayerOverly").d("都不符合 isPlay =${isPlaying} overlyState = $overlyState")
    }
  }


  LaunchedEffect(overlyState.value) {
    Timber.tag("PlayerOverly").d("overlyState: $overlyState")
    if (overlyState.value == PlayerOverlyState.BOTTOM) {
      delay(5000)
      if (isPlaying) {
        overlyState.value = PlayerOverlyState.MINI
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
        targetState = overlyState.value,
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
                    overlyState.value = PlayerOverlyState.BOTTOM
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
                  .innerShadow(
                    shape = MaterialTheme.shapes.medium, shadow = Shadow(
                      radius = 2.dp,
                      color = MaterialTheme.colorScheme.onSurface,
                      alpha = .11f
                    )
                  )
                  .clip(
                    MaterialTheme.shapes.medium
                  )
                  .clickable {
                    overlyState.value = PlayerOverlyState.PLAYER
                  }
              ) {
                currentSong?.let {
                  Bottom()
                }
              }
            }
          }

          PlayerOverlyState.PLAYER -> {
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
                  overlyState.value = PlayerOverlyState.MINI
                },
              )
            }
          }

          PlayerOverlyState.FULLSCREEN_LRC -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .sharedBounds(
                  animatedVisibilityScope = this@AnimatedContent,
                  sharedContentState = sharedContentState,
                )
            ) {
              FullScreenLrc()
            }
          }
        }
      }
    }
  }
}

@Composable
private fun Bottom() {
  PlayerBar()
}

@Composable
private fun FullScreenLrc() {
  val playBackViewModel = activityViewModel<PlayBackViewModel>()
  // 当前播放的歌曲
  val currentPlayingSong = playBackViewModel.currentSongFlow.collectAsState()
    .value

  val currentTime = playBackViewModel.positionSec.collectAsStateWithLifecycle().value


  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(
        MaterialTheme.colorScheme.surfaceContainer
      )
  ) {
    if (currentPlayingSong == null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .clip(
            MaterialTheme.shapes.medium
          ),
        contentAlignment = Alignment.Center
      ) {
        Text("未在播放")
      }
    } else {
      Box {
        LyricsView(
          modifier = Modifier.fillMaxSize(),
          currentTime = currentTime * 1000,
          song = currentPlayingSong,
          onScroll = {
            playBackViewModel.seekTo(it.toLong())
          },
          placeHolder = {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .clip(
                  MaterialTheme.shapes.medium
                ),
              contentAlignment = Alignment.Center
            ) {
              Text("暂无歌词")
            }
          }
        )
      }
    }
  }


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

@Composable
fun PlayerOverlyContent(
  content: @Composable () -> Unit
) {
  val overlyState = remember { mutableStateOf(PlayerOverlyState.HIDE) }
  CompositionLocalProvider(LocalPlayerWidgetState provides overlyState) {
    content.invoke()
  }
}


internal val LocalPlayerWidgetState = staticCompositionLocalOf<MutableState<PlayerOverlyState>> {
  error("CompositionLocal LocalPlayerWidgetState not present")
}

enum class PlayerOverlyState {
  MINI, // 右边悬浮模式
  HIDE, // 隐藏模式
  BOTTOM, // 底栏模式
  PLAYER, // 播放器模式
  FULLSCREEN_LRC // 全屏歌词模式
}