package me.spica27.spicamusic.dsp

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider

/**
 * 为交互式音效控制限制 PCM 预处理音频的排队时长。
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
        // 必须与 AudioTrack 实际允许的倍速一致；调高上限时缓冲随之放大，加速播放不会欠载。
        maxAudioTrackPlaybackSpeed,
    )

    companion object {
        /**
         * AudioTrack 允许的最高倍速，需通过 `AudioTrackAudioOutputProvider.Builder.setMaxPlaybackSpeed`
         * 设置。为保持 200 ms 的音效响应不允许加速：更高的倍速请求会被截断，播放器上报实际生效的速度；减速不受影响。
         */
        const val MAX_PLAYBACK_SPEED = 1f
        private const val TARGET_PCM_BUFFER_US = 200_000
    }
}
