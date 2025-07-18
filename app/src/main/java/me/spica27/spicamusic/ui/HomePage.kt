@file:OptIn(ExperimentalMaterial3Api::class)

package me.spica27.spicamusic.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.noRippleClickable
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.PlaylistItem
import me.spica27.spicamusic.widget.SongControllerPanel
import me.spica27.spicamusic.widget.SongItemWithCover

// 主页
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
  modifier: Modifier = Modifier,
  songViewModel: SongViewModel = hiltViewModel(),
  navigator: NavBackStack? = null
) {

  val likeSong = songViewModel.allLikeSongs.collectAsState().value

  val allSong = songViewModel.allSongs.collectAsState().value


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
      TabBar(
        selectedTabIndex = pagerState.currentPage, onTabSelected = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(it)
          }
        }, tabs = listOf("收藏", "歌单", "最近添加")
      )
      // 歌曲列表
      HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f),
        userScrollEnabled = false,
        beyondViewportPageCount = 3
      ) {
        when (it) {
          0 -> if (likeSong.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
              Text(text = "暂无收藏", modifier = Modifier.offset(y = (-50).dp))
            }
          } else {
            SongList(songs = likeSong)
          }

          1 -> PLayListItems(
            navigator = navigator
          )

          2 -> SongList(songs = allSong)
        }
      }
    }
  }
}


// 歌曲列表
@Composable
private fun SongList(modifier: Modifier = Modifier, songs: List<Song> = emptyList()) {
  val playBackViewModel = hiltViewModel<PlayBackViewModel>()

  val isShowDialogState = remember { mutableStateOf(false) }

  val isShowDialog = isShowDialogState.value

  val selectedSongState = remember { mutableStateOf<Song?>(null) }

  val selectedSong = selectedSongState.value

  val menuOffset = remember { mutableStateOf(Offset.Zero) }

  if (isShowDialog && selectedSong != null) {
    Dialog(onDismissRequest = { isShowDialogState.value = false }) {
      SongControllerPanel(
        songId = selectedSong.songId ?: 0,
      )
    }
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
          selectedSongState.value = song
          menuOffset.value = it
          isShowDialogState.value = true
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
private fun SearchButton(navigator: NavBackStack? = null) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
      .clip(MaterialTheme.shapes.medium)
      .clickable {
        navigator?.add(Routes.SearchAll)
      }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.padding(8.dp),
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "搜索",
        tint = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }
    Text(
      modifier = Modifier.weight(1f),
      text = "从本地乐库中进行搜索",
      style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
        fontWeight = FontWeight.W500,
      )
    )
  }
}


@Composable
private fun TabBar(
  selectedTabIndex: Int,
  onTabSelected: (Int) -> Unit,
  tabs: List<String>,
) {

  val paddingFlag = remember { mutableFloatStateOf(0f) }

  val pauseColor = MaterialTheme.colorScheme.surfaceContainer

  val playColor = MaterialTheme.colorScheme.surfaceContainerHigh

  val radius = remember { mutableFloatStateOf(100000f) }

  val isNight = DataStoreUtil().getForceDarkTheme.collectAsState(false)

  val isFirst = remember { mutableStateOf(true) }

  val currentPadding = animateFloatAsState(
    paddingFlag.floatValue,
    tween(475)
  )

  val indicatorColor = remember {
    mutableStateOf(pauseColor)
  }

  val currentColor = animateColorAsState(
    indicatorColor.value,
    tween(575)
  )

  val indicationIndex = animateFloatAsState(
    selectedTabIndex * 1f,
    tween(425),
    label = ""
  )


  LaunchedEffect(selectedTabIndex, isNight.value) {
    if (isFirst.value){
      isFirst.value = false
      return@LaunchedEffect
    }
    paddingFlag.floatValue = 1f
    indicatorColor.value = playColor
    delay(450)
    paddingFlag.floatValue = 0f
    indicatorColor.value = pauseColor
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(60.dp)
  ) {


    Row(
      modifier = Modifier
        .fillMaxSize()
        .align(Alignment.Center)
        .drawWithCache {

          val left = 16.dp.toPx()

          val right = size.width - 16.dp.toPx()

          val itemWidth = size.width / tabs.size

          val itemHeight = size.height

          val centerX = itemWidth * indicationIndex.value + itemWidth / 2f

          val paddingWidth = 16.dp.toPx()

          val paddingHeight = 8.dp.toPx()

          val topY = paddingHeight - paddingHeight * currentPadding.value

          val bottomY = size.height - paddingHeight + paddingHeight * currentPadding.value

          val leftX = left + (
              (centerX - itemWidth / 2f + paddingWidth) -
                  left
              ) * currentPadding.value

          val rightX = right - (
              right - (
                  centerX - paddingWidth + itemWidth / 2f
                  )
              ) * currentPadding.value

          onDrawBehind {
            drawRoundRect(
              color = currentColor.value,
              topLeft = Offset(
                leftX, topY
              ),
              size = Size(
                width = rightX - leftX,
                height = bottomY - topY
              ),
              cornerRadius = CornerRadius(radius.floatValue)
            )
          }
        },
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceEvenly
    ) {

      tabs.forEachIndexed { index, _ ->
        val isSelected = index == selectedTabIndex
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .noRippleClickable {
              onTabSelected(index)
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = tabs[index],
            style = MaterialTheme.typography.bodyMedium.copy(
              color = if (isSelected) {
                MaterialTheme.colorScheme.onBackground.copy(0.9f)
              } else {
                MaterialTheme.colorScheme.onBackground.copy(0.5f)
              },
              fontSize = 18.sp,
              fontWeight = if (isSelected) {
                FontWeight.W600
              } else {
                FontWeight.W500
              }
            ),
          )
        }
      }

    }
  }


}


