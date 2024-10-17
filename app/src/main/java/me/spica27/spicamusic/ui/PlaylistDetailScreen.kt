package me.spica27.spicamusic.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.viewModel.PlaylistViewModel

/// 歌单详情页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {


  Scaffold(topBar = {
    val playlist = playlistViewModel.playlistFlow.collectAsState(null)
    // 顶部栏
    TopAppBar(title = {
      // 标题
      playlist.value?.let { Text(it.playlistName) }
    }, actions = {
      // 右侧操作按钮

      // 删除歌单
      IconButton(
        onClick = {
          // 删除歌单
          playlistViewModel.deletePlaylist()
          navigator?.navigateUp()
        }
      ) {
        Icon(
          Icons.Default.Delete,
          contentDescription = "删除"
        )
      }

      // 重命名
      IconButton(
        onClick = {
          // 重命名歌单

        }
      ) {
        Icon(
          Icons.Default.Edit,
          contentDescription = "重命名"
        )
      }


      // 新增歌曲
      IconButton(onClick = {
        navigator?.navigate(
          AppScreens.AddSongScreen.createRoute(
            playlistViewModel.playlistId ?: -1
          )
        )
      }) {
        Icon(
          Icons.Outlined.AddCircle,
          contentDescription = "新增"
        )
      }
    })
  }, content = { paddingValues ->

    Box(modifier = Modifier.padding(paddingValues)) {
      // 歌单详情
      Column(
        modifier = Modifier.fillMaxSize()
      ) { // 歌单详情
        Toolbar(
          playlistViewModel = playlistViewModel,
          modifier = Modifier.fillMaxWidth()
        )
        // 歌单列表
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          PlaylistDetailList(
            navigator = navigator,
          )
        }

      }
    }
  })
}

@Composable
private fun PlaylistDetailList(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {


  val playlist =
    playlistViewModel.songInfoWithSongsFlow.map { it?.songs }.collectAsState(emptyList())

  val isSelectMode = playlistViewModel.isSelectMode.collectAsState(false)

  val selectedModePlaylist = combine(
    playlistViewModel.songsFlow,
    playlistViewModel.selectedSongs
  ) { songs, selectedSongs ->
    songs.map {
      Pair(it, selectedSongs.contains(it.songId))
    }
  }.collectAsState(emptyList())


  if (playlist.value == null) {
    // 加载中
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
  } else
    if (playlist.value?.isEmpty() == true) {
      // 空内容
      EmptyContent(
        modifier = Modifier.fillMaxSize(),
        navigator = navigator
      )
    } else {
      // 歌单列表
      Box(modifier = Modifier.fillMaxSize()) {
        if (isSelectMode.value) {
          SelectedList(playlistViewModel = playlistViewModel, songs = selectedModePlaylist)
        } else {
          NormalList(songListState = playlist)
        }
      }
    }
}

@Composable
private fun SelectedList(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  songs: State<List<Pair<Song, Boolean>>>
) {
  LazyColumn(modifier = Modifier.fillMaxSize()) {
    itemsIndexed(songs.value, key = { _, item ->
      item.first.songId ?: -1
    }) { _, item ->
      SelectedSongItem(item.first, item.second, onClick = {
        playlistViewModel.toggleSelectSong(item.first.songId)
      })
    }
  }
}

@Composable
private fun SelectedSongItem(song: Song, selected: Boolean, onClick: () -> Unit = {}) {
  Row(modifier = Modifier
    .fillMaxWidth()
    .padding(vertical = 6.dp, horizontal = 20.dp)
    .background(
      MaterialTheme.colorScheme.surfaceContainer,
      MaterialTheme.shapes.medium
    )
    .clickable {
      onClick()
    }
    .padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {

    AnimatedVisibility(
      visible = selected,
    ) {
      Box(
        modifier = Modifier
          .padding(end = 12.dp)
          .size(24.dp)
          .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
          .padding(4.dp)
      ) {
        // 选中图标
        Icon(
          Icons.Default.Check,
          contentDescription = "Selected",
          tint = MaterialTheme.colorScheme.onSurface
        )
      }
    }

    Column(
      modifier = Modifier
        .weight(1f)
    ) {
      Text(
        text = song.displayName, style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.W600,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ), maxLines = 1
      )
      Text(
        text = song.artist, style = MaterialTheme.typography.bodyMedium.copy(
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        ), maxLines = 1
      )
    }
  }
}


/// 歌单列表
@Composable
private fun NormalList(
  modifier: Modifier = Modifier,
  songListState: State<List<Song>?>,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
  ) {
    itemsIndexed(songListState.value ?: emptyList(), key = { _, song ->
      song.songId ?: -1
    }) { _, song ->
      NormalSongItem(song, onClick = {
        PlaybackStateManager.getInstance().play(song, songListState.value ?: emptyList())
      })
    }
  }
}

/// 歌曲条目
@Composable
private fun NormalSongItem(
  song: Song,
  onClick: () -> Unit = {}
) {
  val painter = rememberAsyncImagePainter(song.getCoverUri())

  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
    .fillMaxWidth()
    .clickable { onClick() }) {
    Spacer(modifier = Modifier.width(16.dp))
    Box(
      modifier = Modifier
        .padding(vertical = 8.dp)
        .width(66.dp)
        .height(66.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
      contentAlignment = Alignment.Center
    ) {

      if (painter.state is AsyncImagePainter.State.Success) {
        Image(
          painter = painter, contentDescription = "封面", modifier = Modifier.size(66.dp)
        )
      } else {
        Icon(
          modifier = Modifier
            .fillMaxWidth()
            .scale(1.5f),
          painter = painterResource(id = R.drawable.ic_dvd),
          contentDescription = "封面",
          tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
      }
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.displayName, maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600
        )
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = song.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
  }
}


@Composable
private fun EmptyContent(
  modifier: Modifier = Modifier,
  navigator: AppComposeNavigator? = null,
  viewModel: PlaylistViewModel = hiltViewModel()
) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    TextButton(
      onClick = {
        navigator?.navigate(
          AppScreens.AddSongScreen.createRoute(
            viewModel.playlistId ?: -1
          )
        )
      }
    ) {
      Text("暂无歌曲,前往添加")
    }
  }
}

@Composable
private fun Toolbar(
  modifier: Modifier = Modifier, playlistViewModel: PlaylistViewModel
) {

  val isSelectMode = playlistViewModel.isSelectMode.collectAsState(false)

  Row(modifier = modifier) {
    // 播放全部按钮
    TextButton(onClick = { /* 播放全部 */ }) {
      Text("播放全部")
    }
    Spacer(modifier = Modifier.weight(1f))
    // 多选模式开关
    IconButton(onClick = {
      playlistViewModel.toggleSelectMode()
    }) {
      Icon(
        Icons.AutoMirrored.Outlined.List, tint = isSelectMode.value.let {
          if (it) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurface
        }, contentDescription = "多选模式开关"
      )
    }
    AnimatedVisibility(
      isSelectMode.value
    ) {
      // 多选模式下的额外按钮
      Row {
        // 删除按钮
        IconButton(onClick = {
          /* 删除歌曲 */
          playlistViewModel.deleteSelectedSongs()
        }) {
          Icon(
            Icons.Default.Delete, contentDescription = "删除"
          )
        }
        // 添加到歌单
        IconButton(onClick = { /* 添加到歌单 */ }) {
          Icon(
            Icons.Default.PlayArrow, contentDescription = "添加到歌单"
          )
        }
      }
    }
  }
}

