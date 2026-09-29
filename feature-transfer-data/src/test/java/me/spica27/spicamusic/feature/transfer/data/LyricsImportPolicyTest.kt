package me.spica27.spicamusic.feature.transfer.data

import me.spica27.spicamusic.feature.transfer.domain.PackageLyrics
import me.spica27.spicamusic.storage.api.StoredLyrics
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsImportPolicyTest {
    private val local = StoredLyrics(1, "local", "", 100, "local", "LOCAL_FILE", true, "")
    private val incoming = PackageLyrics(null, "txt", 100, "ONLINE", "online", true)

    @Test
    fun suppressionIsAnExistingSelectionEvenWithoutText() {
        val suppressed = local.copy(lyrics = "", lyricsSuppressed = true)
        assertFalse(shouldImportLyrics(suppressed, false))
        assertTrue(shouldImportLyrics(suppressed, true))
        assertTrue(hasLyricsConflict(suppressed, incoming, "incoming"))
    }

    @Test
    fun incomingSuppressionConflictsWithActiveLyrics() {
        assertTrue(hasLyricsConflict(local, incoming.copy(suppressed = true), "local"))
        assertFalse(shouldImportLyrics(local, false))
        assertTrue(shouldImportLyrics(local, true))
    }

    @Test
    fun dormantSnapshotsDoNotConflictWhenBothSidesSuppress() {
        assertFalse(hasLyricsConflict(local.copy(lyricsSuppressed = true), incoming.copy(suppressed = true, offsetMs = 999), "other"))
    }

    @Test
    fun emptyLocalStateAcceptsIncomingSelectionAndExistingRulesStayIntact() {
        assertTrue(shouldImportLyrics(null, false))
        assertTrue(shouldImportLyrics(local.copy(lyrics = ""), false))
        assertFalse(hasLyricsConflict(null, incoming.copy(suppressed = true), ""))
        assertFalse(hasLyricsConflict(local, null, ""))
        assertFalse(hasLyricsConflict(local, incoming, "local"))
        assertTrue(hasLyricsConflict(local, incoming.copy(offsetMs = 0), "local"))
    }
}
