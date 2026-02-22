package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
@Immutable
data class PlayStats(
    val totalPlayedDuration: Long = 0L,
    val playEventCount: Long = 0L,
    val uniqueSongCount: Long = 0L,
) : Parcelable
