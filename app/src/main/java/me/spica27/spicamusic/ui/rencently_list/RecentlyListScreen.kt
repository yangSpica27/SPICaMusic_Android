package me.spica27.spicamusic.ui.rencently_list

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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.ScrollHaptics
import me.spica27.spicamusic.utils.ScrollVibrationType
import me.spica27.spicamusic.utils.overScrollVertical
import me.spica27.spicamusic.utils.scrollEndHaptic
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.SimpleTopBar
import me.spica27.spicamusic.widget.SongItemMenu
import me.spica27.spicamusic.widget.SongItemWithCover
import me.spica27.spicamusic.widget.rememberBindPlayerOverlyConnect
import me.spica27.spicamusic.widget.rememberSongItemMenuDialogState
import me.spica27.spicamusic.wrapper.activityViewModel
import java.util.*


/**
 * 最近播放列表
 */
@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RecentlyListScreen(
  navigator: NavController? = null,
  songViewModel: SongViewModel = activityViewModel(),
  playBackViewModel: PlayBackViewModel = activityViewModel()
) {

  val songs = songViewModel.oftenListenSongs.collectAsStateWithLifecycle().value

  val coroutineScope = rememberCoroutineScope()

  val listState = rememberLazyListState()

  val songItemMenuDialogState = rememberSongItemMenuDialogState()

  SongItemMenu(
    songItemMenuDialogState,
    playBackViewModel
  )


  Scaffold(
    topBar = {
      SimpleTopBar(
        onBack = {
          navigator?.popBackStack()
        },
        title =  stringResource(R.string.recently_listen),
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
      ) { isEmpty ->
        if (isEmpty) {
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
            Text(text = "没有歌曲")
          }
        } else {


          ScrollHaptics(
            listState = listState,
            vibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
            enabled = false,
          )

          LazyColumn(
            modifier = Modifier
              .fillMaxSize()
              .scrollEndHaptic()
              .overScrollVertical()
              .nestedScroll(rememberBindPlayerOverlyConnect()),
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
                  playBackViewModel.play(song, songs)
                },
                coverSize = 66.dp,
                showLike = true,
                onLikeClick = {
                  songViewModel.toggleFavorite(song.songId ?: -1)
                },
                showMenu = true,
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