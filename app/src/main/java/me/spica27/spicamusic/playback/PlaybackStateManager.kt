package me.spica27.spicamusic.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import me.spica.music.player.IPlayer
import me.spica.music.player.Queue
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.dsp.EqualizerAudioProcessor
import me.spica27.spicamusic.dsp.ReplayGainAudioProcessor
import me.spica27.spicamusic.player.FFTAudioProcessor


class PlaybackStateManager {

  private var player: IPlayer? = null

  var playerState = IPlayer.State.from(isPlaying = false, isAdvancing = false, 0)
    private set


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

  fun getCurrentListSize() = queue.heap.size

  fun getCurrentSongIndex() = queue.index

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
    queue.reloadNewList(song, queue.heap.toList())
    withContext(Dispatchers.Main) {
      player?.loadSong(song, true)
      updateListenersNewList()
    }
  }

  @Synchronized
  fun play(song: Song) {
    queue.reloadNewList(song, queue.heap.toList())
    player?.loadSong(song, true)
    updateListenersNewList()
  }


  // 下一曲
  fun playNext() {
    if (queue.playNextSong()) {
      player?.loadSong(queue.currentSong(), true)
    }
    updateListenersIndexMove()
  }


  // 上一曲
  fun playPre() {
    queue.playPreSong()
    player?.loadSong(queue.currentSong(), true)
    updateListenersIndexMove()
  }


  //
  fun seekTo(positionMs: Long) {
    player?.seekTo(positionMs)
  }


  fun setPlaying(isPlaying: Boolean) {
    player?.setPlaying(isPlaying)
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
     * 循环方式发生变化
     */
    fun onRepeatChanged(repeatMode: RepeatMode) {}
  }

}