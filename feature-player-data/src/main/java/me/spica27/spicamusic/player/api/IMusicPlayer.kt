package me.spica27.spicamusic.player.api

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.StateFlow

/**
 * 音乐播放器接口
 * 定义播放器的核心功能
 */
interface IMusicPlayer {
    /**
     * 播放模式 Flow
     */
    val playMode: StateFlow<PlayMode>

    /**
     * 播放完成后暂停状态
     */
    val pauseWhenCompletion: StateFlow<Boolean>

    /**
     * 是否正在播放
     */
    val isPlaying: StateFlow<Boolean>

    /**
     * 睡眠定时器状态；无定时器时为 null。
     */
    val sleepTimer: StateFlow<SleepTimerState?>

    /**
     * 当前播放的媒体项
     */
    val currentMediaItem: StateFlow<MediaItem?>

    /**
     * 当前媒体元数据
     */
    val currentMediaMetadata: StateFlow<MediaMetadata?>

    /**
     * 当前播放列表元数据
     */
    val currentPlaylistMetadata: StateFlow<MediaMetadata?>

    /**
     * 当前播放时长
     */
    val currentDuration: StateFlow<Long>

    /**
     * 当前播放列表
     */
    val currentTimelineItems: StateFlow<List<MediaItem>>

    /**
     * 当前播放位置（毫秒）
     */
    val currentPosition: Long

    /**
     * FFT 频谱处理器
     */
    val fftProcessor: IFFTProcessor

    /**
     * 获取用于 ExoPlayer 的 FFT AudioProcessor
     * 需要在创建 ExoPlayer 时添加到渲染器
     */
    @OptIn(UnstableApi::class)
    val fftAudioProcessor: AudioProcessor

    /**
     * 初始化播放器
     */
    fun init()

    /**
     * 执行播放器操作
     */
    fun doAction(action: PlayerAction)

    /**
     * 设置睡眠定时器。到期后暂停当前播放并保留播放位置和播放列表。
     *
     * 定时器按真实经过时间倒计时，即使应用退到后台或当前播放被手动暂停也不会暂停计时。
     * @throws IllegalArgumentException 当 durationMs 小于等于 0 时
     */
    fun setSleepTimer(durationMs: Long)

    /**
     * 取消当前睡眠定时器。
     */
    fun cancelSleepTimer()

    /**
     * 判断指定媒体项是否正在播放
     */
    fun isItemPlaying(mediaId: String): Boolean

    /**
     * 释放播放器资源
     */
    fun release()

    // ==================== 音效控制 ====================

    /**
     * 设置均衡器开关
     */
    fun setEQEnabled(enabled: Boolean)

    /**
     * 设置均衡器频段增益
     * @param band 频段索引 (0-9)
     * @param gainDb 增益值 (-12.0 to +12.0 dB)
     */
    fun setEQBandGain(band: Int, gainDb: Float)

    /**
     * 设置所有均衡器频段
     * @param gains 10个频段的增益数组
     */
    fun setAllEQBands(gains: FloatArray)

    /**
     * 设置响度归一化开关
     *
     * 播放链路使用 EBU R128/LUFS 测量驱动实时 AGC，并在输出端做峰值保护。
     * 目标是平滑曲内/曲间的感知响度，不改变 Media3 协商的 PCM 格式。
     */
    fun setLoudnessNormalizationEnabled(enabled: Boolean)

    /**
     * 设置响度归一化的目标响度（LUFS）
     *
     * 常用值：-14（Spotify/YouTube）、-18（ReplayGain 传统参考）、-23（EBU R128 广播标准）。
     * 因为库里存的是各曲的积分响度而非增益，改这个值不需要重新扫描。
     */
    fun setLoudnessTargetLufs(targetLufs: Float)
}
