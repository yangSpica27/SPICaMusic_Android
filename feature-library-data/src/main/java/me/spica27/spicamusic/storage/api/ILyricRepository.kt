package me.spica27.spicamusic.storage.api

data class StoredLyrics(
    val mediaId: Long,
    val lyrics: String,
    val cover: String,
    val delay: Long,
    val sourceName: String,
    val sourceType: String,
    val isManual: Boolean,
    val sourceUri: String,
)

interface ILyricRepository {
    suspend fun getLyrics(mediaId: Long): StoredLyrics?

    suspend fun updateDelay(mediaId: Long, delay: Long)

    suspend fun saveLyrics(
        mediaId: Long,
        lyrics: String,
        cover: String = "",
        sourceName: String,
        delay: Long,
        sourceType: String,
        isManual: Boolean,
        sourceUri: String = "",
    )
}
