package me.spica27.spicamusic.dsp

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import java.lang.reflect.Proxy
import java.nio.ByteBuffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [35])
class SpicaDspAudioSinkTest {
    private class Processor : PcmDspProcessor {
        override val isAvailable = true
        var calls = 0
        var resets = 0
        var encoding = 0
        var fail = false
        var marker = 1

        override fun configure(sampleRate: Int, channelCount: Int, encoding: Int, maxFrames: Int, channelMask: Int): Boolean {
            this.encoding = encoding
            return true
        }
        override fun process(input: ByteBuffer, output: ByteBuffer, byteCount: Int): Int {
            calls++
            if (fail) {
                output.put(99)
                return -1
            }
            val bytes = input.duplicate()
            bytes.limit(bytes.position() + byteCount)
            while (bytes.hasRemaining()) output.put((bytes.get() + marker).toByte())
            return byteCount
        }
        override fun reset() { resets++ }
    }
    private class Downstream(
        var pauseOnce: Boolean = false,
        val consumeOnPause: Boolean = false,
        var stallOnce: Boolean = false,
    ) {
        val bytes = ArrayList<Byte>()
        val times = ArrayList<Long>()
        val sink = Proxy.newProxyInstance(
            AudioSink::class.java.classLoader,
            arrayOf(AudioSink::class.java),
        ) { _, method, args ->
            when (method.name) {
                "handleBuffer" -> {
                    val buffer = args!![0] as ByteBuffer
                    times.add(args[1] as Long)
                    // stallOnce 模拟 DefaultAudioSink 重置卡住的 AudioTrack：只写出一部分却返回 true。
                    val partial = (pauseOnce && !consumeOnPause) || stallOnce
                    val count = if (partial) minOf(2, buffer.remaining()) else buffer.remaining()
                    repeat(count) { bytes.add(buffer.get()) }
                    val accepted = !pauseOnce
                    pauseOnce = false
                    stallOnce = false
                    accepted
                }
                "getFormatSupport" -> AudioSink.SINK_FORMAT_SUPPORTED_DIRECTLY
                "supportsFormat", "isEnded" -> true
                "hasPendingData" -> false
                else -> null
            }
        } as AudioSink
    }
    private fun configure(sink: SpicaDspAudioSink, encoding: Int = C.ENCODING_PCM_16BIT) {
        sink.configure(AudioSink.AudioSinkConfig.Builder(
            Format.Builder().setSampleMimeType(MimeTypes.AUDIO_RAW)
                .setSampleRate(48000).setChannelCount(2).setPcmEncoding(encoding).build(),
        ).build())
    }
    @Test fun retryDoesNotProcessTheSameSamplesTwice() {
        val processor = Processor()
        val downstream = Downstream(pauseOnce = true)
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val input = ByteBuffer.allocateDirect(8).put(byteArrayOf(0,1,2,3,4,5,6,7)).apply { flip() }
        assertFalse(sink.handleBuffer(input, 1234L, 1))
        assertEquals(0, input.position())
        assertTrue(sink.hasPendingData())
        assertTrue(sink.handleBuffer(input, 1234L, 1))
        assertEquals(1, processor.calls)
        assertEquals(8, input.position())
        assertArrayEquals(byteArrayOf(1,2,3,4,5,6,7,8), downstream.bytes.toByteArray())
        assertEquals(listOf(1234L,1234L), downstream.times)
    }
    @Test fun nativeFailureCopiesOriginalBytes() {
        val processor = Processor().apply { fail = true }
        val downstream = Downstream()
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val original = byteArrayOf(4,3,2,1,9,8,7,6)
        assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(8).put(original).apply { flip() }, 0L, 1))
        assertArrayEquals(original, downstream.bytes.toByteArray())
    }
    @Test fun consumedBufferAwaitingAcknowledgementIsNotProcessedAgain() {
        val processor = Processor()
        val downstream = Downstream(pauseOnce = true, consumeOnPause = true)
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val input = ByteBuffer.allocateDirect(8)
        assertFalse(sink.handleBuffer(input, 100L, 1))
        assertTrue(sink.handleBuffer(input, 100L, 1))
        assertEquals(1, processor.calls)
        assertEquals(8, downstream.bytes.size)
    }
    @Test fun floatAndHighResolutionReachDspBeforeAnyConversion() {
        for ((encoding,width) in listOf(C.ENCODING_PCM_FLOAT to 4,C.ENCODING_PCM_24BIT to 3,C.ENCODING_PCM_32BIT to 4)) {
            val processor = Processor()
            val sink = SpicaDspAudioSink(Downstream().sink, processor)
            configure(sink, encoding)
            assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(8 * width), 0L, 1))
            assertEquals(encoding, processor.encoding)
            assertEquals(1, processor.calls)
        }
    }
    @Test fun flushDiscardsPendingOutputAndResetsDsp() {
        val processor = Processor()
        val downstream = Downstream(pauseOnce = true)
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        assertFalse(sink.handleBuffer(ByteBuffer.allocateDirect(8), 0L, 1))
        sink.flush()
        assertFalse(sink.hasPendingData())
        assertEquals(1, processor.resets)
        assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(8), 100L, 1))
        assertEquals(2, processor.calls)
    }
    @Test fun stalledOutputResetDropsInputAndResetsDspInsteadOfThrowing() {
        val processor = Processor()
        val downstream = Downstream(stallOnce = true)
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val input = ByteBuffer.allocateDirect((960 + 8) * 4)
        assertTrue(sink.handleBuffer(input, 0L, 1))
        assertFalse(input.hasRemaining())
        assertFalse(sink.hasPendingData())
        assertEquals(1, processor.calls)
        assertEquals(1, processor.resets)
        assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(8), 100L, 1))
        assertEquals(2, processor.calls)
        assertEquals(2 + 8, downstream.bytes.size)
    }
    @Test fun oversizedBuffersKeepChunkTimestampsAndAllSamples() {
        val processor = Processor()
        val downstream = Downstream()
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val frames = 960 * 2 + 8
        assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(frames * 4), 1000L, 1))
        assertEquals(3, processor.calls)
        assertEquals(frames * 4, downstream.bytes.size)
        assertEquals(listOf(1000L, 21000L, 41000L), downstream.times)
    }
    @Test fun parameterChangesApplyToTheNextShortChunkWhileOutputIsBackpressured() {
        val processor = Processor()
        val downstream = Downstream(pauseOnce = true)
        val sink = SpicaDspAudioSink(downstream.sink, processor)
        configure(sink)
        val input = ByteBuffer.allocateDirect((960 + 8) * 4)
        assertFalse(sink.handleBuffer(input, 0L, 1))
        processor.marker = 5
        assertTrue(sink.handleBuffer(input, 0L, 1))
        assertEquals(2, processor.calls)
        assertTrue(downstream.bytes.take(960 * 4).all { it == 1.toByte() })
        assertTrue(downstream.bytes.drop(960 * 4).all { it == 5.toByte() })
    }
    @Test fun nonLinearPcmEncodingBypassesDspWithoutThrowing() {
        for (encoding in listOf(Format.NO_VALUE, C.ENCODING_INVALID)) {
            val processor = Processor()
            val downstream = Downstream()
            val sink = SpicaDspAudioSink(downstream.sink, processor)
            configure(sink, encoding)
            assertTrue(sink.handleBuffer(ByteBuffer.allocateDirect(8), 0L, 1))
            assertEquals(0, processor.calls)
            assertEquals(8, downstream.bytes.size)
        }
    }
    @Test fun encodedPassthroughAndOffloadCannotSkipEffects() {
        val sink = SpicaDspAudioSink(Downstream().sink, Processor())
        val encoded = Format.Builder().setSampleMimeType(MimeTypes.AUDIO_AC3).build()
        assertFalse(sink.supportsFormat(encoded))
        assertFalse(sink.getFormatOffloadSupport(encoded).isFormatSupported)
    }
}
