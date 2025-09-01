package me.spica27.spicamusic.widget

import android.content.ClipData
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.PlaylistRepository
import me.spica27.spicamusic.utils.ToastUtils
import me.spica27.spicamusic.utils.rememberVibrator
import me.spica27.spicamusic.utils.tick
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.wrapper.activityViewModel
import org.koin.compose.koinInject

/**
 * 歌曲列表菜单
 */
@Composable
fun rememberSongItemMenuDialogState(): SongListItemMenuDialogState {
  return remember {
    SongListItemMenuDialogState(
      initialCurrentSong = null,
      initialShowItemMenu = false,
      initialShowRemoveToPlaylist = false,
      initialShowAddToPlaylist = false
    )
  }
}

// 将属性改为 MutableState
data class SongListItemMenuDialogState(
  // 提供初始值参数给构造函数
  val initialCurrentSong: Song?,
  val initialShowItemMenu: Boolean,
  val initialShowRemoveToPlaylist: Boolean,
  val initialShowAddToPlaylist: Boolean
) {
  // 使用 by mutableStateOf 将属性委托给 MutableState
  var currentSong by mutableStateOf(initialCurrentSong)
  var showItemMenu by mutableStateOf(initialShowItemMenu)
  var showRemoveToPlaylist by mutableStateOf(initialShowRemoveToPlaylist)
  var showAddToPlaylist by mutableStateOf(initialShowAddToPlaylist)

  fun show(song: Song) {
    currentSong = song
    showItemMenu = true
    showRemoveToPlaylist = false
    showAddToPlaylist = false
  }

  fun dismissItemMenu() {
    showItemMenu = false
  }

  fun dismissRemoveToPlaylist() {
    showRemoveToPlaylist = false
  }

  fun dismissAddToPlaylist() {
    showAddToPlaylist = false
  }
}

@OptIn(UnstableApi::class)
@Composable
fun SongItemMenu(state: SongListItemMenuDialogState, playBackViewModel: PlayBackViewModel) {

  val coroutineScope = rememberCoroutineScope()
  val currentSelectedItem = state.currentSong
  val showItemMenu = state.showItemMenu
  val showRemoveToPlaylist = state.showRemoveToPlaylist
  val showAddToPlaylist = state.showAddToPlaylist
  val vibrator = rememberVibrator()
  val songViewModel = activityViewModel<SongViewModel>()

  LaunchedEffect(showAddToPlaylist, showRemoveToPlaylist, showItemMenu) {
    vibrator.cancel()
    if (showAddToPlaylist || showRemoveToPlaylist || showItemMenu) {
      vibrator.tick()
    }
  }

  if (showItemMenu && currentSelectedItem != null) {
    SongItemDialog(
      song = currentSelectedItem,
      onDismiss = {
        state.dismissItemMenu()
      },
      addSongToPlaylist = {
        state.showAddToPlaylist = true
      },
      removeToPlaylist = {
        state.showRemoveToPlaylist = true
      },
      changeToIgnore = {
        songViewModel.ignore(currentSelectedItem.songId ?: -1, true)
      },
      addToCurrentList = {
        coroutineScope.launch {
          currentSelectedItem.let {
            playBackViewModel.play(it)
          }
        }
      }
    )
  }

  if (showRemoveToPlaylist && currentSelectedItem != null) {
    RemoveToPlayListDialog(
      song = currentSelectedItem,
      onDismiss = {
        state.dismissRemoveToPlaylist()
      }
    )
  }

  if (showAddToPlaylist && currentSelectedItem != null) {
    AddSongToPlayListDialog(
      song = currentSelectedItem,
      onDismiss = {
        state.dismissAddToPlaylist()
      }
    )
  }

}

