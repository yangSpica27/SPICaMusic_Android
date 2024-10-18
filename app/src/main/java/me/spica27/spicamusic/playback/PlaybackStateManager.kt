package me.spica27.spicamusic.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.music.player.IPlayer
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.dsp.EqualizerAudioProcessor
import me.spica27.spicamusic.dsp.ReplayGainAudioProcessor
import me.spica27.spicamusic.player.FFTAudioProcessor
import me.spica27.spicamusic.player.Queue

@Suppress("unused")
class PlaybackStateManager {

  private var player: IPlayer? = null

  var playerState = IPlayer.State.from(isPlaying = false, isAdvancing = false, 0)
    private set

  private var repeatMode = RepeatMode.ALL

  val fftAudioProcessor = FFTAudioProcessor()

  val equalizerAudioProcessor = EqualizerAudioProcessor(false)

  val replayGainAudioProcessor = ReplayGainAudioProcessor()

  private val queue: Queue = Queue()

  companion object {
    @Volatile
    private var INSTANCE: PlaybackStateManager? = null

    fun getInstance(): PlaybackStateManager {
      val currentInstance = INSTANCE

      if (currentInstance != null) {
        return currentInstance
      }

      synchronized(this) {
        val newInstance = PlaybackStateManager()
        INSTANCE = newInstance
        return newInstance
      }
    }
  }

  private val listeners = mutableListOf<Listener>()

  fun getCurrentSong() = queue.currentSong()

  fun getCurrentList() = queue.getPlayList()

  fun getCurrentListSize() = queue.getPlayList().size

  fun getCurrentSongIndex() = queue.getIndex()

  @Synchronized
  fun addListener(listener: Listener) {
    listeners.add(listener)
  }

  @Synchronized
  fun removeListener(listener: Listener) {
    listeners.remove(listener)
  }


  @Synchronized
  fun registerPlayer(player: IPlayer) {
    if (this.player != null) return
    this.player = player
  }

  @Synchronized
  fun unRegisterPlayer() {
    player = null
  }

  @Synchronized
  fun play(song: Song, list: List<Song>) {
    queue.reloadNewList(song, list)
    player?.loadSong(song, true)
    updateListenersNewList()
  }


  suspend fun playAsync(song: Song, list: List<Song>) =
    withContext(Dispatchers.IO) {
      queue.reloadNewList(song, list)
      withContext(Dispatchers.Main) {
        player?.loadSong(song, true)
        updateListenersNewList()
      }
    }


  suspend fun playAsync(song: Song) = withContext(Dispatchers.IO) {
    queue.reloadNewList(song, queue.getPlayList())
    withContext(Dispatchers.Main) {
      player?.loadSong(song, true)
      updateListenersNewList()
    }
  }

  @Synchronized
  fun play(song: Song) {
    queue.reloadNewList(song, queue.getPlayList())
    player?.loadSong(song, true)
    updateListenersNewList()
  }


  // 下一曲
  fun playNext() {
    if (queue.playNextSong()) {
      player?.loadSong(queue.currentSong(), true)
    } else if (repeatMode == RepeatMode.ALL && queue.getPlayList().isNotEmpty()) {
      queue.reloadNewList(queue.getPlayList().first(), queue.getPlayList())
      player?.loadSong(queue.currentSong(), true)
    }
    updateListenersIndexMove()
  }


  // 上一曲
  fun playPre() {
    if (queue.playPreSong()) {
      player?.loadSong(queue.currentSong(), true)
    } else if (repeatMode == RepeatMode.ALL && queue.getPlayList().isNotEmpty()) {
      queue.reloadNewList(queue.getPlayList().last(), queue.getPlayList())
      player?.loadSong(queue.currentSong(), true)
    }
    updateListenersIndexMove()
  }


  //
  fun seekTo(positionMs: Long) {
    synchronized(this) {
      player?.seekTo(positionMs)
      playerState = IPlayer.State(playerState.isPlaying, positionMs)
      updatePositionChanged()
    }
  }


  fun setPlaying(isPlaying: Boolean) {
    synchronized(this) {
      player?.setPlaying(isPlaying)
    }
  }


  fun synchronizePosition(positionMs: Long) {
    synchronized(this) {
      val maxDuration = queue.currentSong()?.duration ?: -1
      if (positionMs <= maxDuration) {
        playerState = IPlayer.State(playerState.isPlaying, positionMs)
        updatePositionChanged()
      }
    }
  }

  @Synchronized
  fun synchronizeState() {
    player?.let {
      val newState = player?.getState(queue.currentSong()?.duration ?: 0)
      if (newState != null && newState != playerState) {
        playerState = newState
      }
      updateListenersState()
    }

  }

  private fun updateListenersNewList() {
    listeners.forEach {
      it.onNewListLoad(queue)
    }
  }

  private fun updateListenersIndexMove() {
    listeners.forEach {
      it.onIndexMoved(queue)
    }
  }

  private fun updateListenersState() {
    listeners.forEach {
      it.onStateChanged(playerState.isPlaying)
    }
  }

  private fun updatePositionChanged() {
    listeners.forEach {
      it.onPositionChanged(playerState.currentPositionMs)
    }
  }


  interface Listener {
    /**
     * 播放歌曲发生变化
     */
    fun onIndexMoved(queue: Queue) {}


    /**
     * 新的播放队列被载入了
     */
    fun onNewListLoad(queue: Queue) {}

    /**
     * 状态发生变化
     */
    fun onStateChanged(isPlaying: Boolean) {}


    /**
     * 播放位置发生变化
     */
    fun onPositionChanged(positionMs: Long) {}

    /**
     * 循环方式发生变化
     */
    fun onRepeatChanged(repeatMode: RepeatMode) {}
  }

}