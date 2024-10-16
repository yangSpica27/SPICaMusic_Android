package me.spica27.spicamusic.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.MusicViewModel


@Composable
fun HomePage(
  modifier: Modifier = Modifier,
  musicViewModel: MusicViewModel = hiltViewModel(),
) {

  val allSongState = musicViewModel.allSongs.collectAsState(emptyList())


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
      // 分类tab
      TabRow(
        selectedTabIndex = pagerState.currentPage, onTabSelected = {
          coroutineScope.launch {
            pagerState.animateScrollToPage(it)
          }
        }, tabs = listOf("收藏", "歌单", "最近添加")
      )
      // 歌曲列表
      HorizontalPager(
        state = pagerState, modifier = Modifier.weight(1f), userScrollEnabled = false
      ) {
        when (it) {
          0 -> SongList(songs = allSongState.value)
          1 -> PLayListItems()
          2 -> SongList(songs = allSongState.value)
        }
      }
    }
  }
}

@Composable
private fun SongList(modifier: Modifier = Modifier, songs: List<Song> = emptyList()) {
  val musicViewModel = hiltViewModel<MusicViewModel>()
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top
  ) {
    item { Spacer(modifier = Modifier.width(12.dp)) }


    itemsIndexed(songs, key = { _, song -> song.songId.toString() }) { index, song ->
      SongItem(song, onClick = {
        musicViewModel.play(song, songs)
      })
    }

    item { Spacer(modifier = Modifier.width(50.dp)) }
  }
}

@Composable
private fun SongItem(song: Song, onClick: () -> Unit = {}) {

  val painter = rememberAsyncImagePainter(song.getCoverUri())

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

      if (painter.state is AsyncImagePainter.State.Success) {
        Image(
          painter = painter, contentDescription = "封面", modifier = Modifier.size(66.dp)
        )
      } else {
        Text(text = song.displayName.first().toString(), style = MaterialTheme.typography.bodyMedium)
      }

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
        },
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.W600 else FontWeight.Normal
      ), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
  }
}


@Composable
private fun PLayListItems(
  viewModel: MusicViewModel = hiltViewModel()
) {
  val allPlayList = viewModel
    .allPlayList
    .collectAsState(null)

  if (allPlayList.value == null) {
    return Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text(text = "加载中")
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize()
    ) { // 歌单列表
      item { AddPlayListItem() }
      itemsIndexed(listOf<Playlist>(
        Playlist(playlistId = 1, playlistName = "歌单1"),
        Playlist(playlistId = 2, playlistName = "歌单2"),
        Playlist(playlistId = 3, playlistName = "歌单3"),
        Playlist(playlistId = 4, playlistName = "歌单4"),
      ), key = { _, item -> item.playlistId ?: 0 })
      { _, playList ->
        PlaylistItem(playList)
      }
    }
  }


}

@Composable
private fun AddPlayListItem(modifier: Modifier = Modifier) {
  Row(
    modifier = Modifier
      .clickable { }
      .padding(16.dp)
      .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      Modifier
        .width(50.dp)
        .height(50.dp)
        .background(
          MaterialTheme.colorScheme.outlineVariant,
          MaterialTheme.shapes.medium
        ),
      contentAlignment = Alignment.Center
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
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.W600
      )
    )
  }
}


@Composable
private fun PlaylistItem(playlist: Playlist) {
  Row(
    modifier = Modifier
      .clickable { }
      .padding(16.dp)
      .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .width(50.dp)
        .height(50.dp)
        .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = (playlist.playlistName.firstOrNull() ?: "A").toString(),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Text(
      text = playlist.playlistName,
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
      )
    )
  }

}


