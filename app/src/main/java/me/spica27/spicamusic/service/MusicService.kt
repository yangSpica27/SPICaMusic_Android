package me.spica27.spicamusic.service


import android.app.Service.START_NOT_STICKY
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.flac.LibflacAudioRenderer
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.dsp.FadeTransitionRenderersFactory
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.player.IPlayer
import me.spica27.spicamusic.service.notification.MediaSessionComponent
import me.spica27.spicamusic.service.notification.NotificationComponent
import me.spica27.spicamusic.utils.DataStoreUtil

private const val MY_MEDIA_ROOT_ID = "media_root_id"
private const val MY_EMPTY_MEDIA_ROOT_ID = "empty_root_id"


/**
 * 音乐播放服务
 */
@OptIn(UnstableApi::class)
class MusicService : MediaBrowserServiceCompat(), Player.Listener, IPlayer,
  MediaSessionComponent.Listener {

  private var mediaSession: MediaSessionCompat? = null


  private val fftAudioProcessor = PlaybackStateManager.getInstance().fftAudioProcessor

  private lateinit var foregroundManager: ForegroundManager

  private var hasPlayed = false

  private lateinit var exoPlayer: ExoPlayer

  private lateinit var mediaSessionComponent: MediaSessionComponent

  private val serviceJob = Job()
  private val coroutineScope = CoroutineScope(serviceJob + Dispatchers.Main)

  private val systemReceiver = PlaybackReceiver()

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate() {
    super.onCreate()
    mediaSession = MediaSessionCompat(this, "spica_music")

    // 避免降采样 采取最近似的采样
    val extractorsFactory = DefaultMediaSourceFactory(
      this,
      DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setConstantBitrateSeekingAlwaysEnabled(true)
    )

//    val audioRenderer = RenderersFactory { handler, _, audioListener, _, _ ->
//      arrayOf(
//        MediaCodecAudioRenderer(
//          this,
//          MediaCodecSelector.DEFAULT,
//          handler,
//          audioListener,
//          AudioCapabilities.DEFAULT_AUDIO_CAPABILITIES,
//          fftAudioProcessor,
//          PlaybackStateManager.getInstance().equalizerAudioProcessor,
//          PlaybackStateManager.getInstance().replayGainAudioProcessor
//        ),
//        LibflacAudioRenderer(
//          handler, audioListener, fftAudioProcessor,
//          PlaybackStateManager.getInstance().equalizerAudioProcessor,
//          PlaybackStateManager.getInstance().replayGainAudioProcessor
//        ),
//      )

    val audioRenderer = FadeTransitionRenderersFactory(
      this,
      coroutineScope,
      extraAudioProcessors = listOf(
        fftAudioProcessor,
        PlaybackStateManager.getInstance().equalizerAudioProcessor,
        PlaybackStateManager.getInstance().replayGainAudioProcessor
      )
    )


    exoPlayer = ExoPlayer.Builder(this, audioRenderer)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .setMediaSourceFactory(extractorsFactory)
      .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(), true
      )
      .setUsePlatformDiagnostics(false).build()
      .also {
        it.addListener(this)
      }

    PlaybackStateManager.getInstance().registerPlayer(this)
    foregroundManager = ForegroundManager(this)
    mediaSessionComponent = MediaSessionComponent(this, this)
    registerReceiver(
      systemReceiver, IntentFilter().apply {
        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        addAction(AudioManager.ACTION_HEADSET_PLUG)
        addAction(ACTION_INC_REPEAT_MODE)
        addAction(ACTION_INVERT_SHUFFLE)
        addAction(ACTION_SKIP_PREV)
        addAction(ACTION_PLAY_PAUSE)
        addAction(ACTION_SKIP_NEXT)
        addAction(ACTION_EXIT)
      }, RECEIVER_NOT_EXPORTED
    )

    coroutineScope.launch {
      while (true) {
        // 每秒同步一次播放进度
        PlaybackStateManager.getInstance().synchronizePosition(exoPlayer.currentPosition)
        delay(1000L)
      }
    }
    coroutineScope.launch {
      DataStoreUtil().getReplayGain.collectLatest {
        PlaybackStateManager.getInstance().replayGainAudioProcessor
          .preAmpGain = it.toDouble()
      }
    }
    coroutineScope.launch {
      DataStoreUtil().getNyquistBand().collectLatest {
        PlaybackStateManager.getInstance().equalizerAudioProcessor
          .setBands(it)
      }
    }

  }


  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    if (intent.action == Intent.ACTION_MEDIA_BUTTON) {
      mediaSessionComponent.handleMediaButtonIntent(intent)
    }
    return START_NOT_STICKY
  }


  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
  ) {
    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
      PlaybackStateManager.getInstance().synchronizePosition(exoPlayer.currentPosition)
    }
  }


  override fun onEvents(player: Player, events: Player.Events) {
    super.onEvents(player, events)
    if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED) && player.playWhenReady) {
      // 开始播放了
      hasPlayed = true
    }

    if (events.containsAny(
        Player.EVENT_PLAY_WHEN_READY_CHANGED,
        Player.EVENT_IS_PLAYING_CHANGED,
      )
    ) {
      // 控制器同步状态
      PlaybackStateManager.getInstance().synchronizeState()
    }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    super.onIsPlayingChanged(isPlaying)
  }

  override fun onPlayerError(error: PlaybackException) {
    super.onPlayerError(error)
    // 播放错误 直接下一首
    PlaybackStateManager.getInstance().playNext()
  }

  /**
   * 连接时候触发
   */
  override fun onGetRoot(
    clientPackageName: String, clientUid: Int, rootHints: Bundle?
  ): BrowserRoot {

    return if (false) {
      // 允许连接
      BrowserRoot(MY_MEDIA_ROOT_ID, null)
    } else {
      // 不允许连接
      BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null)
    }
  }


  override fun onPlaybackStateChanged(playbackState: Int) {
    super.onPlaybackStateChanged(playbackState)
    if (playbackState == Player.STATE_ENDED) {
      // 根据播放模式处理播放结束后的操作
      PlaybackStateManager.getInstance().playNext()
    }
  }

  /**
   * 客户端连接后，可以通过重复调用 MediaBrowserCompat.subscribe() 来遍历内容层次结构，
   */
  override fun onLoadChildren(
    parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>
  ) {
    //  对于不允许连接的 客户端返回空
    if (MY_EMPTY_MEDIA_ROOT_ID == parentId) {
      result.sendResult(null)
      return
    }
    // 其他情况返回真正的播放列表
    val mediaItems = mutableListOf<MediaBrowserCompat.MediaItem>()
    result.sendResult(mediaItems)
  }


  override fun onDestroy() {
    super.onDestroy()
    // 解除绑定
    serviceJob.cancel()
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    PlaybackStateManager.getInstance().unRegisterPlayer()
    exoPlayer.release()
    foregroundManager.release()
    mediaSessionComponent.release()
    unregisterReceiver(systemReceiver)
  }

  override fun loadSong(song: Song?, play: Boolean) {
    if (song == null) {
      exoPlayer.stop()
      return
    }
    exoPlayer.setMediaItem(MediaItem.fromUri(song.getSongUri()))
    exoPlayer.prepare()
    exoPlayer.playWhenReady = play
  }

  override fun getState(durationMs: Long): IPlayer.State {
    return IPlayer.State(
      exoPlayer.isPlaying, exoPlayer.currentPosition.coerceAtLeast(0).coerceAtMost(durationMs)
    )
  }

  private fun stopAndSave() {
    hasPlayed = false
    if (foregroundManager.tryStopForeground()) {
      // TODO-- 保存当前播放状态 --
    }
  }

  override fun seekTo(positionMs: Long) {
    exoPlayer.seekTo(positionMs)
  }

  override fun setPlaying(isPlaying: Boolean) {
    exoPlayer.playWhenReady = isPlaying
  }

  // 广播通知
  companion object {
    private const val APPID = "me.spica27.spicamusic"
    const val ACTION_INC_REPEAT_MODE = APPID + ".action.LOOP"
    const val ACTION_INVERT_SHUFFLE = APPID + ".action.SHUFFLE"
    const val ACTION_SKIP_PREV = APPID + ".action.PREV"
    const val ACTION_PLAY_PAUSE = APPID + ".action.PLAY_PAUSE"
    const val ACTION_SKIP_NEXT = APPID + ".action.NEXT"
    const val ACTION_EXIT = APPID + ".action.EXIT"
    private const val REWIND_THRESHOLD = 3000L
  }


  override fun onPostNotification(notification: NotificationComponent) {
    if (hasPlayed) {
      if (!foregroundManager.tryStartForeground(notification)) {
        notification.post()
      }
    }
  }

  private inner class PlaybackReceiver : BroadcastReceiver() {
    private var initialHeadsetPlugEventHandled = false

    @OptIn(UnstableApi::class)
    override fun onReceive(context: Context, intent: Intent) {
      when (intent.action) {
        // 有线耳机的插入和断开广播
        AudioManager.ACTION_HEADSET_PLUG -> {
          when (intent.getIntExtra("state", -1)) {
            0 -> pauseFromHeadsetPlug() // 设备断开连接广播
            1 -> playFromHeadsetPlug() // 设备连接广播
          }

          initialHeadsetPlugEventHandled = true
        }
        // 当音频输出切回到内置扬声器时的广播（自动暂停）
        AudioManager.ACTION_AUDIO_BECOMING_NOISY -> pauseFromHeadsetPlug()
        ACTION_PLAY_PAUSE -> PlaybackStateManager.getInstance()
          .setPlaying(!PlaybackStateManager.getInstance().playerState.isPlaying)

        ACTION_SKIP_PREV -> PlaybackStateManager.getInstance().playPre()
        ACTION_SKIP_NEXT -> PlaybackStateManager.getInstance().playNext()
        ACTION_EXIT -> {
          PlaybackStateManager.getInstance().setPlaying(false)
          stopAndSave()
        }
      }
    }

    @OptIn(UnstableApi::class)
    private fun playFromHeadsetPlug() {
      if (PlaybackStateManager.getInstance()
          .getCurrentSong() != null && initialHeadsetPlugEventHandled
      ) {
        PlaybackStateManager.getInstance().setPlaying(true)
      }
    }

    private fun pauseFromHeadsetPlug() {
      if (PlaybackStateManager.getInstance().getCurrentSong() != null) {
        PlaybackStateManager.getInstance().setPlaying(false)
      }
    }
  }
}