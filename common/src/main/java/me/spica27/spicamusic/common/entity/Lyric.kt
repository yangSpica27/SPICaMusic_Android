package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 歌词实体 - 通用数据类
 */
@Parcelize
@Serializable
data class Lyric(
    val songId: Long,
    val lyricContent: String,
    val translatedLyric: String? = null,
    val source: String? = null, // 歌词来源
    val updateTime: Long = System.currentTimeMillis(),
) : Parcelable
