package me.spica27.spicamusic.feature.lyrics.domain

import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.ILyricSourceReader

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
     * @return 入库后的缓存形态；文件读取失败返回 null
     */
    suspend fun importLocalLyrics(
        mediaStoreId: Long,
        uri: String,
        delayMs: Long,
    ): CachedLyrics? {
        val file = lyricSourceReader.readLocalFile(uri) ?: return null
        lyricRepository.saveLyrics(
            mediaId = mediaStoreId,
            lyrics = file.text,
            sourceName = file.displayName,
            delay = delayMs,
            sourceType = "LOCAL_FILE",
            isManual = true,
            sourceUri = uri,
        )
        return getCachedLyrics(mediaStoreId)
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
