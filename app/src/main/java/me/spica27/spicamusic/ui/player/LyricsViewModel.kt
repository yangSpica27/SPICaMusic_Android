package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
 * 歌词页面 ViewModel
 *
 * 负责多来源歌词的加载、选择与偏移量持久化。来源决策遵循「缓存权威 + 自动优先级 + 手动覆盖」：
 * 1. 已存在缓存行（手动选择，或自动持久化的在线结果）→ 直接显示，**不联网、不再解析来源**。
 * 2. 无缓存 → 自动按 **内嵌 > 在线** 决定首屏：内嵌实时读取（不联网、不落库），
 *    仅当无内嵌时才联网搜索一次并落库，避免后续重复联网。
 * 3. 用户在切换面板中的选择记为手动（isManual=1），永久优先。
 *
 * 面板的内嵌 / 在线候选按需懒加载（[openPanel]），本地文件通过 SAF 导入（[importLocalFile]）。
 */
@Stable
class LyricsViewModel(
    private val player: PlayerUseCases,
    private val lyricsUseCases: LyricsUseCases,
) : ViewModel() {
    /** 已解析、可直接渲染的歌词。[isSynced] 为 false 时为无时间戳纯文本，UI 应静态展示、不高亮。 */
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
        // 当前显示的歌词
        val displayed: ParsedLyrics? = null,
        // 当前来源原始文本，用于面板"正在使用"匹配与重存
        val displayedRawText: String? = null,
        val currentSourceType: LyricSourceType = LyricSourceType.NONE,
        // 切换面板三分区（按需懒加载）
        val embeddedSource: LyricSource.Embedded? = null,
        val localSource: LyricSource.LocalFile? = null,
        val onlineSources: List<LyricSource.Online> = emptyList(),
        val onlineLoading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // collectLatest：切歌时取消上一首尚未完成的加载，避免结果错位
            player.currentMediaItem.collectLatest { mediaItem ->
                loadLyrics(mediaItem?.mediaId, mediaItem?.mediaMetadata?.title?.toString())
            }
        }
    }

    private suspend fun loadLyrics(
        mediaId: String?,
        title: String?,
    ) {
        if (mediaId == null) {
            _uiState.value = UiState()
            return
        }

        val id = mediaId.toLongOrNull() ?: 0L
        _uiState.value = UiState(isLoading = true, currentMediaStoreId = id, currentTitle = title)

        try {
            val cached = withContext(Dispatchers.IO) { lyricsUseCases.getCachedLyrics(id) }
            // 持久化的偏移量按歌恢复，应用到任何最终展示的来源（含实时内嵌）
            val savedDelay = cached?.delay ?: 0L

            // 1. 手动锁定的缓存：权威来源，跳过自动优先级，直接显示、不联网
            if (cached != null && cached.isManual && cached.lyrics.isNotBlank()) {
                val parsed = parseOffMain(cached.lyrics)
                val type =
                    runCatching { LyricSourceType.valueOf(cached.sourceType) }
                        .getOrDefault(LyricSourceType.ONLINE)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayed = parsed,
                        displayedRawText = cached.lyrics,
                        currentSourceType = type,
                        lyricsOffsetMs = savedDelay,
                        errorMessage = if (parsed.items.isEmpty()) "歌词解析失败" else null,
                        localSource =
                            if (type == LyricSourceType.LOCAL_FILE) {
                                LyricSource.LocalFile(
                                    uri = cached.sourceUri,
                                    fileName = cached.lyricSourceName,
                                    rawLyrics = cached.lyrics,
                                )
                            } else {
                                null
                            },
                    )
                }
                return
            }

            // 2. 自动优先级：内嵌优先（实时读取，不联网、不落库）
            val embedded = if (id > 0L) withContext(Dispatchers.IO) { lyricsUseCases.getEmbeddedLyrics(id) } else null
            if (!embedded.isNullOrBlank()) {
                val parsed = parseOffMain(embedded)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayed = parsed,
                        displayedRawText = embedded,
                        currentSourceType = LyricSourceType.EMBEDDED,
                        embeddedSource = LyricSource.Embedded(rawLyrics = embedded),
                        lyricsOffsetMs = savedDelay,
                        errorMessage = if (parsed.items.isEmpty()) "歌词解析失败" else null,
                    )
                }
                return
            }

            // 3. 自动优先级：无内嵌时用已自动缓存的在线结果（不重复联网）
            if (cached != null && cached.lyrics.isNotBlank()) {
                val parsed = parseOffMain(cached.lyrics)
                val type =
                    runCatching { LyricSourceType.valueOf(cached.sourceType) }
                        .getOrDefault(LyricSourceType.ONLINE)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        displayed = parsed,
                        displayedRawText = cached.lyrics,
                        currentSourceType = type,
                        lyricsOffsetMs = savedDelay,
                        errorMessage = if (parsed.items.isEmpty()) "歌词解析失败" else null,
                    )
                }
                return
            }

            // 4. 自动优先级：在线兜底（联网搜索一次并落库，后续读缓存不再联网）
            if (title.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "歌曲信息缺失") }
                return
            }
            val results = withContext(Dispatchers.IO) { lyricsUseCases.searchAllLyrics(title) }
            if (results.isEmpty()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "暂无歌词") }
                return
            }
            val first = results.first()
            val parsed = parseOffMain(first.lyrics)
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
            // 持久化自动在线结果（isManual=false），下次读缓存直接命中；
            // isManual=false 保证后续若出现内嵌仍走内嵌优先（见分支 2）
            if (id > 0L) {
                withContext(Dispatchers.IO) {
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
        } catch (e: Exception) {
            Timber.e(e, "Failed to load lyrics")
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    // 网络失败但已有显示内容时保留
                    errorMessage = if (state.displayed == null) "加载歌词失败: ${e.message ?: "未知错误"}" else null,
                )
            }
        }
    }

    /** 打开切换面板：按需加载内嵌（实时）与在线（联网）候选到对应分区 */
    fun openPanel() {
        val id = _uiState.value.currentMediaStoreId
        val title = _uiState.value.currentTitle
        viewModelScope.launch {
            if (_uiState.value.embeddedSource == null && id > 0L) {
                val emb = withContext(Dispatchers.IO) { lyricsUseCases.getEmbeddedLyrics(id) }
                // 期间可能已切歌，仅当仍是同一首时才写回，避免把上一首的候选灌进新歌
                if (!emb.isNullOrBlank() && _uiState.value.currentMediaStoreId == id) {
                    _uiState.update { it.copy(embeddedSource = LyricSource.Embedded(rawLyrics = emb)) }
                }
            }
            if (_uiState.value.onlineSources.isEmpty() && !title.isNullOrBlank()) {
                _uiState.update { it.copy(onlineLoading = true) }
                val results =
                    try {
                        withContext(Dispatchers.IO) { lyricsUseCases.searchAllLyrics(title) }
                    } catch (e: Exception) {
                        Timber.w(e, "在线歌词搜索失败")
                        emptyList()
                    }
                // 搜索是挂起点，返回时可能已切歌：结果只属于发起时的那首歌
                if (_uiState.value.currentMediaStoreId == id) {
                    _uiState.update { it.copy(onlineLoading = false, onlineSources = results.toOnlineSources()) }
                }
            }
        }
    }

    /** 手动选择某个来源（内嵌 / 在线候选），快照入库并标记为手动锁定 */
    fun selectSource(source: LyricSource) {
        val id = _uiState.value.currentMediaStoreId
        val offset = _uiState.value.lyricsOffsetMs
        viewModelScope.launch {
            val parsed = parseOffMain(source.rawLyrics)
            if (parsed.items.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "歌词为空或无法解析") }
                return@launch
            }
            _uiState.update {
                it.copy(
                    displayed = parsed,
                    displayedRawText = source.rawLyrics,
                    currentSourceType = source.type,
                    errorMessage = null,
                )
            }
            if (id <= 0L) return@launch
            withContext(Dispatchers.IO) {
                lyricsUseCases.saveLyricsSource(
                    mediaStoreId = id,
                    lyrics = source.rawLyrics,
                    sourceName = source.title,
                    delayMs = offset,
                    sourceType = source.type.name,
                    isManual = true,
                    sourceUri = (source as? LyricSource.LocalFile)?.uri.orEmpty(),
                )
            }
        }
    }

    /** 导入本地歌词文件：验证通过后才快照入库（LOCAL_FILE + 手动锁定）。 */
    fun importLocalFile(uri: String) {
        val id = _uiState.value.currentMediaStoreId
        if (id <= 0L) {
            _uiState.update { it.copy(errorMessage = "请先播放一首歌曲再导入歌词") }
            return
        }
        val offset = _uiState.value.lyricsOffsetMs
        viewModelScope.launch {
            val result =
                withContext(Dispatchers.IO) {
                    lyricsUseCases.importLocalLyricsResult(id, uri, offset)
                }
            if (_uiState.value.currentMediaStoreId != id) return@launch
            when (result) {
                is LocalLyricsImportResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            // 读取器和内容验证均在写缓存前完成，失败时保留当前歌词与来源。
                            errorMessage =
                                when (result.reason) {
                                    LocalLyricsImportResult.FailureReason.INVALID_CONTENT -> "歌词文件为空或无法解析"
                                    LocalLyricsImportResult.FailureReason.UNSUPPORTED_FILE -> "不支持的文件类型，请选择歌词文件"
                                    LocalLyricsImportResult.FailureReason.FILE_TOO_LARGE -> "歌词文件过大（最大 4 MiB）"
                                    LocalLyricsImportResult.FailureReason.BINARY_FILE -> "文件不是有效的文本歌词"
                                    LocalLyricsImportResult.FailureReason.READ_FAILED -> "无法读取该歌词文件"
                                },
                        )
                    }
                    return@launch
                }

                is LocalLyricsImportResult.Success -> {
                    val cached = result.cached
                    val parsed = parseOffMain(cached.lyrics)
                    if (parsed.items.isEmpty()) {
                        // 防御性检查：UseCase 已验证，解析器升级后仍不应污染当前显示。
                        _uiState.update { it.copy(errorMessage = "歌词文件为空或无法解析") }
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            displayed = parsed,
                            displayedRawText = cached.lyrics,
                            currentSourceType = LyricSourceType.LOCAL_FILE,
                            localSource =
                                LyricSource.LocalFile(
                                    uri = uri,
                                    fileName = cached.lyricSourceName,
                                    rawLyrics = cached.lyrics,
                                ),
                            lyricsOffsetMs = offset,
                            errorMessage = null,
                        )
                    }
                }
            }
        }
    }

    /**
     * 更新歌词偏移量并持久化。
     * 内嵌歌词首屏不落库（见 loadLyrics 分支 2），此处首次调整时把当前来源快照入库，
     * 否则偏移量在切歌/重进后丢失。
     */
    fun updateOffset(offsetMs: Long) {
        val state = _uiState.value
        val mediaStoreId = state.currentMediaStoreId
        _uiState.update { it.copy(lyricsOffsetMs = offsetMs) }
        if (mediaStoreId <= 0L) return
        val rawText = state.displayedRawText
        val sourceType = state.currentSourceType
        viewModelScope.launch(Dispatchers.IO) {
            val existing = lyricsUseCases.getCachedLyrics(mediaStoreId)
            if (existing != null) {
                lyricsUseCases.updateDelay(mediaStoreId, offsetMs)
            } else if (!rawText.isNullOrBlank()) {
                // 尚无缓存行（当前多为实时内嵌）：快照入库以承载偏移量。
                // isManual=false：不改变来源优先级，仅让 delay 得以持久化。
                lyricsUseCases.saveLyricsSource(
                    mediaStoreId = mediaStoreId,
                    lyrics = rawText,
                    sourceName = sourceType.name,
                    delayMs = offsetMs,
                    sourceType = sourceType.name,
                    isManual = false,
                )
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
    private suspend fun parseOffMain(text: String): ParsedLyrics = withContext(Dispatchers.Default) { parseAnyLyrics(text) }

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

            // AMLL was already parsed above; only try the other timestamp formats here.
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
