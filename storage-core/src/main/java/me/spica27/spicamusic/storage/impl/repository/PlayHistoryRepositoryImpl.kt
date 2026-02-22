package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.common.entity.PlayHistory
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.TopSong
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.impl.dao.PlayHistoryDao
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.mapper.toCommon
import me.spica27.spicamusic.storage.impl.mapper.toEntity
import java.util.Calendar

class PlayHistoryRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao,
) : IPlayHistoryRepository {
    override fun getAllPlayHistoryFlow(): Flow<List<PlayHistory>> = flow {
        emit(withContext(Dispatchers.IO) {
            playHistoryDao.getAll().map { it.toCommon() }
        })
    }

    override fun getRecentPlayHistoryFlow(limit: Int): Flow<List<PlayHistory>> = flow {
        emit(withContext(Dispatchers.IO) {
            playHistoryDao.getRecent(limit).map { it.toCommon() }
        })
    }

    override suspend fun addPlayHistory(songId: Long) = withContext(Dispatchers.IO) {
        playHistoryDao.insert(
            PlayHistoryEntity(
                mediaId = songId,
                time = System.currentTimeMillis()
            )
        )
    }

    override suspend fun addPlayHistory(item: PlayHistory) = withContext(Dispatchers.IO) {
        playHistoryDao.insert(item.toEntity())
    }

    override fun insertPlayHistory(item: PlayHistory) {
        // synchronous wrapper (prefer suspend addPlayHistory)
        playHistoryDao.insert(item.toEntity())
    }

    override suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        playHistoryDao.deleteAll()
    }

    override suspend fun deletePlayHistory(songId: Long) = withContext(Dispatchers.IO) {
        playHistoryDao.deleteByMediaId(songId)
    }

    override suspend fun getWeeklyStats(): PlayStats = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        val to = now
        val totalDuration = playHistoryDao.sumPlayedDurationRange(from, to) ?: 0L
        val playCount = playHistoryDao.countPlayEventsRange(from, to)
        val uniqueSongs = playHistoryDao.countDistinctMediaRange(from, to)
        PlayStats(totalDuration, playCount, uniqueSongs)
    }

    override suspend fun getAllTimeStats(): PlayStats = withContext(Dispatchers.IO) {
        val totalDuration = playHistoryDao.totalPlayedDuration() ?: 0L
        val playCount = playHistoryDao.totalPlayEvents()
        val uniqueSongs = playHistoryDao.totalDistinctMedia()
        PlayStats(totalDuration, playCount, uniqueSongs)
    }

    override suspend fun getTopSongsByDuration(limit: Int): List<TopSong> = withContext(Dispatchers.IO) {
        playHistoryDao.topSongsAllTime(limit).map { TopSong(it.mediaId, it.totalDuration, it.playCount) }
    }
}
