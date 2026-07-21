package me.spica27.spicamusic.ui.dialog

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

@Stable
class SongMenuViewModel(
    private val song: Song,
    private val songRepository: SongUseCases,
    private val playlistRepository: PlaylistUseCases,
    private val albumRepository: AlbumUseCases,
    private val player: PlayerUseCases,
) : ViewModel() {
    val isLiked: StateFlow<Boolean> =
        songRepository.getSongLikeStatusFlowByMediaStoreId(song.mediaStoreId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = song.like,
        )

    val availablePlaylists: StateFlow<List<Playlist>> =
        playlistRepository.getPlaylistsNotHavingSong(song.mediaStoreId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val albumDetail: StateFlow<Album> =
        albumRepository
            .getAlbumSongsFlow(song.albumId.toString())
            .map { songs ->
                Album(
                    id = song.albumId.toString(),
                    title = song.album,
                    artist = song.artist,
                    numberOfSongs = songs.size,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    Album(
                        id = song.albumId.toString(),
                        title = song.album,
                        artist = song.artist,
                    ),
            )

    val artistDetail: StateFlow<Artist> =
        songRepository
            .getSongsFlow(filter = SongFilter(artists = listOf(song.artist)))
            .map { songs ->
                Artist(
                    name = song.artist,
                    songCount = songs.size,
                    coverAlbumId = song.albumId,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue =
                    Artist(
                        name = song.artist,
                        songCount = 0,
                        coverAlbumId = song.albumId,
                    ),
            )

    fun addToNext() {
        player.doAction(PlayerAction.AddToNext(song.mediaStoreId.toString()))
    }

    fun addToQueue() {
        player.doAction(PlayerAction.AddToQueue(listOf(song.mediaStoreId.toString())))
    }

    fun toggleLike() {
        viewModelScope.launch {
            songRepository.toggleLikeByMediaStoreId(song.mediaStoreId)
        }
    }

    fun ignoreSong() {
        viewModelScope.launch {
            songRepository.updateSongIgnoreStatus(song.mediaStoreId, true)
        }
    }

    fun addToPlaylist(playlistId: Long) {
        viewModelScope.launch {
            playlistRepository.addSongToPlaylist(playlistId, song.mediaStoreId)
        }
    }

    fun createPlaylistAndAdd(name: String) {
        if (name.isBlank()) {
            Timber.w("歌单名称不能为空")
            return
        }

        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name.trim())
            playlistRepository.addSongToPlaylist(playlistId, song.mediaStoreId)
        }
    }
}
