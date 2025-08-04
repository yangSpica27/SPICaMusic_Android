@file:OptIn(ExperimentalMaterial3Api::class)

package me.spica27.spicamusic.ui.main.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toCoilUri
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.utils.clickableNoRippleClickableWithVibration
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.FadingEdges
import me.spica27.spicamusic.widget.InputTextDialog
import me.spica27.spicamusic.widget.PlaylistItem
import me.spica27.spicamusic.widget.SongItemWithCover
import me.spica27.spicamusic.widget.fadingEdges
import me.spica27.spicamusic.wrapper.activityViewModel
import java.util.*


// 主页
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
  modifier: Modifier = Modifier,
  songViewModel: SongViewModel = activityViewModel(),
  navigator: NavBackStack? = null,
  listState: ScrollState = rememberScrollState(),
  connection: NestedScrollConnection
) {


  val oftenListenSongs = songViewModel.oftenListenSongs10.collectAsStateWithLifecycle()

  val playlist = songViewModel.allPlayList.collectAsStateWithLifecycle().value


  val randomSong = songViewModel.randomSongs.collectAsState().value


  val showCreatePlaylistDialog = remember { mutableStateOf(false) }

  if (showCreatePlaylistDialog.value) {
    InputTextDialog(
      onDismissRequest = {
        showCreatePlaylistDialog.value = false
      },
      title = "新建歌单",
      onConfirm = {
        songViewModel.addPlayList(it)
        showCreatePlaylistDialog.value = false
      }
    )
  }

  Box(
    modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Top
    ) {
      // 标题
      SearchTopBar(navigator)
      Column(
        modifier = Modifier
          .weight(1f)
          .nestedScroll(connection)
          .verticalScroll(
            state = listState
          )
          .fadingEdges(FadingEdges.None)
          .padding(bottom = 200.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),

        ) {

        Title(
          "最近常听",
          right = {
            Text(
              "查看更多",
              modifier = Modifier.clickableNoRippleClickableWithVibration {
                navigator?.add(
                  Routes.RecentlyList
                )
              },
              style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                fontWeight = FontWeight.W600,
                fontSize = 15.sp
              )
            )
          }
        )
        AnimatedContent(
          targetState = oftenListenSongs.value,
          modifier = Modifier.fillMaxWidth(),
          contentKey = {
            if (it.isEmpty()) {
              0
            } else {
              1
            }
          }
        ) { songs ->
          if (songs.isEmpty()) {
            OftenListenEmptyContent(
              navigator = navigator
            )
          } else {
            OftenListenSongList(songs = songs)
          }
        }
        HorizontalDivider(
          color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.05f
          ),
          thickness = 2.dp,
          modifier = Modifier.padding(vertical = 6.dp)
        )
        Title("歌单", {
          Text(
            "新建歌单",
            modifier = Modifier.clickableNoRippleClickableWithVibration {
              showCreatePlaylistDialog.value = true
            },
            style = MaterialTheme.typography.bodyMedium.copy(
              color = MaterialTheme.colorScheme.primary.copy(alpha = .6f),
              fontWeight = FontWeight.W600,
              fontSize = 15.sp
            )
          )
        })
        Column(
          modifier = Modifier.fillMaxWidth(),
        ) {
          PlaylistItem(
            playlist = Playlist(
              playlistId = 0,
              playlistName = "我的收藏",
              cover = null
            ),
            onClick = {
              navigator?.add(
                Routes.LikeList
              )
            },
            showMenu = false
          )

          playlist.forEach {
            PlaylistItem(
              playlist = it,
              onClick = {
                navigator?.add(
                  Routes.PlaylistDetail(
                    it.playlistId ?: 0,
                  )
                )
              },
              showMenu = false
            )
          }
        }
        HorizontalDivider(
          color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = 0.05f
          ),
          thickness = 2.dp,
          modifier = Modifier.padding(vertical = 6.dp)
        )
        Title(
          "推荐",
        )

        if (randomSong.isEmpty()) {
          Box(
            modifier = Modifier
              .padding(
                horizontal = 16.dp,
              )
          ) {
            Text(
              "暂无推荐"
            )
          }
        } else {
          Column {
            randomSong.forEach {
              SongItemWithCover(
                song = it,
                onClick = {
                  PlaybackStateManager.getInstance()
                    .play(song = it,randomSong)
                },
                showMenu = false,
                showPlus = false,
                showLike = false
              )
            }
          }
        }
      }
    }

  }
}

