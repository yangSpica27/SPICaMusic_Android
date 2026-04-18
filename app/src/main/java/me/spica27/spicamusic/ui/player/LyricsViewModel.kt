package me.spica27.spicamusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.entity.SongLyrics
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.feature.lyrics.domain.LyricsUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

/**
 * 歌词页面 ViewModel
 * 负责歌词加载（缓存优先）、偏移量持久化、多歌词源管理
 */
class LyricsViewModel(
    private val player: PlayerUseCases,
    private val lyricsUseCases: LyricsUseCases,
) : ViewModel() {
    data class UiState(
        val isLoading: Boolean = false,
        val lyrics: List<LyricItem>? = null,
        val errorMessage: String? = null,
        val lyricsOffsetMs: Long = 0L,
        val allLyricSources: List<SongLyrics> = emptyList(),
        val allParsedLyrics: List<List<LyricItem>> = emptyList(),
        val currentSourceIndex: Int = 0,
        val currentMediaStoreId: Long = 0L,
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class)
            player.currentMediaItem
                .flatMapLatest { mediaItem ->
                    kotlinx.coroutines.flow.flow {
                        emit(mediaItem)
                    }
                }.collect { mediaItem ->
                    loadLyrics(mediaItem?.mediaId, mediaItem?.mediaMetadata?.title?.toString())
                }
        }
    }

    private fun loadLyrics(
        mediaId: String?,
        title: String?,
    ) {
        viewModelScope.launch {
            if (mediaId == null) {
                _uiState.value = UiState()
                return@launch
            }

            val mediaStoreId = mediaId.toLongOrNull() ?: 0L

            _uiState.update {
                UiState(
                    isLoading = true,
                    currentMediaStoreId = mediaStoreId,
                )
            }

            if (title.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "歌曲信息缺失") }
                return@launch
            }

            try {
                // 1. 优先从缓存读取
                val cached =
                    withContext(Dispatchers.IO) {
                        lyricsUseCases.getCachedLyrics(mediaStoreId)
                    }

                var currentLyrics: List<LyricItem>? = null
                var currentOffset = 0L
                var errorMsg: String? = null

                if (cached != null && cached.lyrics.isNotBlank()) {
                    Timber.d("使用缓存歌词: mediaId=$mediaStoreId, source=${cached.lyricSourceName}")
                    currentOffset = cached.delay
                    currentLyrics = parseLyricsInBackground(cached.lyrics)
                    if (currentLyrics.isNullOrEmpty()) {
                        errorMsg = "歌词解析失败"
                        currentLyrics = null
                    }
                }

                // 2. 始终搜索所有源（用于切换面板）
                val results =
                    withContext(Dispatchers.IO) {
                        lyricsUseCases.searchAllLyrics(title)
                    }
                val parsedAll = parseLyricsSourcesInBackground(results)

                // 3. 无缓存时使用第一个结果
                val sourceIndex: Int
                if (cached == null || cached.lyrics.isBlank()) {
                    sourceIndex = 0
                    if (results.isEmpty()) {
                        errorMsg = "暂无歌词"
                    } else {
                        currentLyrics = parsedAll.firstOrNull()?.ifEmpty { null }
                        if (currentLyrics == null) errorMsg = "歌词解析失败"
                    }
                } else {
                    sourceIndex =
                        results
                            .indexOfFirst { "${it.artist} - ${it.name}" == cached.lyricSourceName }
                            .coerceAtLeast(0)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        lyrics = currentLyrics,
                        errorMessage = errorMsg,
                        lyricsOffsetMs = currentOffset,
                        allLyricSources = results,
                        allParsedLyrics = parsedAll,
                        currentSourceIndex = sourceIndex,
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch lyrics")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        // 网络失败但有缓存时保留已加载的歌词
                        errorMessage = if (state.lyrics == null) "加载歌词失败: ${e.message ?: "未知错误"}" else null,
                        allLyricSources = emptyList(),
                        allParsedLyrics = emptyList(),
                    )
                }
            }
        }
    }

    /** 更新歌词偏移量并持久化到数据库 */
    fun updateOffset(offsetMs: Long) {
        val mediaStoreId = _uiState.value.currentMediaStoreId
        _uiState.update { it.copy(lyricsOffsetMs = offsetMs) }
        if (mediaStoreId <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = lyricsUseCases.getCachedLyrics(mediaStoreId)
            if (existing != null) {
                lyricsUseCases.updateDelay(mediaStoreId, offsetMs)
            }
        }
    }

    /** 选择并应用指定索引的歌词源，同时持久化到数据库 */
    fun selectAndSaveLyricSource(index: Int) {
        val state = _uiState.value
        val source = state.allLyricSources.getOrNull(index) ?: return
        val parsed = state.allParsedLyrics.getOrNull(index)

        _uiState.update {
            it.copy(
                currentSourceIndex = index,
                lyrics = if (!parsed.isNullOrEmpty()) parsed else it.lyrics,
                errorMessage = if (!parsed.isNullOrEmpty()) null else it.errorMessage,
            )
        }

        val mediaStoreId = state.currentMediaStoreId
        if (mediaStoreId <= 0L) return

        viewModelScope.launch(Dispatchers.IO) {
            val sourceName = "${source.artist} - ${source.name}"
            lyricsUseCases.saveLyricsSource(mediaStoreId, source.lyrics, sourceName, state.lyricsOffsetMs)
            Timber.d("已缓存歌词: mediaId=$mediaStoreId, source=$sourceName")
        }
    }

    /** 跳转到指定播放位置 */
    fun seekTo(posMs: Long) {
        player.doAction(PlayerAction.SeekTo(posMs))
    }

    /** 获取当前播放位置（毫秒） */
    fun getCurrentPositionMs(): Long = player.currentPosition

    private suspend fun parseLyricsInBackground(lyricsText: String): List<LyricItem>? =
        withContext(Dispatchers.Default) {
            parseLyrics(lyricsText)
        }

    private suspend fun parseLyricsSourcesInBackground(results: List<SongLyrics>): List<List<LyricItem>> =
        withContext(Dispatchers.Default) {
            results.map { parseLyrics(it.lyrics).orEmpty() }
        }

    companion object {
        private fun String.isYrcFormat(): Boolean =
            lineSequence().any { line ->
                line.startsWith("[") && line.contains("](")
            }

        /**
         * 解析歌词文本为 LyricItem 列表
         */
        fun parseLyrics(lyricsText: String): List<LyricItem>? {
            if (lyricsText.isBlank()) return null

            return if (lyricsText.isYrcFormat()) {
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
        }
    }
}
