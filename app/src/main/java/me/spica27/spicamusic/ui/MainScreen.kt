package me.spica27.spicamusic.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.compose.AppTheme
import kotlinx.coroutines.launch
import me.spica27.spicamusic.navigator.AppComposeNavigator


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  navigator: AppComposeNavigator? = null
) {

  val pagerState = rememberPagerState(pageCount = {
    10
  })

  Scaffold() { innerPadding ->

    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {

      HorizontalPager(
        state = pagerState, modifier = Modifier.fillMaxSize(), userScrollEnabled = false,
        key = { it }
      ) { page ->
        when (page) {
          0 -> HomePage()
          1 -> CurrentListPage()
          2 -> SettingPage()
        }
      }
      Box(
        modifier = Modifier.align(Alignment.BottomCenter)
      ) {
        BottomNav(pagerState)
      }
    }

  }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomNav(pagerState: PagerState) {
  val coroutineScope = rememberCoroutineScope()
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(
      modifier = Modifier
        .weight(1f)
        .clickable {
          coroutineScope.launch {
            pagerState.animateScrollToPage(0)
          }
        }
        .padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (pagerState.currentPage == 0) {
        Icon(
          imageVector = Icons.Default.Home, contentDescription = "Home", tint = MaterialTheme.colorScheme.primary
        )
        Text("主页", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
      } else {
        Icon(imageVector = Icons.Default.Home, contentDescription = "Home")
        Text("主页", style = MaterialTheme.typography.bodyMedium.copy())
      }
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .clickable {
          coroutineScope.launch {
            pagerState.animateScrollToPage(1)
          }
        }
        .padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (pagerState.currentPage == 1) {
        Icon(
          imageVector = Icons.Default.Home, contentDescription = "CurrentList", tint = MaterialTheme.colorScheme.primary
        )
        Text("播放列表", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
      } else {
        Icon(imageVector = Icons.Default.Home, contentDescription = "CurrentList")
        Text("播放列表", style = MaterialTheme.typography.bodyMedium.copy())
      }
    }
    Column(
      modifier = Modifier
        .weight(1f)
        .clickable {
          coroutineScope.launch {
            pagerState.animateScrollToPage(2)
          }
        }
        .padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (pagerState.currentPage == 2) {
        Icon(
          imageVector = Icons.Default.Settings, contentDescription = "Setting", tint = MaterialTheme.colorScheme.primary
        )
        Text("主页", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary))
      } else {
        Icon(imageVector = Icons.Default.Settings, contentDescription = "Setting")
        Text("主页", style = MaterialTheme.typography.bodyMedium.copy())
      }
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