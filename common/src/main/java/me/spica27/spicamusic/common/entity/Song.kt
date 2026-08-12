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
    val albumId: Long,
    val sampleRate: Int, // 采样率
    val bitRate: Int, // 比特率
    val channels: Int, // 声道数
    val digit: Int, // 位深度
    val isIgnore: Boolean,
    val sortName: String,
    val codec: String,
    val album: String,
    val waveformData: String? = "",
    /** 专辑内曲目序号（源自 MediaStore.Audio.Media.TRACK，多碟编码为 disc*1000+track），0 表示未知 */
    val trackNumber: Int = 0,
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
