package me.spica27.spicamusic.player.api

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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
}
