package me.spica27.spicamusic.storage.api

/** 统一播放与导出使用的离线歌词选择规则。 */
object SelectedLyricsResolver {
    suspend fun resolve(
        mediaId: Long,
        cached: StoredLyrics?,
        embedded: suspend () -> String?,
    ): StoredLyrics? {
        if (cached?.lyricsSuppressed == true) return cached
        if (cached != null && (cached.isManual || cached.restoredSnapshot) && cached.lyrics.isNotBlank()) {
            return cached
        }
        val raw = embedded()
        if (!raw.isNullOrBlank()) {
            return StoredLyrics(
                mediaId = mediaId,
                lyrics = raw,
                cover = "",
                delay = cached?.delay ?: 0L,
                sourceName = "歌曲内嵌歌词",
                sourceType = "EMBEDDED",
                isManual = false,
                sourceUri = "",
            )
        }
        return cached
    }
}
