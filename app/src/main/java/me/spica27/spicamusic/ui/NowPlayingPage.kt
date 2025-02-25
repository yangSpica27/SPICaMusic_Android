package me.spica27.spicamusic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.viewModel.PlayBackViewModel


/// 正在播放 页面
@Composable
fun NowPlayingPage(
  navigator: AppComposeNavigator? = null,
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
) {

  val nowPlayingSongs = playBackViewModel.playList.collectAsState().value

  val pageState = rememberPagerState(pageCount = { 2 })

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
          CurrentListPage(
            navigator = navigator
          )
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