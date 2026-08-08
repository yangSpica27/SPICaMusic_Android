package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity

@Dao
interface PlayHistoryDao {
    @Insert
    fun insert(playHistory: PlayHistoryEntity)

    // ── 写入：插事件 + 增量维护汇总表（同一事务，避免崩溃后明细与汇总不一致）──────────
    // 用 INSERT OR IGNORE + UPDATE 两句实现累加，而非 UPSERT——
    // minSdk 29 的 SQLite 3.22 不支持 ON CONFLICT DO UPDATE（需 3.24 / API 30）。
    @Transaction
    fun recordEvent(playHistory: PlayHistoryEntity) {
        insert(playHistory)
        val completed = if (playHistory.isCompleted) 1L else 0L
        // 首次出现：建一条零值行，firstPlayedTime 锁定为本次时间
        insertStatIfAbsent(playHistory.mediaId, playHistory.time)
        // 累加（firstPlayedTime 不动）
        incrementStat(
            mediaId = playHistory.mediaId,
            played = playHistory.playedDuration,
            completed = completed,
            now = playHistory.time,
        )
    }

    @Query(
        """
        INSERT OR IGNORE INTO SongPlayStat
            (mediaId, playCount, totalPlayedDuration, completedCount, lastPlayedTime, firstPlayedTime)
        VALUES (:mediaId, 0, 0, 0, :now, :now)
        """,
    )
    fun insertStatIfAbsent(mediaId: Long, now: Long)

    @Query(
        """
        UPDATE SongPlayStat SET
            playCount = playCount + 1,
            totalPlayedDuration = totalPlayedDuration + :played,
            completedCount = completedCount + :completed,
            lastPlayedTime = :now
        WHERE mediaId = :mediaId
        """,
    )
    fun incrementStat(mediaId: Long, played: Long, completed: Long, now: Long)

    // ── 保留窗裁剪（只删原始明细，绝不动汇总表）────────────────────────────────────
    @Query("DELETE FROM PlayHistory WHERE time < :cutoff")
    fun pruneByTime(cutoff: Long): Int

    @Query(
        "DELETE FROM PlayHistory WHERE id NOT IN " +
            "(SELECT id FROM PlayHistory ORDER BY time DESC LIMIT :keep)",
    )
    fun pruneByCount(keep: Int): Int

    // ── 删除操作同步汇总表 ─────────────────────────────────────────────────────────
    @Query("DELETE FROM SongPlayStat WHERE mediaId = :mediaId")
    fun deleteStatByMediaId(mediaId: Long)

    @Query("DELETE FROM SongPlayStat")
    fun deleteAllStat()

    // ── 全时段聚合改读汇总表（成本 = 曲库大小，不随历史累计增长）─────────────────────
    @Query("SELECT SUM(totalPlayedDuration) FROM SongPlayStat")
    fun statTotalPlayedDuration(): Long?

    @Query("SELECT COALESCE(SUM(playCount), 0) FROM SongPlayStat")
    fun statTotalPlayEvents(): Long

    @Query("SELECT COUNT(*) FROM SongPlayStat")
    fun statDistinctMedia(): Long

    @Query(
        "SELECT mediaId as mediaId, totalPlayedDuration as totalDuration, playCount as playCount " +
            "FROM SongPlayStat ORDER BY totalDuration DESC LIMIT :limit",
    )
    fun statTopSongs(limit: Int): List<TopSongEntity>

    @Delete
    fun delete(playHistory: PlayHistoryEntity)

    @Query("SELECT * FROM PlayHistory WHERE mediaId == :mediaId")
    fun getPlayHistory(mediaId: Long): List<PlayHistoryEntity>

    @Query("SELECT COUNT(*) FROM PlayHistory WHERE mediaId == :mediaId")
    fun getPlayCount(mediaId: Long): Long

    @Query("SELECT * FROM PlayHistory WHERE mediaId == :mediaId ORDER BY time DESC LIMIT 1,1")
    fun getLasePlayHistory(mediaId: Long): PlayHistoryEntity?

    @Query("DELETE FROM PlayHistory WHERE mediaId = :mediaId")
    fun deleteByMediaId(mediaId: Long)

    @Query("DELETE FROM PlayHistory")
    fun deleteAll()

    @Query("SELECT * FROM PlayHistory ORDER BY time DESC")
    fun getAll(): List<PlayHistoryEntity>

    @Query("SELECT * FROM PlayHistory ORDER BY time DESC LIMIT :limit")
    fun getRecent(limit: Int): List<PlayHistoryEntity>

    @Query("SELECT SUM(playedDuration) FROM PlayHistory WHERE time BETWEEN :from AND :to")
    fun sumPlayedDurationRange(from: Long, to: Long): Long?

    @Query("SELECT COUNT(*) FROM PlayHistory WHERE time BETWEEN :from AND :to")
    fun countPlayEventsRange(from: Long, to: Long): Long

    @Query("SELECT COUNT(DISTINCT mediaId) FROM PlayHistory WHERE time BETWEEN :from AND :to")
    fun countDistinctMediaRange(from: Long, to: Long): Long

    @Query("SELECT SUM(playedDuration) FROM PlayHistory")
    fun totalPlayedDuration(): Long?

    @Query("SELECT COUNT(*) FROM PlayHistory")
    fun totalPlayEvents(): Long

    @Query("SELECT COUNT(DISTINCT mediaId) FROM PlayHistory")
    fun totalDistinctMedia(): Long

    data class TopSongEntity(val mediaId: Long, val totalDuration: Long, val playCount: Long)

    @Query("SELECT mediaId as mediaId, SUM(playedDuration) as totalDuration, COUNT(*) as playCount FROM PlayHistory WHERE time BETWEEN :from AND :to GROUP BY mediaId ORDER BY totalDuration DESC LIMIT :limit")
    fun topSongsRange(from: Long, to: Long, limit: Int): List<TopSongEntity>

    @Query("SELECT mediaId as mediaId, SUM(playedDuration) as totalDuration, COUNT(*) as playCount FROM PlayHistory GROUP BY mediaId ORDER BY totalDuration DESC LIMIT :limit")
    fun topSongsAllTime(limit: Int): List<TopSongEntity>
}
