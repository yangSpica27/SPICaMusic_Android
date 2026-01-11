package me.spica27.spicamusic.storage.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.spica27.spicamusic.storage.impl.dao.*
import me.spica27.spicamusic.storage.impl.entity.*

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRefEntity::class, 
                LyricEntity::class, PlayHistoryEntity::class],
    version = 17,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricDao(): LyricDao
    abstract fun playHistoryDao(): PlayHistoryDao

    companion object {
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Playlist ADD COLUMN playTimes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE Playlist ADD COLUMN createTimestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE Playlist SET createTimestamp = ${System.currentTimeMillis()}")
            }
        }
        
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Song ADD COLUMN isIgnore INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE PlaylistSongCrossRef ADD COLUMN insertTime INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
