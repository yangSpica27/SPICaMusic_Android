package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 歌曲实体 - 通用数据类
 */
@Immutable
@Parcelize
@Serializable
data class Song(
    val songId: Long? = null,
    val mediaStoreId: Long,
    val path: String,
    val displayName: String,
    val artist: String,
    val size: Long,
    val like: Boolean,
    val duration: Long,
    val sort: Int,
    val mimeType: String,
    /** 应用自建专辑的 ID；没有专辑标签的歌曲为 null */
    val albumId: Long?,
    val sampleRate: Int, // 采样率，0 表示未知
    val bitRate: Int, // 比特率，0 表示未知
    val channels: Int, // 声道数，0 表示未知
    val digit: Int, // 位深度，0 表示未知
    val isIgnore: Boolean,
    val sortName: String,
    val codec: String,
    val album: String,
    val waveformData: String? = "",
    /** 碟内曲目序号（来自标签），0 表示未知 */
    val trackNumber: Int = 0,
    /** 碟号（来自标签），0 表示未知 */
    val discNumber: Int = 0,
    /** 发行年份（来自标签），0 表示未知 */
    val year: Int = 0,
    /** 作曲者显示串，多位以 " / " 连接 */
    val composer: String = "",
    /** 作词者显示串，多位以 " / " 连接 */
    val lyricist: String = "",
) : Parcelable {

    private fun formatTime(millis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format(Locale.CHINESE, "%d:%02d", minutes, seconds)
    }

    fun getFormattedDuration(): String {
        return formatTime(duration)
    }

}
