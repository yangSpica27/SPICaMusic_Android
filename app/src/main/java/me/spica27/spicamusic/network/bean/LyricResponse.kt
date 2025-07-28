package me.spica27.spicamusic.network.bean

import com.squareup.moshi.JsonClass


/**
 * 歌词接口的返回参数
 */
@JsonClass(generateAdapter = true)
data class LyricResponse(
    val album: String?,
    val artist: String?,
    val cover: String?,
    val id: String,
    val lyrics: String,
    val title: String?
)