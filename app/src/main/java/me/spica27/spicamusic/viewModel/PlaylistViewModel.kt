package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.PlaylistWithSongs
import me.spica27.spicamusic.db.entity.Song
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
  private val playlistDao: PlaylistDao,
  private val songDao: SongDao
) : ViewModel() {


  fun songInfoWithSongsFlow(playlistId: Long): Flow<PlaylistWithSongs?> {
    return playlistDao.getPlaylistsWithSongsWithPlayListIdFlow(playlistId).flowOn(Dispatchers.IO)
      .distinctUntilChanged()
  }


  fun songsFlow(playlistId: Long): Flow<List<Song>> {
    return playlistDao.getSongsByPlaylistIdFlow(playlistId).flowOn(Dispatchers.IO)
      .distinctUntilChanged()
  }


  fun playlistFlow(playlistId: Long): SharedFlow<Playlist?> {
    return playlistDao.getPlayListByIdFlow(playlistId)
      .flowOn(Dispatchers.IO)
      .shareIn(viewModelScope, SharingStarted.Lazily, 1)
  }

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

  fun renameCurrentPlaylist(playlistId: Long?, newName: String) {
    viewModelScope.launch(Dispatchers.IO) {
      playlistId.let { playlistDao.renamePlaylist(it ?: -1, newName) }
    }
  }

  fun renamePlaylist(id: Long?, newName: String) {
    if (id == null) return
    viewModelScope.launch(Dispatchers.IO) {
      playlistDao.renamePlaylist(id, newName)
    }
  }

  // 删除歌单
  fun deletePlaylist(playlistId: Long?) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteById(playlistId)
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongToPlaylist(playlistId: Long?, songId: Long) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItem(PlaylistSongCrossRef(playlistId ?: -1, songId))
      }
    }
  }

  // 添加歌曲到歌单
  fun addSongsToPlaylist(playlistId: Long?, songIds: List<Long>) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.insertListItems(songIds.map {
          PlaylistSongCrossRef(
            playlistId,
            it
          )
        })
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongFromPlaylist(playlistId: Long?, songId: Long) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItem(PlaylistSongCrossRef(playlistId, songId))
      }
    }
  }

  // 从歌单中移除歌曲
  fun removeSongsFromPlaylist(playlistId: Long?, songIds: List<Long>) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(songIds.map {
          PlaylistSongCrossRef(
            playlistId,
            it
          )
        })
      }
    }
  }

  // 删除选中的歌曲
  fun deleteSelectedSongs(playlistId: Long?) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItems(
          selectSongsSet.toList().map { PlaylistSongCrossRef(playlistId, it) })
      }
    }
  }

  // 删除选中的歌曲
  fun deleteSelectedSongs(playlistId: Long?, songId: Long) {
    if (playlistId != null) {
      viewModelScope.launch(Dispatchers.IO) {
        playlistDao.deleteListItem(
          PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = songId
          )
        )
      }
    }
  }

  fun fetchSongWithId(songId: Long): Song {
    return songDao.getSongWithId(songId)
  }


  fun fetchPlaylistWithId(playlistId: Long): List<Song> {
    return playlistDao.getSongsByPlaylistId(playlistId)
  }

}