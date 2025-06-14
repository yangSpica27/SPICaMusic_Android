package me.spica27.spicamusic.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlayerBar


/// 主页
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: NavBackStack? = null,
  playBackViewModel: PlayBackViewModel = hiltViewModel()
) {


  // 显示是否退出的提示
  var showToast by remember { mutableStateOf(false) }

  // 是否展示播放器页面
  var showPlayerState by rememberSaveable { mutableStateOf(false) }

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

  SharedTransitionLayout {
    AnimatedContent(
      showPlayerState,
      label = "main_screen_player_transition"
    ) { showPlayer ->
      if (showPlayer) {
        PlayerScreen(
          animatedVisibilityScope = this@AnimatedContent,
          sharedTransitionScope = this@SharedTransitionLayout,
          onBackClick = {
            showPlayerState = false
          }
        )
      } else {
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
                1 -> SettingPage(navigator = navigator)
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
                  .sharedBounds(
                    rememberSharedContentState(key = "player_bound"),
                    animatedVisibilityScope = this@AnimatedContent,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds()
                  )
                  .noRippleClickable {
                    showPlayerState = true
                  }
              )
            }
          }
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