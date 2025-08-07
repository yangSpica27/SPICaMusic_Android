package me.spica27.spicamusic.ui.main

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.ui.main.home.HomePage
import me.spica27.spicamusic.ui.main.player.PlayerScreen
import me.spica27.spicamusic.ui.setting.SettingPage
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.FloatingTabBar
import me.spica27.spicamusic.widget.FloatingTabBarScrollConnection
import me.spica27.spicamusic.widget.MiniPlayBar
import me.spica27.spicamusic.widget.PlayerBar
import me.spica27.spicamusic.widget.rememberFloatingTabBarScrollConnection
import me.spica27.spicamusic.wrapper.activityViewModel
import org.koin.compose.koinInject


/// 主页
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: NavBackStack? = null,
  playBackViewModel: PlayBackViewModel = activityViewModel(),
  sharedTransitionScope: SharedTransitionScope,
  dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>()
) {


  // 显示是否退出的提示
  var showToast by remember { mutableStateOf(false) }

  // 是否展示播放器页面
  var showPlayerState by rememberSaveable { mutableStateOf(false) }

  // 返回键状态
  var backPressState by remember { mutableStateOf<BackPress>(BackPress.Idle) }

  // 获取当前的context
  val context = LocalContext.current


  LaunchedEffect(showToast) {
    if (showToast) {
      Toast.makeText(context, "再按一次退出", Toast.LENGTH_SHORT).show()
      showToast = false
    }
  }


  val homeScrollState = rememberScrollState()

  val scrollConnection = rememberFloatingTabBarScrollConnection()

  val nowPlayingSong = playBackViewModel.currentSongFlow.collectAsState().value

  val isPlaying = playBackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

  LaunchedEffect(key1 = backPressState) {
    if (backPressState == BackPress.InitialTouch) {
      delay(2000)
      backPressState = BackPress.Idle
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      backPressState = BackPress.Idle
    }
  }

  BackHandler((backPressState == BackPress.Idle) || (showPlayerState == true)) {
    if (showPlayerState) {
      showPlayerState = false
      return@BackHandler
    }
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

  val agreePrivacy = dataStoreUtil.getAgreePrivacy().collectAsStateWithLifecycle(null).value

  if (agreePrivacy == false) {
    navigator?.add(Routes.AgreePrivacy)
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
  ) {
    AnimatedContent(
      showPlayerState,
      label = "main_screen_player_transition"
    ) { showPlayer ->
      if (showPlayer) {
        /// 播放器页面
        PlayerScreen(
          navigator = navigator,
          animatedVisibilityScope = this@AnimatedContent,
          sharedTransitionScope = sharedTransitionScope,
          onBackClick = {
            showPlayerState = false
          }
        )
      } else {
        // 主页
        Scaffold { innerPadding ->
          Box(
            modifier = modifier
              .fillMaxSize()
              .padding(
                innerPadding
              )
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
                0 -> HomePage(
                  navigator = navigator,
                  listState = homeScrollState,
                  connection = scrollConnection
                )

                1 -> SettingPage(navigator = navigator)
              }
            }
            // 悬浮底栏
            FloatBottomBar(
              navigator = navigator,
              scrollConnection = scrollConnection,
              sharedTransitionScope = sharedTransitionScope,
              nowPlayingSong = nowPlayingSong,
              isPlaying = isPlaying,
              animatedVisibilityScope = this@AnimatedContent,
              pagerState = pagerState,
              modifier = Modifier
                .align(alignment = Alignment.BottomCenter)
                .padding(bottom = 22.dp)
            ) {
              showPlayerState = true
            }
          }
        }
      }
    }
  }


}


// 返回键状态
private sealed class BackPress {
  object Idle : BackPress()
  object InitialTouch : BackPress()
}


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun FloatBottomBar(
  navigator: NavBackStack?,
  modifier: Modifier = Modifier,
  scrollConnection: FloatingTabBarScrollConnection,
  playBackViewModel: PlayBackViewModel = activityViewModel(),
  sharedTransitionScope: SharedTransitionScope,
  nowPlayingSong: Song?,
  isPlaying: Boolean,
  animatedVisibilityScope: AnimatedVisibilityScope,
  pagerState: PagerState,
  showPlayer: () -> Unit
) {

  val coroutineScope = rememberCoroutineScope()

  FloatingTabBar(
    modifier = Modifier
      .padding(horizontal = 16.dp)
      .then(modifier),
    scrollConnection = scrollConnection,
    inlineAccessory = { modifier, scope ->
      with(sharedTransitionScope) {
        Box(
          modifier = modifier
            .fillMaxWidth()
            .background(
              MaterialTheme.colorScheme.surfaceContainerLow,
              CircleShape
            )
            .sharedBounds(
              rememberSharedContentState(key = "player_bound"),
              animatedVisibilityScope = animatedVisibilityScope,
              enter = fadeIn(),
              exit = fadeOut(),
              placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
            )
            .innerShadow(
              shape = CircleShape,
              Shadow(
                radius = 6.dp,
                color = MaterialTheme.colorScheme.onSurface,
                alpha = .11f
              )
            )
        ) {
          MiniPlayBar(
            modifier = modifier
              .clickable {
                if (nowPlayingSong != null) {
                  showPlayer.invoke()
                }
              },
            song = playBackViewModel.currentSongFlow.collectAsState().value,
            isPlaying = isPlaying,
            togglePlayState = {
              playBackViewModel.togglePlaying()
            }
          )
        }
      }
    },
    expandedAccessory = { modifier, scope ->
      AnimatedVisibility(
        visible = nowPlayingSong != null,
        modifier = Modifier
          .fillMaxWidth(),
        enter = slideInVertically(
          initialOffsetY = { it },
          animationSpec = tween(250)
        ) + fadeIn(),
        exit = slideOutVertically(
          targetOffsetY = { it },
          animationSpec = tween(250)
        ) + fadeOut()
      ) {
        with(sharedTransitionScope) {
          Box(
            modifier = modifier
              .fillMaxWidth()
              .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                CircleShape
              )
              .sharedBounds(
                rememberSharedContentState(key = "player_bound"),
                animatedVisibilityScope = animatedVisibilityScope,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
              )
              .innerShadow(
                shape = CircleShape,
                Shadow(
                  radius = 6.dp,
                  color = MaterialTheme.colorScheme.onSurface,
                  alpha = .11f
                )
              )
          ) {
            PlayerBar(
              modifier = Modifier
                .fillMaxWidth()
                .noRippleClickable {
                  showPlayer.invoke()
                }
            )
          }
        }
      }
    },
    content = {
      tab(
        key = "home",
        title = {
          Text("主页")
        },
        icon = {
          BottomNavIcon(
            imageVector = Icons.Default.Home,
          )
        },
        onClick = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(0)
          }
        },
      )
      tab(
        key = "设置",
        title = {
          Text("设置")
        },
        icon = {
          BottomNavIcon(
            imageVector = Icons.Default.Settings,
          )
        },
        onClick = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(1)
          }
        },
      )
      standaloneTab(
        key = "Search",
        icon = {
          BottomNavIcon(
            imageVector = Icons.Default.Search,
          )
        },
        onClick = {
          navigator?.add(Routes.SearchAll)
        },
      )
    },
    selectedTabKey = {}
  )
}


@Composable
private fun BottomNavIcon(
  imageVector: ImageVector,
) {
  Box(
    modifier = Modifier
      .width(48.dp)
      .height(48.dp)
      .clip(CircleShape)
      .padding(12.dp),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      modifier = Modifier.fillMaxSize(),
      imageVector = imageVector,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    )
  }
}
