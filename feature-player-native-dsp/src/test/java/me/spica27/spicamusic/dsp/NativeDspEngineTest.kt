package me.spica27.spicamusic.dsp

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test

@UnstableApi
class NativeDspEngineTest {
    @Test fun fftResetPublishesNewGenerationWithoutResettingEqHistory() {
        NativeDspEngine().use { engine ->
            assertTrue(engine.isAvailable)
            assertTrue(engine.configure(48000, 2, C.ENCODING_PCM_FLOAT))
            engine.setEqEnabled(true)
            engine.setEqBandGain(5, 6f)
            val input = ByteBuffer.allocateDirect(4096 * 8).order(ByteOrder.LITTLE_ENDIAN)
            repeat(4096) {
                val sample = (0.1 * kotlin.math.sin(2 * Math.PI * 1000 * it / 48000)).toFloat()
                input.putFloat(sample).putFloat(sample)
            }
            input.flip()
            val before = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            repeat(16) { engine.process(input, before, input.remaining()) }
            val epoch = engine.spectrumGeneration.value
            engine.resetFft()
            assertTrue(engine.spectrumGeneration.value > epoch)
            assertTrue(engine.bands.value.all { it == 0f })
            val after = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(input.remaining(), engine.process(input, after, input.remaining()))
            repeat(4096 * 2) {
                assertEquals(before.getFloat(it * 4), after.getFloat(it * 4), 0.000001f)
            }
        }
    }
    @Test fun channelMaskDistinguishesCenterFromLfe() {
        NativeDspEngine().use { engine ->
            assertTrue(engine.isAvailable)
            engine.setLoudnessEnabled(true)
            val input = ByteBuffer.allocateDirect(1200 * 3 * 4).order(ByteOrder.LITTLE_ENDIAN)
            repeat(1200) {
                input.putFloat(0f).putFloat(0f).putFloat(
                    (0.1 * kotlin.math.sin(2 * Math.PI * 1000 * it / 48000)).toFloat(),
                )
            }
            input.flip()
            val output = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            for (mask in listOf(4 or 8 or 16, 4 or 8 or 32)) {
                assertTrue(engine.configure(48000, 3, C.ENCODING_PCM_FLOAT, channelMask = mask))
                repeat(60) { assertEquals(input.remaining(), engine.process(input, output, input.remaining())) }
                val peak = (0 until 1200).maxOf { kotlin.math.abs(output.getFloat((it * 3 + 2) * 4)) }
                if (mask and 32 != 0) assertEquals(0.1f, peak, 0.0001f)
                else assertTrue("Center energy must contribute to LUFS gain", peak > 0.15f)
            }
        }
    }
    @Test fun linearPcmEncodingsRustCannotCodeAreRejectedForBypass() {
        NativeDspEngine().use { engine ->
            assertTrue(engine.isAvailable)
            assertFalse(engine.configure(48000, 2, C.ENCODING_PCM_FLOAT_BIG_ENDIAN))
            assertFalse(engine.configure(48000, 2, C.ENCODING_PCM_DOUBLE))
            assertTrue(engine.configure(48000, 2, C.ENCODING_PCM_24BIT_BIG_ENDIAN))
        }
    }
    @Test fun nativeLibraryLoadsAndBypassPreservesPacked32BitPcm() {
        NativeDspEngine().use { engine ->
            assertTrue("The Rust JNI library must load; bypass is not a successful test", engine.isAvailable)
            assertTrue(engine.configure(192000, 2, C.ENCODING_PCM_32BIT))
            val bytes = ByteArray(256) { (it * 137).toByte() }
            val input = ByteBuffer.allocateDirect(bytes.size).put(bytes).apply { flip() }
            val output = ByteBuffer.allocateDirect(bytes.size)
            assertEquals(bytes.size, engine.process(input, output, bytes.size))
            val actual = ByteArray(bytes.size)
            output.get(actual)
            assertArrayEquals(bytes, actual)
        }
    }
    @Test fun eqChangesDecodedFloatPcmWithoutChangingItsSize() {
        NativeDspEngine().use { engine ->
            assertTrue(engine.isAvailable)
            assertTrue(engine.configure(48000, 2, C.ENCODING_PCM_FLOAT))
            engine.setEqEnabled(true)
            engine.setEqBandGain(5, 6f)
            val input = ByteBuffer.allocateDirect(4096 * 8).order(ByteOrder.LITTLE_ENDIAN)
            repeat(4096) {
                val value = (0.1 * kotlin.math.sin(2 * Math.PI * 1000 * it / 48000)).toFloat()
                input.putFloat(value).putFloat(value)
            }
            input.flip()
            val output = ByteBuffer.allocateDirect(input.remaining()).order(ByteOrder.LITTLE_ENDIAN)
            assertEquals(input.remaining(), engine.process(input, output, input.remaining()))
            assertTrue((0 until 4096 * 2).any { output.getFloat(it * 4) != input.getFloat(it * 4) })
        }
    }
    @Test fun closeWaitsForReadersAndFutureProcessingFailsSafely() {
        val engine = NativeDspEngine()
        assertTrue(engine.isAvailable)
        assertTrue(engine.configure(48000, 2, C.ENCODING_PCM_FLOAT))
        engine.enableFft()
        engine.setPlaybackActive(true)
        val done = CountDownLatch(1)
        val worker = Thread {
            try {
                val input = ByteBuffer.allocateDirect(8192)
                val output = ByteBuffer.allocateDirect(8192)
                repeat(200) { engine.process(input, output, 8192) }
            } finally { done.countDown() }
        }
        worker.start()
        engine.close()
        assertTrue(done.await(3, TimeUnit.SECONDS))
        assertFalse(engine.isAvailable)
        assertEquals(-1, engine.process(ByteBuffer.allocateDirect(8), ByteBuffer.allocateDirect(8), 8))
        engine.close()
    }
    @Test fun parkedReadersWakeAndPublishSpectrumOncePlaybackStarts() {
        NativeDspEngine().use { engine ->
            assertTrue(engine.isAvailable)
            assertTrue(engine.configure(48000, 2, C.ENCODING_PCM_FLOAT))
            engine.enableFft()
            // 尚未播放：Kotlin 读取线程和 Rust FFT 线程都应 park，只能靠状态变化和入队唤醒。
            Thread.sleep(50)
            engine.setPlaybackActive(true)
            val input = ByteBuffer.allocateDirect(4096 * 8).order(ByteOrder.LITTLE_ENDIAN)
            repeat(4096) {
                val sample = (0.1 * kotlin.math.sin(2 * Math.PI * 1000 * it / 48000)).toFloat()
                input.putFloat(sample).putFloat(sample)
            }
            input.flip()
            val output = ByteBuffer.allocateDirect(input.remaining())
            repeat(2) { assertEquals(input.remaining(), engine.process(input, output, input.remaining())) }
            val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2)
            while (engine.bands.value[17] == 0f && System.nanoTime() < deadline) Thread.sleep(5)
            assertTrue("1 kHz band must be published", engine.bands.value[17] > 0f)
        }
    }
    @Test fun closeInterruptsIdleReaderWithoutWaitingForTimeout() {
        val engine = NativeDspEngine()
        assertTrue(engine.isAvailable)
        Thread.sleep(50)
        val started = System.nanoTime()
        engine.close()
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 400)
    }
}
