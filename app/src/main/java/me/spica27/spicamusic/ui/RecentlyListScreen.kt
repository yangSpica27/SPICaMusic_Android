package me.spica27.spicamusic.ui

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.SimpleTopBar
import me.spica27.spicamusic.widget.SongItemWithCover
import java.util.*


/**
 * 最近播放列表
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecentlyListScreen(
  navigator: NavBackStack? = null,
  songViewModel: SongViewModel = hiltViewModel(),
) {

  val songs = songViewModel.oftenListenSongs.collectAsStateWithLifecycle().value

  val coroutineScope = rememberCoroutineScope()

  val listState = rememberLazyListState()


  Scaffold(
    topBar = {
      SimpleTopBar(
        onBack = {
          navigator?.removeLastOrNull()
        },
        title = "最近播放",
        lazyListState = listState
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = songs.isEmpty(),
        modifier = Modifier.fillMaxSize(),
        label = "LikeListScreen"
      ) {
        isEmpty->
        if (isEmpty) {
          Column (
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
            Text(text = "没有歌曲")
          }
        } else {



          ScrollHaptics(
            listState = listState,
            vibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
            enabled = true,
          )

          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(
              lazyListState = listState,
              snapPosition = SnapPosition.Start
            )
          ) {
            itemsIndexed(
              songs,
              key = { _, song ->
                song.songId ?: UUID.randomUUID().toString()
              }
            ) { index, song ->
              SongItemWithCover(
                modifier = Modifier.animateItem(),
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