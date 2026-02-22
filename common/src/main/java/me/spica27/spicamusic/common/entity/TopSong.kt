package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class TopSong(
    val songId: Long,
    val totalDuration: Long,
    val playCount: Long,
) : Parcelable
