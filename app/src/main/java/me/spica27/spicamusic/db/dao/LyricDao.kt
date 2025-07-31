package me.spica27.spicamusic.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.db.entity.Lyric


@Dao
interface LyricDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertLyric(lyric: Lyric)

  @Query("DELETE FROM Lyric WHERE mediaId = :songId")
  fun deleteLyric(songId: Long)

  @Query("SELECT * FROM Lyric WHERE mediaId = :songId LIMIT 1")
  fun getLyricWithSongId(songId: Long): Lyric?

  @Query("SELECT * FROM Lyric")
  fun getLyrics(): Flow<List<Lyric>>

  @Query("DELETE FROM Lyric")
  fun deleteAll()

  @Query("SELECT delay FROM lyric WHERE mediaId == :mediaId LIMIT 1")
  fun getDelayFlow(mediaId: Long): Flow<Long?>

}