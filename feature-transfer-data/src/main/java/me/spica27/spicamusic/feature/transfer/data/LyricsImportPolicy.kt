package me.spica27.spicamusic.feature.transfer.data

import me.spica27.spicamusic.feature.transfer.domain.PackageLyrics
import me.spica27.spicamusic.storage.api.StoredLyrics

internal fun StoredLyrics?.hasLyricsSelection(): Boolean = this != null && (lyricsSuppressed || lyrics.isNotBlank())

internal fun shouldImportLyrics(
    local: StoredLyrics?,
    preferImportedLyrics: Boolean,
): Boolean = preferImportedLyrics || !local.hasLyricsSelection()

internal fun hasLyricsConflict(
    local: StoredLyrics?,
    incoming: PackageLyrics?,
    incomingRaw: String,
): Boolean {
    if (!local.hasLyricsSelection() || incoming == null) return false
    requireNotNull(local)
    if (local.lyricsSuppressed != incoming.suppressed) return true
    // 双方均禁用歌词时，备用快照差异不算冲突。
    if (local.lyricsSuppressed) return false
    return local.lyrics != incomingRaw || local.delay != incoming.offsetMs
}
