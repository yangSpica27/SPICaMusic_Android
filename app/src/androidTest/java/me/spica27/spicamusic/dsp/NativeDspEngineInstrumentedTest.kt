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
    fun antiPhaseStereoStillProducesResponsiveSpectrum() =
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
                    val input = antiPhaseStereoBlock(block * FRAMES_PER_BLOCK)
                    val output = ByteBuffer.allocateDirect(input.remaining())
                    assertEquals(
                        input.remaining(),
                        engine.process(input, output, input.remaining()),
                    )
                }

                val firstBands = spectrum.await()
                assertTrue(
                    "1 kHz band should survive anti-phase stereo analysis",
                    firstBands[ONE_KHZ_BAND_INDEX] > 0.3f,
                )

                engine.setPlaybackActive(false)
                assertTrue(engine.bands.value.all { it == 0f })

                // Exercise shutdown while the reader is in the native
                // condition wait rather than only while it is idle.
                engine.setPlaybackActive(true)
                engine.close()
            }
        }

    private fun antiPhaseStereoBlock(firstFrame: Int): ByteBuffer {
        val buffer =
            ByteBuffer
                .allocateDirect(FRAMES_PER_BLOCK * CHANNEL_COUNT * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
        repeat(FRAMES_PER_BLOCK) { offset ->
            val phase = 2.0 * PI * TONE_HZ * (firstFrame + offset) / SAMPLE_RATE
            val sample = (sin(phase) * AMPLITUDE).toFloat()
            buffer.putFloat(sample)
            buffer.putFloat(-sample)
        }
        buffer.flip()
        return buffer
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
        const val CHANNEL_COUNT = 2
        const val FRAMES_PER_BLOCK = 1_024
        const val INITIAL_BLOCK_COUNT = 4
        const val TONE_HZ = 1_000.0
        const val AMPLITUDE = 0.8
        const val ONE_KHZ_BAND_INDEX = 17
    }
}
