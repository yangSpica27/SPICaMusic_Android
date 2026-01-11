package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.LyricEntity

@Dao
interface LyricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLyric(lyric: LyricEntity)

    @Query("DELETE FROM Lyric WHERE mediaId = :songId")
    fun deleteLyric(songId: Long)

    @Query("SELECT * FROM Lyric WHERE mediaId = :songId LIMIT 1")
    fun getLyricWithMediaId(songId: Long): LyricEntity?

    @Query("SELECT * FROM Lyric")
    fun getLyrics(): Flow<List<LyricEntity>>

    @Query("DELETE FROM Lyric")
    fun deleteAll()

    @Query("SELECT delay FROM lyric WHERE mediaId == :mediaId LIMIT 1")
    fun getDelayFlow(mediaId: Long): Flow<Long?>

    @Query("SELECT lyrics FROM lyric WHERE mediaId == :mediaId LIMIT 1")
    fun getLyricsFlow(mediaId: Long): Flow<String?>

    @Query("UPDATE Lyric SET delay = :delay WHERE mediaId == :mediaId")
    fun updateDelay(mediaId: Long, delay: Long?)
}
