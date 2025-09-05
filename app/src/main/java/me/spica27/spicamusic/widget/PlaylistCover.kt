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
import coil3.executeBlocking
import coil3.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.entity.Playlist
import org.koin.compose.koinInject
import timber.log.Timber

@Composable
fun PlaylistCover(
    modifier: Modifier = Modifier,
    playlist: Playlist? = null,
) {
    val playlistDao = koinInject<PlaylistDao>()

    var cover by remember {
        mutableStateOf(
            if (playlist?.cover != null) {
                playlist.cover?.toUri()
            } else {
                null
            },
        )
    }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            if (playlist == null) return@launch
            if (!playlist.needUpdate) return@launch
            val songs = playlistDao.getSongsByPlaylistId(playlist.playlistId ?: -1)
            Timber.tag("PlaylistCover").d("开始更新封面")
            for (song in songs) {
                val uri = song.getCoverUri()
                val request =
                    ImageRequest
                        .Builder(App.getInstance())
                        .data(uri)
                        .build()
                val result =
                    ImageLoader(App.getInstance())
                        .executeBlocking(request)
                if (result.image != null) {
                    cover = uri
                    if (playlist.cover !== uri.toString()) {
                        playlist.cover = uri.toString()
                        playlist.needUpdate = false
                        playlistDao.insertPlaylist(playlist)
                    }
                    return@launch
                }
                playlist.needUpdate = false
                playlistDao.insertPlaylist(playlist)
            }
            Timber.tag("PlaylistCover").d("更新封面完成")
        }
    }

    AnimatedContent(
        cover,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
    ) { cover ->

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            if (cover == null) {
                Text(
                    "${playlist?.playlistName?.firstOrNull() ?: "A"}",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Normal,
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                AsyncImage(
                    model = cover,
                    contentDescription = "Playlist Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
