package me.spica27.spicamusic.feature.lyrics.domain

import me.spica27.spicamusic.storage.api.LocalLyricFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalLyricValidatorTest {
    @Test
    fun lrcRequiresAtLeastOneValidTimestampedLine() {
        assertTrue(LocalLyricValidator.isValid(LocalLyricFile("[00:01.00]hello", "song.lrc")))
        assertFalse(LocalLyricValidator.isValid(LocalLyricFile("not a lyric", "song.lrc")))
    }

    @Test
    fun ttmlRequiresParseableAmllContent() {
        assertTrue(
            LocalLyricValidator.isValid(
                LocalLyricFile(
                    """
                    <tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                      <body><div><p begin="0s" end="1s"><span>hello</span></p></div></body>
                    </tt>
                    """.trimIndent(),
                    "song.ttml",
                ),
            ),
        )
        assertFalse(LocalLyricValidator.isValid(LocalLyricFile("<html>no lyrics</html>", "song.ttml")))
    }

    @Test
    fun textAllowsUnsynchronisedLyricsButUnknownExtensionDoesNot() {
        assertTrue(LocalLyricValidator.isValid(LocalLyricFile("first line\nsecond line", "song.txt")))
        assertTrue(LocalLyricValidator.isValid(LocalLyricFile("first line", "song")))
        assertFalse(LocalLyricValidator.isValid(LocalLyricFile("first line", "song.mp3")))
    }
}
