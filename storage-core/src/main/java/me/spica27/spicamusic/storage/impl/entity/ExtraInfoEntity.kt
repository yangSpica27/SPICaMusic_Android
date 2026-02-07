package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "extra_info")
data class ExtraInfoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var mediaId: Long = 0,
    var lyrics: String = "", // 歌词内容
    var cover: String = "", // 歌词封面
    var delay: Long = 0, // 歌词延迟(ms)
    var lyricSourceName: String = "", // 歌词源名称 (如 "歌手 - 歌名")
)
