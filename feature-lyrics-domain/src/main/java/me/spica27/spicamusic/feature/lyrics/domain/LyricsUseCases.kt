package me.spica27.spicamusic.feature.lyrics.domain

import kotlinx.coroutines.CancellationException
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
    data class Success(val cached: CachedLyrics) : LocalLyricsImportResult

    enum class FailureReason {
        READ_FAILED,
        FILE_TOO_LARGE,
        BINARY_FILE,
        UNSUPPORTED_FILE,
        INVALID_CONTENT,
    }

    data class Failure(val reason: FailureReason) : LocalLyricsImportResult
}

/** File-name-aware validation used before a local lyric snapshot is persisted. */
internal object LocalLyricValidator {
    fun isValid(file: LocalLyricFile): Boolean {
        val extension = file.displayName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "ttml", "ttml2", "xml" -> AmllParser.parseDetailed(file.text).items.isNotEmpty()
            "yrc" -> runCatching { YrcParser.parseToLyricItems(file.text).isNotEmpty() }.getOrDefault(false)
            "lrc" -> runCatching { LrcParser.parse(file.text).isNotEmpty() }.getOrDefault(false)
            // .txt and extension-less documents are allowed for unsynchronised lyrics.
            "", "txt" -> file.text.lineSequence().any { it.trim().isNotEmpty() }
            else -> false
        }
    }
}

class LyricsUseCases(
    private val apiClient: ApiClient,
    private val lyricRepository: ILyricRepository,
    private val lyricSourceReader: ILyricSourceReader,
) {
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
            )
        }

    /** 读取音频文件内嵌歌词原始文本，无则返回 null */
    suspend fun getEmbeddedLyrics(mediaStoreId: Long): String? =
        lyricSourceReader.readEmbedded(mediaStoreId)

    /**
     * 导入本地歌词文件：读取内容并**快照入库**（type=LOCAL_FILE, isManual=true），
     * 之后即使原文件被移动/删除也能离线复现。
     * @return 入库后的缓存形态；读取、格式校验或保存失败返回 null
     */
    suspend fun importLocalLyrics(
        mediaStoreId: Long,
        uri: String,
        delayMs: Long,
    ): CachedLyrics? = when (val result = importLocalLyricsResult(mediaStoreId, uri, delayMs)) {
        is LocalLyricsImportResult.Success -> getCachedLyrics(mediaStoreId) ?: result.cached
        is LocalLyricsImportResult.Failure -> null
    }

    /**
     * Reads and validates a local file before touching the cache. A failed import is side-effect free:
     * the existing source and delay remain unchanged.
     */
    suspend fun importLocalLyricsResult(
        mediaStoreId: Long,
        uri: String,
        delayMs: Long,
    ): LocalLyricsImportResult {
        val readResult = try {
            lyricSourceReader.readLocalFileResult(uri)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return LocalLyricsImportResult.Failure(LocalLyricsImportResult.FailureReason.READ_FAILED)
        }
        val file = when (val result = readResult) {
            is LocalLyricReadResult.Success -> result.file
            is LocalLyricReadResult.Failure ->
                return LocalLyricsImportResult.Failure(result.reason.toImportFailureReason())
        }
        if (!LocalLyricValidator.isValid(file)) {
            return LocalLyricsImportResult.Failure(LocalLyricsImportResult.FailureReason.INVALID_CONTENT)
        }
        return try {
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

    suspend fun searchAllLyrics(title: String): List<SongLyrics> = apiClient.searchAllLyrics(title)

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
