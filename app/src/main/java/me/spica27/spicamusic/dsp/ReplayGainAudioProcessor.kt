package me.spica27.spicamusic.dsp

import androidx.annotation.OptIn
import androidx.core.math.MathUtils
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.dsp.ByteUtils.Int24_MAX_VALUE
import me.spica27.spicamusic.dsp.ByteUtils.Int24_MIN_VALUE
import me.spica27.spicamusic.dsp.ByteUtils.getInt24
import me.spica27.spicamusic.dsp.ByteUtils.putInt24
import java.nio.ByteBuffer

@OptIn(UnstableApi::class)
class ReplayGainAudioProcessor(
    var preAmpGain: Double = 0.0,
) : BaseAudioProcessor() {
    // 歌曲的增益
    private var trackGain: Double? = null

    private val gain: Double
        get() = preAmpGain + (trackGain ?: 0.0)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_24BIT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (gain != 0.0) {
            val size = inputBuffer.remaining()
            val buffer = replaceOutputBuffer(size)
            val delta = gain.fromDb()
            when (outputAudioFormat.encoding) {
                C.ENCODING_PCM_16BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.short
                        val targetSample =
                            MathUtils
                                .clamp(
                                    (sample * delta),
                                    Short.MIN_VALUE.toDouble(),
                                    Short.MAX_VALUE.toDouble(),
                                ).toInt()
                                .toShort()
                        buffer.putShort(targetSample)
                    }
                }

                C.ENCODING_PCM_24BIT -> {
                    while (inputBuffer.hasRemaining()) {
                        val sample = inputBuffer.getInt24()
                        val targetSample =
                            MathUtils
                                .clamp(
                                    sample * delta,
                                    Int24_MIN_VALUE.toDouble(),
                                    Int24_MAX_VALUE.toDouble(),
                                ).toInt()
                        buffer.putInt24(targetSample)
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

    companion object {
        const val maxPreAmpGain = 12
    }
}