/**
 * 最近常听占位空
 */
@Composable
private fun OftenListenEmptyContent(modifier: Modifier = Modifier,navigator: NavBackStack? = null) {
  Box(
    modifier = Modifier
      .then(modifier)
      .padding(
        horizontal = 16.dp,
      )
      .width(120.dp)
      .height(180.dp)
      .background(
        MaterialTheme.colorScheme.surfaceContainer,
        MaterialTheme.shapes.small
      )
      .padding(
        horizontal = 16.dp,
        vertical = 12.dp
      )
      .clip(MaterialTheme.shapes.small),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      horizontalAlignment = Alignment.Start,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        "空空如也",
        style = MaterialTheme.typography.titleLarge.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
          fontWeight = FontWeight.Black
        )
      )
      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .height(4.dp)
      )
      TextButton(
        onClick = {
          navigator?.add(Routes.Scanner)
        }
      ) {
        Text("扫描本地音乐")
      }
    }
  }
}

/**
 * 最近常听列表
 */
@Composable
private fun OftenListenSongList(
  songs: List<Song> = emptyList(),
  playBackViewModel: PlayBackViewModel = activityViewModel(),
) {
  val listState = rememberLazyListState()

  ScrollHaptics(
    listState = listState,
    vibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
    enabled = true,
  )

  LazyRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    state = listState,
    flingBehavior = rememberSnapFlingBehavior(
      lazyListState = listState,
      snapPosition = SnapPosition.Center
    )
  ) {
    item {
      Spacer(
        modifier = Modifier
          .width(4.dp)
      )
    }
    items(songs, key = {
      it.songId?.toString() ?: UUID.randomUUID().toString()
    }) {
      val coverPainter = rememberAsyncImagePainter(
        model = it.getCoverUri().toCoilUri()
      )
      val coverState = coverPainter.state.collectAsState().value

      Column(
        modifier = Modifier
          .width(150.dp)
          .animateItem()
          .clip(
            MaterialTheme.shapes.small
          )
          .clickable {
            playBackViewModel.play(it, songs)
          }
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(
              MaterialTheme.colorScheme.surfaceContainer,
              MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)

        ) {
          if (coverState is AsyncImagePainter.State.Success) {
            AsyncImage(
              model = it.getCoverUri().toCoilUri(),
              contentDescription = null,
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop
            )
          } else {
            Text(
              modifier = Modifier.rotate(45f),
              text = it.displayName,
              style = MaterialTheme.typography.headlineLarge.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.W900
              )
            )
          }
        }
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
        )
        Text(
          text = it.displayName,
          modifier = Modifier.fillMaxWidth(),
          maxLines = 1,
          style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
        )
        Spacer(
          modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
        )
        Text(
          text = it.artist,
          modifier = Modifier.fillMaxWidth(),
          maxLines = 1,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(
              alpha = 0.6f
            )
          )
        )
      }
    }
    item {
      Spacer(
        modifier = Modifier
          .width(16.dp)
      )
    }
  }
}

// 标题
@Composable
private fun Title(
  title: String,
  right: @Composable () -> Unit = {},
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.W600,
        fontSize = 20.sp
      )
    )
    Spacer(modifier = Modifier.weight(1f))
    right()
  }
}


/**
 * 顶部标题栏
 */
@Composable
private fun SearchTopBar(navigator: NavBackStack? = null) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp),
  ) {

    Text(
      text = "主页",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp
      ),
      modifier = Modifier
        .align(Alignment.Center)
    )

    IconButton(
      onClick = {
        navigator?.add(Routes.SearchAll)
      },
      modifier = Modifier.align(Alignment.CenterEnd)
    ) {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "搜索",
        tint = MaterialTheme.colorScheme.onSurface
      )
    }

  }
}






