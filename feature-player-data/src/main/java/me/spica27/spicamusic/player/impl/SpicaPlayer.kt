package me.spica27.spicamusic.player.impl

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.dsp.SpicaDspAudioSink
import me.spica27.spicamusic.dsp.NativeDspEngine
import me.spica27.spicamusic.player.api.IFFTProcessor
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayMode
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.player.api.SleepTimerState
import me.spica27.spicamusic.player.api.isValidQueueMove
import me.spica27.spicamusic.player.api.normalizedRemovalIndices
import me.spica27.spicamusic.player.impl.dsp.NativeFftProcessor
import me.spica27.spicamusic.player.impl.utils.MediaLibrary
import me.spica27.spicamusic.player.impl.utils.PlayerKVUtils
import me.spica27.spicamusic.player.impl.utils.toMediaItem
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * 音乐播放器控制类
 */
@UnstableApi
class SpicaPlayer(
    private val context: Context,
    private val playbackServiceClass: Class<*>,
) : IMusicPlayer, CoroutineScope, Player.Listener, MediaBrowser.Listener {

    private val TAG = "SpicaPlayer"

    private val playerKVUtils = getKoin().get<PlayerKVUtils>()

    // 播放历史仓库（延迟获取，避免循环依赖问题）
    private val playHistoryRepository by lazy { getKoin().get<IPlayHistoryRepository>() }

    override val coroutineContext: CoroutineContext = Dispatchers.IO + SupervisorJob()

    private val sessionToken by lazy {
        SessionToken(context, ComponentName(context, playbackServiceClass))
    }

    // UI 音效控制与公共 PCM 输出层共用同一个 Rust/OxiMedia 引擎。
    private val _nativeDspEngine = NativeDspEngine()
    private val _nativeFftProcessor = NativeFftProcessor(_nativeDspEngine)
    override val fftProcessor: IFFTProcessor = _nativeFftProcessor

    override fun wrapAudioSink(audioSink: androidx.media3.exoplayer.audio.AudioSink): androidx.media3.exoplayer.audio.AudioSink =
        SpicaDspAudioSink(audioSink, _nativeDspEngine)


    private val _initializing = AtomicBoolean(false)

    private var browserInstance: MediaBrowser? = null

    // 使用可空变量而非 by lazy，以便在 release() 后清空并重新初始化。
    private var _browserFuture: ListenableFuture<MediaBrowser>? = null

    private val playbackPositionSmoother = PlaybackPositionSmoother()

    private fun getOrCreateBrowserFuture(): ListenableFuture<MediaBrowser> =
        _browserFuture ?: MediaBrowser.Builder(context, sessionToken)
            .setListener(this)
            .buildAsync()
            .also { _browserFuture = it }

    override val playMode: StateFlow<PlayMode> = playerKVUtils.getPlayModeFlow()
        .stateIn(this, kotlinx.coroutines.flow.SharingStarted.Eagerly, PlayMode.LOOP)

    private val _pauseWhenCompletion = MutableStateFlow(false)
    override val pauseWhenCompletion: StateFlow<Boolean> = _pauseWhenCompletion

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying

    private val sleepTimerLock = Any()
    private var sleepTimerGeneration = 0L
    private var sleepTimerJob: Job? = null
    private val _sleepTimer = MutableStateFlow<SleepTimerState?>(null)
    override val sleepTimer: StateFlow<SleepTimerState?> = _sleepTimer

    private val _currentMediaItem = MutableStateFlow<MediaItem?>(null)
    override val currentMediaItem: StateFlow<MediaItem?> = _currentMediaItem

    private val _currentMediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    override val currentMediaMetadata: StateFlow<MediaMetadata?> = _currentMediaMetadata

    private val _currentPlaylistMetadata = MutableStateFlow<MediaMetadata?>(null)
    override val currentPlaylistMetadata: StateFlow<MediaMetadata?> = _currentPlaylistMetadata

    private val _currentDuration = MutableStateFlow(0L)
    override val currentDuration: StateFlow<Long> = _currentDuration

    private val _currentTimelineItems = MutableStateFlow<List<MediaItem>>(emptyList())
    override val currentTimelineItems: StateFlow<List<MediaItem>> = _currentTimelineItems

    private val _currentMediaItemIndex = MutableStateFlow(-1)
    override val currentMediaItemIndex: StateFlow<Int> = _currentMediaItemIndex

    // 记录当前播放会话的开始信息，用于计算 playedDuration
    private var playSessionMediaId: String? = null
    private var isRecordingPlay: Boolean = false
    private val playbackDurationTracker = PlaybackDurationTracker()

    override val currentPosition: Long
        get() {
            val browserAndPosition =
                runCatching {
                    val future = _browserFuture
                    val browser = if (future != null && future.isDone) future.get() else null
                    browser to browser?.currentPosition
                }.getOrNull()
            val browser = browserAndPosition?.first
            val mediaId = browser?.currentMediaItem?.mediaId ?: _currentMediaItem.value?.mediaId
            val rawPosition = browserAndPosition?.second
            return if (rawPosition != null) {
                playbackPositionSmoother.sample(mediaId, rawPosition)
            } else {
                playbackPositionSmoother.lastPosition(mediaId)
            }
        }

    override fun isItemPlaying(mediaId: String): Boolean {
        if (!_isPlaying.value) return false
        return _currentMediaItem.value?.mediaId == mediaId
    }

    /**
     * 延迟初始化播放器
     * 仅在需要时才创建 MediaBrowser 连接，减少应用启动时间
     */
    override fun init() {
        if (browserInstance != null) return
        if (!_initializing.compareAndSet(false, true)) return
        launch(Dispatchers.Main) {
            try {
                val browser = getOrCreateBrowserFuture().await()
                browserInstance = browser
                browser.addListener(this@SpicaPlayer)

                val items = withContext(Dispatchers.IO) {
                    playerKVUtils.getHistoryItems().map { it.toMediaItem() }
                }

                if (items.isEmpty()) {
                    Timber.e("No songs found")
                    _initializing.set(false)
                    return@launch
                }

                // 恢复上次的播放模式
                browser.playWhenReady = false
                browser.setMediaItems(items)
                // 替换时间轴后恢复模式
                applyPlayMode(browser, PlayMode.from(playerKVUtils.getPlayMode()))
                browser.prepare()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to initialize player")
                _browserFuture = null
            } finally {
                _initializing.set(false)
            }
        }
    }

    /**
     * 确保播放器已初始化
     * 在执行播放操作前调用，实现懒加载
     */
    private suspend fun ensureInitialized(): MediaBrowser? {
        if (browserInstance == null) {
            init()
        }
        // 设置 10 秒超时，避免 PlaybackService 因进程退出、系统终止
        // 或清单配置错误而启动失败时，doAction 一直挂起。
        return withTimeoutOrNull(10_000L) { getOrCreateBrowserFuture().await() }
            ?.also { browser ->
                // 首次安装没有播放历史，init() 会在恢复播放模式前返回。
                // 每次取得控制器后都应用已保存的模式或默认模式，
                // 保证首次播放的歌单与界面显示的模式一致。
                applyPlayMode(browser, PlayMode.from(playerKVUtils.getPlayMode()))
            }
    }

    override fun doAction(action: PlayerAction) {
        launch(Dispatchers.Main) {
            try {
                val browser = ensureInitialized() ?: return@launch
                Timber.d("doAction: ${action.javaClass.simpleName}")
                when (action) {
                    PlayerAction.Play -> {
                        // 若 ExoPlayer 在服务重启后处于 IDLE 状态，需要先 prepare()
                        if (browser.playbackState == Player.STATE_IDLE) {
                            browser.prepare()
                        }
                        browser.play()
                    }
                    PlayerAction.Pause -> browser.pause()

                    PlayerAction.SkipToNext -> browser.seekToNext()

                    PlayerAction.SkipToPrevious -> browser.seekToPrevious()

                    is PlayerAction.RemoveWithMediaId -> {
                        val index = browser.currentTimeline.indexOf(action.mediaId)
                        if (index != -1) {
                            browser.removeMediaItem(index)
                        }
                    }

                    is PlayerAction.RemoveAtIndices -> {
                        normalizedRemovalIndices(action.indices, browser.mediaItemCount)
                            .forEach(browser::removeMediaItem)
                    }

                    PlayerAction.PlayOrPause -> {
                        // 使用 _isPlaying.value（来自 onIsPlayingChanged 回调的权威状态），
                        // 避免 browser.isPlaying 在服务重连后返回过期值导致误调 pause()
                        if (_isPlaying.value) {
                            browser.pause()
                        } else {
                            if (browser.playbackState == Player.STATE_IDLE) {
                                browser.prepare()
                            }
                            browser.play()
                        }
                    }

                    is PlayerAction.PlayById -> {
                        Timber.tag(TAG).d("PlayById: mediaId=${action.mediaId}")
                        val index = browser.currentTimeline.indexOf(action.mediaId)
                        Timber.tag(TAG)
                            .d("Current timeline index: $index, timeline size: ${browser.currentTimeline.windowCount}")

                        if (index == -1) {
                            // 不在播放列表中，添加并播放
                            val item = withContext(Dispatchers.IO) { MediaLibrary.getItem(action.mediaId) }
                            if (item != null) {
                                Timber.tag(TAG)
                                    .d("Item not in playlist, adding and playing: ${item.mediaId}")
                                val currentIndex = browser.currentMediaItemIndex
                                val toIndex = if (currentIndex == -1) 0 else currentIndex + 1
                                browser.addMediaItem(toIndex, item)
                                applyPlayMode(browser, PlayMode.from(playerKVUtils.getPlayMode()))
                                browser.prepare()
                                browser.seekTo(toIndex, 0)
                                browser.playWhenReady = true
                                Timber.tag(TAG)
                                    .d("Seeking to new item at index: $toIndex, playWhenReady=true")
                            } else {
                                Timber.tag(TAG)
                                    .w("Item with mediaId=${action.mediaId} not found in media library")
                            }
                        } else {
                            Timber.tag(TAG).d("Item already in playlist, seeking to index: $index")
                            browser.seekTo(index, 0)
                            browser.playWhenReady = true
                            Timber.tag(TAG)
                                .d("Play() called on existing item, playWhenReady=${browser.playWhenReady}")
                        }
                    }

                    is PlayerAction.SeekTo -> {
                        // 立即重置进度，避免等待 Media3 的位置跳变回调时，
                        // 歌词界面不能及时跟随用户的跳转操作。
                        val mediaId =
                            browser.currentMediaItem?.mediaId
                                ?: _currentMediaItem.value?.mediaId
                        playbackPositionSmoother.resetTo(mediaId, action.positionMs)
                        browser.seekTo(action.positionMs)
                    }

                    is PlayerAction.PauseWhenCompletion -> {
                        _pauseWhenCompletion.value = !action.cancel
                    }

                    is PlayerAction.SetPlayMode -> {
                        // 保存到 KV 存储
                        playerKVUtils.setPlayMode(action.playMode.name)
                        // 应用播放模式到播放器
                        applyPlayMode(browser, action.playMode)
                        Timber.tag(TAG).d("Play mode applied: ${action.playMode}")
                    }

                    is PlayerAction.AddToNext -> {
                        val item = withContext(Dispatchers.IO) { MediaLibrary.getItem(action.mediaId) }
                        if (item == null) {
                            Timber.tag(TAG)
                                .w("Item with mediaId=${action.mediaId} not found for AddToNext")
                        } else {
                            // 处理空播放列表的情况
                            val currentIndex = browser.currentMediaItemIndex.coerceAtLeast(0)
                            val index = browser.currentTimeline.indexOf(action.mediaId)

                            if (index != -1) {
                                val offset = if (index > currentIndex) 1 else 0
                                browser.moveMediaItem(index, currentIndex + offset)
                            } else {
                                browser.addMediaItem(currentIndex + 1, item)
                            }
                        }
                    }

                    is PlayerAction.UpdateList -> {
                        val index = action.mediaId?.let { action.mediaIds.indexOf(it) }
                            ?.takeIf { it >= 0 } ?: 0
                        val items = withContext(Dispatchers.IO) { MediaLibrary.mediaIdToMediaItems(action.mediaIds) }
                        browser.setMediaItems(items, index, 0)
                        // 设置播放列表后，新建的播放器可能仍处于 REPEAT_MODE_OFF。
                        // 替换时间线后重新应用已保存的模式，也覆盖首次安装后的首次播放。
                        applyPlayMode(browser, PlayMode.from(playerKVUtils.getPlayMode()))
                        if (action.start) {
                            browser.play()
                        }
                    }

                    PlayerAction.ReloadAndPlay -> {
                        Timber.tag(TAG).w("ReloadAndPlay not implemented yet")
                        // TODO: 实现重新加载并播放逻辑
                    }

                    is PlayerAction.MoveItem -> {
                        if (isValidQueueMove(action.fromIndex, action.toIndex, browser.mediaItemCount)) {
                            browser.moveMediaItem(action.fromIndex, action.toIndex)
                        }
                    }

                    is PlayerAction.PlayAtIndex -> {
                        if (action.index in 0 until browser.mediaItemCount) {
                            if (browser.playbackState == Player.STATE_IDLE) {
                                browser.prepare()
                            }
                            browser.seekTo(action.index, 0)
                            browser.playWhenReady = true
                        }
                    }

                    is PlayerAction.AddToQueue -> {
                        val items = withContext(Dispatchers.IO) { MediaLibrary.mediaIdToMediaItems(action.mediaIds) }
                        browser.addMediaItems(items)
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Error executing action: ${action.javaClass.simpleName}")
            }
        }
    }

    override fun setSleepTimer(durationMs: Long) {
        require(durationMs > 0) { "Sleep timer duration must be greater than zero" }

        synchronized(sleepTimerLock) {
            sleepTimerGeneration += 1
            val generation = sleepTimerGeneration
            val deadline = SystemClock.elapsedRealtime() + durationMs

            sleepTimerJob?.cancel()
            _sleepTimer.value =
                SleepTimerState(
                    durationMs = durationMs,
                    remainingMs = durationMs,
                    deadlineElapsedRealtimeMs = deadline,
                )
            sleepTimerJob = launch { runSleepTimer(generation) }
        }
    }

    override fun cancelSleepTimer() {
        synchronized(sleepTimerLock) {
            sleepTimerGeneration += 1
            sleepTimerJob?.cancel()
            sleepTimerJob = null
            _sleepTimer.value = null
        }
    }

    /**
     * 倒计时运行在播放器自己的应用级 scope 中，因此不会因 Activity/Compose 页面销毁而丢失。
     * 每次刷新都校验 generation，避免旧定时器在新定时器设置后覆盖状态或触发暂停。
     */
    private suspend fun runSleepTimer(
        generation: Long,
    ) {
        try {
            while (true) {
                val now = SystemClock.elapsedRealtime()
                val remaining =
                    synchronized(sleepTimerLock) {
                        if (generation != sleepTimerGeneration) {
                            null
                        } else {
                            val current = _sleepTimer.value ?: return@synchronized null
                            val updated = current.updatedAt(now)
                            _sleepTimer.value = updated
                            updated.remainingMs
                        }
                    }

                // null 表示该任务已被新任务替代，或已被显式取消。
                if (remaining == null) return
                if (remaining <= 0L) {
                    val shouldPause =
                        synchronized(sleepTimerLock) {
                            if (generation == sleepTimerGeneration) {
                                _sleepTimer.value = null
                                true
                            } else {
                                false
                            }
                        }
                    // 如果用户已在到期前手动暂停，就不再唤醒或重连 MediaBrowser，
                    // 避免发送多余的暂停命令。
                    if (shouldPause && _isPlaying.value) {
                        doAction(PlayerAction.Pause)
                    }
                    return
                }

                delay(minOf(1_000L, remaining))
            }
        } finally {
            val currentJob = kotlinx.coroutines.currentCoroutineContext()[Job]
            synchronized(sleepTimerLock) {
                if (sleepTimerJob === currentJob) {
                    sleepTimerJob = null
                }
            }
        }
    }

    override fun release() {
        cancelSleepTimer()
        playbackPositionSmoother.clear()
        // 1. 移除监听器
        browserInstance?.removeListener(this)
        // 2. 释放 MediaBrowser 及其 Future（Media3 规范：releaseFuture 负责 Future 的生命周期）
        _browserFuture?.let { MediaBrowser.releaseFuture(it) }
        _browserFuture = null
        browserInstance = null
        // 3. 允许 release 后重新 init（例如服务重启场景）
        _initializing.set(false)
        // 4. 释放 FFT 处理器（取消线程池）
        _nativeDspEngine.close()
        // 5. 取消协程
        coroutineContext.cancel()
    }

    // ==================== MediaController.Listener ====================

    /**
     * PlaybackService 被系统杀死后重连时触发。
     * 重置 browserInstance，使下一次 doAction 能重新走 init() 恢复播放列表。
     */
    override fun onDisconnected(controller: MediaController) {
        Timber.tag(TAG).w("MediaBrowser disconnected, resetting for re-init on next action")
        browserInstance?.removeListener(this)
        browserInstance = null
        _browserFuture = null
        _initializing.set(false)
        _isPlaying.value = false
        playbackPositionSmoother.markDisconnected()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        this@SpicaPlayer._isPlaying.value = isPlaying
        // 采样跟随播放状态：暂停时停止采样并清空旧片段（暂停中 seek/切歌的管线预缓冲不会被采样），
        // 插值层在没有新数据时自动挂起。
        _nativeFftProcessor.setPlaybackActive(isPlaying)
        if (isPlaying) {
            startPlaySession(
                mediaId = _currentMediaItem.value?.mediaId,
                positionMs = browserInstance?.currentPosition ?: 0L,
            )
        } else {
            if (isRecordingPlay) {
                val mediaId = playSessionMediaId
                val currentPos = browserInstance?.currentPosition ?: 0L
                val dur = _currentDuration.value
                val rawPlayedDuration = playbackDurationTracker.playedDurationFromPosition(currentPos)
                // 以歌曲总时长为上限，避免记录超出有效范围。
                val playedDuration = if (dur > 0) rawPlayedDuration.coerceAtMost(dur) else rawPlayedDuration
                val completed = dur > 0 && playedDuration >= (dur * 0.9)
                if (mediaId != null && playedDuration > 0L) {
                    val extra = buildExtraFromMetadata(_currentMediaMetadata.value)
                    val ph = PlayHistory(
                        songId = mediaId.toLongOrNull() ?: 0L,
                        playTime = System.currentTimeMillis(),
                        playCount = 1,
                        userId = null,
                        sessionId = null,
                        deviceId = null,
                        duration = dur,
                        playedDuration = playedDuration,
                        position = currentPos,
                        actionType = if (completed) 3 else 1,
                        contextType = _currentPlaylistMetadata.value?.title?.toString() ?: "",
                        contextId = null,
                        isCompleted = completed,
                        source = "",
                        extra = extra,
                    )
                    launch(Dispatchers.IO) {
                        try {
                            playHistoryRepository.addPlayHistory(ph)
                        } catch (e: Exception) {
                            Timber.e(e)
                        }
                    }
                }
            }
            clearPlaySession()
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        // 频谱片段由音频输出层在 flush 时按 PCM 边界重置；控制器回调与该边界没有时序保证，这里不再重置。
        if (
            reason == Player.DISCONTINUITY_REASON_SEEK ||
                reason == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
        ) {
            val mediaId =
                browserInstance?.currentMediaItem?.mediaId
                    ?: _currentMediaItem.value?.mediaId
            playbackPositionSmoother.resetTo(mediaId, newPosition.positionMs)
        }
        if (reason == Player.DISCONTINUITY_REASON_SEEK && isRecordingPlay) {
            playbackDurationTracker.splitOnSeek(
                oldPositionMs = oldPosition.positionMs,
                newPositionMs = newPosition.positionMs,
                nowMs = SystemClock.elapsedRealtime(),
            )
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        super.onShuffleModeEnabledChanged(shuffleModeEnabled)
        Timber.e("onShuffleModeEnabledChanged $shuffleModeEnabled")
    }

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        // 同格式连续切歌和单曲循环不一定触发 sink 重配，但 renderer 处理到新 stream 时
        // 会调用 handleDiscontinuity，频谱片段在那里按 PCM 边界重置。此回调按播放头触发，
        // 晚于该边界，再次重置会丢弃新歌已入队的数据。
        val previousMediaId = playSessionMediaId
        if (previousMediaId != null && isRecordingPlay) {
            val now = SystemClock.elapsedRealtime()
            val dur = _currentDuration.value
            val rawPlayedDuration = playbackDurationTracker.playedDurationFromElapsed(now)
            // 以实际歌曲时长为上限，避免系统时间跳变或统计异常
            // 导致保存的播放时长偏大。
            val playedDuration = if (dur > 0) rawPlayedDuration.coerceAtMost(dur) else rawPlayedDuration
            val completed = dur > 0 && playedDuration >= (dur * 0.9)
            if (playedDuration > 0L) {
                val extra = buildExtraFromMetadata(_currentMediaMetadata.value)
                val ph = PlayHistory(
                    songId = previousMediaId.toLongOrNull() ?: 0L,
                    playTime = System.currentTimeMillis(),
                    playCount = 1,
                    userId = null,
                    sessionId = null,
                    deviceId = null,
                    duration = dur,
                    playedDuration = playedDuration,
                    position = if (completed) dur else 0L,
                    actionType = if (completed) 3 else 2,
                    contextType = _currentPlaylistMetadata.value?.title?.toString() ?: "",
                    contextId = null,
                    isCompleted = completed,
                    source = "",
                    extra = extra,
                )
                launch(Dispatchers.IO) {
                    try {
                        playHistoryRepository.addPlayHistory(ph)
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }

        Timber.e("onMediaItemTransition $mediaItem $reason")
        _currentMediaItem.value = mediaItem
        _currentMediaItemIndex.value = browserInstance?.currentMediaItemIndex ?: -1
        // 单曲循环时媒体 ID 可能不变，但进度会归零，因此也需要重新建立进度基准。
        // 此处不读取 browser.currentPosition，因为回调期间它可能仍是上一媒体项的最终进度。
        playbackPositionSmoother.resetTo(
            mediaId = mediaItem?.mediaId,
            positionMs = 0L,
        )
        // 切歌时立即重置 duration，避免旧时长污染新歌曲的进度计算
        _currentDuration.value = 0L
        // 尝试从 browser 实例获取新歌曲的时长（可能此时已就绪）
        browserInstance?.let { browser ->
            val dur = browser.duration
            if (dur > 0) {
                _currentDuration.value = dur
            }
        }
        if (_pauseWhenCompletion.value) {
            browserInstance?.pause()
            _pauseWhenCompletion.value = false
        }

        clearPlaySession()
        if (_isPlaying.value) {
            startPlaySession(
                mediaId = _currentMediaItem.value?.mediaId,
                positionMs = browserInstance?.currentPosition ?: 0L,
            )
        }
    }

    private fun startPlaySession(
        mediaId: String?,
        positionMs: Long,
    ) {
        playSessionMediaId = mediaId
        playbackDurationTracker.beginSession(
            positionMs = positionMs,
            nowMs = SystemClock.elapsedRealtime(),
        )
        isRecordingPlay = true
    }

    private fun clearPlaySession() {
        playbackDurationTracker.clear()
        isRecordingPlay = false
        playSessionMediaId = null
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        Timber.e("onMediaMetadataChanged $mediaMetadata")
        _currentMediaItem.value = browserInstance?.currentMediaItem
        _currentMediaItemIndex.value = browserInstance?.currentMediaItemIndex ?: -1
        _currentMediaMetadata.value = mediaMetadata
        // 优先使用 metadata 中的 durationMs，其次从 browser 实例取当前 duration
        val metaDuration = mediaMetadata.durationMs ?: 0L
        val browserDuration = browserInstance?.duration ?: 0L
        // 取有效值（> 0），优先 metaDuration
        _currentDuration.value = when {
            metaDuration > 0 -> metaDuration
            browserDuration > 0 -> browserDuration
            else -> 0L
        }
    }

    override fun onPlaylistMetadataChanged(mediaMetadata: MediaMetadata) {
        _currentPlaylistMetadata.value = mediaMetadata
    }

    override fun onTimelineChanged(
        timeline: Timeline,
        @Player.TimelineChangeReason reason: Int,
    ) {
        updateItems(timeline)
        Timber.e("onTimelineChanged 切换原因 $reason")
    }

    companion object {
        private const val TAG = "SpicaPlayer"

        fun createModule(playbackServiceClass: Class<*>) = module {
            single<PlayerKVUtils> { PlayerKVUtils(androidApplication()) }
            single<IMusicPlayer> { SpicaPlayer(androidApplication(), playbackServiceClass) }
        }
    }

    private fun updateItems(timeline: Timeline?) {
        val items = timeline?.toMediaItems() ?: emptyList()
        _currentTimelineItems.value = items
        // 移动/删除会改变当前项的索引而不触发 onMediaItemTransition
        _currentMediaItemIndex.value =
            browserInstance?.currentMediaItemIndex?.takeIf { items.isNotEmpty() } ?: -1

        val ids = _currentTimelineItems.value.map { it.mediaId }
        playerKVUtils.setHistoryIds(ids.mapNotNull { it.toLongOrNull() })
    }

    private fun Timeline.toMediaItems(): List<MediaItem> =
        (0 until this.windowCount).mapNotNull { this.getWindow(it, Timeline.Window()).mediaItem }

    private fun Timeline.indexOf(mediaId: String): Int {
        var index = -1
        (0 until this.windowCount).forEach {
            if (this.getWindow(it, Timeline.Window()).mediaItem.mediaId == mediaId) {
                index = it
                return@forEach
            }
        }
        return index
    }

    /**
     * 应用播放模式到 MediaBrowser
     */
    private fun applyPlayMode(browser: MediaBrowser, mode: PlayMode) {
        when (mode) {
            PlayMode.LOOP -> {
                browser.repeatMode = Player.REPEAT_MODE_ALL
                browser.shuffleModeEnabled = false
            }

            PlayMode.LIST -> {
                browser.repeatMode = Player.REPEAT_MODE_ONE
                browser.shuffleModeEnabled = false
            }

            PlayMode.SHUFFLE -> {
                browser.repeatMode = Player.REPEAT_MODE_ALL
                browser.shuffleModeEnabled = true
            }
        }
    }

    // ==================== 音效控制实现 ====================

    override fun setEQEnabled(enabled: Boolean) {
        _nativeDspEngine.setEqEnabled(enabled)
    }

    override fun setEQBandGain(band: Int, gainDb: Float) {
        _nativeDspEngine.setEqBandGain(band, gainDb)
    }

    override fun setAllEQBands(gains: FloatArray) {
        _nativeDspEngine.setAllEqBands(gains)
    }

    override fun setLoudnessNormalizationEnabled(enabled: Boolean) {
        _nativeDspEngine.setLoudnessEnabled(enabled)
    }

    override fun setLoudnessTargetLufs(targetLufs: Float) {
        _nativeDspEngine.setTargetLufs(targetLufs)
    }

    private fun buildExtraFromMetadata(metadata: MediaMetadata?): String {
        val title = metadata?.title?.toString() ?: metadata?.displayTitle?.toString() ?: ""
        val artist = metadata?.artist ?: ""
        val album = metadata?.albumTitle ?: ""
        return "{\"title\":\"$title\",\"artist\":\"$artist\",\"album\":\"$album\"}"
    }
}
