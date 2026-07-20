package me.spica27.spicamusic.player.impl.dsp

import androidx.media3.common.C

/**
 * 线性 PCM 解码工具
 * 从交错多声道 PCM 字节流中提取第一个声道并归一化为 [-1.0, 1.0] 浮点样本。
 * 支持 8/16/24/32-bit 整型（小端与大端）以及 32-bit 浮点编码，
 * 以兼容高品质（Hi-Res）音频的采样数据。
 */
internal object PcmMonoDecoder {

    /**
     * 单个样本的字节数；不支持的编码返回 0
     */
    fun bytesPerSample(encoding: Int): Int =
        when (encoding) {
            C.ENCODING_PCM_8BIT -> 1
            C.ENCODING_PCM_16BIT, C.ENCODING_PCM_16BIT_BIG_ENDIAN -> 2
            C.ENCODING_PCM_24BIT, C.ENCODING_PCM_24BIT_BIG_ENDIAN -> 3
            C.ENCODING_PCM_32BIT, C.ENCODING_PCM_32BIT_BIG_ENDIAN, C.ENCODING_PCM_FLOAT -> 4
            else -> 0
        }

    fun isSupported(encoding: Int): Boolean = bytesPerSample(encoding) > 0

    /**
     * 解码第一个声道
     * @param data 交错 PCM 数据
     * @param sizeBytes 有效字节数
     * @param channelCount 声道数
     * @param encoding PCM 编码 (C.ENCODING_PCM_*)
     * @param out 输出数组，容量需不小于 sizeBytes / (bytesPerSample * channelCount)
     * @return 写入 out 的样本数；编码不支持或参数非法时返回 0
     */
    fun decodeFirstChannel(
        data: ByteArray,
        sizeBytes: Int,
        channelCount: Int,
        encoding: Int,
        out: FloatArray,
    ): Int {
        val bytesPerSample = bytesPerSample(encoding)
        if (bytesPerSample == 0 || channelCount <= 0) return 0
        val frameSize = bytesPerSample * channelCount
        val sampleCount = minOf(sizeBytes / frameSize, out.size)
        if (sampleCount <= 0) return 0

        var byteIndex = 0
        when (encoding) {
            C.ENCODING_PCM_8BIT -> {
                // 8-bit PCM 为无符号
                for (i in 0 until sampleCount) {
                    out[i] = ((data[byteIndex].toInt() and 0xFF) - 128) / 128f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_16BIT -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex + 1].toInt() shl 8) or
                        (data[byteIndex].toInt() and 0xFF)
                    out[i] = sample / 32768f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_16BIT_BIG_ENDIAN -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex].toInt() shl 8) or
                        (data[byteIndex + 1].toInt() and 0xFF)
                    out[i] = sample / 32768f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_24BIT -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex + 2].toInt() shl 16) or
                        ((data[byteIndex + 1].toInt() and 0xFF) shl 8) or
                        (data[byteIndex].toInt() and 0xFF)
                    out[i] = sample / 8388608f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_24BIT_BIG_ENDIAN -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex].toInt() shl 16) or
                        ((data[byteIndex + 1].toInt() and 0xFF) shl 8) or
                        (data[byteIndex + 2].toInt() and 0xFF)
                    out[i] = sample / 8388608f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_32BIT -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex + 3].toInt() shl 24) or
                        ((data[byteIndex + 2].toInt() and 0xFF) shl 16) or
                        ((data[byteIndex + 1].toInt() and 0xFF) shl 8) or
                        (data[byteIndex].toInt() and 0xFF)
                    out[i] = sample / 2147483648f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_32BIT_BIG_ENDIAN -> {
                for (i in 0 until sampleCount) {
                    val sample = (data[byteIndex].toInt() shl 24) or
                        ((data[byteIndex + 1].toInt() and 0xFF) shl 16) or
                        ((data[byteIndex + 2].toInt() and 0xFF) shl 8) or
                        (data[byteIndex + 3].toInt() and 0xFF)
                    out[i] = sample / 2147483648f
                    byteIndex += frameSize
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                // Android 平台浮点 PCM 为小端
                for (i in 0 until sampleCount) {
                    val bits = (data[byteIndex + 3].toInt() shl 24) or
                        ((data[byteIndex + 2].toInt() and 0xFF) shl 16) or
                        ((data[byteIndex + 1].toInt() and 0xFF) shl 8) or
                        (data[byteIndex].toInt() and 0xFF)
                    out[i] = Float.fromBits(bits).coerceIn(-1f, 1f)
                    byteIndex += frameSize
                }
            }
        }
        return sampleCount
    }
}
