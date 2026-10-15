package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.spica27.spicamusic.common.entity.ArtistKind
import me.spica27.spicamusic.common.entity.ArtistRole

/** 应用按标签建立的艺人。nameKey 是规范化名称键，同一个键只有一行。 */
@Entity(
    tableName = "Artist",
    indices = [
        Index("nameKey", unique = true),
        Index("sortName"),
    ],
)
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true)
    val artistId: Long = 0,
    val nameKey: String,
    /** 显示名；未知艺人为空串 */
    val name: String,
    val sortName: String,
    @ColumnInfo(defaultValue = "NORMAL")
    val kind: ArtistKind = ArtistKind.NORMAL,
)

/** 歌曲署名。role 按枚举名存储，新增角色不需要迁移。 */
@Entity(
    tableName = "SongArtist",
    primaryKeys = ["songId", "role", "artistId"],
    indices = [Index(value = ["artistId", "role"])],
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["songId"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["artistId"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SongArtistEntity(
    val songId: Long,
    val role: ArtistRole,
    val artistId: Long,
    /** 同一角色内的署名顺序，从 0 开始 */
    val creditOrder: Int,
)

/** 专辑艺人 */
@Entity(
    tableName = "AlbumArtist",
    primaryKeys = ["albumId", "artistId"],
    indices = [Index("artistId")],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["artistId"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class AlbumArtistEntity(
    val albumId: Long,
    val artistId: Long,
    val creditOrder: Int,
)

/**
 * 曲库状态，单行（id = 1）。没有这一行时按默认值处理，全新安装不需要重建。
 * - pendingRebuild：v22 → v23 迁移后为 true，首次完整重扫成功后清零
 * - indexPolicy：当前关系图所用解释规则的指纹，与当前规则不同时重建关系
 */
@Entity(tableName = "LibraryState")
data class LibraryStateEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(defaultValue = "0")
    val pendingRebuild: Boolean = false,
    @ColumnInfo(defaultValue = "")
    val indexPolicy: String = "",
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

/**
 * v22 → v23 迁移时暂存的歌曲标记，按 MediaStore ID 在重扫入库时回填，重建完成后清空。
 * 旧 songId 随歌曲表一起失效，所以键用 MediaStore ID。
 */
@Entity(tableName = "LegacySongCarryOver")
data class LegacySongCarryOverEntity(
    @PrimaryKey
    val mediaStoreId: Long,
    @ColumnInfo(defaultValue = "0")
    val isLiked: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isImported: Boolean = false,
)
