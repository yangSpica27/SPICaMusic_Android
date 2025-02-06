package me.spica27.spicamusic.utils

import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.media3.common.MimeTypes


@ColorInt
fun String.getColorFromMimeTypeString(): Int {
  return when (this) {
    MimeTypes.AUDIO_OGG -> Color.parseColor("#87e8de")
    MimeTypes.AUDIO_MP4 -> Color.parseColor("#95de64")
    MimeTypes.AUDIO_MPEG -> Color.parseColor("#b7eb8f")
    MimeTypes.AUDIO_FLAC -> Color.parseColor("#ffd666")
    else -> Color.parseColor("#d9d9d9")
  }
}