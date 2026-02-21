package me.spica27.spicamusic.common.entity

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Immutable
@Parcelize
@Serializable
data class Album (
    val id: String,
    val title: String,
    val artist: String,
    val artworkUri: String? = null,
    val year: Int = 0,
    val numberOfSongs: Int = 0,
    val dateModified: Long = System.currentTimeMillis()
): Parcelable