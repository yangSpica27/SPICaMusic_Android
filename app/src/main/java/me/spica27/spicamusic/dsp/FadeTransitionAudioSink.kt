package me.spica27.spicamusic.dsp

import android.content.Context
import androidx.annotation.OptIn
import androidx.dynamicanimation.animation.SpringForce
import androidx.dynamicanimation.animation.springAnimationOf
import androidx.dynamicanimation.animation.withSpringForceProperties
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 参考LMusic https://github.com/cy745/lmusic
@OptIn(UnstableApi::class)
class FadeTransitionAudioSink(
  sink: AudioSink,
  val scope: CoroutineScope,
) : ForwardingAudioSink(sink) {
  private var volumeOverride = 0f
    set(value) {
      field = value
      super.setVolume((value / 100f).coerceIn(0f..1f))
    }

  private var onFinished: (() -> Unit)? = null
  private val animation = springAnimationOf(
    getter = { volumeOverride },
    setter = { volumeOverride = it },
  ).withSpringForceProperties {
    stiffness = SpringForce.STIFFNESS_HIGH
    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
  }.apply {
    setStartValue(0f)
    setStartVelocity(0f)
    addEndListener { _, canceled, _, _ ->
      if (!canceled) onFinished?.invoke()
    }
  }

  override fun setVolume(volume: Float) {
    volumeOverride = volume
  }

  override fun play() {
    scope.launch(Dispatchers.Main) { animation.animateToFinalPosition(100f) }
    onFinished = null
    super.play()
  }

  override fun pause() {
    scope.launch(Dispatchers.Main) { animation.animateToFinalPosition(0f) }
    onFinished = { super.pause() }
  }
}

@OptIn(UnstableApi::class)
class FadeTransitionRenderersFactory(
  context: Context,
  val scope: CoroutineScope,
  teeBufferListener: TeeAudioProcessor.AudioBufferSink? = null,
  val extraAudioProcessors: List<AudioProcessor> = emptyList()
) : DefaultRenderersFactory(context), AudioProcessorChain {



  private val teeAudioProcessor = teeBufferListener
    ?.let { TeeAudioProcessor(it) }

  override fun buildAudioSink(
    context: Context,
    enableFloatOutput: Boolean,
    enableAudioTrackPlaybackParams: Boolean
  ): AudioSink {
    val defaultAudioSink = DefaultAudioSink.Builder(context)
      .setEnableFloatOutput(enableFloatOutput)
      .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
      .setAudioProcessorChain(this)
      .build()

    return FadeTransitionAudioSink(defaultAudioSink, scope)
  }

  override fun getAudioProcessors(): Array<AudioProcessor> {
    if (teeAudioProcessor != null) {
      return arrayOf(teeAudioProcessor, *extraAudioProcessors.toTypedArray())
    }
    return extraAudioProcessors.toTypedArray()
  }

  override fun getMediaDuration(playoutDuration: Long): Long = playoutDuration
  override fun getSkippedOutputFrameCount(): Long = 0
  override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean = skipSilenceEnabled
  override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters =
    playbackParameters
}