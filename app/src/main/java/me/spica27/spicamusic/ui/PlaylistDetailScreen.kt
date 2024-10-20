package me.spica27.spicamusic.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.widget.SongItemWithCover

/// 歌单详情页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val showDeleteSureDialog = remember { mutableStateOf(false) }

  if (showDeleteSureDialog.value) {
    // 删除确认对话框
    DeleteSureDialog(
      onDismissRequest = {
        showDeleteSureDialog.value = false
      },
      playlistViewModel = playlistViewModel,
      navigator = navigator
    )
  }


  Scaffold(topBar = {
    val playlistState = playlistViewModel.playlistFlow.collectAsState(null)
    // 顶部栏
    TopAppBar(title = {
      // 标题
      playlistState.value?.let { Text(it.playlistName) }
    }, actions = {
      // 右侧操作按钮

      // 删除歌单
      TextButton(
        onClick = {
          // 删除歌单
          showDeleteSureDialog.value = true
        }
      ) {
        Text("删除歌单")
      }

      // 重命名
      TextButton(
        onClick = {
          // 重命名歌单

        }
      ) {
        Text("重命名")
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
fun DeleteSureDialog(
  onDismissRequest: () -> Unit = { },
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {
  val coroutineScope = rememberCoroutineScope()
  AlertDialog(
    onDismissRequest = { onDismissRequest() },
    title = {
      Text("删除歌单")
    },
    text = {
      Text("确定要删除这个歌单吗?")
    },
    confirmButton = {
      TextButton(
        onClick = {
          // 确认删除
          coroutineScope.launch {
            playlistViewModel.deletePlaylist()
            onDismissRequest()
            navigator?.navigateUp()
          }
        }
      ) {
        Text("确定")
      }
    },
    dismissButton = {
      TextButton(
        onClick = {
          // 取消删除
          onDismissRequest()
        }
      ) {
        Text("取消")
      }
    }
  )
}

@Composable
private fun PlaylistDetailList(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {


  val list =
    playlistViewModel.songInfoWithSongsFlow.map { it?.songs }.collectAsState(emptyList()).value

  val isSelectedMode = playlistViewModel.isSelectMode.collectAsState(false).value

  BackHandler(enabled = isSelectedMode) {
    playlistViewModel.toggleSelectMode()
  }

  val selectedModePlaylist = combine(
    playlistViewModel.songsFlow,
    playlistViewModel.selectedSongs
  ) { songs, selectedSongs ->
    songs.map {
      Pair(it, selectedSongs.contains(it.songId))
    }
  }.collectAsState(emptyList())


  if (list == null) {
    // 加载中
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      CircularProgressIndicator()
    }
  } else
    if (list.isEmpty()) {
      // 空内容
      EmptyContent(
        modifier = Modifier.fillMaxSize(),
        navigator = navigator
      )
    } else {
      // 歌单列表
      Box(modifier = Modifier.fillMaxSize()) {
        if (isSelectedMode) {
          SelectedList(playlistViewModel = playlistViewModel, songs = selectedModePlaylist)
        } else {
          NormalList(songList = list)
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
      }
      )
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
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun NormalList(
  modifier: Modifier = Modifier,
  songList: List<Song>,
) {

  val coroutineScope = rememberCoroutineScope()

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
  ) {
    itemsIndexed(songList, key = { _, song ->
      song.songId ?: -1
    }) { _, song ->
      SongItemWithCover(
        modifier = Modifier.animateItem(),
        showMenu = true,
        showPlus = false,
        song = song, onClick = {
          coroutineScope.launch {
            PlaybackStateManager.getInstance().playAsync(song, songList)
          }
        })
    }
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

  val isSelectModeState = playlistViewModel.isSelectMode.collectAsState(false)

  Row(modifier = modifier) {
    // 清楚所有选中按钮

    AnimatedVisibility(
      visible = isSelectModeState.value,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      TextButton(onClick = {
        playlistViewModel.clearSelectedSongs()
      }) {
        Text("清除所有选中")
      }
    }
    Spacer(modifier = Modifier.weight(1f))
    // 多选模式开关
    IconButton(onClick = {
      playlistViewModel.toggleSelectMode()
    }) {
      Icon(
        Icons.AutoMirrored.Outlined.List, tint = isSelectModeState.value.let {
          if (it) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.onSurface
        }, contentDescription = "多选模式开关"
      )
    }
    AnimatedVisibility(
      isSelectModeState.value
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

