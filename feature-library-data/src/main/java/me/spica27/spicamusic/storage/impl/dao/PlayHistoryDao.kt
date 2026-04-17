package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity

@Dao
interface PlayHistoryDao {
    @Insert
    fun insert(playHistory: PlayHistoryEntity)

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
