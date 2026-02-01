package me.spica27.spicamusic.player.impl

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.player.api.IFFTProcessor
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.player.impl.dsp.FFTAudioProcessor
import me.spica27.spicamusic.player.impl.dsp.FFTAudioProcessorWrapper
import me.spica27.spicamusic.player.impl.utils.MediaLibrary
import me.spica27.spicamusic.player.impl.utils.PlayerKVUtils
import me.spica27.spicamusic.player.impl.utils.toMediaItem
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
  private val context: Context,
  private val playbackServiceClass: Class<*>,
) : IMusicPlayer, CoroutineScope, Player.Listener {

  private val TAG = "SpicaPlayer"

  private val playerKVUtils = getKoin().get<PlayerKVUtils>()

  override val coroutineContext: CoroutineContext = Dispatchers.IO

  private val sessionToken by lazy {
    SessionToken(context, ComponentName(context, playbackServiceClass))
  }

  // FFT 音频处理器
  private val _fftProcessor = FFTAudioProcessor()

  override val fftProcessor: IFFTProcessor = _fftProcessor

  // FFT AudioProcessor 包装器 (用于 ExoPlayer)
  private val _fftAudioProcessorWrapper = FFTAudioProcessorWrapper(_fftProcessor)
  override val fftAudioProcessor: AudioProcessor =
    _fftAudioProcessorWrapper

  private var browserInstance: MediaBrowser? = null
  private val browserFuture by lazy {
    MediaBrowser.Builder(context, sessionToken).buildAsync()
  }

  override val playMode: StateFlow<PlayMode> = playerKVUtils.getPlayModeFlow()
    .stateIn(this, kotlinx.coroutines.flow.SharingStarted.Eagerly, PlayMode.LOOP)

  private val _pauseWhenCompletion = MutableStateFlow(false)
  override val pauseWhenCompletion: StateFlow<Boolean> = _pauseWhenCompletion

  private val _isPlaying = MutableStateFlow(false)
  override val isPlaying: StateFlow<Boolean> = _isPlaying

  private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
  override val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem

  private val _currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
  override val currentMediaMetadata: StateFlow<MediaMetadata?> = _currentMediaMetadata

  private val _currentPlaylistMetadata = MutableStateFlow<MediaMetadata?>(null)
  override val currentPlaylistMetadata: StateFlow<MediaMetadata?> = _currentPlaylistMetadata

  private val _currentDuration = MutableStateFlow(0L)
  override val currentDuration: StateFlow<Long> = _currentDuration

  private val _currentTimelineItems = MutableStateFlow<List<MediaItem>>(emptyList())
  override val currentTimelineItems: StateFlow<List<MediaItem>> = _currentTimelineItems

  override val currentPosition: Long
    get() = runCatching { if (browserFuture.isDone) browserFuture.get()?.currentPosition else null }.getOrNull()
      ?: 0L

  override fun isItemPlaying(mediaId: String): Boolean {
    if (!_isPlaying.value) return false
    return _currentMediaItem.value?.mediaId == mediaId
  }

  init {
    init()
  }

  override fun init() {
    if (browserInstance != null) return
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
      
      // 恢复上次的播放模式
      val savedPlayMode = PlayMode.from(playerKVUtils.getPlayMode())
      applyPlayMode(browser, savedPlayMode)
      
      browser.playWhenReady = false
      browser.setMediaItems(items)
      browser.prepare()
    }
  }

  override fun doAction(action: PlayerAction) {
    launch(Dispatchers.Main) {
      try {
        val browser = browserFuture.await()
        Timber.d("doAction: ${action.javaClass.simpleName}")
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
          Timber.tag(TAG).d("PlayById: mediaId=${action.mediaId}")
          val index = browser.currentTimeline.indexOf(action.mediaId)
          Timber.tag(TAG)
            .d("Current timeline index: $index, timeline size: ${browser.currentTimeline.windowCount}")

          if (index == -1) {
            // 不在播放列表中，添加并播放
            launch(Dispatchers.IO) {
              val item = async { MediaLibrary.getItem(action.mediaId) }.await()

              withContext(Dispatchers.Main) {
                if (item != null) {
                  Timber.tag(TAG).d("Item not in playlist, adding and playing: ${item.mediaId}")
                  val currentIndex = browser.currentMediaItemIndex
                  val toIndex = if (currentIndex == -1) 0 else currentIndex + 1
                  browser.addMediaItem(toIndex, item)
                  browser.prepare()
                  browser.seekTo(toIndex, 0)
                  browser.playWhenReady = true
                  Timber.tag(TAG).d("Seeking to new item at index: $toIndex, playWhenReady=true")
                } else {
                  Timber.tag(TAG)
                    .w("Item with mediaId=${action.mediaId} not found in media library")
                }
              }
            }
          } else {
            Timber.tag(TAG).d("Item already in playlist, seeking to index: $index")
            browser.seekTo(index, 0)
            browser.playWhenReady = true
            Timber.tag(TAG)
              .d("Play() called on existing item, playWhenReady=${browser.playWhenReady}")
          }
        }

        is PlayerAction.SeekTo -> {
          browser.seekTo(action.positionMs)
        }

        is PlayerAction.PauseWhenCompletion -> {
          _pauseWhenCompletion.value = !action.cancel
        }

        is PlayerAction.SetPlayMode -> {
          // 保存到 KV 存储
          playerKVUtils.setPlayMode(action.playMode.name)
          // 应用播放模式到播放器
          applyPlayMode(browser, action.playMode)
          Timber.tag(TAG).d("Play mode applied: ${action.playMode}")
        }

        is PlayerAction.AddToNext -> {
          launch(Dispatchers.IO) {
            val item = MediaLibrary.getItem(action.mediaId)

            withContext(Dispatchers.Main) {
              if (item == null) {
                Timber.tag(TAG).w("Item with mediaId=${action.mediaId} not found for AddToNext")
                return@withContext
              }

              // 处理空播放列表的情况
              val currentIndex = browser.currentMediaItemIndex.coerceAtLeast(0)
              val index = browser.currentTimeline.indexOf(action.mediaId)

              if (index != -1) {
                val offset = if (index > currentIndex) 1 else 0
                browser.moveMediaItem(index, currentIndex + offset)
              } else {
                browser.addMediaItem(currentIndex + 1, item)
              }
            }
          }
        }

        is PlayerAction.UpdateList -> {
          launch(Dispatchers.IO) {
            val index = action.mediaId?.let { action.mediaIds.indexOf(it) }?.takeIf { it >= 0 } ?: 0
            val items = MediaLibrary.mediaIdToMediaItems(action.mediaIds)
            withContext(Dispatchers.Main) {
              browser.setMediaItems(items, index, 0)
              if (action.start) {
                browser.play()
              }
            }
          }
        }

        PlayerAction.ReloadAndPlay -> {
          Timber.tag(TAG).w("ReloadAndPlay not implemented yet")
          // TODO: 实现重新加载并播放逻辑
        }
      }
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, "Error executing action: ${action.javaClass.simpleName}")
    }
  }
  }

  override fun release() {
    browserInstance?.release()
    browserInstance = null
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    this@SpicaPlayer._isPlaying.value = isPlaying
  }

  override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
    super.onShuffleModeEnabledChanged(shuffleModeEnabled)
    Timber.e("onShuffleModeEnabledChanged $shuffleModeEnabled")
  }

  override fun onMediaItemTransition(
    mediaItem: MediaItem?,
    reason: Int,
  ) {
    Timber.e("onMediaItemTransition $mediaItem $reason")
    _currentMediaItem.value = mediaItem
    if (_pauseWhenCompletion.value) {
      browserInstance?.pause()
      _pauseWhenCompletion.value = false
    }
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    Timber.e("onMediaMetadataChanged $mediaMetadata")
    _currentMediaItem.value = browserInstance?.currentMediaItem
    _currentMediaMetadata.value = mediaMetadata
    _currentDuration.value = mediaMetadata.durationMs ?: browserInstance?.duration ?: 0L
    // TODO 此处获取到的duration仍然可能是上一首歌曲的时长
  }

  override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
    _currentPlaylistMetadata.value = mediaMetadata
  }

  override fun onTimelineChanged(
    timeline: Timeline,
    @Player.TimelineChangeReason reason: Int,
  ) {
    updateItems(timeline)
    Timber.e("onTimelineChanged 切换原因 $reason")
  }

  companion object {
    private const val TAG = "SpicaPlayer"

    fun createModule(playbackServiceClass: Class<*>) = module {
      single<PlayerKVUtils> { PlayerKVUtils(androidApplication()) }
      single<IMusicPlayer> { SpicaPlayer(androidApplication(), playbackServiceClass) }
    }
  }

  private fun updateItems(timeline: Timeline?) {
    val items = timeline?.toMediaItems() ?: emptyList()
    _currentTimelineItems.value = items

    val ids = _currentTimelineItems.value.map { it.mediaId }
    playerKVUtils.setHistoryIds(ids.mapNotNull { it.toLongOrNull() })
  }

  private fun Timeline.toMediaItems(): List<MediaItem> =
    (0 until this.windowCount).mapNotNull { this.getWindow(it, Timeline.Window()).mediaItem }

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

  /**
   * 应用播放模式到 MediaBrowser
   */
  private fun applyPlayMode(browser: MediaBrowser, mode: PlayMode) {
    when (mode) {
      PlayMode.LOOP -> {
        browser.repeatMode = Player.REPEAT_MODE_ALL
        browser.shuffleModeEnabled = false
      }
      PlayMode.LIST -> {
        browser.repeatMode = Player.REPEAT_MODE_ONE
        browser.shuffleModeEnabled = false
      }
      PlayMode.SHUFFLE -> {
        browser.repeatMode = Player.REPEAT_MODE_ALL
        browser.shuffleModeEnabled = true
      }
    }
  }
}
