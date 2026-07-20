package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.utils.extractDominantColorFromUri
import timber.log.Timber

/**
 * 播放器 ViewModel
 * 全局共享的播放器状态和控制逻辑
 * 可在多个页面中共享使用
 */
@Stable
class PlayerViewModel(
    private val player: PlayerUseCases,
    private val songRepository: SongUseCases,
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSongIsLike: StateFlow<Boolean> =
        player.currentMediaItem
            .flatMapLatest { item ->
                val mediaStoreId = item?.mediaId?.toLongOrNull() ?: -1L
                if (mediaStoreId == -1L) {
                    kotlinx.coroutines.flow.flowOf(false)
                } else {
                    songRepository
                        .getSongLikeStatusFlowByMediaStoreId(mediaStoreId)
                        .distinctUntilChanged()
                        .flowOn(Dispatchers.IO)
                }
            }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

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

    val playerThemeColor: StateFlow<Color> =
        currentMediaItem
            .map {
                extractDominantColorFromUri(
                    context = App.getInstance(),
                    uri = it?.mediaMetadata?.artworkUri,
                )
            }.flowOn(Dispatchers.IO)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                Color(0xFF2196F3),
            )

    // ==================== 基础播放控制 ====================

    init {
        player.init()
    }

    // ==================== FFT 插值器 ====================

    /**
     * FFT 数据插值器
     * 自动将原始 FFT 数据插值为适合绘制的 60fps 数据
     */
    private val fftInterpolator = FFTInterpolator(player.fftProcessor, viewModelScope)

    /**
     * 插值后的 FFT 绘制数据 (31个频段, 0.0-1.0)
     * 适合直接用于 UI 绘制，约 60fps 平滑更新
     *
     * 直接通过 collectAsStateWithLifecycle 收集即可：
     * 计算随收集自动启停，页面不可见/应用后台时自动停止，无需手动订阅管理。
     */
    val fftDrawData: StateFlow<FloatArray> = fftInterpolator.interpolatedData

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
    fun playByMediaStoreId(mediaId: String) {
        player.doAction(PlayerAction.PlayById(mediaId))
    }

    /**
     * 播放歌曲
     */
    fun playSong(song: Song) {
        song.mediaStoreId.toString().let { id ->
            playByMediaStoreId(id)
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
        addToNext(song.mediaStoreId.toString())
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
        removeFromPlaylist(song.mediaStoreId.toString())
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
        val mediaIds = songs.map { it.mediaStoreId.toString() }
        val startMediaId = startSong?.mediaStoreId?.toString()
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

    fun getCurrentPositionMs(): Long = player.currentPosition

    /**
     * 切换当前歌曲的喜欢状态
     */
    fun toggleLikeCurrentSong() {
        viewModelScope.launch(Dispatchers.IO) {
            val mediaStoreId = currentMediaItem.value?.mediaId?.toLongOrNull() ?: return@launch
            Timber
                .tag("PlayerViewModel")
                .d("切换喜欢状态: mediaStoreId=$mediaStoreId")
            songRepository.toggleLikeByMediaStoreId(mediaStoreId)
        }
    }
}
