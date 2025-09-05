package me.spica27.spicamusic.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import me.spica27.spicamusic.db.entity.PlayHistory

@Dao
interface PlayHistoryDao {
    @Insert
    fun insert(playHistory: PlayHistory)

    @Delete
    fun delete(playHistory: PlayHistory)

    @Query("SELECT * FROM playhistory WHERE mediaId == :mediaId")
    fun getPlayHistory(mediaId: Long): List<PlayHistory>

    @Query("SELECT COUNT(*) FROM playhistory WHERE mediaId == :mediaId")
    fun getPlayCount(mediaId: Long): Long

    @Query("SELECT * FROM playhistory WHERE mediaId == :mediaId ORDER BY time DESC LIMIT 1,1")
    fun getLasePlayHistory(mediaId: Long): PlayHistory?

    @Query("DELETE FROM PlayHistory")
    fun deleteAll()
}
