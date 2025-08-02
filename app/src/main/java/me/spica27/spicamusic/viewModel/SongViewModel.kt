package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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


  // 所有收藏的歌曲
  val allLikeSong: StateFlow<List<Song>> = songDao.getAllLikeSong()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  // 所有歌单
  val allPlayList: StateFlow<List<Playlist>> = playlistDao.getAllPlaylist()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val oftenListenSongs10: StateFlow<List<Song>> = songDao.getOftenListenSong10()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val oftenListenSongs: StateFlow<List<Song>> = songDao.getOftenListenSongs()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

  val randomSongs: StateFlow<List<Song>> = songDao.randomSong()
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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