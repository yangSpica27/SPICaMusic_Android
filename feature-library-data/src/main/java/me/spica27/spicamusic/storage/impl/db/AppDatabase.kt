package me.spica27.spicamusic.storage.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.spica27.spicamusic.storage.impl.dao.AlbumDao
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.dao.PlayHistoryDao
import me.spica27.spicamusic.storage.impl.dao.PlaylistDao
import me.spica27.spicamusic.storage.impl.dao.ScanFolderDao
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.ScanFolderEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRefEntity::class,
        ExtraInfoEntity::class, PlayHistoryEntity::class, AlbumEntity::class,
        ScanFolderEntity::class],
    version = 10,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricDao(): ExtraInfoDao
    abstract fun playHistoryDao(): PlayHistoryDao
    abstract fun albumDao(): AlbumDao
    abstract fun scanFolderDao(): ScanFolderDao

    companion object {
        /** v5 → v6: Song 表新增 dateModified 列，用于增量扫描 */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Song ADD COLUMN dateModified INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v9 → v10: 新增 ScanFolder 表，支持额外扫描文件夹和忽略文件夹 */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ScanFolder (
                        id          INTEGER PRIMARY KEY AUTOINCREMENT,
                        uriString   TEXT    NOT NULL,
                        displayName TEXT    NOT NULL,
                        folderType  INTEGER NOT NULL DEFAULT 0,
                        pathPrefix  TEXT,
                        addedAt     INTEGER NOT NULL DEFAULT 0,
                        isAccessible INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
