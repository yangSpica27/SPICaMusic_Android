package me.spica27.spicamusic.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.utils.secsToMs
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel


/// 主页
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: NavBackStack? = null,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {


  // 显示是否退出的提示
  var showToast by remember { mutableStateOf(false) }


  // 返回键状态
  var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }

  // 获取当前的context
  val context = LocalContext.current

  if (showToast) {
    Toast.makeText(context, "再次点按返回按键退出", Toast.LENGTH_SHORT).show()
    showToast = false
  }


  LaunchedEffect(key1 = backPressState) {
    if (backPressState == BackPress.InitialTouch) {
      delay(2000)
      backPressState = BackPress.Idle
    }
  }

  BackHandler(backPressState == BackPress.Idle) {
    backPressState = BackPress.InitialTouch
    showToast = true
  }

  BackHandler(backPressState == BackPress.InitialTouch) {
    Runtime.getRuntime().exit(0)
  }

  val pagerState = rememberPagerState(
    pageCount = {
      10
    },
  )
  Scaffold(
    bottomBar = {
      BottomNav(pagerState)
    }
  ) { innerPadding ->
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // 水平滚动的页面
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false,
        key = { it },
        beyondViewportPageCount = 2
      ) { page ->
        when (page) {
          0 -> HomePage(navigator = navigator)
          1 -> SettingPage()
        }
      }
      AnimatedVisibility(
        visible = playBackViewModel.currentSongFlow.collectAsState().value != null,
        modifier = Modifier
          .align(alignment = Alignment.BottomCenter)
          .fillMaxWidth(),
        enter = slideInVertically(
          initialOffsetY = { it },
          animationSpec = tween(450)
        ) + fadeIn(),
        exit = slideOutVertically(
          targetOffsetY = { it },
          animationSpec = tween(450)
        ) + fadeOut()
      ) {
        PlayerBar(
          modifier = Modifier
            .fillMaxWidth()
            .noRippleClickable {
              navigator?.add(Routes.Player)
            }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerBar(
  modifier: Modifier = Modifier,
  songViewModel: SongViewModel = hiltViewModel(),
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
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .background(
        MaterialTheme.colorScheme.tertiaryContainer,
        shape = MaterialTheme.shapes.medium
      )
      .padding(horizontal = 16.dp, vertical = 16.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
    }
  }
}


// 底部导航栏
@Composable
private fun BottomNav(pagerState: PagerState) {
  val coroutineScope = rememberCoroutineScope()

  val items = listOf("主页", "设置")
  val selectedIcons =
    listOf(Icons.Filled.Home, Icons.Filled.Settings)
  val unselectedIcons =
    listOf(Icons.Outlined.Home, Icons.Outlined.Settings)

  NavigationBar {
    items.forEachIndexed { index, item ->
      NavigationBarItem(
        icon = {
          Icon(
            if (pagerState.currentPage == index) selectedIcons[index] else unselectedIcons[index],
            contentDescription = item
          )
        },
//        label = { Text(item) },
        selected = pagerState.currentPage == index,
        onClick = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(index)
          }
        }
      )
    }
  }
}

// 返回键状态
private sealed class BackPress {
  object Idle : BackPress()
  object InitialTouch : BackPress()
}

@Preview
@Composable
fun MainScreenPreview() {
  AppTheme {
    MainScreen()
  }
}