package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.entity.SongLyrics
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.LyricSource
import me.spica27.spicamusic.common.entity.LyricSourceType
import me.spica27.spicamusic.common.utils.AmllParser
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.feature.lyrics.domain.LocalLyricsImportResult
import me.spica27.spicamusic.feature.lyrics.domain.LyricsUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

/**
 * 管理歌词加载、来源选择和时间偏移。
 * 选择“无匹配歌词”后保留快照并停止自动加载。
 * 优先使用手动或恢复的快照，否则依次尝试内嵌、缓存和在线歌词。
 * 候选按需加载，手动选择保存为快照。
 */
@Stable
class LyricsViewModel(
    private val player: PlayerUseCases,
    private val lyricsUseCases: LyricsUseCases,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val parseDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    /** 解析后的歌词；[isSynced] 为 false 时静态显示。 */
    data class ParsedLyrics(
        val items: List<LyricItem>,
        val isSynced: Boolean,
        val amllMetadata: AmllParser.Metadata? = null,
        val parseWarnings: List<String> = emptyList(),
        val parseError: String? = null,
    )

    data class UiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val lyricsOffsetMs: Long = 0L,
        val currentMediaStoreId: Long = 0L,
        val currentTitle: String? = null,
        val currentArtist: String? = null,
        // 当前显示的歌词
        val displayed: ParsedLyrics? = null,
        // 当前来源原始文本，用于面板"正在使用"匹配与重存
        val displayedRawText: String? = null,
        val currentSourceType: LyricSourceType = LyricSourceType.NONE,
        val lyricsSuppressed: Boolean = false,
        // 切换面板三分区（按需懒加载）
        val embeddedSource: LyricSource.Embedded? = null,
        val localSource: LyricSource.LocalFile? = null,
        val onlineSources: List<LyricSource.Online> = emptyList(),
        val cachedOnlineSource: LyricSource.Online? = null,
        val onlineLoading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // 串行写入，防止已取消任务的数据库写入覆盖新选择。
    // 切歌后仍将手动选择保存到原歌曲。
    private val writeMutex = Mutex()
    private var loadJob: Job? = null
    private var panelJob: Job? = null

    private data class SelectionTask(
        val job: Job,
        val suppress: Boolean,
    )

    private val selectionJobs = mutableMapOf<Long, SelectionTask>()
    private var generation = 0L
    private var songGeneration = 0L

    init {
        viewModelScope.launch {
            player.currentMediaItem.collect { mediaItem ->
                generation++
                songGeneration++
                loadJob?.cancel()
                panelJob?.cancel()
                val version = generation
                loadJob =
                    launch {
                        loadLyrics(
                            mediaItem?.mediaId,
                            mediaItem?.mediaMetadata?.title?.toString(),
                            mediaItem?.mediaMetadata?.artist?.toString(),
                            version,
                        )
                    }
            }
        }
    }

    private fun isCurrent(
        id: Long,
        version: Long,
    ): Boolean = generation == version && _uiState.value.currentMediaStoreId == id

    private suspend fun loadLyrics(
        mediaId: String?,
        title: String?,
        artist: String?,
        version: Long,
    ) {
        if (mediaId == null) {
            _uiState.value = UiState()
            return
        }
        val id = mediaId.toLongOrNull() ?: 0L
        _uiState.value = UiState(isLoading = true, currentMediaStoreId = id, currentTitle = title, currentArtist = artist)
        try {
            val selected =
                writeMutex.withLock {
                    withContext(ioDispatcher) { lyricsUseCases.getSelectedOfflineLyrics(id) }
                }
            if (!isCurrent(id, version)) return
            if (selected != null && (selected.lyricsSuppressed || selected.lyrics.isNotBlank())) {
                val type =
                    runCatching { LyricSourceType.valueOf(selected.sourceType) }
                        .getOrDefault(LyricSourceType.ONLINE)
                val parsed = if (selected.lyricsSuppressed) null else parseOffMain(selected.lyrics)
                if (!isCurrent(id, version)) return
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayed = parsed,
                        displayedRawText = selected.lyrics.takeUnless { selected.lyricsSuppressed },
                        currentSourceType = type,
                        lyricsSuppressed = selected.lyricsSuppressed,
                        lyricsOffsetMs = selected.delay,
                        errorMessage = if (parsed?.items?.isEmpty() == true) "歌词解析失败" else null,
                        embeddedSource =
                            if (type == LyricSourceType.EMBEDDED && selected.lyrics.isNotBlank()) {
                                LyricSource.Embedded(rawLyrics = selected.lyrics)
                            } else {
                                null
                            },
                        cachedOnlineSource =
                            if (type == LyricSourceType.ONLINE && selected.lyrics.isNotBlank()) {
                                LyricSource.Online(
                                    id = 0,
                                    title = title ?: selected.lyricSourceName,
                                    subtitle = selected.lyricSourceName,
                                    rawLyrics = selected.lyrics,
                                    stableKey = "cached-online:$id",
                                )
                            } else {
                                null
                            },
                        localSource =
                            if (type == LyricSourceType.LOCAL_FILE && selected.lyrics.isNotBlank()) {
                                LyricSource.LocalFile(selected.sourceUri, selected.lyricSourceName, selected.lyrics)
                            } else {
                                null
                            },
                    )
                }
                return
            }
            if (title.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "歌曲信息缺失") }
                return
            }
            val results = withContext(ioDispatcher) { lyricsUseCases.searchAllLyrics(title, artist.orEmpty()) }
            if (!isCurrent(id, version)) return
            if (results.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "暂无歌词") }
                return
            }
            val first = results.first()
            val parsed = parseOffMain(first.lyrics)
            if (!isCurrent(id, version)) return
            writeMutex.withLock {
                currentCoroutineContext().ensureActive()
                if (!isCurrent(id, version)) return
                if (id > 0L) {
                    withContext(ioDispatcher) {
                        lyricsUseCases.saveLyricsSource(
                            mediaStoreId = id,
                            lyrics = first.lyrics,
                            sourceName = "${first.artist} - ${first.name}",
                            delayMs = 0L,
                            sourceType = LyricSourceType.ONLINE.name,
                            isManual = false,
                        )
                    }
                }
                if (!isCurrent(id, version)) return
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayed = parsed,
                        displayedRawText = first.lyrics,
                        currentSourceType = LyricSourceType.ONLINE,
                        onlineSources = results.toOnlineSources(),
                        errorMessage = if (parsed.items.isEmpty()) "歌词解析失败" else null,
                    )
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to load lyrics")
            if (isCurrent(id, version)) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "加载歌词失败: ${e.message ?: "未知错误"}") }
            }
        }
    }

    /** 仅加载候选，不改变当前歌词选择。 */
    fun openPanel() {
        val state = _uiState.value
        val id = state.currentMediaStoreId
        val epoch = songGeneration
        panelJob?.cancel()
        panelJob =
            viewModelScope.launch {
                try {
                    if (state.embeddedSource == null && id > 0L) {
                        val embedded = withContext(ioDispatcher) { lyricsUseCases.getEmbeddedLyrics(id) }
                        if (epoch != songGeneration) return@launch
                        if (!embedded.isNullOrBlank()) {
                            _uiState.update { it.copy(embeddedSource = LyricSource.Embedded(rawLyrics = embedded)) }
                        }
                    }
                    if (_uiState.value.onlineSources.isEmpty() && !state.currentTitle.isNullOrBlank()) {
                        _uiState.update { it.copy(onlineLoading = true) }
                        val results =
                            withContext(ioDispatcher) {
                                lyricsUseCases.searchAllLyrics(state.currentTitle, state.currentArtist.orEmpty())
                            }
                        if (epoch == songGeneration) {
                            _uiState.update { it.copy(onlineSources = results.toOnlineSources()) }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "歌词候选加载失败")
                } finally {
                    if (epoch == songGeneration) _uiState.update { it.copy(onlineLoading = false) }
                }
            }
    }

    private fun startSelection(
        suppress: Boolean = false,
        action: suspend (UiState, Long) -> Unit,
    ) {
        val state = _uiState.value
        val id = state.currentMediaStoreId
        if (id <= 0L) return
        generation++
        val version = generation
        loadJob?.cancel()
        // 先保存“无匹配歌词”，避免后续无效选择导致设置丢失。
        selectionJobs[id]?.takeUnless { it.suppress }?.job?.cancel()
        _uiState.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                lyricsSuppressed = suppress || it.lyricsSuppressed,
                displayed = if (suppress) null else it.displayed,
                displayedRawText = if (suppress) null else it.displayedRawText,
            )
        }
        val job =
            viewModelScope.launch {
                try {
                    writeMutex.withLock {
                        currentCoroutineContext().ensureActive()
                        action(state, version)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Failed to save lyric selection")
                    if (isCurrent(id, version)) {
                        _uiState.update {
                            it.copy(errorMessage = if (suppress) "无匹配歌词设置未保存成功，请重新选择" else "歌词选择未保存成功，请重试")
                        }
                    }
                }
            }
        selectionJobs[id] = SelectionTask(job, suppress)
        job.invokeOnCompletion {
            if (selectionJobs[id]?.job === job) selectionJobs.remove(id)
        }
    }

    fun selectNoMatchingLyrics() {
        startSelection(suppress = true) { state, _ ->
            withContext(ioDispatcher) { lyricsUseCases.suppressLyrics(state.currentMediaStoreId) }
        }
    }

    /** 新歌词校验并保存成功后，才解除禁用。 */
    fun selectSource(source: LyricSource) {
        startSelection { state, version ->
            val id = state.currentMediaStoreId
            val parsed = parseOffMain(source.rawLyrics)
            if (parsed.items.isEmpty()) {
                if (isCurrent(id, version)) _uiState.update { it.copy(errorMessage = "歌词为空或无法解析") }
                return@startSelection
            }
            withContext(ioDispatcher) {
                lyricsUseCases.saveLyricsSource(
                    mediaStoreId = id,
                    lyrics = source.rawLyrics,
                    sourceName = source.title,
                    delayMs = state.lyricsOffsetMs,
                    sourceType = source.type.name,
                    isManual = true,
                    sourceUri = (source as? LyricSource.LocalFile)?.uri.orEmpty(),
                )
            }
            if (isCurrent(id, version)) {
                _uiState.update {
                    it.copy(
                        displayed = parsed,
                        displayedRawText = source.rawLyrics,
                        currentSourceType = source.type,
                        lyricsSuppressed = false,
                        localSource = (source as? LyricSource.LocalFile) ?: it.localSource,
                        cachedOnlineSource = (source as? LyricSource.Online) ?: it.cachedOnlineSource,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun importLocalFile(uri: String) {
        startSelection { state, version ->
            val id = state.currentMediaStoreId
            val result =
                withContext(ioDispatcher) {
                    lyricsUseCases.importLocalLyricsResult(id, uri, state.lyricsOffsetMs)
                }
            if (!isCurrent(id, version)) return@startSelection
            when (result) {
                is LocalLyricsImportResult.Failure ->
                    _uiState.update {
                        it.copy(
                            errorMessage =
                                when (result.reason) {
                                    LocalLyricsImportResult.FailureReason.INVALID_CONTENT -> "歌词文件为空或无法解析"
                                    LocalLyricsImportResult.FailureReason.UNSUPPORTED_FILE -> "不支持的文件类型，请选择歌词文件"
                                    LocalLyricsImportResult.FailureReason.FILE_TOO_LARGE -> "歌词文件过大（最大 4 MiB）"
                                    LocalLyricsImportResult.FailureReason.BINARY_FILE -> "文件不是有效的文本歌词"
                                    LocalLyricsImportResult.FailureReason.READ_FAILED -> "无法读取或保存该歌词文件"
                                },
                        )
                    }
                is LocalLyricsImportResult.Success -> {
                    val cached = result.cached
                    val parsed = parseOffMain(cached.lyrics)
                    if (!isCurrent(id, version)) return@startSelection
                    _uiState.update {
                        it.copy(
                            displayed = parsed,
                            displayedRawText = cached.lyrics,
                            currentSourceType = LyricSourceType.LOCAL_FILE,
                            lyricsSuppressed = false,
                            localSource = LyricSource.LocalFile(uri, cached.lyricSourceName, cached.lyrics),
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /** 禁用歌词时保留原时间偏移。 */
    fun updateOffset(offsetMs: Long) {
        val state = _uiState.value
        if (state.lyricsSuppressed) return
        val id = state.currentMediaStoreId
        val version = generation
        _uiState.update { it.copy(lyricsOffsetMs = offsetMs) }
        if (id <= 0L) return
        viewModelScope.launch {
            writeMutex.withLock {
                if (!isCurrent(id, version)) return@withLock
                withContext(ioDispatcher) {
                    val existing = lyricsUseCases.getCachedLyrics(id)
                    if (existing?.lyricsSuppressed == true) return@withContext
                    if (existing != null) {
                        lyricsUseCases.updateDelay(id, offsetMs)
                    } else if (!state.displayedRawText.isNullOrBlank()) {
                        lyricsUseCases.saveLyricsSource(
                            mediaStoreId = id,
                            lyrics = state.displayedRawText,
                            sourceName = state.currentSourceType.name,
                            delayMs = offsetMs,
                            sourceType = state.currentSourceType.name,
                            isManual = false,
                        )
                    }
                }
            }
        }
    }

    /** 跳转到指定播放位置 */
    fun seekTo(posMs: Long) {
        player.doAction(PlayerAction.SeekTo(posMs))
    }

    /** 获取当前播放位置（毫秒） */
    fun getCurrentPositionMs(): Long = player.currentPosition

    /** 在后台线程解析歌词，避免大段 YRC/LRC 阻塞主线程 */
    private suspend fun parseOffMain(text: String): ParsedLyrics = withContext(parseDispatcher) { parseAnyLyrics(text) }

    private fun List<SongLyrics>.toOnlineSources(): List<LyricSource.Online> =
        map { s ->
            LyricSource.Online(
                id = s.id,
                title = s.name,
                subtitle = s.artist,
                album = s.album,
                albumArt = s.albumArt,
                duration = s.duration,
                rawLyrics = s.lyrics,
            )
        }

    companion object {
        private fun String.isYrcFormat(): Boolean =
            lineSequence().any { line ->
                line.startsWith("[") && line.contains("](")
            }

        /**
         * 解析带时间戳的歌词文本为 LyricItem 列表（AMLL/TTML、YRC、LRC）。
         * 无时间戳的纯文本会返回空——纯文本兜底见 [parseAnyLyrics]。
         */
        fun parseLyrics(lyricsText: String): List<LyricItem>? {
            if (lyricsText.isBlank()) return null

            val amll = AmllParser.parse(lyricsText)
            if (amll.isNotEmpty()) return amll

            return parseNonAmllLyrics(lyricsText)
        }

        private fun parseNonAmllLyrics(lyricsText: String): List<LyricItem> =
            if (lyricsText.isYrcFormat()) {
                try {
                    YrcParser.parseToLyricItems(lyricsText).ifEmpty {
                        LrcParser.parse(lyricsText)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "YRC parse failed, fallback to LRC")
                    LrcParser.parse(lyricsText)
                }
            } else {
                LrcParser.parse(lyricsText)
            }

        /**
         * 解析任意歌词文本：优先按时间戳解析（[parseLyrics]）；
         * 若无有效时间戳（内嵌/本地常见的纯文本），按行降级为静态 NormalLyric，[ParsedLyrics.isSynced] = false。
         */
        fun parseAnyLyrics(lyricsText: String): ParsedLyrics {
            if (lyricsText.isBlank()) return ParsedLyrics(emptyList(), isSynced = false)

            val amllResult = AmllParser.parseDetailed(lyricsText)
            if (amllResult.items.isNotEmpty()) {
                return ParsedLyrics(
                    items = amllResult.items,
                    isSynced = true,
                    amllMetadata = amllResult.metadata,
                    parseWarnings = amllResult.warnings,
                    parseError = amllResult.error,
                )
            }

            // AMLL 已解析，仅尝试其他时间戳格式。
            val synced = parseNonAmllLyrics(lyricsText)
            if (!synced.isNullOrEmpty()) {
                return ParsedLyrics(
                    items = synced,
                    isSynced = true,
                    amllMetadata = amllResult.metadata.takeIf { it != AmllParser.Metadata() },
                    parseWarnings = amllResult.warnings,
                    parseError = amllResult.error,
                )
            }

            // 纯文本兜底：无时间戳，按非空行静态展示（time 仅用于稳定排序，UI 依 isSynced 关闭高亮/seek）
            val items =
                lyricsText
                    .lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .mapIndexed { index, line ->
                        LyricItem.NormalLyric(
                            content = line,
                            translation = null,
                            time = index.toLong(),
                            key = "plain:$index",
                        )
                    }.toList()
            return ParsedLyrics(
                items = items,
                isSynced = false,
                amllMetadata = amllResult.metadata.takeIf { it != AmllParser.Metadata() },
                parseWarnings = amllResult.warnings,
                parseError = amllResult.error,
            )
        }
    }
}
