package me.spcia.lyric_core.entity


import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class LyricResponse(
    val code: Int,
    val lrc: Lrc?,
    val lyricUser: LyricUser?,
    val qfy: Boolean,
    val sfy: Boolean,
    val sgc: Boolean,
    val transUser: TransUser?
) {
    @Keep
    data class Lrc(
        val lyric: String,
        val version: Int
    )

    @Keep
    data class LyricUser(
        val demand: Int,
        val id: Int,
        val nickname: String,
        val status: Int,
        val uptime: Long,
        val userid: Int
    )

    @Keep
    data class TransUser(
        val demand: Int,
        val id: Int,
        val nickname: String,
        val status: Int,
        val uptime: Long,
        val userid: Int
    )
}