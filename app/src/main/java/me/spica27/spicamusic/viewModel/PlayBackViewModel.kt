package me.spica27.spicamusic.viewModel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import linc.com.amplituda.Amplituda
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.playback.RepeatMode
import me.spica27.spicamusic.player.Queue
import me.spica27.spicamusic.utils.contentResolverSafe
import me.spica27.spicamusic.utils.msToSecs
import timber.log.Timber
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayBackViewModel @OptIn(UnstableApi::class)
@Inject constructor(
  private val songDao: SongDao,
  private val amplituda: Amplituda
) : ViewModel(),
  PlaybackStateManager.Listener {


  // 播放列表
  private val _nowPlayingList = MutableStateFlow(listOf<Song>())

  val playList: Flow<List<Song>>
    get() = _nowPlayingList

  // 当前歌单大小大小
  val nowPlayingListSize: Flow<Int> = playList.map {
    it.size
  }.distinctUntilChanged()


  // 当前播放的乐曲
  private val _playingSong = MutableStateFlow<Song?>(null)


  val currentSongFlow: Flow<Song?>
    get() = _playingSong


  // 是否正在播放
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: Flow<Boolean>
    get() = _isPlaying

  // 当前的进度
  private val _positionSec = MutableStateFlow(0L)

  val positionSec: Flow<Long>
    get() = _positionSec

  private val _playlistCurrentIndex = MutableStateFlow(0)

  val playlistCurrentIndex: Flow<Int>
    get() = _playlistCurrentIndex


  // 循环模式
  private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
  val repeatMode: Flow<RepeatMode>
    get() = _repeatMode


  // 正在播放的歌曲的振幅
  private val _playingSongAmplitudes = MutableStateFlow(listOf<Int>())

  val playingSongAmplitudes: Flow<List<Int>>
    get() = _playingSongAmplitudes


  // 是否随机播放
  private val _isShuffled = MutableStateFlow(false)
  val isShuffled: Flow<Boolean>
    get() = _isShuffled


  init {
    Timber.tag("MusicViewModel").d("init")
    PlaybackStateManager.getInstance().addListener(this)
    viewModelScope.launch(Dispatchers.Default) {
      Timber.tag("MusicViewModel").d("init")
      _playingSong.emit(PlaybackStateManager.getInstance().getCurrentSong())
      _nowPlayingList.emit(PlaybackStateManager.getInstance().getCurrentList())
      _isPlaying.emit(PlaybackStateManager.getInstance().playerState.isPlaying)
      _playlistCurrentIndex.emit(PlaybackStateManager.getInstance().getCurrentSongIndex())
    }
    viewModelScope.launch(Dispatchers.Default) {
      _playingSong.collectLatest { song ->
        if (song?.getSongUri() != null) {
          val inputStream = App.getInstance().contentResolverSafe.openInputStream(song.getSongUri())
          if (inputStream != null) {
            val amplitudes = amplituda.processAudio(inputStream)
            _playingSongAmplitudes.emit(amplitudes.get().amplitudesAsList())
            withContext(Dispatchers.IO) {
              inputStream.close()
            }
          } else {
            _playingSongAmplitudes.emit(listOf())
          }
        }
      }
    }
  }


  override fun onCleared() {
    super.onCleared()
    PlaybackStateManager.getInstance().removeListener(this)
  }


  fun togglePlaying() {
    PlaybackStateManager.getInstance()
      .setPlaying(!PlaybackStateManager.getInstance().playerState.isPlaying)
  }

  fun playPre() {
    PlaybackStateManager.getInstance().playPre()
  }

  fun playNext() {
    PlaybackStateManager.getInstance().playNext()
  }

  // 收藏/不收藏歌曲
  fun toggleLike(song: Song) {
    viewModelScope.launch {
      songDao.toggleLike(song.songId ?: -1)
      _playingSong.emit(
        song.copy(
          like = !song.like
        )
      )
    }
  }

  fun play(song: Song, list: List<Song>) {
    viewModelScope.launch(Dispatchers.Default) {
      PlaybackStateManager.getInstance().playAsync(song, list)
      songDao.addPlayTime(song.songId ?: -1)
      _playingSong.emit(song)
      _nowPlayingList.emit(list)
    }

  }

  fun play(song: Song) {
    viewModelScope.launch(Dispatchers.Default) {
      PlaybackStateManager.getInstance().playAsync(song)
      _playingSong.emit(song)
    }
  }

  fun seekTo(position: Long) {
    PlaybackStateManager.getInstance().seekTo(position)
  }


  override fun onIndexMoved(queue: Queue) {
    super.onIndexMoved(queue)
    Timber.tag("MusicViewModel").d("onIndexMoved")
    viewModelScope.launch {
      _playingSong.emit(queue.currentSong())
      _playlistCurrentIndex.emit(queue.getIndex())
    }
  }

  override fun onNewListLoad(queue: Queue) {
    super.onNewListLoad(queue)
    Timber.tag("MusicViewModel").d("onNewListLoad")
    viewModelScope.launch {
      _nowPlayingList.emit(queue.getPlayList())
      _playingSong.emit(queue.currentSong())
    }
  }

  override fun onPositionChanged(positionMs: Long) {
    super.onPositionChanged(positionMs)
    _positionSec.value = positionMs.msToSecs()
  }

  override fun onStateChanged(isPlaying: Boolean) {
    super.onStateChanged(isPlaying)
    _isPlaying.value = isPlaying
  }

  override fun onRepeatChanged(repeatMode: RepeatMode) {
    super.onRepeatChanged(repeatMode)
    _repeatMode.value = repeatMode
  }

}