@Composable
private fun AddSongToPlayListDialog(song: Song, onDismiss: () -> Unit) {


  val playlistRepository = koinInject<PlaylistRepository>()

  val allList =
    playlistRepository.getPlaylistsNotHaveSong(song.songId ?: -1).collectAsState(emptyList())

  var keyword by remember { mutableStateOf("") }

  val showList = remember(
    allList, keyword
  ) {
    derivedStateOf {
      allList.value.filter {
        it.playlistName.contains(keyword)
      }
    }
  }

  val coroutineScope = rememberCoroutineScope()

  Dialog(
    onDismissRequest = onDismiss, properties = DialogProperties(
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
        modifier = Modifier.fillMaxWidth()
      )
      Text(
        text = "添加歌曲到歌单", style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.W500,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        ), modifier = Modifier.padding(horizontal = 16.dp)
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
        shape = MaterialTheme.shapes.large,
        colors = TextFieldDefaults.colors().copy(
          disabledIndicatorColor = Color.Transparent,
          errorIndicatorColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent
        )
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        AnimatedContent(
          targetState = showList.value.isEmpty(),
        ) { isEmpty ->
          if (isEmpty) {
            Box(
              modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
              Text("没有符合条件的歌单", style = MaterialTheme.typography.titleMedium.copy())
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
            ) {
              items(showList.value, key = {
                it.playlistId.toString()
              }) {
                PlaylistItem(playlist = it, modifier = Modifier.animateItem()) {
                  coroutineScope.launch {
                    playlistRepository.addSongToPlaylist(
                      it.playlistId, song.songId ?: -1
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PlaylistItem(
  modifier: Modifier = Modifier, playlist: Playlist, onClick: () -> Unit
) {
  Row(
    modifier = modifier
      .fillMaxSize()
      .clickable {
        onClick.invoke()
      }
      .padding(
        horizontal = 16.dp, vertical = 6.dp
      )
      .clip(MaterialTheme.shapes.small),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .background(
          color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium
        )
        .clip(MaterialTheme.shapes.medium), contentAlignment = Alignment.Center
    ) {
      Text(
        text = playlist.playlistName.firstOrNull().toString(),
        style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.W900,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
      )
    }
    Text(
      text = playlist.playlistName,
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun RemoveToPlayListDialog(
  song: Song, onDismiss: () -> Unit,
) {

  val playlistRepository = koinInject<PlaylistRepository>()

  val allList =
    playlistRepository.getPlaylistsHaveSong(song.songId ?: -1).collectAsState(emptyList())

  var keyword by remember { mutableStateOf("") }

  val showList = remember(
    allList, keyword
  ) {
    derivedStateOf {
      allList.value.filter {
        it.playlistName.contains(keyword)
      }
    }
  }


  val coroutineScope = rememberCoroutineScope()

  Dialog(
    onDismissRequest = onDismiss, properties = DialogProperties(
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
        modifier = Modifier.fillMaxWidth()
      )
      Text(
        text = stringResource(R.string.remove_from_playlist),
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
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        AnimatedContent(
          targetState = showList.value.isEmpty(),
        ) { isEmpty ->
          if (isEmpty) {
            Box(
              modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
            ) {
              Text("没有符合条件的歌单", style = MaterialTheme.typography.titleMedium.copy())
            }
          } else {
            LazyColumn(
              modifier = Modifier.fillMaxSize(),
            ) {
              items(showList.value, key = {
                it.playlistId.toString()
              }) {
                PlaylistItem(
                  playlist = it, modifier = Modifier.animateItem()
                ) {
                  coroutineScope.launch {
                    playlistRepository.removeSongFromPlaylist(
                      it.playlistId, song.songId ?: -1
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}


@Composable
private fun SongItemDialog(
  song: Song,
  onDismiss: () -> Unit,
  addSongToPlaylist: () -> Unit,
  removeToPlaylist: () -> Unit,
  addToCurrentList: () -> Unit,
  changeToIgnore: () -> Unit
) {

  val clipboardManager = LocalClipboard.current

  val coroutineScope = rememberCoroutineScope()

  Dialog(
    onDismissRequest = onDismiss, properties = DialogProperties()
  ) {
    Column(
      modifier = Modifier
        .background(
          MaterialTheme.colorScheme.background, MaterialTheme.shapes.large
        )
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
              contentDescription = stringResource(R.string.tab_cover),
              tint = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
          }
          AsyncImage(
            model = song.getCoverUri(),
            contentDescription = stringResource(R.string.tab_cover),
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
            style = MaterialTheme.typography.bodyLarge.copy(
              fontWeight = FontWeight.W700
            ),
            modifier = Modifier
              .fillMaxWidth()
              .basicMarquee()
          )
          Text(
            text = song.artist, style = MaterialTheme.typography.bodyLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            ), modifier = Modifier
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
            ToastUtils.showToast(App.getInstance().getString(R.string.copy_success))
          }, modifier = Modifier
            .width(40.dp)
            .height(40.dp)
        ) {
          Icon(
            painter = painterResource(id = R.drawable.ic_copy),
            contentDescription = stringResource(android.R.string.copy),
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
        stringResource(R.string.add_to_now_playing_list), modifier = Modifier
          .fillMaxWidth()
          .clickable {
            addToCurrentList()
            onDismiss()
          })
      ItemMenu(
        stringResource(R.string.add_to_playlist), modifier = Modifier
          .fillMaxWidth()
          .clickable {
            addSongToPlaylist()
            onDismiss()
          })
      ItemMenu(
        stringResource(R.string.remove_from_playlist), modifier = Modifier
          .fillMaxWidth()
          .clickable {
            removeToPlaylist()
            onDismiss()
          })
      ItemMenu(
        stringResource(R.string.add_to_ignore_list), modifier = Modifier
          .fillMaxWidth()
          .clickable {
            changeToIgnore()
            onDismiss()
          })
    }
  }
}

@Composable
private fun ItemMenu(title: String, modifier: Modifier) {
  Text(
    text = title, style = MaterialTheme.typography.bodyLarge.copy(
      fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
    ), modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
  )
}