package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 播放历史实体 - 通用数据类
 */
@Parcelize
@Serializable
data class PlayHistory(
    val id: Long? = null,
    val songId: Long,
    val playTime: Long = System.currentTimeMillis(),
    val playCount: Int = 1,
) : Parcelable
