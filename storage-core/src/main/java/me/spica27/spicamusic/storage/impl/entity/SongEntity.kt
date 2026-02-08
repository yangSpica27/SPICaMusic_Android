package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Song",
    indices = [
        Index("displayName"),
        Index("mediaStoreId", unique = true),
        Index("isIgnore"),
        Index("sortName"),
    ],
)
data class SongEntity(
    @ColumnInfo(index = true)
    @PrimaryKey(autoGenerate = true)
    var songId: Long? = null,
    var mediaStoreId: Long,
    var path: String,
    var displayName: String,
    var artist: String,
    var size: Long,
    var like: Boolean,
    val duration: Long,
    var sort: Int,
    var sortName: String,
    var mimeType: String,
    var albumId: Long,
    var sampleRate: Int,
    var bitRate: Int,
    var channels: Int,
    var digit: Int,
    var isIgnore: Boolean,
    /** MediaStore 的 DATE_MODIFIED，用于增量扫描判断文件是否变更 */
    var dateModified: Long = 0,
)
