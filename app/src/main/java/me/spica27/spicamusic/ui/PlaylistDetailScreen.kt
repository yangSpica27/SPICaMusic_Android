package me.spica27.spicamusic.ui


import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.viewModel.PlaylistViewModel
import me.spica27.spicamusic.widget.InputTextDialog
import me.spica27.spicamusic.widget.SongItemWithCover

/// 歌单详情页面
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: NavBackStack? = null,
  playlistId: Long
) {


  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()


  val songs =
    playlistViewModel.songsFlow(playlistId).collectAsStateWithLifecycle(emptyList()).value

  val playlist = playlistViewModel.playlistFlow(playlistId).collectAsStateWithLifecycle(null).value

  val coroutineScope = rememberCoroutineScope()

  Scaffold(
    modifier = Modifier
      .nestedScroll(scrollBehavior.nestedScrollConnection)
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surfaceContainer),
    topBar = {
      TopAppBar(
        title = {
          Header(
            playlist = playlist ?: return@TopAppBar,
            navigator = navigator
          )
        },
        expandedHeight = 170.dp,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainer,
          scrolledContainerColor = MaterialTheme.colorScheme.background
        )
      )
    }
  ) { paddingValues ->
    if (songs.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
        )
        AsyncImage(
          modifier = Modifier.height(130.dp),
          model = R.drawable.load_error,
          contentDescription = null
        )
        Spacer(
          modifier = Modifier.height(10.dp)
        )
        Text(
          "空空如也",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }
    } else {
      Column(
        modifier = Modifier
          .padding(paddingValues)
          .fillMaxSize()
          .background(
            color = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(
              topStart = 12.dp,
              topEnd = 12.dp
            )
          ),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              coroutineScope.launch {
                if (songs.isNotEmpty()) {
                  PlaybackStateManager.getInstance()
                    .playAsync(
                      songs.first(),
                      songs
                    )
                }
              }
            }
            .padding(
              horizontal = 16.dp,
              vertical = 12.dp
            )
        ) {
          Box(
            modifier = Modifier
              .size(60.dp)
              .background(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.shapes.small
              ),
            contentAlignment = Alignment.Center
          ) {
            AsyncImage(
              model = R.drawable.ic_play,
              contentDescription = "",
              modifier = Modifier.width(48.dp),
              colorFilter = ColorFilter.tint(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            )
          }
          Column(
            modifier = Modifier
              .weight(1f)
              .height(60.dp)
              .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center
          ) {
            Text("播放全部", style = MaterialTheme.typography.bodyLarge)
            Text(
              "已经播放xx次数", style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
              )
            )
          }

          IconButton(
            onClick = {

            }
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Default.List,
              contentDescription = null
            )
          }

        }
        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(songs, key = {
            it.songId ?: -1
          }) {
            SongItemWithCover(
              modifier = Modifier
                .fillMaxWidth()
                .animateItem(),
              song = it,
              onClick = {
                coroutineScope.launch {
                  PlaybackStateManager.getInstance().playAsync(it, songs)
                }
              },
              coverSize = 66.dp
            )
          }
        }
      }
    }
  }
}


@Composable
fun Header(
  modifier: Modifier = Modifier,
  playlist: Playlist,
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: NavBackStack? = null,
) {

  val showRenameDialog = remember { mutableStateOf(false) }

  if (showRenameDialog.value) {
    InputTextDialog(
      onDismissRequest = {
        showRenameDialog.value = false
      },
      title = "重命名歌单",
      onConfirm = {
        playlistViewModel.renamePlaylist(playlist.playlistId, it)
        showRenameDialog.value = false
      },
      defaultText = playlist.playlistName,
      placeholder = "请输入歌单名称"
    )
  }

  val showDeleteDialog = remember { mutableStateOf(false) }

  if (showDeleteDialog.value) {
    DeleteSureDialog(
      playlistId = playlist.playlistId ?: -1,
      onDismissRequest = {
        showDeleteDialog.value = false
      },
      navigator = navigator
    )
  }

  Column(
    modifier = modifier.fillMaxSize()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .width(80.dp)
          .height(80.dp)
          .border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            shape = MaterialTheme.shapes.small
          )
          .background(
            MaterialTheme.colorScheme.surfaceContainer,
            MaterialTheme.shapes.small
          ),
      ) {
        Text(
          modifier = Modifier.align(Alignment.Center),
          text = "歌单封面",
          style = MaterialTheme.typography.titleMedium
        )
      }
      Spacer(
        modifier = Modifier
          .width(12.dp)
      )
      Text(
        text = playlist.playlistName,
        style = MaterialTheme.typography.titleLarge.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
          fontWeight = FontWeight.ExtraBold
        ),
        modifier = Modifier
      )

    }
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ElevatedButton(
        shape = MaterialTheme.shapes.small,
        onClick = {
          showRenameDialog.value = true
        },
        colors = ButtonDefaults.elevatedButtonColors().copy(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      ) {
        Text("重命名歌单")
      }
      ElevatedButton(
        shape = MaterialTheme.shapes.small,
        onClick = {
          navigator?.add(
            Routes.AddSong(
              playlistId = playlist.playlistId ?: -1
            )
          )
        },
        colors = ButtonDefaults.elevatedButtonColors().copy(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      ) {
        Text("新增")
      }
      ElevatedButton(
        shape = MaterialTheme.shapes.small,
        onClick = {
          showDeleteDialog.value = true
        },
        colors = ButtonDefaults.elevatedButtonColors().copy(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      ) {
        Text("删除")
      }
    }
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(12.dp)
    )
  }
}


/**
 * 确认删除歌单的弹框
 */
@Composable
private fun DeleteSureDialog(
  playlistId: Long,
  onDismissRequest: () -> Unit = { },
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: NavBackStack? = null
) {
  val coroutineScope = rememberCoroutineScope()
  AlertDialog(
    shape = MaterialTheme.shapes.small,
    onDismissRequest = { onDismissRequest() }, title = {
    Text("删除歌单")
  }, text = {
    Text("确定要删除这个歌单吗?")
  }, confirmButton = {
    TextButton(onClick = {
      // 确认删除
      coroutineScope.launch {
        playlistViewModel.deletePlaylist(playlistId)
        onDismissRequest()
        navigator?.removeLastOrNull()
      }
    }) {
      Text("确定")
    }
  }, dismissButton = {
    TextButton(onClick = {
      // 取消删除
      onDismissRequest()
    }) {
      Text("取消")
    }
  })
}


@OptIn(ExperimentalMaterial3Api::class)
suspend fun TopAppBarScrollBehavior.expandAnimating() {
  AnimationState(
    initialValue = this.state.heightOffset
  )
    .animateTo(
      targetValue = 0f,
      animationSpec = tween(durationMillis = 500)
    ) { this@expandAnimating.state.heightOffset = value }
}




