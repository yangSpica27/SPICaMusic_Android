package me.spica27.spicamusic.dsp

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider

/**
 * 为交互式音效控制限制 PCM 预处理音频的排队时长。
 *
 * Media3 默认按最高支持倍速放大缓冲；开启平台倍速控制后，500 ms 会被放大到 4 s。
 * PCM 按正常播放时 200 ms 的目标分配，并由默认实现保证不低于设备要求的最小容量。
 * 这里只调整缓冲策略，不限制播放器可设置的倍速范围。
 */
@UnstableApi
class InteractiveAudioTrackBufferSizeProvider : DefaultAudioSink.AudioTrackBufferSizeProvider {
    private val delegate = DefaultAudioTrackBufferSizeProvider.Builder()
        .setTargetPcmBufferDurationUs(TARGET_PCM_BUFFER_US)
        .build()

    override fun getBufferSizeInBytes(
        minBufferSizeInBytes: Int,
        encoding: Int,
        outputMode: Int,
        pcmFrameSize: Int,
        sampleRate: Int,
        bitrate: Int,
        maxAudioTrackPlaybackSpeed: Double,
    ): Int = delegate.getBufferSizeInBytes(
        minBufferSizeInBytes,
        encoding,
        outputMode,
        pcmFrameSize,
        sampleRate,
        bitrate,
        if (outputMode == DefaultAudioSink.OUTPUT_MODE_PCM) 1.0 else maxAudioTrackPlaybackSpeed,
    )

    private companion object {
        const val TARGET_PCM_BUFFER_US = 200_000
    }
}
