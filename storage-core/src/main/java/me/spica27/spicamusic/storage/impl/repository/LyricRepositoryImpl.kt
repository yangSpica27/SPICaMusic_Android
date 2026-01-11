package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.Lyric
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.impl.dao.LyricDao
import me.spica27.spicamusic.storage.impl.mapper.toCommon
import me.spica27.spicamusic.storage.impl.mapper.toEntity

class LyricRepositoryImpl(
    private val lyricDao: LyricDao,
) : ILyricRepository {
    override fun getLyricFlowBySongId(songId: Long): Flow<Lyric?> = 
        lyricDao.getLyricsFlow(songId).map { lyrics -> 
            if (lyrics != null) Lyric(songId, lyrics) else null 
        }

    override suspend fun saveLyric(lyric: Lyric) = withContext(Dispatchers.IO) {
        lyricDao.insertLyric(lyric.toEntity())
    }

    override suspend fun deleteLyric(songId: Long) = withContext(Dispatchers.IO) {
        lyricDao.deleteLyric(songId)
    }

    override suspend fun saveLyrics(lyrics: List<Lyric>) = withContext(Dispatchers.IO) {
        lyrics.forEach { lyricDao.insertLyric(it.toEntity()) }
    }
}
