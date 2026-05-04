package me.spica27.spicamusic.ui.player

import me.spica27.spicamusic.common.entity.LyricItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsViewModelTest {
    @Test
    fun parseLyrics_preservesWordTimingForYrc() {
        val lyricsText = "[0,500](0,200,0)你(200,300,0)好"

        val parsed = LyricsViewModel.parseLyrics(lyricsText)

        assertEquals(1, parsed?.size)
        val lyric = parsed?.single() as LyricItem.WordsLyric
        assertEquals(0L, lyric.startTime)
        assertEquals(500L, lyric.endTime)
        assertEquals(listOf("你", "好"), lyric.words.map { it.content })
        assertEquals(listOf(0L, 200L), lyric.words.map { it.startTime })
        assertEquals(listOf(200L, 500L), lyric.words.map { it.endTime })
    }

    @Test
    fun parseLyrics_keepsNormalLrcAsNormalLyric() {
        val lyricsText = "[00:01.00]hello"

        val parsed = LyricsViewModel.parseLyrics(lyricsText)

        assertEquals(1, parsed?.size)
        assertTrue(parsed?.single() is LyricItem.NormalLyric)
    }
}
