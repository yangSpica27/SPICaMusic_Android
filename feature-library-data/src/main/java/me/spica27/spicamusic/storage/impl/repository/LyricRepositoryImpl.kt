package me.spica27.spicamusic.storage.impl.repository

import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.StoredLyrics
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity

class LyricRepositoryImpl(
    private val extraInfoDao: ExtraInfoDao,
) : ILyricRepository {
    override suspend fun getLyrics(mediaId: Long): StoredLyrics? =
        extraInfoDao.getLyricWithMediaId(mediaId)?.let { lyric ->
            StoredLyrics(
                mediaId = lyric.mediaId,
                lyrics = lyric.lyrics,
                cover = lyric.cover,
                delay = lyric.delay,
                sourceName = lyric.lyricSourceName,
                sourceType = lyric.sourceType,
                isManual = lyric.isManual,
                sourceUri = lyric.sourceUri,
            )
        }

    override suspend fun updateDelay(
        mediaId: Long,
        delay: Long,
    ) {
        extraInfoDao.updateDelay(mediaId, delay)
    }

    override suspend fun saveLyrics(
        mediaId: Long,
        lyrics: String,
        cover: String,
        sourceName: String,
        delay: Long,
        sourceType: String,
        isManual: Boolean,
        sourceUri: String,
    ) {
        val existing = extraInfoDao.getLyricWithMediaId(mediaId)
        if (existing != null) {
            extraInfoDao.updateLyricsAndSource(mediaId, lyrics, sourceName, sourceType, isManual, sourceUri)
            if (existing.delay != delay) {
                extraInfoDao.updateDelay(mediaId, delay)
            }
            return
        }

        extraInfoDao.insertLyric(
            ExtraInfoEntity(
                mediaId = mediaId,
                lyrics = lyrics,
                cover = cover,
                delay = delay,
                lyricSourceName = sourceName,
                sourceType = sourceType,
                isManual = isManual,
                sourceUri = sourceUri,
            ),
        )
    }
}
