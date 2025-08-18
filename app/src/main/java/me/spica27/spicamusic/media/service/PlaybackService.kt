package me.spica27.spicamusic.media.service

import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.spica27.spicamusic.dsp.EqualizerAudioProcessor
import me.spica27.spicamusic.dsp.FadeTransitionRenderersFactory
import me.spica27.spicamusic.dsp.ReplayGainAudioProcessor
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.visualiser.FFTAudioProcessor
import org.koin.java.KoinJavaComponent.getKoin

@OptIn(UnstableApi::class)
class PlaybackService : MediaLibraryService(), MediaSession.Callback {


  private val serviceJob = SupervisorJob()

  private val coroutineScope = CoroutineScope(serviceJob + Dispatchers.Main)


  private var exoPlayer: Player? = null
  private var mediaSession: MediaLibrarySession? = null

  override fun onCreate() {
    super.onCreate()
    initializeSessionAndPlayer()
  }

  private val fftAudioProcessor = getKoin().get<FFTAudioProcessor>()

  private val replayGainAudioProcessor = getKoin().get<ReplayGainAudioProcessor>()

  private val equalizerAudioProcessor = getKoin().get<EqualizerAudioProcessor>()

  private fun initializeSessionAndPlayer() {
    setMediaNotificationProvider(
      SpicaNotificationProvider(this)
    )
    val audioRenderersFactory = FadeTransitionRenderersFactory(
      this,
      coroutineScope,
      extraAudioProcessors = listOf(
        fftAudioProcessor,
        equalizerAudioProcessor,
        replayGainAudioProcessor
      )
    ).apply {
      this.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
    }


    exoPlayer = ExoPlayer.Builder(this, audioRenderersFactory)
      .setWakeMode(C.WAKE_MODE_LOCAL)
//        .setMediaSourceFactory(extractorsFactory)
      .setMaxSeekToPreviousPositionMs(Long.MAX_VALUE)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(C.USAGE_MEDIA)
          .setSpatializationBehavior(C.SPATIALIZATION_BEHAVIOR_AUTO)
          .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_ALL)
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .build(), true
      )
      .setUsePlatformDiagnostics(false).build()

    exoPlayer?.addListener(PlayerListener(exoPlayer!!))


    mediaSession = MediaLibrarySession
      .Builder(this, exoPlayer!!, ServiceCallback(exoPlayer!!))
      .build()

    coroutineScope.launch {
      DataStoreUtil().getReplayGain.collectLatest {
        replayGainAudioProcessor
          .preAmpGain = it.toDouble()
      }
    }
    coroutineScope.launch {
      DataStoreUtil().getNyquistBand().collectLatest {
        equalizerAudioProcessor
          .setBands(it)
      }
    }
  }


  override fun onDestroy() {
    exoPlayer?.stop()
    exoPlayer?.release()
    exoPlayer = null
    mediaSession?.release()
    mediaSession = null
    serviceJob.cancel(CancellationException("PlaybackService onDestroy()"))
    super.onDestroy()
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? =
    mediaSession
}