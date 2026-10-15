package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * 应用按文件标签建立的专辑。
 * 歌曲数与代表曲只统计未忽略的歌曲；封面按代表曲本体取图。
 */
@Immutable
@Parcelize
@Serializable
data class Album(
    val id: Long,
    val title: String,
    /** 专辑艺人显示串；未知时为空 */
    val artist: String,
    val year: Int = 0,
    val numberOfSongs: Int = 0,
    val coverMediaStoreId: Long = 0L,
) : Parcelable
