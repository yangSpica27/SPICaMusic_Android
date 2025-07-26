package me.spica27.spicamusic.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.spica27.spicamusic.db.entity.Lyric


@Dao
interface LyricDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertLyric(lyric: Lyric)

  @Query("DELETE FROM Lyric WHERE songId = :songId")
  fun deleteLyric(songId: Long)

  @Query("SELECT * FROM Lyric WHERE songId = :songId")
  fun  getLyricWithSongId(songId: Long): List<Lyric>


  @Query("DELETE FROM Lyric")
  fun deleteAll()

}