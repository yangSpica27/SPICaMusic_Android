package me.spica27.spicamusic.ui

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.PlayBackViewModel


@kotlin.OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onBackClick: () -> Unit
) {

  val nowPlayingSongs = playBackViewModel.playList.collectAsState().value

  val isPlaying = playBackViewModel.isPlaying.collectAsState(false).value

  val pageState = rememberPagerState(pageCount = { 2 })

  with(sharedTransitionScope) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .sharedBounds(
          rememberSharedContentState(key = "player_bound"),
          animatedVisibilityScope = animatedVisibilityScope,
          enter = fadeIn() + scaleIn(),
          exit = fadeOut() + scaleOut(),
          resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
        ),
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = {
                onBackClick.invoke()
              }
            ) {
              Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
            }
          },
          title = {
            Text(
              if (isPlaying) {
                "Now Playing"
              } else {
                "Now Pause"
              },
              style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.ExtraBold
              ),
            )
          }
        )
      },
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(it)
      ) {
        if (nowPlayingSongs.isEmpty()) {
          // 播放列表为空
          EmptyPage()
        } else {
          // 播放列表不为空
          VerticalPager(
            modifier = Modifier.fillMaxSize(),
            state = pageState,
            key = { it },
            flingBehavior =
              PagerDefaults.flingBehavior(state = pageState, snapPositionalThreshold = .2f)
          ) {
            when (it) {
              0 -> {
                PlayerPage()
              }

              1 -> {
                CurrentListPage()
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyPage() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
  ) {
    Text("没有播放中的音乐", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedButton(
      onClick = { }
    ) {
      Text("选取音乐")
    }
  }
}

@Composable
private fun Title(
  modifier: Modifier = Modifier, playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val indexState = playBackViewModel.playlistCurrentIndex.collectAsStateWithLifecycle()

  val playlistSizeState = playBackViewModel.nowPlayingListSize.collectAsStateWithLifecycle()

  Row(
    modifier = modifier,
  ) {
    Text(
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small
        )
        .padding(vertical = 4.dp, horizontal = 8.dp),
      text = "循环播放",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
    )
    Spacer(modifier = Modifier.width(10.dp))
    Text(
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small
        )
        .padding(vertical = 4.dp, horizontal = 8.dp),
      text = "第 ${indexState.value + 1} / ${playlistSizeState.value} 首",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
    )
  }
}

/// 封面
@OptIn(UnstableApi::class)
@Composable
private fun Cover(
  modifier: Modifier = Modifier,
  songState: State<Song?>,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {

  val context = LocalContext.current

  val coverPainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(context).data(songState.value?.getCoverUri()).transformations(
      CircleCropTransformation()
    ).build(),
  )


  val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()
  val backgroundColor = MaterialTheme.colorScheme.surface
  val onSurfaceColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

  val infiniteTransition = rememberInfiniteTransition(label = "infinite")
  val rotateState = infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
      animation = tween(10000, easing = LinearEasing),
      repeatMode = androidx.compose.animation.core.RepeatMode.Restart
    ), label = ""
  )

  Box(
    contentAlignment = Alignment.Center, modifier = modifier
  ) {


//    AndroidView(
//      factory = { context ->
//        VisualizerSurfaceView(context).apply {
//          setBgColor(backgroundColor.toArgb())
//          setThemeColor(onSurfaceColor.toArgb())
//        }
//      }, update = { view ->
//        view.setBgColor(backgroundColor.toArgb())
//        view.setThemeColor(onSurfaceColor.toArgb())
//      }, modifier = Modifier
//        .fillMaxWidth()
//        .aspectRatio(1f)
//    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(60.dp + 12.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
        .clip(CircleShape)
        .border(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), CircleShape)
        .rotate(rotateState.value), contentAlignment = Alignment.Center
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
          text = songState.value?.displayName ?: "Unknown",
          style = MaterialTheme.typography.headlineLarge.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            fontWeight = FontWeight.W900
          )
        )
      }

    }

  }
}

