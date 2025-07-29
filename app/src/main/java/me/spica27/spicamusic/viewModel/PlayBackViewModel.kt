package me.spica27.spicamusic.viewModel

import android.widget.Toast
import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.linc.amplituda.Amplituda
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.lyric.LrcParser
import me.spica27.spicamusic.lyric.LyricItem
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.playback.RepeatMode
import me.spica27.spicamusic.player.Queue
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.utils.toast
import timber.log.Timber
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class PlayBackViewModel @OptIn(UnstableApi::class)
@Inject constructor(
  private val songDao: SongDao,
  private val amplituda: Amplituda,
  private val lyricDao: LyricDao
) : ViewModel(),
  PlaybackStateManager.Listener {


  // 播放列表
  private val _nowPlayingList = MutableStateFlow(listOf<Song>())

  fun getAmplituda(): Amplituda {
    return amplituda
  }

  val playList: StateFlow<List<Song>>
    get() = _nowPlayingList

  // 当前歌单大小大小
  val nowPlayingListSize: StateFlow<Int> = playList.map {
    it.size
  }
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
      initialValue = 0
    )


  // 当前播放的乐曲
  private val _playingSong = MutableStateFlow<Song?>(null)


  val currentSongFlow: StateFlow<Song?>
    get() = _playingSong

  /**
   * 当前的歌词
   */
  private val _currentLyric = MutableStateFlow(emptyList<LyricItem>())

  val currentLyric: StateFlow<List<LyricItem>>
    get() = _currentLyric

  // 是否正在播放
  private val _isPlaying = MutableStateFlow(false)

  @kotlin.OptIn(FlowPreview::class)
  val isPlaying: SharedFlow<Boolean>
    get() = _isPlaying
      .debounce(250)
      .flowOn(Dispatchers.Default)
      .shareIn(viewModelScope, SharingStarted.Lazily, 1)

  // 当前的进度
  private val _positionSec = MutableStateFlow(0L)

  val positionSec: StateFlow<Long>
    get() = _positionSec

  private val _playlistCurrentIndex = MutableStateFlow(0)

  val playlistCurrentIndex: StateFlow<Int>
    get() = _playlistCurrentIndex


  // 循环模式
  private val _repeatMode = MutableStateFlow(RepeatMode.NONE)
  val repeatMode: Flow<RepeatMode>
    get() = _repeatMode


  // 是否随机播放
  private val _isShuffled = MutableStateFlow(false)
  val isShuffled: Flow<Boolean>
    get() = _isShuffled

  private var songCollectJob: Job? = null

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
    viewModelScope.launch(
      Dispatchers.Default +
          CoroutineExceptionHandler { _, throwable ->
            run {
              Timber.tag("获取波形图错误").e(throwable)
              if (throwable is NoSuchFileException) {
                App.getInstance().toast("未找到音频文件", Toast.LENGTH_SHORT)
                viewModelScope.launch {
                  PlaybackStateManager.getInstance().getCurrentSong()?.let {
                    // 从播放列表删除当前歌曲
                    PlaybackStateManager.getInstance()
                      .removeSong(PlaybackStateManager.getInstance().getCurrentSongIndex())
                    // 从数据库删除当前歌曲
                    songDao.delete(it)
                    // 如果播放列表还有歌曲，播放下一首
                    if (PlaybackStateManager.getInstance().getCurrentList().size > 1) {
                      PlaybackStateManager.getInstance().playNext()
                    } else {
                      // 播放列表没有歌曲，停止播放
                      PlaybackStateManager.getInstance().setPlaying(false)
                    }
                  }
                }
              }
            }
          },

      ) {
      // 播放歌曲时，获取歌曲的振幅
      songCollectJob = _playingSong.collect {
        _playingSong.value?.let { song ->
          Timber.tag("MusicViewModel").e("搜索歌词 songId = ${song.mediaStoreId}")
          val lyric = lyricDao.getLyricWithSongId(song.mediaStoreId)
          if (lyric != null) {
            val p: List<LyricItem> = LrcParser.parse(lyric.lyrics)
            _currentLyric.value = (p)
          }else{
            Timber.tag("MusicViewModel").e("lyric is null")
            _currentLyric.value = emptyList()
          }
        }
      }
    }
    viewModelScope.launch(Dispatchers.IO) {
      lyricDao.getLyrics().collectLatest {
        _playingSong.value?.let { song ->

          val lyric = lyricDao.getLyricWithSongId(song.mediaStoreId)
          if (lyric != null) {
            val p: List<LyricItem> = LrcParser.parse(lyric.lyrics)
            _currentLyric.value = (p)
          }else{
            Timber.tag("MusicViewModel").e("lyric is null")
            _currentLyric.value = emptyList()
          }
        }
      }
    }

  }


  override fun onCleared() {
    songCollectJob?.cancel()
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

  fun removeSong(index: Int) {
    PlaybackStateManager.getInstance().removeSong(index)
  }


}