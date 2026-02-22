package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 播放历史实体 - 通用数据类
 * 扩展字段用于推荐、自动歌单和听歌统计
 */
@Parcelize
@Serializable
data class PlayHistory(
    val id: Long? = null,
    val songId: Long,
    val playTime: Long = System.currentTimeMillis(),
    val playCount: Int = 1,

    // Extended fields
    val userId: String? = null,
    val sessionId: String? = null,
    val deviceId: String? = null,
    val duration: Long = 0L,
    val playedDuration: Long = 0L,
    val position: Long = 0L,
    val actionType: Int = 0,
    val contextType: String = "",
    val contextId: Long? = null,
    val isCompleted: Boolean = false,
    val source: String = "",
    val extra: String = "",
) : Parcelable
