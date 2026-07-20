package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmMonoDecoderTest {

    private val out = FloatArray(16)

    private fun decode(
        data: ByteArray,
        channelCount: Int,
        encoding: Int,
        sizeBytes: Int = data.size,
    ): Int = PcmMonoDecoder.decodeFirstChannel(data, sizeBytes, channelCount, encoding, out)

    @Test
    fun `bytesPerSample 覆盖所有支持的编码`() {
        assertEquals(1, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_8BIT))
        assertEquals(2, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_16BIT))
        assertEquals(2, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_16BIT_BIG_ENDIAN))
        assertEquals(3, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_24BIT))
        assertEquals(3, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_24BIT_BIG_ENDIAN))
        assertEquals(4, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_32BIT))
        assertEquals(4, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_32BIT_BIG_ENDIAN))
        assertEquals(4, PcmMonoDecoder.bytesPerSample(C.ENCODING_PCM_FLOAT))
        assertEquals(0, PcmMonoDecoder.bytesPerSample(C.ENCODING_INVALID))
        assertEquals(0, PcmMonoDecoder.bytesPerSample(C.ENCODING_AC3))
    }

    @Test
    fun `16bit 小端单声道`() {
        // 16384 (0.5), -16384 (-0.5), -32768 (-1.0)
        val data = byteArrayOf(
            0x00, 0x40,
            0x00, 0xC0.toByte(),
            0x00, 0x80.toByte(),
        )
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_16BIT)
        assertEquals(3, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(-0.5f, out[1], 1e-6f)
        assertEquals(-1.0f, out[2], 1e-6f)
    }

    @Test
    fun `16bit 双声道只取第一声道`() {
        // L=16384, R=-32768; L=8192, R=0
        val data = byteArrayOf(
            0x00, 0x40, 0x00, 0x80.toByte(),
            0x00, 0x20, 0x00, 0x00,
        )
        val count = decode(data, channelCount = 2, encoding = C.ENCODING_PCM_16BIT)
        assertEquals(2, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(0.25f, out[1], 1e-6f)
    }

    @Test
    fun `16bit 大端单声道`() {
        val data = byteArrayOf(0x40, 0x00, 0xC0.toByte(), 0x00)
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_16BIT_BIG_ENDIAN)
        assertEquals(2, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(-0.5f, out[1], 1e-6f)
    }

    @Test
    fun `24bit 小端单声道`() {
        // 4194304 (0.5), -4194304 (-0.5), -1 (约 -1,2e-7)
        val data = byteArrayOf(
            0x00, 0x00, 0x40,
            0x00, 0x00, 0xC0.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
        )
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_24BIT)
        assertEquals(3, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(-0.5f, out[1], 1e-6f)
        assertEquals(-1f / 8388608f, out[2], 1e-9f)
    }

    @Test
    fun `24bit 大端双声道只取第一声道`() {
        // L=0x400000 (0.5), R=0; L=0xC00000 (-0.5), R=0
        val data = byteArrayOf(
            0x40, 0x00, 0x00, 0x00, 0x00, 0x00,
            0xC0.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00,
        )
        val count = decode(data, channelCount = 2, encoding = C.ENCODING_PCM_24BIT_BIG_ENDIAN)
        assertEquals(2, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(-0.5f, out[1], 1e-6f)
    }

    @Test
    fun `32bit 小端单声道`() {
        // 0x40000000 = 1073741824 (0.5); 0x80000000 = Int.MIN_VALUE (-1.0)
        val data = byteArrayOf(
            0x00, 0x00, 0x00, 0x40,
            0x00, 0x00, 0x00, 0x80.toByte(),
        )
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_32BIT)
        assertEquals(2, count)
        assertEquals(0.5f, out[0], 1e-6f)
        assertEquals(-1.0f, out[1], 1e-6f)
    }

    @Test
    fun `32bit 大端单声道`() {
        val data = byteArrayOf(0x40, 0x00, 0x00, 0x00)
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_32BIT_BIG_ENDIAN)
        assertEquals(1, count)
        assertEquals(0.5f, out[0], 1e-6f)
    }

    @Test
    fun `float 小端单声道`() {
        val bits = 0.75f.toRawBits()
        val data = byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits shr 8) and 0xFF).toByte(),
            ((bits shr 16) and 0xFF).toByte(),
            ((bits shr 24) and 0xFF).toByte(),
        )
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_FLOAT)
        assertEquals(1, count)
        assertEquals(0.75f, out[0], 1e-6f)
    }

    @Test
    fun `float 超出正负1时被钳制`() {
        val bits = 2.5f.toRawBits()
        val data = byteArrayOf(
            (bits and 0xFF).toByte(),
            ((bits shr 8) and 0xFF).toByte(),
            ((bits shr 16) and 0xFF).toByte(),
            ((bits shr 24) and 0xFF).toByte(),
        )
        decode(data, channelCount = 1, encoding = C.ENCODING_PCM_FLOAT)
        assertEquals(1.0f, out[0], 1e-6f)
    }

    @Test
    fun `8bit 无符号单声道`() {
        // 128 → 0.0；255 → 127/128；0 → -1.0
        val data = byteArrayOf(128.toByte(), 255.toByte(), 0)
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_8BIT)
        assertEquals(3, count)
        assertEquals(0.0f, out[0], 1e-6f)
        assertEquals(127f / 128f, out[1], 1e-6f)
        assertEquals(-1.0f, out[2], 1e-6f)
    }

    @Test
    fun `不完整帧的尾部字节被忽略`() {
        // 5 字节的 16bit 单声道数据 = 2 个完整样本 + 1 个残缺字节
        val data = byteArrayOf(0x00, 0x40, 0x00, 0x20, 0x7F)
        val count = decode(data, channelCount = 1, encoding = C.ENCODING_PCM_16BIT)
        assertEquals(2, count)
    }

    @Test
    fun `不支持的编码返回0`() {
        val data = byteArrayOf(0x00, 0x40)
        assertEquals(0, decode(data, channelCount = 1, encoding = C.ENCODING_AC3))
    }

    @Test
    fun `样本数受输出数组容量限制`() {
        val small = FloatArray(2)
        val data = ByteArray(8) // 4 个 16bit 单声道样本
        val count = PcmMonoDecoder.decodeFirstChannel(data, data.size, 1, C.ENCODING_PCM_16BIT, small)
        assertEquals(2, count)
    }
}
