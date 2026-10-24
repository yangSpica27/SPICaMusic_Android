package me.spica27.spicamusic.dsp

import java.nio.ByteBuffer

/** 原生引擎与 Media3 音频输出层共用的交错 PCM 处理接口。 */
interface PcmDspProcessor {
    val isAvailable: Boolean

    fun configure(
        sampleRate: Int,
        channelCount: Int,
        encoding: Int,
        maxFrames: Int = 32 * 1024,
        channelMask: Int = -1,
    ): Boolean

    fun process(input: ByteBuffer, output: ByteBuffer, byteCount: Int): Int

    fun reset()
}
