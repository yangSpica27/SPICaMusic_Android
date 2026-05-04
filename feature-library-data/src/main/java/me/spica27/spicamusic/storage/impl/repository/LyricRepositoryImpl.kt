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
    ) {
        val existing = extraInfoDao.getLyricWithMediaId(mediaId)
        if (existing != null) {
            extraInfoDao.updateLyricsAndSource(mediaId, lyrics, sourceName)
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
            ),
        )
    }
}
