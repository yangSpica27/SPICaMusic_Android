package me.spica27.spicamusic.ui

import android.os.ParcelFileDescriptor
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.TimeUtils.prettyTime
import me.spica27.spicamusic.utils.contentResolverSafe
import me.spica27.spicamusic.utils.formatDurationDs
import me.spica27.spicamusic.utils.formatDurationSecs
import me.spica27.spicamusic.utils.msToDs
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.utils.rememberVibrator
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.utils.tick
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.visualiser.MusicVisualiser
import me.spica27.spicamusic.widget.LyricSettingDialog
import me.spica27.spicamusic.widget.LyricsView
import me.spica27.spicamusic.widget.SongControllerPanel
import me.spica27.spicamusic.widget.VisualizerView
import me.spica27.spicamusic.widget.audio_seekbar.AudioWaveSlider
import me.spica27.spicamusic.wrapper.Taglib
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber
import java.util.*


@Composable
fun PlayerPage(
  playBackViewModel: PlayBackViewModel = activityViewModel(),
  songViewModel: SongViewModel = activityViewModel(),
  navigator: NavBackStack? = null,
) {

  // 当前播放的歌曲
  val showEmpty =
    playBackViewModel.currentSongFlow.map { it == null }.collectAsStateWithLifecycle(true).value

  val currentLyric = playBackViewModel.currentLyric.collectAsStateWithLifecycle().value

  val currentTime = playBackViewModel.positionSec.collectAsStateWithLifecycle().value

  val coroutineScope = rememberCoroutineScope()

  val horizontalPagerState = rememberPagerState { 3 }

  val vibrator = rememberVibrator()

  var isFirst by remember { mutableStateOf(true) }

  LaunchedEffect(horizontalPagerState.currentPage) {
    if (isFirst) {
      isFirst = false
      return@LaunchedEffect
    }
    vibrator.tick()
  }


  if (showEmpty) {
    return Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      CircularProgressIndicator()
    }
  } else {

    val currentPlayingSong = playBackViewModel.currentSongFlow.collectAsStateWithLifecycle().value

    Box(
      modifier = Modifier.fillMaxSize(),
    ) {

      Column(
        modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
      ) {

        // tab
        TabBar(
          selectedTabIndex = horizontalPagerState.currentPage,
          onTabSelected = {
            coroutineScope.launch {
              horizontalPagerState.animateScrollToPage(it)
            }
          },
          tabs = listOf("封面", "歌词", "信息"),
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 封面
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true,
            beyondViewportPageCount = 1,
            key = {
              it
            }
          ) { index ->

            Box(
              modifier = Modifier
                .fillMaxSize()
                .padding(
                  horizontal = 16.dp,
                )
                .background(
                  MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small
                )
                .innerShadow(
                  shape = MaterialTheme.shapes.medium, shadow = Shadow(
                    radius = 10.dp, color = MaterialTheme.colorScheme.onSurface, alpha = .11f
                  )
                )
            ) {
              when (index) {
                0 -> {
                  Cover(
                    modifier = Modifier.fillMaxSize(),
                    songState = songViewModel.getSongFlow(currentPlayingSong?.songId ?: -1)
                      .collectAsState(null),
                    playBackViewModel = playBackViewModel
                  )
                }

                1 -> {
                  if (currentLyric.isNotEmpty()) {
                    LyricsView(
                      modifier = Modifier.fillMaxSize(),
                      currentLyric = currentLyric,
                      currentTime = currentTime * 1000
                    )
                  } else {
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .clip(
                          MaterialTheme.shapes.medium
                        )
                        .clickable {
                          currentPlayingSong?.let {
                            navigator?.add(
                              Routes.LyricsSearch(
                                currentPlayingSong
                              )
                            )
                          }
                        },
                      contentAlignment = Alignment.Center
                    ) {
                      Text("暂无歌词,点击搜索")
                    }
                  }
                }

                2 -> {
                  SongInfoCard(
                    song = currentPlayingSong
                  )
                }
              }
            }
          }
        }

        // 歌名和歌手
        SongInfo(
          songId = currentPlayingSong?.songId ?: -1,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
          navigator = navigator
        )
        ControlPanel(
          modifier = Modifier.padding(vertical = 15.dp, horizontal = 20.dp),
          playBackViewModel = playBackViewModel
        )
        Text(
          modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth(),
          textAlign = TextAlign.Center,
          text = "向上滑动查看播放列表",
          style = MaterialTheme.typography.bodyMedium.copy(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        )
      }
    }
  }
}


