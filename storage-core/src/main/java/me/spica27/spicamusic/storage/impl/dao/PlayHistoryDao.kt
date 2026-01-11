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

    @Query("SELECT * FROM playhistory WHERE mediaId == :mediaId")
    fun getPlayHistory(mediaId: Long): List<PlayHistoryEntity>

    @Query("SELECT COUNT(*) FROM playhistory WHERE mediaId == :mediaId")
    fun getPlayCount(mediaId: Long): Long

    @Query("SELECT * FROM playhistory WHERE mediaId == :mediaId ORDER BY time DESC LIMIT 1,1")
    fun getLasePlayHistory(mediaId: Long): PlayHistoryEntity?

    @Query("DELETE FROM PlayHistory")
    fun deleteAll()
}
