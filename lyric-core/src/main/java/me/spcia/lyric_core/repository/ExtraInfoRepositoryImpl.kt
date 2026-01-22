package me.spcia.lyric_core.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.ApiClient
import me.spcia.lyric_core.api.IExtraInfoRepository
import me.spica27.spicamusic.common.entity.Lyric
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.mapper.toEntity

class ExtraInfoRepositoryImpl(
  private val extraInfoDao: ExtraInfoDao,
  private val apiClient: ApiClient
) : IExtraInfoRepository {
  override fun getLyricFlowBySongId(songId: Long): Flow<Lyric?> =
    extraInfoDao.getLyricsFlow(songId).map { lyrics ->
      if (lyrics != null) Lyric(songId, lyrics) else null
        }

    override suspend fun saveLyric(lyric: Lyric) = withContext(Dispatchers.IO) {
      extraInfoDao.insertLyric(lyric.toEntity())
    }

    override suspend fun deleteLyric(songId: Long) = withContext(Dispatchers.IO) {
      extraInfoDao.deleteLyric(songId)
    }

    override suspend fun saveLyrics(lyrics: List<Lyric>) = withContext(Dispatchers.IO) {
      lyrics.forEach { extraInfoDao.insertLyric(it.toEntity()) }
    }
}