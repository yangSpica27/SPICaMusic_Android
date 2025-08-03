package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.Song


class SelectSongViewModel(
  private val songDao: SongDao,
  private val playlistDao: PlaylistDao,
) : ViewModel() {


  fun getAllSongsNotInPlaylist(playlistId: Long?):  Flow<List<Song>>{
   return songDao.getSongsNotInPlayList(playlistId ?: -1)
  }

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

  // 添加到播放列表
  suspend fun addSongToPlaylist(playlistId: Long?) {
    playlistDao.insertListItems(
      _selectedSongsIds.value.map { songId -> PlaylistSongCrossRef(playlistId ?: 0, songId) })
  }


}