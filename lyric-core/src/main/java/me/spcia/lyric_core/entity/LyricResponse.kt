package me.spcia.lyric_core.entity


import androidx.annotation.Keep
import com.squareup.moshi.JsonClass

@Keep
@JsonClass(generateAdapter = true)
data class LyricResponse(
    val code: Int,
    val lrc: Lrc?,               // 普通LRC歌词
    val yrc: Yrc?,               // 逐字歌词（YRC格式）
    val tlyric: Lrc?,            // 翻译歌词
    val romalrc: Lrc?,           // 罗马音歌词
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
    data class Yrc(
        val lyric: String,       // YRC 格式字符串
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