@Composable
private fun PLayListItems(
  songViewModel: SongViewModel = hiltViewModel(),
  navigator: NavBackStack? = null,
  playlistViewModel: PlaylistViewModel = hiltViewModel()
) {

  val showAddPlaylistDialogState = rememberSaveable { mutableStateOf(false) }

  val showMenu = rememberSaveable { mutableStateOf(false) }

  val selectedPlayListId = rememberSaveable { mutableLongStateOf(-1L) }

  val playlists = songViewModel.allPlayList.collectAsState()

  val showRenameDialog = rememberSaveable { mutableStateOf(false) }


  if (showRenameDialog.value) {
    val playlistNameState = remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = {
      showRenameDialog.value = false
    }, title = { Text("重命名歌单") }, text = {
      TextField(
        value = playlistNameState.value,
        onValueChange = { playlistNameState.value = it },
        placeholder = { Text("请输入新的歌单名称") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
      )
    }, confirmButton = {
      IconButton(onClick = {
        playlistViewModel.renamePlaylist(selectedPlayListId.longValue, playlistNameState.value)
        showRenameDialog.value = false
      }) {
        Text(
          "确定",
          color = MaterialTheme.colorScheme.primary,
          style = MaterialTheme.typography.bodyMedium
        )
      }
    }, dismissButton = {
      IconButton(onClick = { showRenameDialog.value = false }) {
        Text(
          "取消",
          color = MaterialTheme.colorScheme.onSurface,
          style = MaterialTheme.typography.bodyMedium
        )
      }
    })
  }


  // 歌单操作菜单
  if (showMenu.value && selectedPlayListId.longValue != -1L) {
    Dialog(
      onDismissRequest = {
        showMenu.value = false
      },
    ) {
      Column(
        modifier = Modifier
          .clip(MaterialTheme.shapes.medium)
          .background(MaterialTheme.colorScheme.surfaceContainer)
      ) {
        ListItem(
          modifier = Modifier.clickable {
            showMenu.value = false
            showRenameDialog.value = true
          },
          headlineContent = { Text("重命名") },
        )
        ListItem(
          modifier = Modifier.clickable {
            playlistViewModel.deletePlaylist(selectedPlayListId.longValue)
            showMenu.value = false
          },
          headlineContent = { Text("删除") },
        )
      }
    }

  }

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
          songViewModel.addPlayList(playlistNameState.value)
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

  LazyColumn(
    modifier = Modifier.fillMaxSize()
  ) { // 歌单列表
    item {
      AddPlayListItem(onClick = {
        showAddPlaylistDialogState.value = true
      })
    }
    itemsIndexed(
      playlists.value,
      key = { _, item -> item.playlistId ?: 0 }) { _, playList ->
      PlaylistItem(
        modifier = Modifier.animateItem(),
        playlist = playList,
        showMenu = true,
        onClickMenu = {
          selectedPlayListId.longValue = playList.playlistId ?: -1
          showMenu.value = true
        },
        onClick = {
          navigator?.add(Routes.PlaylistDetail(playlistId = playList.playlistId ?: -1))
        })
    }
  }


}

@Composable
private fun AddPlayListItem(onClick: () -> Unit = {}) {
  Row(
    modifier = Modifier
      .clickable {
        onClick()
      }
      .padding(vertical = 6.dp, horizontal = 16.dp)
      .fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically) {
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





