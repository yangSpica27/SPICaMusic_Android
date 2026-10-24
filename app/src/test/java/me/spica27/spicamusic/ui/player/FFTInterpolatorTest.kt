package me.spica27.spicamusic.ui.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.spica27.spicamusic.player.api.IFFTProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FFTInterpolatorTest {
    private class Processor : IFFTProcessor {
        override val bands = MutableStateFlow(FloatArray(IFFTProcessor.BAND_COUNT))
        override val generation = MutableStateFlow(0L)
        override val isEnabled = MutableStateFlow(true)

        override fun enable() {
            isEnabled.value = true
        }

        override fun disable() {
            isEnabled.value = false
            reset()
        }

        /** 模拟音频输出层在 PCM 边界清空频谱并递增代次。 */
        fun reset() {
            bands.value = FloatArray(IFFTProcessor.BAND_COUNT)
            generation.value++
        }
    }

    @Test
    fun seekFadesFromRenderedSpectrumInsteadOfJumpingToZero() =
        runTest {
            val processor = Processor()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT) { 1f }
            val interpolator =
                FFTInterpolator(
                    processor,
                    backgroundScope,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            backgroundScope.launch { interpolator.interpolatedData.collect {} }
            runCurrent()
            advanceTimeBy(120)
            runCurrent()
            assertTrue(interpolator.interpolatedData.value.all { it == 1f })

            processor.reset()
            runCurrent()
            // 清除原始采样不能使当前画面直接塌到零。
            assertTrue(interpolator.interpolatedData.value.all { it == 1f })
            advanceTimeBy(16)
            runCurrent()
            assertTrue(interpolator.interpolatedData.value.all { it > 0f && it < 1f })
            advanceTimeBy(120)
            runCurrent()
            assertTrue(interpolator.interpolatedData.value.all { it == 0f })
        }

    @Test
    fun conflatedTrackResetUsesLastRenderedHeightInsteadOfOldRawTarget() =
        runTest {
            val processor = Processor()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[0] = 1f }
            val interpolator =
                FFTInterpolator(
                    processor,
                    backgroundScope,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val frames = mutableListOf<FloatArray>()
            backgroundScope.launch { interpolator.interpolatedData.collect { frames.add(it) } }
            runCurrent()
            advanceTimeBy(40)
            runCurrent()
            val before = interpolator.interpolatedData.value.copyOf()
            assertTrue(before[0] > 0f && before[0] < 1f)
            val count = frames.size

            processor.reset()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[20] = 1f }
            runCurrent()
            if (frames.size == count) {
                advanceTimeBy(8)
                runCurrent()
            }
            assertArrayEquals(before, frames[count], 0.000001f)
            advanceTimeBy(16)
            runCurrent()
            assertTrue(interpolator.interpolatedData.value[0] in 0f..before[0])
            assertTrue(interpolator.interpolatedData.value[20] > 0f)
            advanceTimeBy(120)
            runCurrent()
            assertEquals(0f, interpolator.interpolatedData.value[0], 0f)
            assertEquals(1f, interpolator.interpolatedData.value[20], 0f)
        }

    @Test
    fun repeatedSeeksPreserveVisualContinuity() =
        runTest {
            val processor = Processor()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[0] = 1f }
            val interpolator =
                FFTInterpolator(
                    processor,
                    backgroundScope,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            val frames = mutableListOf<FloatArray>()
            backgroundScope.launch { interpolator.interpolatedData.collect { frames.add(it) } }
            runCurrent()
            advanceTimeBy(120)
            runCurrent()
            repeat(4) { index ->
                val before = interpolator.interpolatedData.value.copyOf()
                val count = frames.size
                processor.reset()
                processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[index + 1] = 1f }
                runCurrent()
                if (frames.size == count) {
                    advanceTimeBy(8)
                    runCurrent()
                }
                assertArrayEquals(before, frames[count], 0.000001f)
                advanceTimeBy(24)
                runCurrent()
                assertTrue(interpolator.interpolatedData.value[index + 1] > 0f)
            }
        }

    @Test
    fun firstFreshFrameAfterResetDoesNotCompressTransitionToEightMilliseconds() =
        runTest {
            val processor = Processor()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[0] = 1f }
            val interpolator =
                FFTInterpolator(
                    processor,
                    backgroundScope,
                    { testScheduler.currentTime },
                    StandardTestDispatcher(testScheduler),
                )
            backgroundScope.launch { interpolator.interpolatedData.collect {} }
            runCurrent()
            advanceTimeBy(120)
            runCurrent()
            processor.reset()
            runCurrent()
            advanceTimeBy(8)
            runCurrent()
            val before = interpolator.interpolatedData.value.copyOf()
            processor.bands.value = FloatArray(IFFTProcessor.BAND_COUNT).apply { this[20] = 1f }
            advanceTimeBy(16)
            runCurrent()
            val after = interpolator.interpolatedData.value
            assertTrue(before.indices.all { kotlin.math.abs(after[it] - before[it]) <= 0.081f })
        }
}
