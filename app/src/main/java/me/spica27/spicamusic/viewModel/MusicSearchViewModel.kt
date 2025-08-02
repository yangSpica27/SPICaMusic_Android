package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Song



class MusicSearchViewModel(
  val songDao: SongDao
) : ViewModel() {


  // 搜索关键字
  private val _searchKey = MutableStateFlow("")

  val searchKey: Flow<String>
    get() = _searchKey

  // 是否过滤收藏的歌曲
  private val _filterNoLike = MutableStateFlow(false)

  val filterNoLike: Flow<Boolean>
    get() = _filterNoLike

  // 过滤过短的歌曲
  private val _filterShort = MutableStateFlow(false)

  val filterShort: Flow<Boolean>
    get() = _filterShort


  val songFlow:Flow<List<Song>> = combine(
    songDao.getAll(),
    _searchKey,
    _filterNoLike,
    _filterShort
  ) { songs, key, like, short ->
    songs
      .filter {
        (key.isEmpty() || it.displayName.contains(key, ignoreCase = true) ||
            it.artist.contains(
              key,
              ignoreCase = true
            )) &&
            (!like || it.like) &&
            (!short || it.duration > 3000)
      }
  }
    .flowOn(Dispatchers.IO)

  // 收藏/不收藏歌曲
  fun toggleLike(song: Song) {
    viewModelScope.launch {
      songDao.toggleLike(song.songId ?: -1)
    }
  }


  fun onSearchKeyChange(newKey: String) {
    _searchKey.value = newKey
  }

  fun toggleFilterNoLike() {
    _filterNoLike.value = !_filterNoLike.value
  }

  fun toggleFilterShort() {
    _filterShort.value = !_filterShort.value
  }

}