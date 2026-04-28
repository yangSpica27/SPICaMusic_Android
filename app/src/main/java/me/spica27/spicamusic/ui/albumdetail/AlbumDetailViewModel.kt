package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction

@Stable
class AlbumDetailViewModel(
    private val albumId: String,
    private val albumRepository: AlbumUseCases,
    private val player: PlayerUseCases,
) : ViewModel() {
    val songs: StateFlow<List<Song>> =
        albumRepository.getAlbumSongsFlow(albumId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun playAll() {
        val songList = songs.value
        if (songList.isEmpty()) return
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    start = true,
                ),
            )
        }
    }

    fun playSongInList(song: Song) {
        val songList = songs.value
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    mediaId = song.mediaStoreId.toString(),
                    start = true,
                ),
            )
        }
    }
}
