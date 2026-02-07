package me.spica27.spicamusic.storage.impl.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.spica27.spicamusic.storage.impl.dao.ExtraInfoDao
import me.spica27.spicamusic.storage.impl.dao.PlayHistoryDao
import me.spica27.spicamusic.storage.impl.dao.PlaylistDao
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlayHistoryEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRefEntity::class, 
                ExtraInfoEntity::class, PlayHistoryEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun lyricDao(): ExtraInfoDao
    abstract fun playHistoryDao(): PlayHistoryDao

}
