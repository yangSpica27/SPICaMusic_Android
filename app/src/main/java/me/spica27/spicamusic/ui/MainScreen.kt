package me.spica27.spicamusic.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.clickableNoRippleClickableWithVibration
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlayerBar


/// 主页
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: NavBackStack? = null,
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  sharedTransitionScope: SharedTransitionScope,
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
        PlayerScreen(
          navigator= navigator,
          animatedVisibilityScope = this@AnimatedContent,
          sharedTransitionScope = sharedTransitionScope,
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
              with(sharedTransitionScope){
                PlayerBar(
                  modifier = Modifier
                    .fillMaxWidth()
                    .sharedBounds(
                      rememberSharedContentState(key = "player_bound"),
                      animatedVisibilityScope = this@AnimatedContent,
                      enter = scaleIn() + fadeIn(),
                      exit = scaleOut() + fadeOut(),
                      resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
                      placeHolderSize = SharedTransitionScope.PlaceHolderSize.animatedSize
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

  val currentIndex = remember { mutableIntStateOf(pagerState.currentPage) }

  val indicationIndex = animateFloatAsState(
    currentIndex.intValue * 1f,
    tween(500),
    label = ""
  )


  val indicationPadding = remember { mutableFloatStateOf(1f) }

  val indicationPaddingAnim =
    animateFloatAsState(indicationPadding.floatValue,
      spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow,
      ), label = "")

  val pauseColor = MaterialTheme.colorScheme.surfaceContainerHigh

  val playColor = MaterialTheme.colorScheme.surfaceContainerHighest

  val indicationColor = remember {
    mutableStateOf(pauseColor)
  }

  val indicationColorAnim = animateColorAsState(
    indicationColor.value,
    tween(550),
    label = ""
  )

  val isFirst = remember { mutableStateOf(true) }

  val isNight = DataStoreUtil().getForceDarkTheme.collectAsState(false)

  LaunchedEffect(currentIndex.intValue) {
    if (isFirst.value) {
      isFirst.value = false
      return@LaunchedEffect
    }
    indicationColor.value = playColor
    indicationPadding.floatValue = 0.5f
    delay(300)
    indicationColor.value = pauseColor
    indicationPadding.floatValue = 1f
  }

  LaunchedEffect(isNight.value) {
    indicationColor.value = pauseColor
  }


  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(80.dp)
      .background(MaterialTheme.colorScheme.surfaceContainerLow)
  ) {

    Row(
      modifier = Modifier
        .fillMaxSize()
        .align(Alignment.Center)
        .drawBehind {
          val itemWidth = size.width / items.size

          val centerX = itemWidth * indicationIndex.value + itemWidth / 2

          val paddingWidth = itemWidth / 3 * indicationPaddingAnim.value

          val paddingHeight = size.height / 5 * indicationPaddingAnim.value

          drawRoundRect(
            color = indicationColorAnim.value,
            topLeft = Offset(
              x = centerX - itemWidth / 2 + paddingWidth,
              y = paddingHeight
            ),
            size = Size(
              width = itemWidth - paddingWidth * 2,
              height = size.height - paddingHeight * 2
            ),
            cornerRadius = CornerRadius(40.dp.value)
          )

        },
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {

      selectedIcons.forEachIndexed { index, _ ->
        val isSelected = currentIndex.intValue == index
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clickableNoRippleClickableWithVibration {
              coroutineScope.launch {
                pagerState.animateScrollToPage(index)
              }
              currentIndex.intValue = index
            },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (
              isSelected
            ) {
              selectedIcons[index]
            } else {
              unselectedIcons[index]
            },
            contentDescription = items[index],
            tint = if (isSelected) {
              MaterialTheme.colorScheme.inversePrimary
            } else {
              MaterialTheme.colorScheme.onBackground
            }
          )
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

