package me.spica27.spicamusic.widget

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.entity.Playlist
import org.koin.compose.koinInject


@Composable
fun PlaylistCover(
  playlist: Playlist? = null,
  modifier: Modifier = Modifier
) {

  val playlistDao = koinInject<PlaylistDao>()

  var cover by remember {
    mutableStateOf(
      if (playlist?.cover != null) {
        playlist.cover?.toUri()
      } else {
        null
      }
    )
  }



  LaunchedEffect(Unit) {
    launch(Dispatchers.IO) {
      if (playlist == null) return@launch
      val songs = playlistDao.getSongsByPlaylistId(playlist.playlistId ?: -1)
      for (song in songs) {
        val uri = song.getCoverUri()
        val request = ImageRequest
          .Builder(App.getInstance())
          .data(uri)
          .build()
        val result = ImageLoader(App.getInstance())
          .execute(request)
        if (result.image != null) {
          cover = uri
          if (playlist.cover !== uri.toString()) {
            playlist.cover = uri.toString()
            playlistDao.insertPlaylist(playlist)
          }
          break
        }
      }
    }
  }



  AnimatedContent(
    cover,
    transitionSpec = {
      fadeIn() togetherWith fadeOut()
    }
  ) { cover ->

    Box(
      modifier = modifier,
      contentAlignment = Alignment.Center
    ) {
      if (cover == null) {
        Text(
          "${playlist?.playlistName?.firstOrNull() ?: "A"}",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Normal
          ),
          color = MaterialTheme.colorScheme.onSurface
        )
      } else {
        AsyncImage(
          model = cover,
          contentDescription = "Playlist Cover",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }
    }
  }
}