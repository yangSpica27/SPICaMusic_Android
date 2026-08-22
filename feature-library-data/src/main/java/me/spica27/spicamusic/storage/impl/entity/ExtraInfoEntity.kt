package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "extra_info",
    indices = [
        Index("mediaId", unique = true),
    ],
)
data class ExtraInfoEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var mediaId: Long = 0,
    var lyrics: String = "", // 歌词内容（当前选中来源的原始文本，本地/在线均为快照）
    var cover: String = "", // 歌词封面
    var delay: Long = 0, // 歌词延迟(ms)
    var lyricSourceName: String = "", // 歌词源名称 (如 "歌手 - 歌名")
    // 当前选中来源类型，对应 LyricSourceType（EMBEDDED/LOCAL_FILE/ONLINE/NONE），重启后据此恢复显示
    var sourceType: String = "ONLINE",
    // 手动锁定标记：为 true 时跳过"内嵌>本地>在线"自动优先级，永远使用这份快照
    var isManual: Boolean = false,
    // 本地文件来源的原始 URI/文件名，仅作展示与提示（内容已快照入库，不依赖其长期有效）
    var sourceUri: String = "",
)
