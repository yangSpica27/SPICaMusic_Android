package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 歌单实体 - 通用数据类
 */
@Parcelize
@Serializable
data class Playlist(
    val playlistId: Long? = null,
    val playlistName: String,
    val cover: String? = null,
    val createTimestamp: Long = System.currentTimeMillis(),
    val playTimes: Int = 0,
    val needUpdate: Boolean = true, // 标记是否需要更新封面
) : Parcelable