@Composable
private fun Title(
  modifier: Modifier = Modifier, playBackViewModel: PlayBackViewModel = activityViewModel(),
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
  playBackViewModel: PlayBackViewModel =activityViewModel(),
) {

  val context = LocalContext.current

  val coverPainter = rememberAsyncImagePainter(
    model = ImageRequest.Builder(context).data(songState.value?.getCoverUri()).transformations(
      CircleCropTransformation()
    ).build(),
  )

  val lineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

  val shadowLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

  val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()
  val infiniteTransition = rememberInfiniteTransition(label = "infinite")
  val rotateState = infiniteTransition.animateFloat(
    initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(
      animation = tween(10000, easing = LinearEasing), repeatMode = RepeatMode.Restart
    ), label = ""
  )


  val pn = remember {
    mutableStateListOf<Float>()
  }

  val grayLineYs = remember { FloatArray(MusicVisualiser.FREQUENCY_BAND_LIMITS.size * 2) }

  val blackLineData = remember(pn) {
    pn
  }.map {
    animateFloatAsState(
      it, label = "black_line", animationSpec = tween(
        durationMillis = 135, easing = EaseOut
      )
    )
  }


//  DisposableEffect(Unit) {
//    (0 until MusicVisualiser.FREQUENCY_BAND_LIMITS.size).forEach { i ->
//      pn.add(0f)
//      pn.add(0f)
//    }
//    val musicVisualiser = MusicVisualiser()
//    musicVisualiser.setListener(object : MusicVisualiser.Listener {
//      override fun getDrawData(list: List<Float>) {
//        for ((index, f) in list.withIndex()) {
//          pn[index] = f
//        }
//      }
//    })
//    musicVisualiser.ready()
//    onDispose {
//      musicVisualiser.dispose()
//    }
//  }


  AndroidView(
    factory = { context ->
      VisualizerView(context)
    }, update = { view ->
      view.setThemeColor(lineColor.toArgb())
    }, modifier = Modifier
      .fillMaxWidth()
  )


  //  Compose 版本的频谱动效开销多占 25%的性能 暂时屏蔽
//  Spacer(
//    modifier = Modifier
//      .fillMaxSize()
//      .drawWithCache {
//
//
//        val centerX = size.width / 2
//
//        val startY = size.width - 30.dp.toPx() - 12.dp.toPx()
//
//        val changeY = 40.dp.toPx()
//
//        onDrawWithContent {
//          for ((index, state) in blackLineData.withIndex()) {
//            this.rotate(
//              index * 360f / pn.size,
//            ) {
//
//              val blackLineY = startY + changeY * state.value
//
//              val shadowY = Math.max(blackLineY, grayLineYs[index])
//
//              drawLine(
//                color = shadowLineColor,
//                start = Offset(
//                  centerX, startY
//                ),
//                end = Offset(
//                  centerX,
//                  shadowY
//                ),
//                strokeWidth = 8.dp.toPx(),
//                cap = StrokeCap.Round,
//              )
//
//              grayLineYs[index] = shadowY - 1.dp.toPx() / 10f
//
//              drawLine(
//                color = lineColor,
//                start = Offset(centerX, startY),
//                end = Offset(
//                  centerX,
//                  blackLineY
//                ),
//                strokeWidth = 8.dp.toPx(),
//                cap = StrokeCap.Round,
//              )
//            }
//          }
//        }
//
//      }
//  )
  Box(
    modifier = Modifier
      .fillMaxHeight()
      .aspectRatio(1f)
      .padding(60.dp + 12.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
      .clip(CircleShape)
      .border(
        12.dp, MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f), CircleShape
      )
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


/// 控制面板
@Composable
private fun ControlPanel(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = activityViewModel(),
) {

  val song = playBackViewModel.currentSongFlow.collectAsStateWithLifecycle().value
  // 快速傅里叶变换后的振幅
  val ampState = remember { mutableStateOf(listOf<Int>()) }

  val isPlaying = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  val songState = playBackViewModel.currentSongFlow.collectAsStateWithLifecycle().value

  val positionSec = playBackViewModel.positionSec.collectAsStateWithLifecycle().value

  var isSeekingState by remember { mutableStateOf(false) }

  var seekValueState by remember { mutableFloatStateOf(0f) }

  LaunchedEffect(positionSec) {
    if (isSeekingState) return@LaunchedEffect
    seekValueState = positionSec.secsToMs() * 1f
  }

  LaunchedEffect(song) {
    withContext(Dispatchers.IO + CoroutineExceptionHandler { _, _ ->

    }) {
      val amplituda = playBackViewModel.getAmplituda()
      if (song?.getSongUri() != null) {
        val inputStream = App.getInstance().contentResolverSafe.openInputStream(song.getSongUri())
        inputStream.use { inputStream ->
          if (inputStream != null) {
            val amplitudes = amplituda.processAudio(inputStream)
            val amplitudesList = amplitudes.get().amplitudesAsList()
            ampState.value = amplitudesList
          } else {
            ampState.value = arrayListOf()
          }
        }
      }
    }
  }

  Column(
    modifier = modifier,
  ) {
    // 振幅 进度条
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(80.dp), contentAlignment = Alignment.Center
    ) {
      AudioWaveSlider(
        amplitudes = ampState.value,
        waveformBrush = SolidColor(MaterialTheme.colorScheme.surfaceVariant),
        progressBrush = SolidColor(MaterialTheme.colorScheme.onSurfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        progress = (seekValueState / (songState?.duration ?: 1)).coerceIn(0f, 1f),
        onProgressChangeFinished = {
          playBackViewModel.seekTo(seekValueState.toLong())
          isSeekingState = false
        },
        onProgressChange = {
          Timber.d("Seeking to $it")
          seekValueState = it * (songState?.duration ?: 1).toFloat()
          isSeekingState = true
        })
    }

    Row(
      modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
    ) {
      // Current Time
      Text(
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
        text = positionSec.formatDurationSecs(),
        style = MaterialTheme.typography.bodyMedium
      )

      Spacer(modifier = Modifier.width(8.dp))

      // 滑动到的地方
      AnimatedVisibility(
        visible = isSeekingState,
      ) {
        Text(
          modifier = Modifier
            .background(
              MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small
            )
            .padding(vertical = 4.dp, horizontal = 8.dp),
          text = seekValueState.toLong().msToSecs().formatDurationSecs(),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Total Time
      Text(
        text = songState?.duration?.msToDs()?.formatDurationDs() ?: "0:00",
        style = MaterialTheme.typography.bodyMedium
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
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
private fun SongInfo(
  songId: Long,
  modifier: Modifier = Modifier,
  songViewModel: SongViewModel = activityViewModel(),
  navigator: NavBackStack? = null,
) {

  val song = songViewModel.getSongFlow(songId).collectAsStateWithLifecycle(null).value

  var showMenuState by remember { mutableStateOf(false) }


  var showLyricsSetting by remember { mutableStateOf(false) }

  if (showLyricsSetting) {
    LyricSettingDialog(
      onDismissRequest = {
        showLyricsSetting = false
      }
    )
  }


  if (song == null) {

    Box(modifier = modifier)
    return
  }

  if (showMenuState) {
    Dialog(
      onDismissRequest = {
        showMenuState = false
      }) {
      SongControllerPanel(
        songId = songId,
        onDismiss = {
          showMenuState = false
        },
        navigator = navigator,
        songViewModel = songViewModel
      )
    }
  }

  Row(
    modifier = modifier
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
          fontWeight = FontWeight.ExtraBold
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
      onClick = {
        songViewModel.toggleFavorite(songId)
      },
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
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
      }
    }
    Spacer(Modifier.width(10.dp))
    IconButton(
      onClick = {
        showLyricsSetting = true
      }) {
      Icon(
        modifier = Modifier.size(24.dp),
        painter = painterResource(R.drawable.ic_lyrics_line),
        contentDescription = "lyrics",
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
      )
    }
    Spacer(Modifier.width(10.dp))
    IconButton(
      onClick = {
        showMenuState = false
      },
    ) {
      Icon(
        Icons.Default.MoreVert,
        contentDescription = "More",
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
      )
    }
  }
}


@Composable
private fun TabBar(
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  tabs: List<String>,
) {

  Box(
    modifier = Modifier
      .height(48.dp)
      .padding(horizontal = 16.dp)
  ) {

    TabRow(
      selectedTabIndex = selectedTabIndex,
      containerColor = Color.Transparent,
      contentColor = Color.Transparent,
      divider = {},
      indicator = { tabPositions ->
        SecondaryIndicator(
          modifier =
            Modifier
              .tabIndicatorOffset(tabPositions[selectedTabIndex])
              .clip(
                MaterialTheme.shapes.small
              ),
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = .05f),
          height = 48.dp
        )
      }) {
      tabs.forEachIndexed { index, tabTitle ->
        Tab(
          interactionSource = object : MutableInteractionSource {
            override val interactions: Flow<Interaction> = emptyFlow()

            override suspend fun emit(interaction: Interaction) {}

            override fun tryEmit(interaction: Interaction) = true
          },
          modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
          selected = selectedTabIndex == index,
          selectedContentColor = Color.Transparent,
          unselectedContentColor = Color.Transparent,
          onClick = { onTabSelected(index) }) {
          val isSelected = index == selectedTabIndex
          Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxSize(),
            text = tabTitle,
            style = MaterialTheme.typography.bodyMedium.copy(
              color = if (isSelected) {
                MaterialTheme.colorScheme.onBackground.copy(0.9f)
              } else {
                MaterialTheme.colorScheme.onBackground.copy(0.5f)
              }, fontSize = 15.sp, fontWeight = if (isSelected) {
                FontWeight.W600
              } else {
                FontWeight.W500
              }
            ),
          )
        }
      }
    }
  }
}

@Composable
fun SongInfoCard(modifier: Modifier = Modifier, song: Song?) {

  if (song == null) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
      Text("暂无播放中的歌曲")
    }
  } else {


    LaunchedEffect(song) {


      launch(Dispatchers.IO) {
        val fd: ParcelFileDescriptor? =
          App.getInstance().contentResolverSafe.openFileDescriptor(song.getSongUri(), "r")
        fd?.use { fd ->
          val metadata = Taglib.retrieveMetadataWithFD(fd.detachFd())
          Timber.tag("歌曲信息").d("metadata: $metadata")
        }
      }

    }


    val lastPlayTimeText: State<String> = remember(song.lastPlayTime) {
      derivedStateOf {
        if (song.lastPlayTime == 0L) return@derivedStateOf "现在"
        return@derivedStateOf prettyTime.format(Date(song.lastPlayTime * 1L))
      }
    }

    Column(
      modifier = modifier.padding(
        horizontal = 16.dp, vertical = 12.dp
      )
    ) {
      Row(
        modifier = Modifier.fillMaxWidth()
      ) {

        Column(
          modifier = Modifier
            .weight(1f)
            .background(
              MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small
            )
            .padding(
              horizontal = 16.dp, vertical = 12.dp
            )
        ) {
          Text(
            "上次播放的时间", style = MaterialTheme.typography.titleMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.height(5.dp))
          Text(
            lastPlayTimeText.value, style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
              fontWeight = FontWeight.W700,
              fontSize = 20.sp
            )
          )
          Spacer(modifier = Modifier.height(5.dp))
          Text("夏天的午后", style = MaterialTheme.typography.bodyLarge)
          Spacer(modifier = Modifier.height(5.dp))
        }
        Spacer(
          modifier = Modifier.width(12.dp)
        )
        Column(
          modifier = Modifier
            .weight(1f)
            .background(
              MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small
            )
            .padding(
              horizontal = 16.dp, vertical = 12.dp
            )
        ) {
          Text(
            "播放次数", style = MaterialTheme.typography.titleMedium.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.height(5.dp))
          Text(
            "${song.playTimes}次", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
              fontWeight = FontWeight.W700,
              fontSize = 20.sp
            )
          )
          Spacer(modifier = Modifier.height(5.dp))
          Text("经常喜欢听", style = MaterialTheme.typography.bodyLarge)
          Spacer(modifier = Modifier.height(5.dp))
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .background(
            MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small
          )
          .padding(
            horizontal = 16.dp, vertical = 12.dp
          )
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
        ) {
          Text(
            "歌名", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            song.displayName, style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
        ) {
          Text(
            "时长", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            song.duration.msToSecs().formatDurationSecs(),
            style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
        ) {
          Text(
            "声道", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            "2", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top
        ) {
          Text(
            "格式", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            song.mimeType, style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface
            )
          )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .border(
              1.dp,
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
              MaterialTheme.shapes.small
            ), contentAlignment = Alignment.Center
        ) {
          Text(
            "夜雪初霁，荠麦弥望。", style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
              fontWeight = FontWeight.W700,
              fontSize = 17.sp
            )
          )
        }
      }
    }
  }
}


