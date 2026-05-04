package me.spica27.spicamusic.feature.lyrics.domain

data class CachedLyrics(
    val mediaId: Long,
    val lyrics: String,
    val delay: Long,
    val lyricSourceName: String,
    val cover: String,
)
