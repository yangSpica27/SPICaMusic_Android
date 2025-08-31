package me.spica27.spicamusic.viewModel

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.linc.amplituda.Amplituda
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.PlayHistory
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.media.SpicaPlayer
import me.spica27.spicamusic.media.action.MediaControl
import me.spica27.spicamusic.media.action.PlayerAction
import me.spica27.spicamusic.repository.PlayHistoryRepository
import me.spica27.spicamusic.repository.PlaylistRepository
import timber.log.Timber

@OptIn(UnstableApi::class)
class PlayBackViewModel(
  private val songDao: SongDao,
  private val amplituda: Amplituda,
  private val playlistRepository: PlaylistRepository,
  val player: SpicaPlayer,
  private val playHistoryRepository: PlayHistoryRepository
) : ViewModel() {


  fun getAmplituda(): Amplituda {
    return amplituda
  }

  val playList: StateFlow<List<Song>>
    get() = player.currentTimelineItems
      .mapNotNull {
        it.mapNotNull { item ->
          songDao.getSongWithMediaStoreId(item.mediaId.toLongOrNull() ?: -1)
        }
      }.flowOn(Dispatchers.IO)
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  // 当前歌单大小大小
  val nowPlayingListSize: StateFlow<Int> = playList.map {
    it.size
  }
    .distinctUntilChanged()
    .flowOn(Dispatchers.IO)
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
      initialValue = 0
    )


  val currentSongFlow: StateFlow<Song?> = player.currentMediaItem.map {
    songDao.getSongWithMediaStoreId(it?.mediaId?.toLongOrNull() ?: -1)
  }
    .flowOn(Dispatchers.IO)
    .distinctUntilChanged()
    .stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
      initialValue = null
    )


  @kotlin.OptIn(FlowPreview::class)
  val isPlaying: SharedFlow<Boolean>
    get() = player.isPlaying
      .debounce(250)
      .distinctUntilChanged()
      .flowOn(Dispatchers.Default)
      .shareIn(viewModelScope, SharingStarted.Eagerly, 1)

  // 当前的进度
  private val _positionSec = flow {
    while (currentCoroutineContext().isActive) {
      emit(player.currentPosition)
      delay(1000)
    }
  }

  val positionSec: StateFlow<Long>
    get() = _positionSec.distinctUntilChanged()
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 0
      )

  private val _playlistCurrentIndex = combine(
    playList,
    currentSongFlow
  ) { list, song ->
    list.indexOf(song)
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
    initialValue = 0
  )

  val playlistCurrentIndex: StateFlow<Int>
    get() = _playlistCurrentIndex


  // 是否随机播放
  private val _isShuffled = MutableStateFlow(false)
  val isShuffled: Flow<Boolean>
    get() = _isShuffled


  init {
    Timber.tag("MusicViewModel").d("init")
    viewModelScope.launch(Dispatchers.IO) {
      currentSongFlow.collectLatest {
        playHistoryRepository.insertPlayHistory(
          PlayHistory(
            mediaId = it?.songId ?: -1,
            title = it?.displayName ?: "",
            artist = it?.artist ?: "",
          )
        )
      }
    }
  }


  fun createPlaylistWithSongs(name: String, list: List<Song>) {
    viewModelScope.launch {
      playlistRepository.createPlaylistWithSongs(name, list)
    }
  }


  fun togglePlaying() {
    player.doAction(
      PlayerAction.PlayOrPause
    )
  }

  fun playPre() {
    player.doAction(
      PlayerAction.SkipToPrevious
    )
  }

  fun playNext() {
    player.doAction(
      PlayerAction.SkipToNext
    )
  }

  // 收藏/不收藏歌曲
  fun toggleLike(song: Song) {
    viewModelScope.launch {
      songDao.toggleLike(song.songId ?: -1)
    }
  }

  fun play(song: Song, list: List<Song>) {
    MediaControl.playWithList(
      mediaIds = list.map { it.mediaStoreId.toString() },
      mediaId = song.mediaStoreId.toString(),
      start = true
    )
  }

  fun play(song: Song) {
    viewModelScope.launch(Dispatchers.Default) {
      PlayerAction.PlayById(mediaId = song.mediaStoreId.toString())
        .action()
    }
  }

  fun removeSong(song: Song) {
    player.doAction(PlayerAction.RemoveWithMediaId(song.mediaStoreId.toString()))
  }

  fun clear() {
    player.doAction(
      PlayerAction.UpdateList(
        mediaIds = emptyList()
      )
    )
  }

  fun seekTo(position: Long) {
    player.doAction(PlayerAction.SeekTo(position))
  }


}