package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一行对应一个 MediaStore 音频文件。
 * 文件事实取自 MediaStore；标签与音频参数取自 TagLib；albumId / sortName 由索引器写入。
 * 写入只能用 @Upsert 或 UPDATE：REPLACE 会先删旧行，级联删掉这首歌的艺人关系。
 */
@Entity(
    tableName = "Song",
    indices = [
        Index("mediaStoreId", unique = true),
        Index(value = ["albumId", "discNumber", "trackNumber"]),
        Index("sortName"),
        Index("displayName"),
        Index("isIgnore"),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["albumId"],
            childColumns = ["albumId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val songId: Long = 0,
    val mediaStoreId: Long,
    // 文件事实：来自 MediaStore
    val path: String,
    val size: Long,
    val mimeType: String,
    /** MediaStore 的 DATE_MODIFIED（秒），与 size 一起判断文件是否变更 */
    val dateModified: Long,
    // 标签：来自 TagLib
    val displayName: String,
    /** 艺人显示串，保留标签原文（含分隔符） */
    val artist: String,
    /** 专辑名，空串表示没有专辑标签 */
    val album: String,
    /** 以下四列为拆分前的原始标签值（JSON 字符串数组），切换拆分规则时据此重建关系 */
    @ColumnInfo(defaultValue = "[]")
    val artistTags: String = "[]",
    @ColumnInfo(defaultValue = "[]")
    val albumArtistTags: String = "[]",
    @ColumnInfo(defaultValue = "[]")
    val composerTags: String = "[]",
    @ColumnInfo(defaultValue = "[]")
    val lyricistTags: String = "[]",
    @ColumnInfo(defaultValue = "0")
    val trackNumber: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val discNumber: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val year: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isCompilation: Boolean = false,
    // 音频参数：来自 TagLib，0 表示未知
    val duration: Long,
    @ColumnInfo(defaultValue = "0")
    val sampleRate: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val bitRate: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val channels: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val digit: Int = 0,
    val codec: String,
    /** TAGLIB：标签读取成功；FALLBACK：读取失败，标题等取自 MediaStore / 文件名 */
    val tagStatus: TagStatus,
    /** 写入时的提取版本，低于当前版本的歌曲在下次扫描时重新提取 */
    val tagVersion: Int,
    // 派生：由索引器写入
    val albumId: Long? = null,
    val sortName: String,
    // 用户状态与缓存：扫描 upsert 时保留
    @ColumnInfo(defaultValue = "0")
    val like: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val isIgnore: Boolean = false,
    /** 明确导入的歌曲不受扫描规则（时长 / 体积 / 格式）过滤 */
    @ColumnInfo(defaultValue = "0")
    val isImported: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val sort: Int = 0,
    val waveformData: String? = null,
)

enum class TagStatus {
    TAGLIB,
    FALLBACK,
}
