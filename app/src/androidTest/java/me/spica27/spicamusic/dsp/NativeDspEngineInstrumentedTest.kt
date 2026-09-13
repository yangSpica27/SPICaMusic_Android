package me.spica27.spicamusic.dsp

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

@UnstableApi
@RunWith(AndroidJUnit4::class)
class NativeDspEngineInstrumentedTest {
    @Test
    fun stereoMixIncludesBothInputChannels() =
        runBlocking {
            assertTrue(NativeDspEngine.isNativeLibraryLoaded)

            NativeDspEngine().use { engine ->
                assertTrue(
                    engine.configure(
                        sampleRate = SAMPLE_RATE,
                        channelCount = CHANNEL_COUNT,
                        encoding = C.ENCODING_PCM_FLOAT,
                        maxFrames = FRAMES_PER_BLOCK,
                    ),
                )
                engine.enableFft()
                engine.setPlaybackActive(true)

                val spectrum =
                    async(Dispatchers.Default) {
                        withTimeout(2_000L) {
                            engine.bands.first { bands -> bands.any { it > 0f } }
                        }
                    }

                repeat(INITIAL_BLOCK_COUNT) { block ->
                    val input = stereoToneBlock(block * FRAMES_PER_BLOCK)
                    val output = ByteBuffer.allocateDirect(input.remaining())
                    assertEquals(
                        input.remaining(),
                        engine.process(input, output, input.remaining()),
                    )
                }

                val firstBands = spectrum.await()
                assertTrue(
                    "1 kHz left-channel tone should be present in the stereo mix",
                    firstBands[ONE_KHZ_BAND_INDEX] > 0.3f,
                )
                assertTrue(
                    "2 kHz right-channel tone should be present in the stereo mix",
                    firstBands[TWO_KHZ_BAND_INDEX] > 0.3f,
                )

                engine.setPlaybackActive(false)
                assertTrue(engine.bands.value.all { it == 0f })

                // Exercise shutdown while the reader is in the native
                // condition wait rather than only while it is idle.
                engine.setPlaybackActive(true)
                engine.close()
            }
        }

    private fun stereoToneBlock(firstFrame: Int): ByteBuffer {
        val buffer =
            ByteBuffer
                .allocateDirect(FRAMES_PER_BLOCK * CHANNEL_COUNT * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
        repeat(FRAMES_PER_BLOCK) { offset ->
            val frame = firstFrame + offset
            val leftPhase = 2.0 * PI * LEFT_TONE_HZ * frame / SAMPLE_RATE
            val rightPhase = 2.0 * PI * RIGHT_TONE_HZ * frame / SAMPLE_RATE
            buffer.putFloat((sin(leftPhase) * AMPLITUDE).toFloat())
            buffer.putFloat((sin(rightPhase) * AMPLITUDE).toFloat())
        }
        buffer.flip()
        return buffer
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNEL_COUNT = 2
        const val FRAMES_PER_BLOCK = 1_024
        const val INITIAL_BLOCK_COUNT = 4
        const val LEFT_TONE_HZ = 1_000.0
        const val RIGHT_TONE_HZ = 2_000.0
        const val AMPLITUDE = 0.8
        const val ONE_KHZ_BAND_INDEX = 17
        const val TWO_KHZ_BAND_INDEX = 20
    }
}
