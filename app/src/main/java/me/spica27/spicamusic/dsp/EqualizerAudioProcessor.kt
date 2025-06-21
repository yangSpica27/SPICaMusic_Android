package me.spica27.spicamusic.dsp


import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.dsp.ByteUtils.getInt24
import me.spica27.spicamusic.dsp.ByteUtils.putInt24
import timber.log.Timber
import java.lang.Math.clamp
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class EqualizerAudioProcessor : BaseAudioProcessor() {

  private var bandProcessors = emptyList<BandProcessor>()


  // Maximum allowed gain/cut for each band
  val maxBandGain = 12

  val bands: ArrayList<NyquistBand> = ArrayList<NyquistBand>()


  /**
   * 设置曲线数据
   */
  fun setBands(bands: List<NyquistBand>) {
    this.bands.clear()
    this.bands.addAll(bands)
    updateBandProcessors()
  }


  private fun updateBandProcessors() {
    if (outputAudioFormat.channelCount <= 0) {
      return
    }

    bandProcessors = bands.map { band ->
      BandProcessor(
        band.toNyquistBand(),
        sampleRate = outputAudioFormat.sampleRate,
        channelCount = outputAudioFormat.channelCount,
        referenceGain = 0.0
      )
    }.toList()
  }

  override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    super.onConfigure(inputAudioFormat)

    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_24BIT) {
      throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
    }

    updateBandProcessors()

    return inputAudioFormat
  }

  override fun onFlush() {
    super.onFlush()

    Timber.v("onFlush() called")
    updateBandProcessors()
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    if (bands.isNotEmpty()) {
      val size = inputBuffer.remaining()
      val buffer = replaceOutputBuffer(size)

      when (outputAudioFormat.encoding) {
        C.ENCODING_PCM_16BIT -> {
          while (inputBuffer.hasRemaining()) {
            for (channelIndex in 0 until outputAudioFormat.channelCount) {
              val sample = inputBuffer.short
              var targetSample = sample.toFloat()
              for (band in bandProcessors) {
                targetSample = band.processSample(targetSample, channelIndex)
              }
              buffer.putShort(
                clamp(
                  targetSample,
                  Short.MIN_VALUE.toFloat(),
                  Short.MAX_VALUE.toFloat()
                ).toInt().toShort()
              )
              if (!inputBuffer.hasRemaining()) {
                break
              }
            }
          }
        }

        C.ENCODING_PCM_24BIT -> {
          while (inputBuffer.hasRemaining()) {
            for (channelIndex in 0 until outputAudioFormat.channelCount) {
              val sample = inputBuffer.getInt24()
              var targetSample = sample.toFloat()
              for (band in bandProcessors) {
                targetSample = band.processSample(targetSample, channelIndex)
              }
              buffer.putInt24(
                clamp(
                  targetSample,
                  ByteUtils.Int24_MIN_VALUE.toFloat(),
                  ByteUtils.Int24_MAX_VALUE.toFloat()
                ).toInt()
              )
              if (!inputBuffer.hasRemaining()) {
                break
              }
            }
          }
        }

        else -> {
          // No op
        }
      }
      inputBuffer.position(inputBuffer.limit())
      buffer.flip()
    } else {
      val remaining = inputBuffer.remaining()
      if (remaining == 0) {
        return
      }
      replaceOutputBuffer(remaining).put(inputBuffer).flip()
    }
  }
}
