package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SelectedLyricsResolverTest {
    private fun cached(
        manual: Boolean = false,
        restored: Boolean = false,
    ) = StoredLyrics(1, "online", "", -250, "selected", "ONLINE", manual, "", restored)

    @Test
    fun manualAndRestoredSnapshotsDoNotReadAnotherSource() =
        runBlocking {
            listOf(cached(manual = true), cached(restored = true)).forEach { stored ->
                assertEquals(stored, SelectedLyricsResolver.resolve(1, stored) { error("Must not read embedded") })
            }
        }

    @Test
    fun suppressedLyricsSkipEmbeddedEvenWithoutASnapshot() =
        runBlocking {
            listOf("", "previous snapshot").forEach { text ->
                val stored = cached().copy(lyrics = text, lyricsSuppressed = true)
                assertEquals(stored, SelectedLyricsResolver.resolve(1, stored) { error("Must not read embedded") })
            }
        }

    @Test
    fun automaticEmbeddedLyricsKeepExistingTimingOffset() =
        runBlocking {
            val selected = SelectedLyricsResolver.resolve(1, cached()) { "embedded" }!!
            assertEquals("embedded", selected.lyrics)
            assertEquals(-250L, selected.delay)
            assertEquals("EMBEDDED", selected.sourceType)
            assertFalse(selected.isManual)
        }

    @Test
    fun noEmbeddedUsesExistingCacheWithoutNetwork() =
        runBlocking {
            assertEquals(cached(), SelectedLyricsResolver.resolve(1, cached()) { null })
            assertNull(SelectedLyricsResolver.resolve(1, null) { null })
        }
}
