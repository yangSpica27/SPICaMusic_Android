package me.spica27.spicamusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.player.api.FFTListener
import me.spica27.spicamusic.player.api.IFFTProcessor
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.player.api.PlayerAction

/**
 * 播放器 ViewModel
 * 全局共享的播放器状态和控制逻辑
 * 可在多个页面中共享使用
 */
class PlayerViewModel(
    private val player: IMusicPlayer,
) : ViewModel() {
    // ==================== 播放状态 ====================

    /**
     * 是否正在播放
     */
    @OptIn(FlowPreview::class)
    val isPlaying: StateFlow<Boolean> =
        player.isPlaying
            .debounce(250)
            .distinctUntilChanged()
            .conflate()
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * 当前播放模式
     */
    val playMode: StateFlow<PlayMode> = player.playMode

    /**
     * 播放完成后暂停状态
     */
    val pauseWhenCompletion: StateFlow<Boolean> = player.pauseWhenCompletion

    /**
     * 当前播放的媒体项
     */
    val currentMediaItem: StateFlow<MediaItem?> = player.currentMediaItem

    /**
     * 当前媒体元数据
     */
    val currentMediaMetadata: StateFlow<MediaMetadata?> = player.currentMediaMetadata

    /**
     * 当前播放时长 (毫秒)
     */
    val currentDuration: StateFlow<Long> = player.currentDuration

    /**
     * 当前播放列表
     */
    val currentPlaylist: StateFlow<List<MediaItem>> = player.currentTimelineItems

    /**
     * 当前播放位置 (毫秒)
     */
    val currentPosition: StateFlow<Long> =
        flow {
            while (currentCoroutineContext().isActive) {
                emit(player.currentPosition)
                kotlinx.coroutines.delay(1000)
            }
        }.conflate()
            .stateIn(
                viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = 0L,
            )

    // ==================== 基础播放控制 ====================

    init {
        enableFFT()
    }

    /**
     * 播放
     */
    fun play() {
        player.doAction(PlayerAction.Play)
    }

    /**
     * 暂停
     */
    fun pause() {
        player.doAction(PlayerAction.Pause)
    }

    /**
     * 播放/暂停切换
     */
    fun togglePlayPause() {
        player.doAction(PlayerAction.PlayOrPause)
    }

    /**
     * 下一曲
     */
    fun skipToNext() {
        player.doAction(PlayerAction.SkipToNext)
    }

    /**
     * 上一曲
     */
    fun skipToPrevious() {
        player.doAction(PlayerAction.SkipToPrevious)
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        player.doAction(PlayerAction.SeekTo(positionMs))
    }

    // ==================== 播放模式 ====================

    /**
     * 设置播放模式
     */
    fun setPlayMode(mode: PlayMode) {
        player.doAction(PlayerAction.SetPlayMode(mode))
    }

    /**
     * 切换播放模式 (循环: 列表循环 -> 单曲循环 -> 随机)
     */
    fun togglePlayMode() {
        val nextMode =
            when (playMode.value) {
                PlayMode.LOOP -> PlayMode.LIST
                PlayMode.LIST -> PlayMode.SHUFFLE
                PlayMode.SHUFFLE -> PlayMode.LOOP
            }
        setPlayMode(nextMode)
    }

    // ==================== 播放列表操作 ====================

    /**
     * 根据 ID 播放歌曲
     */
    fun playById(mediaId: String) {
        player.doAction(PlayerAction.PlayById(mediaId))
    }

    /**
     * 播放歌曲
     */
    fun playSong(song: Song) {
        song.mediaStoreId.toString().let { id ->
            playById(id)
        }
    }

    /**
     * 添加到下一曲播放
     */
    fun addToNext(mediaId: String) {
        player.doAction(PlayerAction.AddToNext(mediaId))
    }

    /**
     * 添加歌曲到下一曲播放
     */
    fun addSongToNext(song: Song) {
        song.songId?.let { id ->
            addToNext(id.toString())
        }
    }

    /**
     * 从播放列表移除
     */
    fun removeFromPlaylist(mediaId: String) {
        player.doAction(PlayerAction.RemoveWithMediaId(mediaId))
    }

    /**
     * 从播放列表移除歌曲
     */
    fun removeSongFromPlaylist(song: Song) {
        song.songId?.let { id ->
            removeFromPlaylist(id.toString())
        }
    }

    /**
     * 更新播放列表
     * @param mediaIds 媒体ID列表
     * @param startMediaId 开始播放的媒体ID (可选)
     * @param autoStart 是否自动开始播放
     */
    fun updatePlaylist(
        mediaIds: List<String>,
        startMediaId: String? = null,
        autoStart: Boolean = false,
    ) {
        player.doAction(
            PlayerAction.UpdateList(
                mediaIds = mediaIds,
                mediaId = startMediaId,
                start = autoStart,
            ),
        )
    }

    /**
     * 更新播放列表 (使用歌曲列表)
     */
    fun updatePlaylistWithSongs(
        songs: List<Song>,
        startSong: Song? = null,
        autoStart: Boolean = false,
    ) {
        val mediaIds = songs.mapNotNull { it.songId?.toString() }
        val startMediaId = startSong?.songId?.toString()
        updatePlaylist(mediaIds, startMediaId, autoStart)
    }

    /**
     * 从头开始播放当前列表
     */
    fun reloadAndPlay() {
        player.doAction(PlayerAction.ReloadAndPlay)
    }

    // ==================== 其他功能 ====================

    /**
     * 设置播放完成后暂停
     */
    fun setPauseWhenCompletion(enabled: Boolean) {
        player.doAction(PlayerAction.PauseWhenCompletion(cancel = !enabled))
    }

    /**
     * 判断指定媒体是否正在播放
     */
    fun isItemPlaying(mediaId: String): Boolean = player.isItemPlaying(mediaId)

    /**
     * 判断指定歌曲是否正在播放
     */
    fun isSongPlaying(song: Song): Boolean = song.songId?.let { isItemPlaying(it.toString()) } ?: false

    // ==================== FFT 频谱分析 ====================

    /**
     * FFT 频谱数据 (31个频段, 0.0-1.0)
     * 频段: 20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
     *       800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
     *       12500, 16000, 20000 Hz
     */
    val fftBands: StateFlow<FloatArray> = player.fftProcessor.bands

    /**
     * FFT 是否启用
     */
    val fftEnabled: StateFlow<Boolean> = player.fftProcessor.isEnabled

    /**
     * 启用 FFT 分析
     */
    fun enableFFT() {
        player.fftProcessor.enable()
    }

    /**
     * 禁用 FFT 分析
     */
    fun disableFFT() {
        player.fftProcessor.disable()
    }

    /**
     * 添加 FFT 监听器
     * @param listener 频谱数据回调
     */
    fun addFFTListener(listener: FFTListener) {
        player.fftProcessor.addListener(listener)
    }

    /**
     * 移除 FFT 监听器
     */
    fun removeFFTListener(listener: FFTListener) {
        player.fftProcessor.removeListener(listener)
    }

    /**
     * 获取频段信息
     */
    val frequencyBands: FloatArray = IFFTProcessor.FREQUENCY_BANDS
}
