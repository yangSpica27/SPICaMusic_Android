package me.spica27.spicamusic.service

import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.spica27.spicamusic.dsp.FadeTransitionRenderersFactory
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.utils.DataStoreUtil
import kotlin.coroutines.CoroutineContext

@OptIn(UnstableApi::class)
class MediaSessionService2 : MediaSessionService(), CoroutineScope,Player.Listener {

  private var mediaSession: MediaSession? = null

  private val job = SupervisorJob()

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO + job

  override fun onCreate() {
    super.onCreate()
    val player = createExoPlayer()
    player.playWhenReady = true
    val forwardingPlayer = ForwardingSimpleBasePlayer(player)

    mediaSession = MediaSession.Builder(this, forwardingPlayer)
      .build()

    setMediaNotificationProvider(DefaultMediaNotificationProvider(this))

    setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_ALWAYS)

    launch(Dispatchers.Main) {
      while (true) {
        PlaybackStateManager.getInstance().synchronizePosition(player.currentPosition)
        delay(1000L)
      }
    }

    launch {
      DataStoreUtil().getReplayGain.collectLatest {
        PlaybackStateManager.getInstance().replayGainAudioProcessor
          .preAmpGain = it.toDouble()
      }
    }
    launch {
      DataStoreUtil().getNyquistBand().collectLatest {
        PlaybackStateManager.getInstance().equalizerAudioProcessor
          .setBands(it)
      }
    }
  }


  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }


  override fun onTaskRemoved(rootIntent: Intent?) {
    pauseAllPlayersAndStopSelf()
  }


  private fun createExoPlayer(): ExoPlayer {
    val audioRenderersFactory = FadeTransitionRenderersFactory(
      this,
      this,
      extraAudioProcessors = listOf(
        PlaybackStateManager.getInstance().fftAudioProcessor,
        PlaybackStateManager.getInstance().equalizerAudioProcessor,
        PlaybackStateManager.getInstance().replayGainAudioProcessor
      )
    ).apply {
      this.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }


    val exoPlayer = ExoPlayer.Builder(
      this,
      audioRenderersFactory
    )
      .setWakeMode(C.WAKE_MODE_LOCAL)
//        .setMediaSourceFactory(extractorsFactory)
      .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)

      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(), true
      )
      .setUsePlatformDiagnostics(false).build()

    exoPlayer.addListener(this)

    return exoPlayer
  }

  override fun onEvents(player: Player, events: Player.Events) {
    super.onEvents(player, events)
    if (events.containsAny(
        Player.EVENT_PLAY_WHEN_READY_CHANGED,
        Player.EVENT_IS_PLAYING_CHANGED,
      )
    ) {
      launch(Dispatchers.Main) {
        // 控制器同步状态
        PlaybackStateManager.getInstance().synchronizeState()
      }
    }
  }

  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int
  ) {
    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
      launch(Dispatchers.Main) {
        PlaybackStateManager.getInstance().synchronizePosition(newPosition.positionMs)
      }
    }
  }




  override fun onPlayerError(error: PlaybackException) {
    super.onPlayerError(error)
    // 播放错误 直接下一首
    launch(Dispatchers.Main) {
      PlaybackStateManager.getInstance().playNext()
    }
  }

  override fun onDestroy() {
    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
    job.completeExceptionally(Exception("Service destroyed"))
  }


}