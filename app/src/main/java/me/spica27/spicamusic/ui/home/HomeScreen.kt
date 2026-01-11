package me.spica27.spicamusic.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 首页界面
 * 通过 LocalNavController.current 获取导航控制器
 * 通过 LocalPlayerViewModel.current 获取播放器 ViewModel
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    // 如需导航，使用: val navController = LocalNavController.current
    // 播放器控制: Activity 级别的单例
    val playerViewModel = LocalPlayerViewModel.current

    // 首页数据
    val allSongs by homeViewModel.allSongs.collectAsState()
    val playlists by homeViewModel.playlists.collectAsState()

    // 播放器状态 (来自 PlayerViewModel)
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val currentPlaylist by playerViewModel.currentPlaylist.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "柠檬音乐",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "歌曲数量: ${allSongs.size}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "歌单数量: ${playlists.size}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = "播放列表: ${currentPlaylist.size} 首",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = if (isPlaying) "▶ 正在播放" else "⏸ 已暂停",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
