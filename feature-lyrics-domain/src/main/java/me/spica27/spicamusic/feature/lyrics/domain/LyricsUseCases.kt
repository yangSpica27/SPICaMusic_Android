package me.spica27.spicamusic.feature.lyrics.domain

import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.storage.api.ILyricRepository

class LyricsUseCases(
    private val apiClient: ApiClient,
    private val lyricRepository: ILyricRepository,
) {
    suspend fun getCachedLyrics(mediaStoreId: Long): CachedLyrics? =
        lyricRepository.getLyrics(mediaStoreId)?.let { lyric ->
            CachedLyrics(
                mediaId = lyric.mediaId,
                lyrics = lyric.lyrics,
                delay = lyric.delay,
                lyricSourceName = lyric.sourceName,
                cover = lyric.cover,
            )
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
    ) {
        lyricRepository.saveLyrics(
            mediaId = mediaStoreId,
            lyrics = lyrics,
            sourceName = sourceName,
            delay = delayMs,
        )
    }
}
