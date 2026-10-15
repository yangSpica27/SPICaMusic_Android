package me.spica27.spicamusic.storage.impl.index

import me.spica27.spicamusic.common.entity.ArtistKind
import me.spica27.spicamusic.common.entity.ArtistRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryIndexerTest {
    /** JVM 上没有 android.icu：繁简只映射用例里出现的字，首字母只认 A-Z */
    private val text =
        IndexText(
            simplify = { it.replace('倫', '伦') },
            initial = { name -> name.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' }?.toString() ?: "#" },
        )
    private val indexer = LibraryIndexer(text)
    private val off = IndexPolicy(splitArtists = false)
    private val on = IndexPolicy(splitArtists = true)

    private var nextId = 1L

    private fun song(
        artists: List<String> = emptyList(),
        album: String = "",
        albumArtists: List<String> = emptyList(),
        path: String = "/storage/emulated/0/Music/x.mp3",
        composers: List<String> = emptyList(),
        lyricists: List<String> = emptyList(),
        year: Int = 0,
        compilation: Boolean = false,
        id: Long = nextId++,
    ) = IndexSong(id, path, album, artists, albumArtists, composers, lyricists, year, compilation)

    private fun LibraryGraph.performers(songId: Long): List<String> =
        songCredits.getValue(songId).filter { it.role == ArtistRole.ARTIST }.map { key -> artistName(key.artistKey) }

    private fun LibraryGraph.artistName(key: String) = artists.single { it.key == key }.name

    private fun LibraryGraph.album(songId: Long): IndexedAlbum? = songAlbums[songId]?.let { key -> albums.single { it.groupKey == key } }

    // ===== 拆分 =====

    @Test
    fun singleValueIsSplitOnlyWhenEnabled() {
        val s = song(artists = listOf("周杰伦/费玉清"))
        assertEquals(listOf("周杰伦/费玉清"), indexer.build(listOf(s), off).performers(s.songId))
        assertEquals(listOf("周杰伦", "费玉清"), indexer.build(listOf(s), on).performers(s.songId))
    }

    @Test
    fun nativeMultiValuesAreNeverSplitAgain() {
        val s = song(artists = listOf("A/B", "C"))
        assertEquals(listOf("A/B", "C"), indexer.build(listOf(s), off).performers(s.songId))
        assertEquals(listOf("A/B", "C"), indexer.build(listOf(s), on).performers(s.songId))
    }

    @Test
    fun escapedSeparatorAndFeaturingRules() {
        assertEquals(listOf("AC/DC"), ArtistSplitter.split(listOf("AC\\/DC"), true))
        assertEquals(listOf("A", "B"), ArtistSplitter.split(listOf("A (feat. B)"), true))
        assertEquals(listOf("A", "B", "C"), ArtistSplitter.split(listOf("A、B ft. C"), true))
        assertEquals(listOf("Defeat. X"), ArtistSplitter.split(listOf("Defeat. X"), true))
        assertEquals(listOf("Earth, Wind & Fire"), ArtistSplitter.split(listOf("Earth, Wind & Fire"), false))
        assertEquals(listOf("A & B"), ArtistSplitter.split(listOf(" A & B "), false))
    }

    @Test
    fun variousAliasIsNotSplit() {
        val s = song(artists = listOf("V/A"))
        val graph = indexer.build(listOf(s), on)
        val artist = graph.artists.single()
        assertEquals(ArtistKind.VARIOUS, artist.kind)
        assertEquals("V/A", artist.name)
    }

    // ===== 名称键 =====

    @Test
    fun spellingVariantsShareOneArtistAndMostCommonSpellingWins() {
        val songs =
            listOf(
                song(artists = listOf("周杰倫")),
                song(artists = listOf("周杰伦")),
                song(artists = listOf("周杰伦")),
                song(artists = listOf("ＪＡＹ")),
                song(artists = listOf("jay")),
                song(artists = listOf("Jay​")),
            )
        val graph = indexer.build(songs, off)
        assertEquals(listOf("Jay", "周杰伦"), graph.artists.map { it.name }.sorted())
    }

    @Test
    fun missingArtistFallsBackToAlbumArtistThenUnknown() {
        val withAlbumArtist = song(album = "X", albumArtists = listOf("A"))
        val none = song(album = "Y")
        val graph = indexer.build(listOf(withAlbumArtist, none), off)
        assertEquals(listOf("A"), graph.performers(withAlbumArtist.songId))
        val unknown = graph.artists.single { it.kind == ArtistKind.UNKNOWN }
        assertEquals("", unknown.name)
        assertEquals(listOf(unknown.key), graph.songCredits.getValue(none.songId).map { it.artistKey })
    }

    @Test
    fun composerAndLyricistBecomeSeparateRoles() {
        val s = song(artists = listOf("A"), composers = listOf("C1", "C2"), lyricists = listOf("A"))
        val credits = indexer.build(listOf(s), off).songCredits.getValue(s.songId)
        assertEquals(listOf(ArtistRole.ARTIST, ArtistRole.COMPOSER, ArtistRole.COMPOSER, ArtistRole.LYRICIST), credits.map { it.role })
        assertEquals(listOf(0, 0, 1, 0), credits.map { it.order })
        assertEquals(credits.first().artistKey, credits.last().artistKey)
    }

    // ===== 专辑分组 =====

    @Test
    fun sameAlbumInTwoDirectoriesWithSameAlbumArtistIsOneAlbum() {
        val a = song(artists = listOf("A"), album = "Hits", albumArtists = listOf("A"), path = "/storage/emulated/0/Music/A/Hits/1.flac")
        val b = song(artists = listOf("A"), album = "hits", albumArtists = listOf("A"), path = "/storage/emulated/0/Other/2.flac")
        val graph = indexer.build(listOf(a, b), off)
        assertEquals(1, graph.albums.size)
        assertEquals(graph.songAlbums[a.songId], graph.songAlbums[b.songId])
    }

    @Test
    fun sameTitleWithDifferentAlbumArtistsIsTwoAlbums() {
        val a = song(artists = listOf("A"), album = "Hits", albumArtists = listOf("A"), path = "/m/d/1.mp3")
        val b = song(artists = listOf("B"), album = "Hits", albumArtists = listOf("B"), path = "/m/d/2.mp3")
        assertEquals(2, indexer.build(listOf(a, b), off).albums.size)
    }

    @Test
    fun featuredTracksJoinTheLeadArtistsAlbum() {
        val a = song(artists = listOf("A"), album = "Solo", path = "/storage/emulated/0/Download/1.mp3")
        val b = song(artists = listOf("A feat. B"), album = "Solo", path = "/storage/emulated/0/Download/2.mp3")
        val graph = indexer.build(listOf(a, b), on)
        val album = requireNotNull(graph.album(a.songId))
        assertEquals(album, graph.album(b.songId))
        assertEquals("A", album.artist)
    }

    @Test
    fun untaggedSongInFlatDirectoryJoinsTaggedPartitionByPerformer() {
        val tagged = song(artists = listOf("A"), album = "Hits", albumArtists = listOf("A"), path = "/m/a/1.mp3")
        val loose = song(artists = listOf("A"), album = "Hits", path = "/storage/emulated/0/Download/2.mp3")
        val graph = indexer.build(listOf(tagged, loose), off)
        assertEquals(graph.songAlbums[tagged.songId], graph.songAlbums[loose.songId])
    }

    @Test
    fun sameTitleByDifferentArtistsInFlatDirectoryStaysApart() {
        val a = song(artists = listOf("A"), album = "Greatest Hits", path = "/storage/emulated/0/Download/1.mp3")
        val b = song(artists = listOf("B"), album = "Greatest Hits", path = "/storage/emulated/0/Download/2.mp3")
        val graph = indexer.build(listOf(a, b), off)
        assertEquals(2, graph.albums.size)
        assertEquals(setOf("A", "B"), graph.albums.map { it.artist }.toSet())
    }

    @Test
    fun soundtrackInOneDirectoryBecomesVariousArtists() {
        val dir = "/storage/emulated/0/Music/OST"
        val songs = listOf("A", "B", "C").map { song(artists = listOf(it), album = "OST", path = "$dir/$it.mp3") }
        val graph = indexer.build(songs, off)
        val album = graph.albums.single()
        assertTrue(album.isCompilation)
        assertEquals(LibraryIndexer.DEFAULT_VARIOUS_NAME, album.artist)
        assertEquals(ArtistKind.VARIOUS, graph.artists.single { it.key == album.artistKeys.single() }.kind)
    }

    @Test
    fun twoSameNameAlbumsInOneDirectoryWithoutAlbumArtistAreMerged() {
        // 已知局限：需要给标签补上 ALBUMARTIST
        val dir = "/storage/emulated/0/Music/Mixed"
        val a = song(artists = listOf("A"), album = "Best", path = "$dir/1.mp3")
        val b = song(artists = listOf("B"), album = "Best", path = "$dir/2.mp3")
        val album = indexer.build(listOf(a, b), off).albums.single()
        assertEquals("A / B", album.artist)
    }

    @Test
    fun compilationFlagAndVariousAliasesMapToVarious() {
        val flagged = song(artists = listOf("A"), album = "Now 1", compilation = true, path = "/m/n/1.mp3")
        val aliased = song(artists = listOf("B"), album = "Now 2", albumArtists = listOf("群星"), path = "/m/n2/1.mp3")
        val graph = indexer.build(listOf(flagged, aliased), off)
        val various = graph.artists.single { it.kind == ArtistKind.VARIOUS }
        assertEquals("群星", various.name)
        graph.albums.forEach { assertEquals(listOf(various.key), it.artistKeys) }
    }

    @Test
    fun emptyAlbumHasNoAlbum() {
        val s = song(artists = listOf("A"), album = "  ")
        val graph = indexer.build(listOf(s), off)
        assertNull(graph.songAlbums[s.songId])
        assertTrue(graph.albums.isEmpty())
    }

    @Test
    fun discMarkersAreStrippedSoDiscsShareOneAlbum() {
        val d1 = song(artists = listOf("A"), album = "Live (Disc 1)", albumArtists = listOf("A"), path = "/m/l/1.mp3")
        val d2 = song(artists = listOf("A"), album = "Live [CD2]", albumArtists = listOf("A"), path = "/m/l/2.mp3")
        val album = indexer.build(listOf(d1, d2), off).albums.single()
        assertEquals("Live", album.title)
    }

    @Test
    fun albumYearIsMostCommonNonZeroYear() {
        val songs =
            listOf(2001, 2003, 2003, 0).map { year ->
                song(artists = listOf("A"), album = "Y", albumArtists = listOf("A"), year = year, path = "/m/y/$year.mp3")
            }
        assertEquals(2003, indexer.build(songs, off).albums.single().year)
    }

    @Test
    fun outputIsIndependentOfInputOrder() {
        val songs =
            listOf(
                song(artists = listOf("A"), album = "X", path = "/storage/emulated/0/Download/1.mp3"),
                song(artists = listOf("B"), album = "X", path = "/storage/emulated/0/Download/2.mp3"),
                song(artists = listOf("A/B"), album = "X", albumArtists = listOf("A"), path = "/m/x/3.mp3"),
                song(artists = listOf("C"), album = "Y", path = "/m/y/4.mp3"),
                song(artists = listOf("D"), album = "Y", path = "/m/y/5.mp3"),
                song(artists = listOf("E"), album = "Y", path = "/m/y/6.mp3"),
                song(artists = listOf("F"), album = "", path = "/m/z/7.mp3"),
            )
        val expected = indexer.build(songs, on)
        repeat(5) { seed ->
            assertEquals(expected, indexer.build(songs.shuffled(kotlin.random.Random(seed)), on))
        }
    }

    @Test
    fun colliddingGroupKeysGetStableSuffixes() {
        // 两组未知艺人的同名专辑在不同目录：推断结果相同，groupKey 加序号区分
        val a = song(album = "Demo", path = "/m/a/1.mp3")
        val b = song(album = "Demo", path = "/m/b/1.mp3")
        val graph = indexer.build(listOf(a, b), off)
        assertEquals(2, graph.albums.size)
        assertEquals(graph.albums.map { it.groupKey }.toSet().size, 2)
        assertTrue(requireNotNull(graph.songAlbums[b.songId]).endsWith("#2"))
    }

    @Test
    fun policyFingerprintRoundTrips() {
        assertEquals(on, IndexPolicy.parse(on.fingerprint))
        assertEquals(off, IndexPolicy.parse(off.fingerprint))
        assertNull(IndexPolicy.parse(""))
    }

    @Test
    fun flatDirectoriesAreNotGroupingEvidence() {
        assertNull(LibraryIndexer.directoryOf("/storage/emulated/0/a.mp3"))
        assertNull(LibraryIndexer.directoryOf("/storage/emulated/0/Download/a.mp3"))
        assertNull(LibraryIndexer.directoryOf("/storage/1234-ABCD/Music/a.mp3"))
        assertNull(LibraryIndexer.directoryOf("/storage/1234-ABCD/a.mp3"))
        assertEquals("/storage/emulated/0/Music/A", LibraryIndexer.directoryOf("/storage/emulated/0/Music/A/a.mp3"))
    }
}
