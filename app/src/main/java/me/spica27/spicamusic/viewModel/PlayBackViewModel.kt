package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.playback.RepeatMode
import me.spica27.spicamusic.player.Queue
import me.spica27.spicamusic.utils.msToSecs
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlayBackViewModel @Inject constructor(
  private val songDao: SongDao,
  private val playlistDao: PlaylistDao
) : ViewModel(),
  PlaybackStateManager.Listener {

  // 所有歌曲
  val allSongs: Flow<List<Song>> = songDao.getAll()

  // 所有喜欢的歌曲
  val allLikeSongs: Flow<List<Song>> = songDao.getAllLikeSong()

  // 所有歌单
  val allPlayList: Flow<List<Playlist>> = playlistDao.getAllPlaylist()


  // 播放列表
  private val _nowPlayingList = MutableStateFlow(listOf<Song>())

  val playList: Flow<List<Song>>
    get() = _nowPlayingList

  // 当前歌单大小大小
  val nowPlayingListSize: Flow<Int> = playList.map { it.size }


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


  // 是否随机播放
  private val _isShuffled = MutableStateFlow(false)
  val isShuffled: Flow<Boolean>
    get() = _isShuffled


  init {
    Timber.tag("MusicViewModel").d("init")
    PlaybackStateManager.getInstance().addListener(this)
    viewModelScope.launch(Dispatchers.Default) {
      _playingSong.emit(PlaybackStateManager.getInstance().getCurrentSong())
      _nowPlayingList.emit(PlaybackStateManager.getInstance().getCurrentList())
      _isPlaying.emit(PlaybackStateManager.getInstance().playerState.isPlaying)
      _playlistCurrentIndex.emit(PlaybackStateManager.getInstance().getCurrentSongIndex())
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

  // 喜欢/不喜欢歌曲
  fun toggleLike(song: Song) {
    viewModelScope.launch {
      songDao.toggleLike(song.songId ?: -1)
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
    viewModelScope.launch {
      _playingSong.emit(queue.currentSong())
      _playlistCurrentIndex.emit(queue.getIndex())
    }
  }

  override fun onNewListLoad(queue: Queue) {
    super.onNewListLoad(queue)
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

  fun addPlayList(value: String) {
    viewModelScope.launch {
      playlistDao.insertPlaylist(Playlist(playlistName = value))
    }
  }
}