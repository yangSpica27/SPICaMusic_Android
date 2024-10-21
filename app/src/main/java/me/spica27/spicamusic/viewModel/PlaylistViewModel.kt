package me.spica27.spicamusic.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.navigator.AppScreens
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
  savedStateHandle: SavedStateHandle,
  private val playlistDao: PlaylistDao
) : ViewModel() {

  val playlistId: Long? = savedStateHandle.get<Long>(AppScreens.playlist_id)

  val songInfoWithSongsFlow =
    playlistDao.getPlaylistsWithSongsWithPlayListIdFlow(playlistId ?: -1)
      .distinctUntilChanged()

  val songsFlow = playlistDao.getSongsByPlaylistId(playlistId ?: -1)
    .distinctUntilChanged()

  val playlistFlow = playlistDao
    .getPlayListById(playlistId ?: -1)
    .distinctUntilChanged()

  // 选择模式
  private val _isSelectMode = MutableStateFlow(false)

  // 选中的歌曲
  private val _selectedSongs = MutableStateFlow<HashSet<Long>>(hashSetOf())

  private val selectSongsSet = hashSetOf<Long>()

  val isSelectMode: Flow<Boolean>
    get() = _isSelectMode

  val selectedSongs: Flow<Set<Long>>
    get() = _selectedSongs


  // 切换选择模式
  fun toggleSelectMode() {
    _isSelectMode.value = !_isSelectMode.value
    if (!_isSelectMode.value) {
      clearSelectedSongs()
    }
  }

  fun toggleSelectSong(songId: Long?) {
    if (songId == null) return
    if (selectSongsSet.contains(songId)) {
      selectSongsSet.remove(songId)
    } else {
      selectSongsSet.add(songId)
    }
    _selectedSongs.value = selectSongsSet.toHashSet()
  }

  fun clearSelectedSongs() {
    selectSongsSet.clear()
    _selectedSongs.value = selectSongsSet.toHashSet()
  }


  fun deletePlaylist(id: Long) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistDao.deleteById(id)
    }
  }

  fun renameCurrentPlaylist(newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistId?.let { playlistDao.renamePlaylist(it, newName) }
    }
  }

  fun renamePlaylist(id: Long, newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistDao.renamePlaylist(id, newName)
    }
  }

  // 删除歌单
  fun deletePlaylist() {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteById(playlistId)
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongToPlaylist(songId: Long) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItem(PlaylistSongCrossRef(playlistId, songId))
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongsToPlaylist(songIds: List<Long>) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItems(songIds.map { PlaylistSongCrossRef(playlistId, it) })
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongFromPlaylist(songId: Long) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItem(PlaylistSongCrossRef(playlistId, songId))
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongsFromPlaylist(songIds: List<Long>) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(songIds.map { PlaylistSongCrossRef(playlistId, it) })
      }
    }
  }

  // 删除选中的歌曲
  fun deleteSelectedSongs() {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(
          selectSongsSet.toList().map { PlaylistSongCrossRef(playlistId, it) })
      }
    }
  }


}