package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import coil3.compose.rememberAsyncImagePainter
import coil3.toCoilUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.viewModel.PlaylistViewModel

@Composable
fun PlayListItemDetailScreen(
  playlistId: Long,
  songId: Long,
  playlistViewModel: PlaylistViewModel = hiltViewModel(),
  navigator: NavBackStack? = null
) {

  val song = remember { mutableStateOf<Song?>(null) }

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      song.value = playlistViewModel.fetchSongWithId(songId)
    }
  }


  val coverPainter = rememberAsyncImagePainter(song.value?.getCoverUri()?.toCoilUri())
  val coverPainterState = coverPainter.state.collectAsStateWithLifecycle()

  val coroutineScope = rememberCoroutineScope()




}