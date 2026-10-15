package me.spica27.spicamusic.storage.impl.index

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagParserTest {
    @Test
    fun multiValueFieldsArePreferredAndDisplayKeepsTagText() {
        val tags =
            TagParser.parse(
                mapOf(
                    "TITLE" to listOf(" Song "),
                    "ARTIST" to listOf("A feat. B"),
                    "ARTISTS" to listOf("A", "B"),
                    "ALBUMARTIST" to listOf("A"),
                    "ALBUMARTISTS" to listOf("A", "C"),
                ),
            )
        assertEquals("Song", tags.title)
        assertEquals("A feat. B", tags.artistDisplay)
        assertEquals(listOf("A", "B"), tags.artists)
        assertEquals(listOf("A", "C"), tags.albumArtists)
    }

    @Test
    fun nativeMultipleArtistValuesAreJoinedForDisplay() {
        val tags = TagParser.parse(mapOf("ARTIST" to listOf("A", "", "B")))
        assertEquals(listOf("A", "B"), tags.artists)
        assertEquals("A / B", tags.artistDisplay)
    }

    @Test
    fun albumArtistWithSpaceKeyIsRecognized() {
        assertEquals(listOf("X"), TagParser.parse(mapOf("ALBUM ARTIST" to listOf("X"))).albumArtists)
    }

    @Test
    fun numbersYearAndCompilation() {
        val tags =
            TagParser.parse(
                mapOf(
                    "TRACKNUMBER" to listOf("3/12"),
                    "DISCNUMBER" to listOf("2 / 2"),
                    "DATE" to listOf("2019-05-01"),
                    "COMPILATION" to listOf("1"),
                ),
            )
        assertEquals(3, tags.trackNumber)
        assertEquals(2, tags.discNumber)
        assertEquals(2019, tags.year)
        assertTrue(tags.isCompilation)
    }

    @Test
    fun emptyTagsUseUnknownValues() {
        val tags = TagParser.parse(emptyMap())
        assertNull(tags.title)
        assertEquals("", tags.artistDisplay)
        assertEquals("", tags.album)
        assertEquals(0, tags.trackNumber)
        assertEquals(0, tags.discNumber)
        assertEquals(0, tags.year)
        assertFalse(tags.isCompilation)
        assertEquals(0, TagParser.parseNumber("abc"))
        assertEquals(0, TagParser.parseYear("0"))
    }

    @Test
    fun discNumberFallsBackToAlbumTitleMarker() {
        assertEquals(2, TagParser.parse(mapOf("ALBUM" to listOf("Live (Disc 2)"))).discNumber)
        assertEquals(1, TagParser.parse(mapOf("ALBUM" to listOf("Live (Disc 2)"), "DISCNUMBER" to listOf("1"))).discNumber)
    }

    @Test
    fun discMarkerStripping() {
        assertEquals("Album", AlbumTitles.strip("Album (Disc 1)"))
        assertEquals("Album", AlbumTitles.strip("Album [CD2]"))
        assertEquals("Album", AlbumTitles.strip("Album - CD 2"))
        assertEquals("Album", AlbumTitles.strip("Album Disc2"))
        assertEquals("MDCD 2", AlbumTitles.strip("MDCD 2"))
        assertEquals("Disc 1", AlbumTitles.strip("Disc 1"))
        assertEquals(0, AlbumTitles.discMarker("Disc 1"))
        assertEquals("Greatest Hits CD", AlbumTitles.strip("Greatest Hits CD"))
    }

    @Test
    fun tagJsonRoundTripsAndToleratesGarbage() {
        val values = listOf("A", "B \"quoted\"", "周杰伦")
        assertEquals(values, TagJson.decode(TagJson.encode(values)))
        assertEquals(emptyList<String>(), TagJson.decode("not json"))
    }
}
