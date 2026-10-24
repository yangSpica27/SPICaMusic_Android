package me.spica27.spicamusic.dsp

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class InteractiveAudioTrackBufferSizeProviderTest {
    private val provider = InteractiveAudioTrackBufferSizeProvider()

    @Test
    fun pcmDoesNotReserveFourSecondsForMaximumSupportedSpeed() {
        val size = provider.getBufferSizeInBytes(
            4096, C.ENCODING_PCM_FLOAT, DefaultAudioSink.OUTPUT_MODE_PCM, 8, 44100, -1, 8.0,
        )
        assertEquals(8820 * 8, size)
    }

    @Test
    fun outputStillRespectsSystemMinimumAndPcmFrameAlignment() {
        val minimum = 200003
        val size = provider.getBufferSizeInBytes(
            minimum, C.ENCODING_PCM_FLOAT, DefaultAudioSink.OUTPUT_MODE_PCM, 8, 44100, -1, 8.0,
        )
        assertTrue(size >= minimum)
        assertEquals(0, size % 8)
    }

    @Test
    fun highResolutionAndMultichannelUseTheSameNominalQueueDuration() {
        val size = provider.getBufferSizeInBytes(
            4096, C.ENCODING_PCM_FLOAT, DefaultAudioSink.OUTPUT_MODE_PCM, 32, 192000, -1, 8.0,
        )
        assertEquals(38400 * 32, size)
    }
}
