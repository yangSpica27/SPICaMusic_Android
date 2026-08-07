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
     * 当前播放位置（秒）
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
     * 设置混响开关
     */
    fun setReverbEnabled(enabled: Boolean)

    /**
     * 设置混响参数
     * @param level 混响强度 (0.0 - 1.0)
     * @param roomSize 房间大小 (0.0 - 1.0)
     */
    fun setReverb(level: Float, roomSize: Float)

    /**
     * 设置响度归一化开关
     *
     * 已测得积分响度（EBU R128）的歌曲会施加一个常数增益，实现**曲间音量齐平**；
     * 尚未测量的歌曲回退到实时滑动窗口 AGC。
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
