package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope.ResizeMode.Companion.ScaleToBounds
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.ui.main.player.PlayerScreen
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.PlayerBar
import me.spica27.spicamusic.widget.materialSharedAxisYIn
import me.spica27.spicamusic.widget.materialSharedAxisYOut
import me.spica27.spicamusic.widget.materialSharedAxisZIn
import me.spica27.spicamusic.widget.materialSharedAxisZOut
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerOverly(
  navigator: NavController? = null,
) {

  val playbackViewModel: PlayBackViewModel = activityViewModel()

  val isPlaying = playbackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  val currentSong = playbackViewModel.currentSongFlow.collectAsStateWithLifecycle(null).value


  val overlyState = LocalPlayerWidgetState.current


  BackHandler(overlyState.value == PlayerOverlyState.PLAYER) {
    overlyState.value = PlayerOverlyState.BOTTOM
  }


  LaunchedEffect(isPlaying) {
    if (isPlaying && overlyState.value == PlayerOverlyState.HIDE) {
      overlyState.value = PlayerOverlyState.BOTTOM
    } else if (!isPlaying && overlyState.value == PlayerOverlyState.BOTTOM) {
      overlyState.value = PlayerOverlyState.HIDE
    } else {
      Timber.tag("PlayerOverly").d("都不符合 isPlay =${isPlaying} overlyState = $overlyState")
    }
  }


//  LaunchedEffect(overlyState.value) {
//    Timber.tag("PlayerOverly").d("overlyState: $overlyState")
//    if (overlyState.value == PlayerOverlyState.BOTTOM) {
//      delay(5000)
//      if (isPlaying) {
//        overlyState.value = PlayerOverlyState.BOTTOM
//      }
//    }
//  }


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
        label = "player_overly_state",
        contentKey = { it.name },
        transitionSpec = {
          fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f)) +
              scaleIn(initialScale = 0.8f, animationSpec = spring(0.73f, 900f)) togetherWith
              materialSharedAxisYOut(forward = true)
        }
      ) { state ->
        when (state) {
//          PlayerOverlyState.MINI -> {
//            Box(
//              modifier = Modifier.fillMaxSize()
//            ) {
//              Box(
//                modifier = Modifier
//                  .size(width = 64.dp, height = 64.dp)
//                  .absoluteOffset(x = (32).dp)
//                  .sharedBounds(
//                    animatedVisibilityScope = this@AnimatedContent,
//                    sharedContentState = sharedContentState,
//                    renderInOverlayDuringTransition = false,
//                    enter = scaleIn(
//                      animationSpec = spring(
//                        dampingRatio = Spring.DampingRatioLowBouncy,
//                        stiffness = Spring.StiffnessLow,
//                      )
//                    ) + fadeIn(),
//                    exit = scaleOut() + fadeOut(),
//                  )
//                  .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
//                  .clip(CircleShape)
//                  .innerShadow(
//                    shape = CircleShape,
//                    Shadow(
//                      radius = 6.dp,
//                      color = MaterialTheme.colorScheme.onSurface,
//                      alpha = .11f
//                    )
//                  )
//                  .clickable {
//                    overlyState.value = PlayerOverlyState.BOTTOM
//                  }
//                  .align(Alignment.CenterEnd)
//              ) {
//                currentSong?.let {
//                  Mimi(
//                    currentSong,
//                  )
//                }
//              }
//            }
//          }

          PlayerOverlyState.HIDE -> {
           Box(
             modifier = Modifier.fillMaxSize()
           ){
             Box(
               modifier = Modifier
                 .fillMaxWidth()
                 .align(alignment = Alignment.BottomCenter)
                 .sharedBounds(
                   animatedVisibilityScope = this@AnimatedContent,
                   sharedContentState = sharedContentState,
                   enter = materialSharedAxisYIn(false),
                   exit = materialSharedAxisYOut(false),
                   resizeMode = ScaleToBounds(ContentScale.FillHeight, Center)
                 )
             )
           }
          }

          PlayerOverlyState.BOTTOM -> {
            Box(
              modifier = Modifier
                .fillMaxSize()
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .sharedBounds(
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedContentState = sharedContentState,
                    enter = fadeIn(animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f)) +
                        scaleIn(initialScale = 0.8f, animationSpec = spring(0.73f, 900f)),
                    exit = slideOutVertically { it },
                    resizeMode = ScaleToBounds(ContentScale.Fit, Center)
                  )
                  .align(Alignment.BottomCenter)
                  .clickable {
                    overlyState.value = PlayerOverlyState.PLAYER
                  }
                  .navigationBarsPadding()
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
                  enter = materialSharedAxisZIn(true),
                  exit = materialSharedAxisZOut(true),
                )
            ) {
              PlayerScreen(
                navigator = navigator,
                onBackClick = {
                  overlyState.value = PlayerOverlyState.BOTTOM
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
private fun Bottom() {
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
  AnimatedContent(
    song,
    transitionSpec = {
      slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
    },
    contentKey = { "${it.songId}" }
  ) { song ->
    CoverWidget(
      modifier = Modifier
        .fillMaxSize()
        .padding(4.dp)
        .clip(CircleShape)
        .rotate(rotateState.value),
      song = song
    )
  }
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
  //  MINI, // 右边悬浮模式
  HIDE, // 隐藏模式
  BOTTOM, // 底栏模式
  PLAYER, // 播放器模式
}