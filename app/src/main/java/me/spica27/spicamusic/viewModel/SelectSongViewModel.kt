package me.spica27.spicamusic.viewModel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.playback.PlaybackStateManager
import timber.log.Timber
import javax.inject.Inject


@HiltViewModel
class SelectSongViewModel @Inject constructor(
  private val songDao: SongDao,
  private val playlistDao: PlaylistDao,
  savedStateHandle: SavedStateHandle,
) : ViewModel() {

  val playlistId: Long? = savedStateHandle.get<Long>(AppScreens.playlist_id)

  fun getAllSongsNotInPlaylist() = songDao.getSongsNotInPlayList(playlistId ?: -1)

  fun getAllSongs() = songDao.getAll()

  private val selectIdsSet = hashSetOf<Long>()

  private val _selectedSongsIds = MutableStateFlow(hashSetOf<Long>())

  val selectedSongsIds: Flow<HashSet<Long>>
    get() = _selectedSongsIds


  fun clearSelectedSongs() {
    selectIdsSet.clear()
    _selectedSongsIds.value = selectIdsSet.toHashSet()
  }

  // 切换歌曲选择状态
  fun toggleSongSelection(songId: Long?) {
    if (songId == null) return
    if (selectIdsSet.contains(songId)) {
      selectIdsSet.remove(songId)
    } else {
      selectIdsSet.add(songId)
    }
    _selectedSongsIds.value = selectIdsSet.toHashSet()
  }

  // 添加到当前播放列表
  fun addSongToCurrentPlaylist(song: Song) {
    viewModelScope.launch(Dispatchers.Default) {
      PlaybackStateManager.getInstance().play(song)
    }
  }

  // 添加到播放列表
  suspend fun addSongToPlaylist() {
    playlistDao.insertListItems(
      _selectedSongsIds.value.map { songId -> PlaylistSongCrossRef(playlistId ?: 0, songId) })
  }


}