package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
  private val playlistDao: PlaylistDao,
) : ViewModel() {

  val playlistId: MutableStateFlow<Long?> = MutableStateFlow(null)



  val songInfoWithSongsFlow = playlistId.map { playlistId ->
    playlistDao.getPlaylistsWithSongsWithPlayListId(playlistId ?: -1)
  }.flowOn(Dispatchers.IO)
    .distinctUntilChanged()


  val songsFlow = playlistId.map { playlistId ->
    playlistDao.getSongsByPlaylistId(playlistId ?: -1)
  }.flowOn(Dispatchers.IO)
    .distinctUntilChanged()

  val playlistFlow = playlistId.map { playlistId ->
    playlistDao
      .getPlayListById(playlistId ?: -1)
  }.flowOn(Dispatchers.IO)
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

  fun setPlaylistId(id: Long) {
    playlistId.value = id
  }

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
      playlistId.value.let { playlistDao.renamePlaylist(it ?: -1, newName) }
    }
  }

  fun renamePlaylist(id: Long, newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistDao.renamePlaylist(id, newName)
    }
  }

  // 删除歌单
  fun deletePlaylist() {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteById(playlistId.value ?: -1)
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongToPlaylist(songId: Long) {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItem(PlaylistSongCrossRef(playlistId.value ?: -1, songId))
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongsToPlaylist(songIds: List<Long>) {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItems(songIds.map {
          PlaylistSongCrossRef(
            playlistId.value ?: -1L,
            it
          )
        })
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongFromPlaylist(songId: Long) {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItem(PlaylistSongCrossRef(playlistId.value ?: -1, songId))
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongsFromPlaylist(songIds: List<Long>) {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(songIds.map {
          PlaylistSongCrossRef(
            playlistId.value ?: -1,
            it
          )
        })
      }
    }
  }

  // 删除选中的歌曲
  fun deleteSelectedSongs() {
    if (playlistId.value != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(
          selectSongsSet.toList().map { PlaylistSongCrossRef(playlistId.value ?: -1, it) })
      }
    }
  }


}