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
import me.spica27.spicamusic.storage.impl.entity.SongPlayStatEntity

@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRefEntity::class,
        ExtraInfoEntity::class, PlayHistoryEntity::class, AlbumEntity::class,
        ScanFolderEntity::class, SongPlayStatEntity::class],
    version = 19,
    exportSchema = false,
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

        /** v12 → v13: Song 表新增 waveformData 列，存储波形数据 */
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE Song ADD COLUMN waveformData TEXT
                    """.trimIndent()
                )
            }

        }

        /** v13 -> v14: PlaylistSongCrossRef 新增 sortOrder，用于自定义歌单手动排序 */
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    ALTER TABLE PlaylistSongCrossRef
                    ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0
                    """.trimIndent()
                )
                db.execSQL("UPDATE PlaylistSongCrossRef SET sortOrder = insertTime * 1000000")
                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_PlaylistSongCrossRef_playlistId_sortOrder
                    ON PlaylistSongCrossRef(playlistId, sortOrder)
                    """.trimIndent()
                )
            }
        }

        /**
         * v14 -> v15: 补链用的空迁移。
         *
         * v15 当初是靠 fallbackToDestructiveMigration 落地的（没有对应的 Migration 对象），
         * 表结构与 v14 实际一致。这里补一个空实现把链条接上，
         * 否则停留在 v14 的设备升级时仍会走破坏性回退、整库被抹掉。
         */
        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 无结构变更，仅补全版本链
            }
        }

        /** v15 -> v16: Song 表新增响度归一化字段（EBU R128） */
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 积分响度（LUFS），可空表示尚未测得
                db.execSQL("ALTER TABLE Song ADD COLUMN integratedLufs REAL")
                // 采样峰值（线性幅度 0..1），用于提升时限制增益防削波
                db.execSQL("ALTER TABLE Song ADD COLUMN samplePeak REAL")
                db.execSQL(
                    "ALTER TABLE Song ADD COLUMN loudnessSource INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        /** v16 -> v17: 移除 EBU R128 响度归一化字段，改用纯 AGC 方案 */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite 不支持 ALTER TABLE DROP COLUMN（需要 API 30+）
                // 使用重建表的方式删除响度字段
                db.execSQL(
                    """
                    CREATE TABLE Song_new (
                        songId INTEGER PRIMARY KEY AUTOINCREMENT,
                        mediaStoreId INTEGER NOT NULL,
                        path TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        artist TEXT NOT NULL,
                        size INTEGER NOT NULL,
                        `like` INTEGER NOT NULL,
                        duration INTEGER NOT NULL,
                        sort INTEGER NOT NULL,
                        sortName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        albumId INTEGER NOT NULL,
                        album TEXT NOT NULL,
                        sampleRate INTEGER NOT NULL,
                        bitRate INTEGER NOT NULL,
                        channels INTEGER NOT NULL,
                        digit INTEGER NOT NULL,
                        isIgnore INTEGER NOT NULL,
                        dateModified INTEGER NOT NULL DEFAULT 0,
                        codec TEXT NOT NULL,
                        waveformData TEXT
                    )
                    """.trimIndent()
                )

                // 复制数据（排除响度字段）
                db.execSQL(
                    """
                    INSERT INTO Song_new
                    SELECT songId, mediaStoreId, path, displayName, artist, size, `like`,
                           duration, sort, sortName, mimeType, albumId, album, sampleRate,
                           bitRate, channels, digit, isIgnore, dateModified, codec, waveformData
                    FROM Song
                    """.trimIndent()
                )

                // 删除旧表
                db.execSQL("DROP TABLE Song")

                // 重命名新表
                db.execSQL("ALTER TABLE Song_new RENAME TO Song")

                // 重建索引
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Song_displayName ON Song(displayName)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_Song_mediaStoreId ON Song(mediaStoreId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Song_isIgnore ON Song(isIgnore)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Song_sortName ON Song(sortName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_Song_songId ON Song(songId)")
            }
        }

        /**
         * v17 -> v18: 播放历史治理。
         * 1) 删除 PlayHistory 上从不参与查询的 4 个索引（actionType/contextType/sessionId/isCompleted），
         *    降低每次插入的写放大与文件体积。
         * 2) 新增按歌聚合的 SongPlayStat 表，并从存量事件一次性回填，
         *    供“常听/最常播放/全时段统计”读取，使这些聚合不再全表扫描 PlayHistory。
         * 注：不在迁移里 VACUUM（大库会阻塞首次打开）；空闲页由后续插入复用，文件增长自然收敛到平台。
         */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) 删无用索引（名称为 Room 生成规则 index_<表>_<列>）
                db.execSQL("DROP INDEX IF EXISTS index_PlayHistory_actionType")
                db.execSQL("DROP INDEX IF EXISTS index_PlayHistory_contextType")
                db.execSQL("DROP INDEX IF EXISTS index_PlayHistory_sessionId")
                db.execSQL("DROP INDEX IF EXISTS index_PlayHistory_isCompleted")

                // 2) 建汇总表
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS SongPlayStat (
                        mediaId             INTEGER NOT NULL PRIMARY KEY,
                        playCount           INTEGER NOT NULL DEFAULT 0,
                        totalPlayedDuration INTEGER NOT NULL DEFAULT 0,
                        completedCount      INTEGER NOT NULL DEFAULT 0,
                        lastPlayedTime      INTEGER NOT NULL DEFAULT 0,
                        firstPlayedTime     INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )

                // 3) 从存量事件回填（口径与运行时增量维护一致）
                db.execSQL(
                    """
                    INSERT INTO SongPlayStat
                        (mediaId, playCount, totalPlayedDuration, completedCount, lastPlayedTime, firstPlayedTime)
                    SELECT
                        mediaId,
                        COUNT(*),
                        COALESCE(SUM(playedDuration), 0),
                        SUM(CASE WHEN isCompleted THEN 1 ELSE 0 END),
                        MAX(time),
                        MIN(time)
                    FROM PlayHistory
                    GROUP BY mediaId
                    """.trimIndent(),
                )
            }
        }

        /** v18 -> v19: Song 表新增 trackNumber 列，存储专辑内曲目序号，用于专辑详情正确排序 */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Song ADD COLUMN trackNumber INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
