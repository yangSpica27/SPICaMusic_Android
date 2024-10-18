package me.spica27.spicamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.PlaylistItem
import me.spica27.spicamusic.widget.SongItemWithCover
import kotlin.math.roundToInt

// 主页
@Composable
fun HomePage(
  modifier: Modifier = Modifier,
  playBackViewModel: PlayBackViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val likeSongState = playBackViewModel.allLikeSongs.collectAsState(emptyList())
  val allSongState = playBackViewModel.allSongs.collectAsState(emptyList())


  Box(
    modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart
  ) {

    val coroutineScope = rememberCoroutineScope()

    val tabs = remember { mutableStateListOf("收藏", "最近常听", "最近添加") }

    val pagerState = rememberPagerState(pageCount = {
      tabs.size
    })

    Column {
      // 标题
      Spacer(modifier = Modifier.height(10.dp))
      SearchButton(navigator)
      Spacer(modifier = Modifier.height(10.dp))
      // 分类tab
      TabRow(
        selectedTabIndex = pagerState.currentPage, onTabSelected = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(it)
          }
        }, tabs = listOf("收藏", "歌单", "最近添加")
      )
      HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp / 2,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
      )
      // 歌曲列表
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f),
        userScrollEnabled = false,
        beyondViewportPageCount = 3
      ) {
        when (it) {
          0 -> if (likeSongState.value.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
              Text(text = "暂无收藏", modifier = Modifier.offset(y = (-50).dp))
            }
          } else {
            SongList(songs = likeSongState.value)
          }

          1 -> PLayListItems(
            navigator = navigator
          )

          2 -> SongList(songs = allSongState.value)
        }
      }
    }
  }
}

// 歌曲列表
@Composable
private fun SongList(modifier: Modifier = Modifier, songs: List<Song> = emptyList()) {
  val playBackViewModel = hiltViewModel<PlayBackViewModel>()

  val isExpandedMenu = remember { mutableStateOf(false) }

  val selectedSong = remember { mutableStateOf<Song?>(null) }

  val menuOffset = remember { mutableStateOf(Offset.Zero) }

  DropdownMenu(
    expanded = isExpandedMenu.value,
    onDismissRequest = { isExpandedMenu.value = false },
    shape = MaterialTheme.shapes.medium,
    offset = convertIntOffsetToDpOffset(
      IntOffset(
        menuOffset.value.x.roundToInt(),
        menuOffset.value.y.roundToInt()
      )
    )
  ) {
    DropdownMenuItem(onClick = {}, text = {
      Text(
        if (selectedSong.value?.like == true) "取消收藏"
        else "收藏"
      )
    })

    DropdownMenuItem(onClick = {}, text = { Text("添加到播放列表") })

    DropdownMenuItem(onClick = {}, text = { Text("歌曲信息") })


  }

  LazyColumn(
    modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.Top
  ) {
    item { Spacer(modifier = Modifier.width(12.dp)) }
    itemsIndexed(songs, key = { _, song -> song.songId.toString() }) { _, song ->
      SongItemWithCover(
        modifier = Modifier.animateItem(),
        song = song,
        coverSize = 56.dp,
        onClick = {
          playBackViewModel.play(song, songs)
        },
        onMenuClick = {
          selectedSong.value = song
          menuOffset.value = it
          isExpandedMenu.value = true
        },
        onLikeClick = {
          playBackViewModel.toggleLike(song)
        },
        showLike = true,
      )
    }

    item { Spacer(modifier = Modifier.width(50.dp)) }
  }
}

@Composable
fun convertIntOffsetToDpOffset(intOffset: IntOffset): DpOffset {
  val density = LocalDensity.current
  return with(density) {
    DpOffset(intOffset.x.toDp(), intOffset.y.toDp())
  }
}


@Composable
private fun SearchButton(navigator: AppComposeNavigator? = null) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)
      .clickable {
        navigator?.navigate(AppScreens.SearchAll.route)
      }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      modifier = Modifier.weight(1f),
      text = "从本地乐库中进行搜索",
      style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
        fontWeight = FontWeight.W600,
      )
    )
    Box(
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f), shape = CircleShape
        )
        .padding(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "搜索",
        tint = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }
  }
}


@Composable
private fun TabRow(
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  tabs: List<String>,
) {
  LazyRow {
    item { Spacer(modifier = Modifier.width(16.dp)) }
    items(tabs.size) { index ->
      Tab(
        isSelected = index == selectedTabIndex,
        text = tabs[index],
        onClick = { onTabSelected(index) })
    }
    item { Spacer(modifier = Modifier.width(16.dp)) }
  }

}

@Composable
private fun Tab(isSelected: Boolean, text: String, onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .padding(8.dp)
      .background(
        if (isSelected) {
          MaterialTheme.colorScheme.primaryContainer
        } else {
          MaterialTheme.colorScheme.surfaceContainer
        }, MaterialTheme.shapes.medium
      )
      .clickable {
        onClick()
      }, contentAlignment = Alignment.Center
  ) {
    Text(
      text = text, style = MaterialTheme.typography.bodyMedium.copy(
        color = if (isSelected) {
          MaterialTheme.colorScheme.onPrimaryContainer
        } else {
          MaterialTheme.colorScheme.onSurface
        }, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal
      ), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
  }
}


@Composable
private fun PLayListItems(
  viewModel: PlayBackViewModel = hiltViewModel(),
  navigator: AppComposeNavigator? = null
) {

  val showAddPlaylistDialogState = remember { mutableStateOf(false) }

  val dataState = viewModel.allPlayList.collectAsState(null)

  if (showAddPlaylistDialogState.value) {
    val playlistNameState = remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddPlaylistDialogState.value = false },
      title = { Text("创建歌单") },
      text = {
        TextField(
          value = playlistNameState.value,
          onValueChange = { playlistNameState.value = it },
          placeholder = { Text("请输入歌单名称") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        IconButton(onClick = {
          viewModel.addPlayList(playlistNameState.value)
          showAddPlaylistDialogState.value = false
        }) {
          Text(
            "确定",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      },
      dismissButton = {
        IconButton(onClick = { showAddPlaylistDialogState.value = false }) {
          Text(
            "取消",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium
          )
        }
      })
  }

  if (dataState.value == null) {
    return Box(
      modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
      Text(text = "加载中")
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize()
    ) { // 歌单列表
      item {
        AddPlayListItem(onClick = {
          showAddPlaylistDialogState.value = true
        })
      }
      itemsIndexed(
        dataState.value ?: listOf(),
        key = { _, item -> item.playlistId ?: 0 }) { _, playList ->
        PlaylistItem(
          modifier = Modifier.animateItem(),
          playlist = playList,
          showMenu = true,
          onClickMenu = {},
          onClick = {
            navigator?.navigate(AppScreens.PlaylistDetail.createRoute(playList.playlistId ?: -1))
          })
      }
    }
  }


}

@Composable
private fun AddPlayListItem(onClick: () -> Unit = {}) {
  Row(modifier = Modifier
    .clickable {
      onClick()
    }
    .padding(vertical = 6.dp, horizontal = 16.dp)
    .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Box(
      Modifier
        .width(50.dp)
        .height(50.dp)
        .background(
          MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium
        ), contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "添加",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = "创建歌单", style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600
      )
    )
  }
}





