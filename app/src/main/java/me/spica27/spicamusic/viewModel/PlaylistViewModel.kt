package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.PlaylistRepository


class PlaylistViewModel(
  private val playlistRepository: PlaylistRepository
) : ViewModel() {


  fun songsFlow(playlistId: Long): Flow<List<Song>> {
    return playlistRepository.getSongsByPlaylistIdFlow(playlistId).flowOn(Dispatchers.IO)
      .distinctUntilChanged()
  }


  fun playlistFlow(playlistId: Long): SharedFlow<Playlist?> {
    return playlistRepository.getPlayListByIdFlow(playlistId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.Lazily, 1)
  }


  fun deletePlaylistItem(playlistId: Long, songId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }
  }

  fun deletePlaylist(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.deletePlaylist(id)
    }
  }

  fun addPlayCount(playlistId: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistRepository.addPlaylistPlayTime(playlistId)
    }
  }

  fun renamePlaylist(id: Long?, newName: String) {
    if (id == null) return
    viewModelScope.launch {
      playlistRepository.renamePlaylist(
        newName = newName,
        playlistId = id
      )
    }
  }


}