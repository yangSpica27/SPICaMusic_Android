package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onError
import com.skydoves.sandwich.onException
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.comm.NetworkState
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.network.bean.LyricResponse
import me.spica27.spicamusic.repository.LyricRepository


class LyricViewModel(
  private val lyricRepository: LyricRepository
) : ViewModel() {


  private val _lyricsFlow = MutableStateFlow<List<LyricResponse>>(emptyList())

  val lyricsFlow: Flow<List<LyricResponse>> = _lyricsFlow

  private val _state = MutableStateFlow<NetworkState>(NetworkState.IDLE)

  val state: Flow<NetworkState> = _state


  /**
   * 获取歌词
   * @param title 歌曲名
   * @param artist 歌手名
   */
  fun fetchLyric(title: String, artist: String?) {
    _state.value = NetworkState.LOADING
    viewModelScope.launch(Dispatchers.IO) {
      lyricRepository.fetchLyric(title, artist)
        .onSuccess {
          _lyricsFlow.value = data
          _state.value = NetworkState.SUCCESS
        }
        .onError {
          _state.value = NetworkState.ERROR(message = message())
        }
        .onException {
          _state.value = NetworkState.ERROR(message = message())
        }
    }
  }

  fun applyLyric(lyric: LyricResponse, song: Song) {
    viewModelScope.launch(Dispatchers.IO) {
      lyricRepository.saveSongLyric(lyric, song)
    }
  }

}