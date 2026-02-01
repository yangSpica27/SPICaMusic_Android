package me.spcia.lyric_core.entity

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

/**
 * 歌词API响应
 */
@Keep
@JsonClass(generateAdapter = true)
data class LyricsApiResponse(
    val code: Int,
    val message: String,
    val data: List<SongLyrics>?
)

/**
 * 歌曲歌词信息
 */
@Keep
@JsonClass(generateAdapter = true)
data class SongLyrics(
    val id: Long,
    val name: String,
    val artist: String,
    val album: String,
    val albumArt: String,
    val duration: Int,
    val lyrics: String
)
