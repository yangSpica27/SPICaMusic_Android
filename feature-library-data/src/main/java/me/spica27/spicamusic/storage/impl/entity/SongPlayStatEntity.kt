package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 按歌曲聚合的播放统计（全时段累计）。
 */
@Entity(tableName = "SongPlayStat")
data class SongPlayStatEntity(
    @PrimaryKey
    val mediaId: Long,
    // 累计播放事件次数
    var playCount: Long = 0,
    // 累计实际收听时长（ms）
    var totalPlayedDuration: Long = 0,
    // 累计“听完”次数（isCompleted）
    var completedCount: Long = 0,
    // 最近一次播放时间戳
    var lastPlayedTime: Long = 0,
    // 首次播放时间戳（仅首次写入，之后不变）
    var firstPlayedTime: Long = 0,
)
