package me.spica27.spicamusic.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.Lyric
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.Song


/**
 * 数据库
 */
@Database(
  entities = [Song::class, Playlist::class, PlaylistSongCrossRef::class, Lyric::class],
  version = 11,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

  abstract fun songDao(): SongDao

  abstract fun playlistDao(): PlaylistDao

  abstract fun lyricDao(): LyricDao

}