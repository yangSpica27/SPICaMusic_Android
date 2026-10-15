package me.spica27.spicamusic.ui.dialog

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.ArtistCredit
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.ArtistUseCases
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
    private val artistRepository: ArtistUseCases,
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

    /** 歌曲所属专辑；没有专辑标签的歌曲为 null */
    val albumDetail: StateFlow<Album?> =
        (song.albumId?.let(albumRepository::getAlbumFlow) ?: flowOf(null)).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    /** 这首歌的全部署名（演唱 / 作曲 / 作词），供"查看艺人"选择 */
    val credits: StateFlow<List<ArtistCredit>> =
        flow { emit(artistRepository.getSongCredits(song.mediaStoreId)) }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
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
