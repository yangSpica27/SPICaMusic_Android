package me.spica27.spicamusic.ui.like_list

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.SimpleTopBar
import me.spica27.spicamusic.widget.SongItemMenu
import me.spica27.spicamusic.widget.SongItemWithCover
import me.spica27.spicamusic.widget.rememberSongItemMenuDialogState
import me.spica27.spicamusic.wrapper.activityViewModel
import java.util.*


@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LikeListScreen(
  navigator: NavBackStack? = null,
  songViewModel: SongViewModel = activityViewModel(),
) {

  val songs = songViewModel.allLikeSong.collectAsStateWithLifecycle().value

  val isEmpty =
    songViewModel.allLikeSong.map { it.isEmpty() }
      .collectAsStateWithLifecycle(true).value

  val coroutineScope = rememberCoroutineScope()


  val listState = rememberLazyListState()

  val songItemMenuDialogState = rememberSongItemMenuDialogState()

  SongItemMenu(
    songItemMenuDialogState
  )

  Scaffold(
    topBar = {
      SimpleTopBar(
        onBack = {
          navigator?.removeLastOrNull()
        },
        title = "我的收藏",
        lazyListState = listState
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = isEmpty,
        modifier = Modifier.fillMaxSize(),
        label = "LikeListScreen"
      ) {
        if (it) {
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
          ) {
            Spacer(
              modifier = Modifier.height(70.dp)
            )
            AsyncImage(
              modifier = Modifier.height(130.dp),
              model = R.drawable.load_error,
              contentDescription = null
            )
            Spacer(
              modifier = Modifier.height(10.dp)
            )
            Text(text = "没有收藏歌曲")
          }
        } else {
          ScrollHaptics(
            listState = listState,
            vibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
            enabled = true,
          )

          LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = rememberSnapFlingBehavior(
              lazyListState = listState,
              snapPosition = SnapPosition.Start
            )
          ) {
            items(
              songs,
              key = { song ->
                song.songId ?: UUID.randomUUID().toString()
              }
            ) { song ->
              SongItemWithCover(
                modifier = Modifier
                  .animateItem(),
                song = song,
                onClick = {
                  coroutineScope.launch {
                    PlaybackStateManager.getInstance().playAsync(song, songs)
                  }
                },
                coverSize = 66.dp,
                showLike = true,
                onLikeClick = {
                  songViewModel.toggleFavorite(song.songId ?: -1)
                },
                onMenuClick = {
                  songItemMenuDialogState.show(song)
                }
              )
            }
            item {
              Spacer(
                modifier = Modifier.height(100.dp)
              )
            }
          }
        }
      }
    }
  }
}