package me.spica27.spicamusic.feature.lyrics.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spcia.lyric_core.parser.YrcParser
import me.spica27.spicamusic.common.utils.AmllParser
import me.spica27.spicamusic.common.utils.LrcParser
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.LocalLyricFile
import me.spica27.spicamusic.storage.api.LocalLyricReadResult

sealed interface LocalLyricsImportResult {
    data class Success(
        val cached: CachedLyrics,
    ) : LocalLyricsImportResult

    enum class FailureReason {
        READ_FAILED,
        FILE_TOO_LARGE,
        BINARY_FILE,
        UNSUPPORTED_FILE,
        INVALID_CONTENT,
    }

    data class Failure(
        val reason: FailureReason,
    ) : LocalLyricsImportResult
}

/** 保存前按文件扩展名校验歌词内容。 */
internal object LocalLyricValidator {
    fun isValid(file: LocalLyricFile): Boolean {
        val extension = file.displayName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "ttml", "ttml2", "xml" -> AmllParser.parseDetailed(file.text).items.isNotEmpty()
            "yrc" -> runCatching { YrcParser.parseToLyricItems(file.text).isNotEmpty() }.getOrDefault(false)
            "lrc" -> runCatching { LrcParser.parse(file.text).isNotEmpty() }.getOrDefault(false)
            // 文本文件和无扩展名文件允许使用无时间戳歌词。
            "", "txt" -> file.text.lineSequence().any { it.trim().isNotEmpty() }
            else -> false
        }
    }
}

class LyricsUseCases(
    private val lyricRepository: ILyricRepository,
    private val lyricSourceReader: ILyricSourceReader,
    private val searchLyrics: suspend (String, String) -> List<SongLyrics>,
) {
    constructor(apiClient: ApiClient, lyricRepository: ILyricRepository, lyricSourceReader: ILyricSourceReader) :
        this(lyricRepository, lyricSourceReader, apiClient::searchAllLyrics)

    suspend fun getCachedLyrics(mediaStoreId: Long): CachedLyrics? =
        lyricRepository.getLyrics(mediaStoreId)?.let { lyric ->
            CachedLyrics(
                mediaId = lyric.mediaId,
                lyrics = lyric.lyrics,
                delay = lyric.delay,
                lyricSourceName = lyric.sourceName,
                cover = lyric.cover,
                sourceType = lyric.sourceType,
                isManual = lyric.isManual,
                sourceUri = lyric.sourceUri,
                restoredSnapshot = lyric.restoredSnapshot,
                lyricsSuppressed = lyric.lyricsSuppressed,
            )
        }

    /** 按禁用状态、手动快照及自动优先级读取离线歌词。 */
    suspend fun getSelectedOfflineLyrics(mediaStoreId: Long): CachedLyrics? {
        val selected =
            me.spica27.spicamusic.storage.api.SelectedLyricsResolver.resolve(
                mediaStoreId,
                lyricRepository.getLyrics(mediaStoreId),
            ) { lyricSourceReader.readEmbedded(mediaStoreId) } ?: return null
        return CachedLyrics(
            mediaId = selected.mediaId,
            lyrics = selected.lyrics,
            delay = selected.delay,
            lyricSourceName = selected.sourceName,
            cover = selected.cover,
            sourceType = selected.sourceType,
            isManual = selected.isManual,
            sourceUri = selected.sourceUri,
            restoredSnapshot = selected.restoredSnapshot,
            lyricsSuppressed = selected.lyricsSuppressed,
        )
    }

    suspend fun getEmbeddedLyrics(mediaStoreId: Long): String? = lyricSourceReader.readEmbedded(mediaStoreId)

    /**
     * 导入并保存本地歌词快照，不依赖原文件长期存在。
     * @return 保存后的快照，导入失败返回 null。
     */
    suspend fun importLocalLyrics(
        mediaStoreId: Long,
        uri: String,
        delayMs: Long,
    ): CachedLyrics? =
        when (val result = importLocalLyricsResult(mediaStoreId, uri, delayMs)) {
            is LocalLyricsImportResult.Success -> getCachedLyrics(mediaStoreId) ?: result.cached
            is LocalLyricsImportResult.Failure -> null
        }

    /** 校验通过后保存；导入失败时保留原歌词状态。 */
    suspend fun importLocalLyricsResult(
        mediaStoreId: Long,
        uri: String,
        delayMs: Long,
    ): LocalLyricsImportResult {
        val readResult =
            try {
                lyricSourceReader.readLocalFileResult(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return LocalLyricsImportResult.Failure(LocalLyricsImportResult.FailureReason.READ_FAILED)
            }
        val file =
            when (val result = readResult) {
                is LocalLyricReadResult.Success -> result.file
                is LocalLyricReadResult.Failure ->
                    return LocalLyricsImportResult.Failure(result.reason.toImportFailureReason())
            }
        if (!LocalLyricValidator.isValid(file)) {
            return LocalLyricsImportResult.Failure(LocalLyricsImportResult.FailureReason.INVALID_CONTENT)
        }
        return try {
            currentCoroutineContext().ensureActive()
            lyricRepository.saveLyrics(
                mediaId = mediaStoreId,
                lyrics = file.text,
                sourceName = file.displayName,
                delay = delayMs,
                sourceType = "LOCAL_FILE",
                isManual = true,
                sourceUri = uri,
            )
            LocalLyricsImportResult.Success(
                CachedLyrics(
                    mediaId = mediaStoreId,
                    lyrics = file.text,
                    delay = delayMs,
                    lyricSourceName = file.displayName,
                    cover = "",
                    sourceType = "LOCAL_FILE",
                    isManual = true,
                    sourceUri = uri,
                ),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            LocalLyricsImportResult.Failure(LocalLyricsImportResult.FailureReason.READ_FAILED)
        }
    }

    private fun LocalLyricReadResult.FailureReason.toImportFailureReason(): LocalLyricsImportResult.FailureReason =
        when (this) {
            LocalLyricReadResult.FailureReason.UNSUPPORTED_FILE -> LocalLyricsImportResult.FailureReason.UNSUPPORTED_FILE
            LocalLyricReadResult.FailureReason.FILE_TOO_LARGE -> LocalLyricsImportResult.FailureReason.FILE_TOO_LARGE
            LocalLyricReadResult.FailureReason.BINARY_FILE -> LocalLyricsImportResult.FailureReason.BINARY_FILE
            LocalLyricReadResult.FailureReason.EMPTY_FILE -> LocalLyricsImportResult.FailureReason.INVALID_CONTENT
            LocalLyricReadResult.FailureReason.READ_FAILED -> LocalLyricsImportResult.FailureReason.READ_FAILED
        }

    suspend fun searchAllLyrics(title: String, artist: String = ""): List<SongLyrics> = searchLyrics(title, artist)

    suspend fun suppressLyrics(mediaStoreId: Long) = lyricRepository.suppressLyrics(mediaStoreId)

    suspend fun updateDelay(
        mediaStoreId: Long,
        delayMs: Long,
    ) {
        lyricRepository.updateDelay(mediaStoreId, delayMs)
    }

    suspend fun saveLyricsSource(
        mediaStoreId: Long,
        lyrics: String,
        sourceName: String,
        delayMs: Long,
        sourceType: String = "ONLINE",
        isManual: Boolean = true,
        sourceUri: String = "",
    ) {
        if (lyrics.isBlank()) return
        lyricRepository.saveLyrics(
            mediaId = mediaStoreId,
            lyrics = lyrics,
            sourceName = sourceName,
            delay = delayMs,
            sourceType = sourceType,
            isManual = isManual,
            sourceUri = sourceUri,
        )
    }
}
