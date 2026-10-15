package me.spica27.spicamusic.storage.impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 应用按标签建立的专辑。groupKey 由标题键与专辑艺人键组成，键不变则 albumId 不变。
 * 歌曲数与代表曲在查询时计算，不落库。
 */
@Entity(
    tableName = "Album",
    indices = [
        Index("groupKey", unique = true),
        Index("sortName"),
    ],
)
data class AlbumEntity(
    @PrimaryKey(autoGenerate = true)
    val albumId: Long = 0,
    val groupKey: String,
    val title: String,
    /** 专辑艺人显示串；未知艺人时为空 */
    val artist: String,
    val sortName: String,
    @ColumnInfo(defaultValue = "0")
    val year: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isCompilation: Boolean = false,
)
