package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

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
) : Parcelable
