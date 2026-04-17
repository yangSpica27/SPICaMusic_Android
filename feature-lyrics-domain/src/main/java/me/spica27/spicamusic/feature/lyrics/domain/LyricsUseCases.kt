package me.spica27.spicamusic.feature.lyrics.domain

import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity

class LyricsUseCases(
    private val apiClient: ApiClient,
    private val extraInfoDao: ExtraInfoDao,
) {
    suspend fun getCachedLyrics(mediaStoreId: Long): ExtraInfoEntity? = extraInfoDao.getLyricWithMediaId(mediaStoreId)

    suspend fun searchAllLyrics(title: String): List<SongLyrics> = apiClient.searchAllLyrics(title)

    suspend fun updateDelay(
        mediaStoreId: Long,
        delayMs: Long,
    ) {
        extraInfoDao.updateDelay(mediaStoreId, delayMs)
    }

    suspend fun saveLyricsSource(
        mediaStoreId: Long,
        lyrics: String,
        sourceName: String,
        delayMs: Long,
    ) {
        val existing = extraInfoDao.getLyricWithMediaId(mediaStoreId)
        if (existing != null) {
            extraInfoDao.updateLyricsAndSource(mediaStoreId, lyrics, sourceName)
            if (existing.delay != delayMs) {
                extraInfoDao.updateDelay(mediaStoreId, delayMs)
            }
            return
        }

        extraInfoDao.insertLyric(
            ExtraInfoEntity(
                mediaId = mediaStoreId,
                lyrics = lyrics,
                lyricSourceName = sourceName,
                delay = delayMs,
            ),
        )
    }
}
