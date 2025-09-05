package me.spica27.spicamusic.wrapper

import android.graphics.Bitmap
import androidx.annotation.Keep

@Keep
data class Metadata(
    val title: String,
    val album: String,
    val artist: String,
    val albumArtist: String,
    val composer: String,
    val lyricist: String,
    val comment: String,
    val genre: String,
    val track: String,
    val disc: String,
    val date: String,
    val duration: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val cover: Bitmap? = null,
)
