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
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.mapper.toCommon
import me.spica27.spicamusic.storage.impl.mapper.toEntity
import timber.log.Timber
import java.util.Calendar

class PlayHistoryRepositoryImpl(
    private val playHistoryDao: PlayHistoryDao,
    private val appDatabase: AppDatabase,
) : IPlayHistoryRepository {

    companion object {

        /** 原始明细保留天数（覆盖 YEAR 区间；与行数上限取先到者裁剪）。 */
        private const val RETENTION_DAYS = 365L

        /** 原始明细保留的最大行数（与时间窗取先到者裁剪）。约 15–20MB 上限。 */
        private const val RETENTION_MAX_ROWS = 50_000

        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
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
        // recordEvent 在同一事务里插明细并累加 SongPlayStat 汇总
        playHistoryDao.recordEvent(
            PlayHistoryEntity(
                mediaId = songId,
                time = System.currentTimeMillis()
            )
        )
    }

    override suspend fun addPlayHistory(item: PlayHistory) = withContext(Dispatchers.IO) {
        playHistoryDao.recordEvent(item.toEntity())
    }

    override fun insertPlayHistory(item: PlayHistory) {
        // synchronous wrapper (prefer suspend addPlayHistory)
        playHistoryDao.recordEvent(item.toEntity())
    }

    override suspend fun clearPlayHistory() = withContext(Dispatchers.IO) {
        // 明细与汇总一起清
        playHistoryDao.deleteAll()
        playHistoryDao.deleteAllStat()
    }

    override suspend fun deletePlayHistory(songId: Long) = withContext(Dispatchers.IO) {
        // 删单曲明细的同时移除其汇总行
        playHistoryDao.deleteByMediaId(songId)
        playHistoryDao.deleteStatByMediaId(songId)
    }

    override suspend fun pruneHistory() = withContext(Dispatchers.IO) {
        // 时间窗与行数上限取先到者：两条各删一次，联合效果 = 只留“既在窗内又在最近 N 条”内的明细。
        // 只裁剪原始明细，SongPlayStat 全时段累计不受影响。
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * DAY_MS
        val byTime = playHistoryDao.pruneByTime(cutoff)
        val byCount = playHistoryDao.pruneByCount(RETENTION_MAX_ROWS)
        if (byTime + byCount > 0) {
            // 回收空闲页（auto_vacuum=INCREMENTAL 的库有效；NONE 的库为无操作，空闲页仍会被后续插入复用）
            runCatching {
                appDatabase.openHelper.writableDatabase.execSQL("PRAGMA incremental_vacuum")
            }.onFailure { Timber.w(it, "incremental_vacuum failed") }
        }
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
        // 全时段统计读汇总表：不再全表扫描 PlayHistory，成本 = 曲库大小
        val totalDuration = playHistoryDao.statTotalPlayedDuration() ?: 0L
        val playCount = playHistoryDao.statTotalPlayEvents()
        val uniqueSongs = playHistoryDao.statDistinctMedia()
        PlayStats(totalDuration, playCount, uniqueSongs)
    }

    override suspend fun getTopSongsByDuration(limit: Int): List<TopSong> = withContext(Dispatchers.IO) {
        playHistoryDao.statTopSongs(limit).map { TopSong(it.mediaId, it.totalDuration, it.playCount) }
    }

    override suspend fun getTopSongsByDurationInRange(from: Long, to: Long, limit: Int): List<TopSong> = withContext(Dispatchers.IO) {
        playHistoryDao.topSongsRange(from, to, limit).map { TopSong(it.mediaId, it.totalDuration, it.playCount) }
    }
}
