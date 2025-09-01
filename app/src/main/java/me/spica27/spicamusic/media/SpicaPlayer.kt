package me.spica27.spicamusic.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.dsp.EqualizerAudioProcessor
import me.spica27.spicamusic.dsp.ReplayGainAudioProcessor
import me.spica27.spicamusic.media.action.Action
import me.spica27.spicamusic.media.action.PlayerAction
import me.spica27.spicamusic.media.common.PlayMode
import me.spica27.spicamusic.media.common.playMode
import me.spica27.spicamusic.media.service.PlaybackService
import me.spica27.spicamusic.media.utils.MediaLibrary
import me.spica27.spicamusic.media.utils.PlayerKVUtils
import me.spica27.spicamusic.utils.msToSecs
import me.spica27.spicamusic.visualiser.FFTAudioProcessor
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import timber.log.Timber
import kotlin.coroutines.CoroutineContext

/**
 * 音乐播放器控制类
 */
@UnstableApi
class SpicaPlayer(
  private val context: Context
) : CoroutineScope, Player.Listener {

  private val playerKVUtils = getKoin().get<PlayerKVUtils>()

  override val coroutineContext: CoroutineContext = Dispatchers.IO

  private val sessionToken by lazy {
    SessionToken(context, ComponentName(context, PlaybackService::class.java))
  }

  private var browserInstance: MediaBrowser? = null
  private val browserFuture by lazy {
    MediaBrowser
      .Builder(context, sessionToken)
      .buildAsync()
  }

  private val _pauseWhenCompletion = MutableStateFlow(false)
  val pauseWhenCompletion: StateFlow<Boolean> = _pauseWhenCompletion
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying: StateFlow<Boolean> = _isPlaying
  private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
  val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem
  private val _currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
  val currentMediaMetadata: StateFlow<MediaMetadata?> = _currentMediaMetadata
  private val _currentPlaylistMetadata = MutableStateFlow<MediaMetadata?>(null)
  val currentPlaylistMetadata: StateFlow<MediaMetadata?> = _currentPlaylistMetadata
  private val _currentDuration = MutableStateFlow(0L)
  val currentDuration: StateFlow<Long> = _currentDuration
  private val _currentTimelineItems = MutableStateFlow<List<MediaItem>>(emptyList())
  val currentTimelineItems: StateFlow<List<MediaItem>> = _currentTimelineItems

  val currentPosition: Long
    get() = runCatching { if (browserFuture.isDone) browserFuture.get()?.currentPosition?.msToSecs() else null }
      .getOrNull() ?: 0L

  fun isItemPlaying(mediaId: String): Boolean {
    if (!_isPlaying.value) return false
    return _currentMediaItem.value?.mediaId == mediaId
  }

  internal fun init() {
    if (browserInstance!=null)return
    launch(Dispatchers.Main) {
      val browser = browserFuture.await()
      browserInstance = browser
      browser.addListener(this@SpicaPlayer)

      val items = withContext(Dispatchers.IO) {
        playerKVUtils.getHistoryItems().map { it.toMediaItem() }
      }

      if (items.isEmpty()) {
        Timber.e("No songs found")
        return@launch
      }

      browser.playMode = PlayMode.LOOP
      browser.playWhenReady = false
      browser.setMediaItems(items)
      browser.prepare()
    }
  }

  fun doAction(action: Action) = launch(Dispatchers.Main) {
    val browser = browserFuture.await()
    Timber.e("doACTION = ${action.javaClass.name}")
    when (action) {
      PlayerAction.Play -> browser.play()
      PlayerAction.Pause -> browser.pause()

      PlayerAction.SkipToNext -> {
        browser.seekToNext()
      }

      PlayerAction.SkipToPrevious -> {
        browser.seekToPrevious()
      }

      is PlayerAction.RemoveWithMediaId -> {
        val index = browser.currentTimeline.indexOf(action.mediaId)
        if (index != -1) {
          browser.removeMediaItem(index)
        }
      }

      PlayerAction.PlayOrPause -> {
        if (browser.isPlaying) {
          browser.pause()
        } else {
          browser.play()
        }
      }

      is PlayerAction.PlayById -> {
        val index = browser.currentTimeline.indexOf(action.mediaId)
        if (index == -1) {
          val currentIndex = browser.currentMediaItemIndex
          if (browser.isPlaying) {
            browser.pause()
          }
          val item = browser.getItem(action.mediaId)
            .await().value ?: return@launch

          browser.addMediaItem(currentIndex + 1, item)
          browser.seekTo(currentIndex + 1, 0)
          browser.prepare()
          browser.play()
        } else {
          browser.seekTo(index, 0)
          browser.play()
        }
      }

      is PlayerAction.SeekTo -> {
        browser.seekTo(action.positionMs)
      }

      is PlayerAction.CustomAction -> {}
      is PlayerAction.PauseWhenCompletion -> {
        _pauseWhenCompletion.value = !action.cancel
      }

      is PlayerAction.SetPlayMode -> {
        browser.playMode = action.playMode
      }

      is PlayerAction.AddToNext -> {
        val item = browser.getItem(action.mediaId).await().value ?: return@launch
        val index = browser.currentTimeline.indexOf(action.mediaId)

        if (index != -1) {
          val offset = if (index > browser.currentMediaItemIndex) 1 else 0
          browser.moveMediaItem(index, browser.currentMediaItemIndex + offset)
        } else {
          browser.addMediaItem(browser.currentMediaItemIndex + 1, item)
        }
      }

      is PlayerAction.UpdateList -> {
        launch(Dispatchers.IO) {
          val index = action.mediaId?.let { action.mediaIds.indexOf(it) }
            ?.takeIf { it >= 0 }
            ?: 0
          val items = MediaLibrary.mediaIdToMediaItems(action.mediaIds)
          withContext(Dispatchers.Main) {
            browser.setMediaItems(items, index, 0)
            if (action.start) {
              browser.play()
            }
          }
        }
      }
    }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    this@SpicaPlayer._isPlaying.value = isPlaying
  }

  override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
    Timber.e("onMediaItemTransition ${mediaItem.toString()} ${reason}")
    _currentMediaItem.value = mediaItem
    if (_pauseWhenCompletion.value) {
      browserInstance?.pause()
      _pauseWhenCompletion.value = false
    }
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    Timber.e("onMediaMetadataChanged ${mediaMetadata}")
    _currentMediaItem.value = browserInstance?.currentMediaItem
    _currentMediaMetadata.value = mediaMetadata
    _currentDuration.value = mediaMetadata.durationMs ?: browserInstance?.duration ?: 0L
    // TODO 此处获取到的duration仍然可能是上一首歌曲的时长
  }

  override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
    _currentPlaylistMetadata.value = mediaMetadata
  }

  override fun onTimelineChanged(timeline: Timeline, @Player.TimelineChangeReason reason: Int) {
    updateItems(timeline)
    Timber.e("onTimelineChanged 切换原因 ${reason}")
  }

  companion object {
    val module = module {
      single<PlayerKVUtils> { PlayerKVUtils(androidApplication()) }
      single<SpicaPlayer> { SpicaPlayer(androidApplication()) }
      single<FFTAudioProcessor> { FFTAudioProcessor() }
      single<ReplayGainAudioProcessor> { ReplayGainAudioProcessor() }
      single<EqualizerAudioProcessor> { EqualizerAudioProcessor() }
    }
  }

  private fun updateItems(
    timeline: Timeline?
  ) {
    val items = timeline?.toMediaItems() ?: emptyList()
    _currentTimelineItems.value = items

    val ids = _currentTimelineItems.value.map { it.mediaId }
    playerKVUtils.setHistoryIds(ids.mapNotNull { it.toLongOrNull() })
  }

  private fun Timeline.toMediaItems(): List<MediaItem> {
    return (0 until this.windowCount)
      .mapNotNull { this.getWindow(it, Timeline.Window()).mediaItem }
  }

  private fun Timeline.indexOf(mediaId: String): Int {
    var index = -1
    (0 until this.windowCount).forEach {
      if (this.getWindow(it, Timeline.Window()).mediaItem.mediaId == mediaId) {
        index = it
        return@forEach
      }
    }
    return index
  }
}



