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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.MusicViewModel


@Composable
fun HomePage(
  modifier: Modifier = Modifier,
  musicViewModel: MusicViewModel = hiltViewModel()
) {

  val allSongState = musicViewModel.allSongs.collectAsState(emptyList())

  val likeSongState = musicViewModel.allLikeSongs.collectAsState(emptyList())

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
      Text(
        text = "专辑", style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp), modifier = Modifier.padding(start = 16.dp)
      )
      // 专辑列表
      AlbumList()
      Spacer(modifier = Modifier.height(8.dp))
      // 分类tab
      TabRow(
        selectedTabIndex = pagerState.currentPage, onTabSelected = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(it)
          }
        }, tabs = listOf("收藏", "最近常听", "最近添加")
      )
      // 歌曲列表
      HorizontalPager(
        state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = false
      ) {
        when (it) {
          0 -> SongList(songs = allSongState.value)
          1 -> SongList(songs = likeSongState.value)
          2 -> SongList(songs = allSongState.value)
        }
      }
    }
  }
}

@Composable
private fun SongList(modifier: Modifier = Modifier, songs: List<Song> = emptyList()) {
  val musicViewModel = hiltViewModel<MusicViewModel>()
  LazyColumn(modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top) {
    item { Spacer(modifier = Modifier.width(12.dp)) }
    items(count = songs.size, key = { songs[it].songId ?: -1 }) {
      SongItem(songs[it], onClick = {
        musicViewModel.play(songs[it], songs)
      })
    }
    item { Spacer(modifier = Modifier.width(50.dp)) }
  }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit = {}) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
    .fillMaxWidth()
    .clickable { onClick() }) {
    Spacer(modifier = Modifier.width(16.dp))
    Box(
      modifier = Modifier
        .padding(vertical = 16.dp)
        .width(66.dp)
        .height(66.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center
    ) {

//      AsyncImage(
//        model = song.getCoverUri(),
//        contentDescription = null,
//        modifier = Modifier.size(66.dp),
//      )

    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = song.displayName,
        maxLines = 1,
        style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600
        )
      )
      Text(
        text = song.artist,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1
      )
    }

    IconButton(onClick = {}) {
      Icon(
        imageVector = Icons.Outlined.FavoriteBorder, contentDescription = "更多"
      )
    }

    IconButton(onClick = {}) {
      Icon(
        imageVector = Icons.Default.MoreVert, contentDescription = "更多"
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
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
      Tab(isSelected = index == selectedTabIndex, text = tabs[index], onClick = { onTabSelected(index) })
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
        }
      ), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
  }
}


@Composable
private fun AlbumList() {
  LazyRow(
    modifier = Modifier.fillMaxWidth(),
  ) {
    item {
      Spacer(modifier = Modifier.width(16.dp))
    }

    items(count = 100) {
      AlbumItem()
    }

    item {
      Spacer(modifier = Modifier.width(16.dp))
    }
  }
}

@Composable
private fun AlbumItem() {
  Box(
    modifier = Modifier
      .width(100.dp)
      .height(100.dp)
      .padding(10.dp)
      .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium), contentAlignment = Alignment.Center
  ) {
    Text(text = "专辑", style = MaterialTheme.typography.bodyLarge)
  }
}


