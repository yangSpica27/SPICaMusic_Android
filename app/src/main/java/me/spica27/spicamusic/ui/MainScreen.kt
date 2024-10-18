package me.spica27.spicamusic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.compose.AppTheme
import kotlinx.coroutines.launch
import me.spica27.spicamusic.navigator.AppComposeNavigator


/// 主页
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: AppComposeNavigator? = null
) {

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
        beyondViewportPageCount = 3
      ) { page ->
        when (page) {
          0 -> HomePage(navigator = navigator)
          1 -> CurrentListPage(navigator = navigator)
          2 -> SettingPage()
        }
      }
    }
  }
}


// 底部导航栏
@Composable
fun BottomNav(pagerState: PagerState) {
  val coroutineScope = rememberCoroutineScope()

  val items = listOf("主页", "播放列表", "设置")
  val selectedIcons =
    listOf(Icons.Filled.Home, Icons.AutoMirrored.Filled.List, Icons.Filled.Settings)
  val unselectedIcons =
    listOf(Icons.Outlined.Home, Icons.AutoMirrored.Outlined.List, Icons.Outlined.Settings)

  NavigationBar {
    items.forEachIndexed { index, item ->
      NavigationBarItem(
        icon = {
          Icon(
            if (pagerState.currentPage == index) selectedIcons[index] else unselectedIcons[index],
            contentDescription = item
          )
        },
        label = { Text(item) },
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


@Preview
@Composable
fun MainScreenPreview() {
  AppTheme {
    MainScreen()
  }
}