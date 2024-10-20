package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SongViewModel
@Inject constructor(
  private val songDao: SongDao,
  private val playlistDao: PlaylistDao
) : ViewModel() {

  fun getSongFlow(id: Long) = songDao.getSongFlowWithId(id)

  // 所有歌曲
  val allSongs: Flow<List<Song>> = songDao.getAll()

  // 所有收藏的歌曲
  val allLikeSongs: Flow<List<Song>> = songDao.getAllLikeSong()

  // 所有歌单
  val allPlayList: Flow<List<Playlist>> = playlistDao.getAllPlaylist()


  // 切换喜欢状态
  fun toggleFavorite(id: Long) {
    Timber.e("toggleFavorite: $id")
    viewModelScope.launch(Dispatchers.IO) {
      songDao.toggleLike(id)
    }
  }

  // 添加歌单
  fun addPlayList(value: String) {
    viewModelScope.launch {
      playlistDao.insertPlaylist(Playlist(playlistName = value))
    }
  }

}