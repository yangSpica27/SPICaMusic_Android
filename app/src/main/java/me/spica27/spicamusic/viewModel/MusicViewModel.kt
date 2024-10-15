package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzx.starrysky.OnPlayerEventListener
import com.lzx.starrysky.StarrySky
import com.lzx.starrysky.manager.PlaybackStage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Song
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MusicViewModel @Inject constructor(private val songDao: SongDao) : ViewModel() {

  // 所有歌曲
  val allSongs: Flow<List<Song>> = songDao.getAll()

  // 所有喜欢的歌曲
  val allLikeSongs: Flow<List<Song>> = songDao.getAllLikeSong()

  // 播放列表
  private val _playlist = MutableStateFlow(listOf<Song>())
  val playList: Flow<List<Song>>
    get() = _playlist


  // 当前播放的乐曲
  private val _song = MutableStateFlow<Song?>(null)


  val currentSongFlow: Flow<Song?>
    get() = _song


  @OptIn(ExperimentalCoroutinesApi::class)
  val likeFlow: Flow<Boolean>
    get() = _song.filterNotNull()
      .flatMapLatest {
        songDao.getSongIsLikeFlowWithId(it.songId ?: 0)
      }
      .map { it == 1 }
      .distinctUntilChanged()
      .flowOn(Dispatchers.IO)


  // 是否正在播放
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: Flow<Boolean>
    get() = _isPlaying

  // 当前的进度
  private val _positionDs = flow {
    while (true) {
      StarrySky.with().getPlayingPosition().let {
        emit(it)
      }
      delay(1000)
    }
  }

  val positionDs: Flow<Long>
    get() = _positionDs.distinctUntilChanged()


  init {
    Timber.tag("MusicViewModel").e("init")
    StarrySky.with().addPlayerEventListener(object : OnPlayerEventListener {
      override fun onPlaybackStageChange(stage: PlaybackStage) {
        _isPlaying.value = !stage.isStop
        viewModelScope.launch(Dispatchers.IO) {
          _song.value = songDao.getSongWithMediaStoreId(StarrySky.with().getNowPlayingSongInfo()?.songId?.toLong() ?: 0)
        }
      }
    }, "MusicViewModel")

  }


  fun togglePlaying() {
    StarrySky.with().isPlaying().let {
      if (it) {
        StarrySky.with().pauseMusic()
      } else {
        StarrySky.with().restoreMusic()
      }
    }
  }

  fun playPre() {
    StarrySky.with().skipToNext()
  }

  fun playNext() {
    StarrySky.with().skipToNext()
  }

  fun play(song: Song, list: List<Song>) {
    viewModelScope.launch(Dispatchers.IO) {
      StarrySky.with().updatePlayList(list.map {
        it.toSongInfo()
      }.toMutableList())
      withContext(Dispatchers.Main) {
        StarrySky.with().playMusicById(song.mediaStoreId.toString())
      }
      _song.value = song
      _playlist.value = list
    }
  }

  fun play(song: Song) {
    StarrySky.with().addSongInfo(StarrySky.with().getNowPlayingIndex(), song.toSongInfo())
    _song.value = song
  }

  fun seekTo(position: Long) {
    StarrySky.with().seekTo(position)
  }


  override fun onCleared() {
    super.onCleared()
    StarrySky.with().removePlayerEventListener("MusicViewModel")
  }
}