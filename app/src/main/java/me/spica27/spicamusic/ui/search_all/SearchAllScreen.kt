package me.spica27.spicamusic.ui.search_all

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.repository.PlaylistRepository
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.utils.ToastUtils
import me.spica27.spicamusic.utils.rememberVibrator
import me.spica27.spicamusic.utils.tick
import me.spica27.spicamusic.viewModel.MusicSearchViewModel
import me.spica27.spicamusic.widget.SimpleTopBar
import me.spica27.spicamusic.widget.SongItemWithCover
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


/// 搜索所有歌曲的页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAllScreen(
  musicViewModel: MusicSearchViewModel = koinViewModel(),
  navigator: NavBackStack
) {
  Scaffold(
    topBar = {
      SimpleTopBar(
        onBack = {
          navigator.removeLastOrNull()
        },
        title = "搜索所有歌曲",
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
          FiltersBar(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            viewModel = musicViewModel
          )
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

  var currentSelectedItem by remember { mutableStateOf<Song?>(null) }
  var showItemMenu by remember { mutableStateOf(false) }
  var showRemoveToPlaylist by remember { mutableStateOf(false) }
  var showAddToPlaylist by remember { mutableStateOf(false) }

  val vibrator = rememberVibrator()

  LaunchedEffect(showAddToPlaylist, showRemoveToPlaylist, showItemMenu) {
    vibrator.cancel()
    if (showAddToPlaylist || showRemoveToPlaylist || showItemMenu) {
      vibrator.tick()
    }
  }

  if (showItemMenu && currentSelectedItem != null) {
    SongItemDialog(
      song = currentSelectedItem!!,
      onDismiss = {
        showItemMenu = false
      },
      addSongToPlaylist = {
        showAddToPlaylist = true
      },
      removeToPlaylist = {
        showRemoveToPlaylist = true
      },
      addToCurrentList = {
        coroutineScope.launch {
          currentSelectedItem?.let {
            PlaybackStateManager.getInstance().playAsync(it)
          }
        }
      }
    )
  }

  if (showRemoveToPlaylist && currentSelectedItem != null) {
    RemoveToPlayListDialog(
      song = currentSelectedItem!!,
      onDismiss = {
        showRemoveToPlaylist = false
      }
    )
  }

  if (showAddToPlaylist && currentSelectedItem != null) {
    AddSongToPlayListDialog(
      song = currentSelectedItem!!,
      onDismiss = {
        showAddToPlaylist = false
      }
    )
  }


  if (dataState.value.isEmpty()) {
    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center
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
          onClick = {
            coroutineScope.launch {
              PlaybackStateManager.getInstance().playAsync(song, dataState.value)
            }
          },
          coverSize = 66.dp,
          showMenu = true,
          showLike = true,
          showPlus = false,
          onMenuClick = {
            currentSelectedItem = song
            showItemMenu = true
          },
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
      verticalAlignment = Alignment.CenterVertically
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


@Composable
private fun SongItemDialog(
  song: Song,
  onDismiss: () -> Unit,
  addSongToPlaylist: () -> Unit,
  removeToPlaylist: () -> Unit,
  addToCurrentList: () -> Unit
) {

  val clipboardManager = LocalClipboard.current

  val coroutineScope = rememberCoroutineScope()

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties()
  ) {
    Column(
      modifier = Modifier
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
        .fillMaxWidth()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .background(
              color = MaterialTheme.colorScheme.surfaceContainer,
              shape = MaterialTheme.shapes.medium
            )
            .clip(MaterialTheme.shapes.medium),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            Modifier
              .fillMaxSize()
              .padding(8.dp),
          ) {
            Icon(
              modifier = Modifier.fillMaxWidth(),
              painter = painterResource(id = R.drawable.ic_dvd),
              contentDescription = "封面",
              tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
          }
          AsyncImage(
            model = song.getCoverUri(),
            contentDescription = "封面",
            modifier = Modifier.fillMaxSize()
          )
        }
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp),
          horizontalAlignment = Alignment.Start,
          verticalArrangement = Arrangement.Center
        ) {
          Text(
            text = song.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
          Text(
            text = song.artist,
            style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ),
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
        }
        IconButton(
          onClick = {
            val clipData = ClipData.newPlainText("songName", song.displayName)
            val clipEntry = ClipEntry(clipData)
            coroutineScope.launch {
              clipboardManager.setClipEntry(clipEntry)
            }
            ToastUtils.showToast("已复制歌名到剪切板")
          },
          modifier = Modifier
            .width(40.dp)
            .height(40.dp)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_copy),
            contentDescription = "复制",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
          )
        }
      }
      HorizontalDivider(
        modifier = Modifier.fillMaxWidth(),
        thickness = 1.dp / 2,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
      )
      ItemMenu(
        "添加到当前播放列表",
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            addToCurrentList()
            onDismiss()
          })
      ItemMenu(
        "添加到歌单",
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            addSongToPlaylist()
            onDismiss()
          })
      ItemMenu(
        "从歌单中移除",
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            removeToPlaylist()
            onDismiss()
          })
    }
  }
}

@Composable
private fun ItemMenu(title: String, modifier: Modifier) {
  Text(
    text = title,
    style = MaterialTheme.typography.bodyLarge.copy(
      fontWeight = FontWeight.Normal,
      color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    ),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
  )
}


@Composable
private fun AddSongToPlayListDialog(song: Song, onDismiss: () -> Unit) {

  val playlistRepository = koinInject<PlaylistRepository>()

  var keyword by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false
    )
  ) {
    Column(
      modifier = Modifier
        .padding(top = 300.dp)
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
        .fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
      )
      Text(
        text = "添加歌曲到歌单",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.W500,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        ),
        modifier = Modifier.padding(horizontal = 16.dp)
      )
      TextField(
        value = keyword,
        onValueChange = {
          keyword = it
        },
        maxLines = 1,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        placeholder = {
          Text(text = "请输入歌单名称关键词")
        },
        colors = TextFieldDefaults.colors().copy(
          disabledIndicatorColor = Color.Transparent,
          errorIndicatorColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        )
      )
    }
  }
}

@Composable
private fun RemoveToPlayListDialog(
  song: Song, onDismiss: () -> Unit,
) {
  val playlistRepository = koinInject<PlaylistRepository>()

  var keyword by remember { mutableStateOf("") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(
      usePlatformDefaultWidth = false
    )
  ) {
    Column(
      modifier = Modifier
        .padding(top = 300.dp)
        .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
        .fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),

    ) {
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
      )
      Text(
        text = "从歌单中移除",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.W500,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        ),
        modifier = Modifier.padding(horizontal = 16.dp)
      )
      TextField(
        value = keyword,
        onValueChange = {
          keyword = it
        },
        maxLines = 1,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp),
        placeholder = {
          Text(text = "请输入歌单名称关键词")
        },
        colors = TextFieldDefaults.colors().copy(
          disabledIndicatorColor = Color.Transparent,
          errorIndicatorColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        )
      )
    }
  }
}
