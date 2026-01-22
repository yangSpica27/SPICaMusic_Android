package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity

@Dao
interface ExtraInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLyric(lyric: ExtraInfoEntity)

    @Query("DELETE FROM extra_info WHERE mediaId = :songId")
    fun deleteLyric(songId: Long)

    @Query("SELECT * FROM extra_info WHERE mediaId = :songId LIMIT 1")
    fun getLyricWithMediaId(songId: Long): ExtraInfoEntity?

    @Query("SELECT * FROM extra_info")
    fun getLyrics(): Flow<List<ExtraInfoEntity>>

    @Query("DELETE FROM extra_info")
    fun deleteAll()

    @Query("SELECT delay FROM extra_info WHERE mediaId == :mediaId LIMIT 1")
    fun getDelayFlow(mediaId: Long): Flow<Long?>

    @Query("SELECT lyrics FROM extra_info WHERE mediaId == :mediaId LIMIT 1")
    fun getLyricsFlow(mediaId: Long): Flow<String?>

    @Query("UPDATE extra_info SET delay = :delay WHERE mediaId == :mediaId")
    fun updateDelay(mediaId: Long, delay: Long?)
}
