package me.spica27.spicamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.viewModel.MusicSearchViewModel
import me.spica27.spicamusic.widget.SongItemWithCover
import org.koin.androidx.compose.koinViewModel


/// 搜索所有歌曲的页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAllScreen(
  musicViewModel: MusicSearchViewModel = koinViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = "搜索")
        },
        actions = { }
      )
    },
    content = { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          // 搜索框
          SearchBar(modifier = Modifier.padding(horizontal = 20.dp), viewModel = musicViewModel)
          // 过滤开关组
          FiltersBar(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp), viewModel = musicViewModel)
          HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp / 2,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
          )
          // 搜索结果列表
          SongList(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            musicViewModel = musicViewModel
          )
        }
      }
    }
  )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun SongList(
  modifier: Modifier = Modifier,
  musicViewModel: MusicSearchViewModel
) {
  val dataState = musicViewModel.songFlow.collectAsState(emptyList())
  val coroutineScope = rememberCoroutineScope()

  if (dataState.value.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
      Text(text = "没有找到相关歌曲")
    }
  } else {

    val listState = rememberLazyListState()

    ScrollHaptics(
      listState = listState,
      vibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
      enabled = true,
    )

    // 歌曲列表
    LazyColumn(
      modifier = modifier,
      state = listState
    ) {
      itemsIndexed(
        dataState.value,
        key = { _, song -> song.songId ?: -1 }
      ) { _, song ->
        SongItemWithCover(
          modifier = Modifier.animateItem(),
          song = song,
          onClick = {},
          coverSize = 66.dp,
          showMenu = true,
          showLike = true,
          showPlus = true,
          onMenuClick = {},
          onLikeClick = {
            musicViewModel.toggleLike(song)
          },
          onPlusClick = {
            coroutineScope.launch {
              PlaybackStateManager.getInstance().playAsync(song)
            }
          }
        )
      }
    }
  }
}


/// 搜索框
@Composable
private fun SearchBar(
  modifier: Modifier = Modifier,
  viewModel: MusicSearchViewModel
) {
  // 搜索关键字
  val inputState = viewModel.searchKey.collectAsState("")

  Box(
    modifier = modifier.background(
      color = MaterialTheme.colorScheme.surfaceContainer,
      shape = MaterialTheme.shapes.small
    )
  ) {
    Row(
      modifier = Modifier
        .padding(vertical = 8.dp, horizontal = 16.dp)
        .fillMaxWidth(),
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.padding(8.dp)
      ) {
        Icon(
          modifier = Modifier.size(24.dp),
          imageVector = Icons.Default.Search,
          contentDescription = "search",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }
      BasicTextField(
        value = inputState.value,
        onValueChange = {
          viewModel.onSearchKeyChange(it)
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.W600,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

/// 过滤开关组
@Composable
private fun FiltersBar(
  modifier: Modifier = Modifier,
  viewModel: MusicSearchViewModel
) {

  val filterNoLikeState = viewModel.filterNoLike.collectAsState(false)

  val filterShortState = viewModel.filterShort.collectAsState(false)

  Row(modifier = modifier) {
    FilterItem(
      title = "过滤非收藏的",
      checked = filterNoLikeState.value,
      onChange = {
        viewModel.toggleFilterNoLike()
      },
      modifier = Modifier.padding(end = 8.dp)
    )
    FilterItem(
      title = "过滤过短的",
      checked = filterShortState.value,
      onChange = {
        viewModel.toggleFilterShort()
      },
      modifier = Modifier.padding(end = 8.dp)
    )
  }
}


/// 过滤开关项
@Composable
fun FilterItem(
  title: String, checked: Boolean, onChange: () -> Unit,
  modifier: Modifier
) {

  Box(
    modifier = modifier
      .background(
        color = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small
      )
      .clickable {
        onChange()
      }
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.W600,
        color = if (checked) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
      )
    )
  }
}
