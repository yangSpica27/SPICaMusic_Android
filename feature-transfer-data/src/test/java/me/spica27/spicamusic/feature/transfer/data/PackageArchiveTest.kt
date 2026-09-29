package me.spica27.spicamusic.feature.transfer.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.spica27.spicamusic.feature.transfer.domain.LibraryPackage
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.PackageLyrics
import me.spica27.spicamusic.feature.transfer.domain.PackageManifest
import me.spica27.spicamusic.feature.transfer.domain.PackagePlaylist
import me.spica27.spicamusic.feature.transfer.domain.PackagePlaylistEntry
import me.spica27.spicamusic.feature.transfer.domain.PackageTrack
import me.spica27.spicamusic.feature.transfer.domain.SourceReference
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PackageArchiveTest {
    private val audio = ByteArray(4096) { (it % 251).toByte() }
    private val lyric = "<tt><body>原文与逐字时间 &amp; 翻译</body></tt>\n"

    private fun fixture(
        suppressed: Boolean = false,
        includeSnapshot: Boolean = true,
    ): Pair<LibraryPackage, Map<String, ByteArray>> {
        val audioPath = "audio/${PackageArchive.digest(audio)}.bin"
        val lyricBytes = lyric.toByteArray()
        val lyricPath = "lyrics/${PackageArchive.digest(lyricBytes)}.ttml"
        val source = SourceReference("future-platform", "id/strings-are-opaque", "server-1", buildJsonObject { put("version", "live") })
        val track =
            PackageTrack(
                "track-1",
                "歌曲",
                "歌手",
                "专辑",
                12345,
                "song.flac",
                "audio/flac",
                audioPath,
                true,
                lyrics = PackageLyrics(lyricPath.takeIf { includeSnapshot }, "ttml", -235, "ONLINE", "已选版本", true, suppressed),
                sources = listOf(source),
            )
        val second = track.copy(id = "track-2", title = "重复文件，独立状态", favorite = false, lyrics = null)
        val tracks = listOf(track, second)
        val playlists =
            listOf(
                PackagePlaylist(
                    "playlist-1",
                    "测试歌单",
                    123,
                    listOf(PackagePlaylistEntry("entry-2", "track-2"), PackagePlaylistEntry("entry-1", "track-1")),
                ),
            )
        val payload =
            linkedMapOf(
                audioPath to audio,
                lyricPath to lyricBytes,
                "tracks.json" to PackageJson.encodeToString(tracks).toByteArray(),
                "playlists.json" to PackageJson.encodeToString(playlists).toByteArray(),
            ).also { if (!includeSnapshot) it.remove(lyricPath) }
        val assets =
            payload.map { (path, bytes) ->
                PackageArchive.asset(path, bytes, if (path.endsWith(".json")) "metadata" else path.substringBefore('/'))
            }
        val features =
            listOf("complete-audio", "selected-lyrics") + if (suppressed) listOf(LibraryPackage.LYRICS_SUPPRESSION) else emptyList()
        return LibraryPackage(
            PackageManifest(archiveId = "archive-1", createdAt = 456, assets = assets, requiredFeatures = features),
            tracks,
            playlists,
        ) to
            payload
    }

    @Test
    fun suppressedSelectionsRoundTripWithAndWithoutDormantSnapshot() =
        runBlocking {
            listOf(true, false).forEach { includeSnapshot ->
                val (library, payload) = fixture(suppressed = true, includeSnapshot = includeSnapshot)
                val output = ByteArrayOutputStream()
                PackageArchive().write(library, output, { ByteArrayInputStream(payload.getValue(it)) })
                withArchive(output.toByteArray()) { file ->
                    val restored = PackageArchive().inspect(file)
                    assertEquals(library, restored)
                    assertTrue(
                        restored.tracks
                            .first()
                            .lyrics!!
                            .suppressed,
                    )
                    assertTrue(LibraryPackage.LYRICS_SUPPRESSION in restored.manifest.requiredFeatures)
                }
            }
        }

    @Test
    fun legacyPackageWithoutSuppressedFieldDefaultsToFalse() =
        runBlocking {
            val (library, payload) = fixture()
            val oldTracks =
                payload
                    .getValue("tracks.json")
                    .toString(Charsets.UTF_8)
                    .replace(",\"suppressed\":false", "")
                    .toByteArray()
            val legacy =
                library.copy(
                    manifest =
                        library.manifest.copy(
                            assets =
                                library.manifest.assets.map {
                                    if (it.path == "tracks.json") PackageArchive.asset(it.path, oldTracks, "metadata") else it
                                },
                        ),
                )
            withArchive(uncheckedZip(legacy, payload + ("tracks.json" to oldTracks))) { file ->
                val restored = PackageArchive().inspect(file)
                assertFalse(
                    restored.tracks
                        .first()
                        .lyrics!!
                        .suppressed,
                )
                assertFalse(LibraryPackage.LYRICS_SUPPRESSION in restored.manifest.requiredFeatures)
            }
        }

    @Test
    fun suppressionWithoutRequiredFeatureIsRejected() =
        runBlocking {
            val (library, payload) = fixture(suppressed = true)
            rejects(
                uncheckedZip(
                    library.copy(manifest = library.manifest.copy(requiredFeatures = listOf("complete-audio", "selected-lyrics"))),
                    payload,
                ),
            )
        }

    @Test
    fun roundTripPreservesAudioLyricsOrderIndependentIdentitiesAndFutureSource() =
        runBlocking {
            val (library, payload) = fixture()
            val bytes = ByteArrayOutputStream()
            PackageArchive().write(library, bytes, { ByteArrayInputStream(payload.getValue(it)) })
            withArchive(bytes.toByteArray()) { file ->
                assertEquals(library, PackageArchive().inspect(file))
                java.util.zip.ZipFile(file).use { zip ->
                    assertArrayEquals(audio, zip.getInputStream(zip.getEntry(library.tracks.first().audio)).readBytes())
                    assertEquals(
                        lyric,
                        PackageArchive.readSmall(
                            zip,
                            library.tracks
                                .first()
                                .lyrics!!
                                .asset!!,
                            10000,
                        ),
                    )
                    assertEquals(1, zip.entries().asSequence().count { it.name.startsWith("audio/") })
                }
            }
        }

    @Test
    fun corruptAudioIsRejectedBeforeImport() =
        runBlocking {
            val (library, payload) = fixture()
            val tampered = payload + (library.tracks.first().audio to audio.copyOf().apply { this[0] = 99 })
            rejects(uncheckedZip(library, tampered))
        }

    @Test
    fun missingAudioIsRejected() =
        runBlocking {
            val (library, payload) = fixture()
            rejects(uncheckedZip(library, payload - library.tracks.first().audio))
        }

    @Test
    fun undeclaredTraversalEntryIsRejected() =
        runBlocking {
            val (library, payload) = fixture()
            rejects(uncheckedZip(library, payload + ("../../outside" to byteArrayOf(1))))
        }

    @Test
    fun unsupportedVersionsAndRequiredFeaturesAreRejected() =
        runBlocking {
            val (library, payload) = fixture()
            rejects(uncheckedZip(library.copy(manifest = library.manifest.copy(version = 2)), payload))
            rejects(uncheckedZip(library.copy(manifest = library.manifest.copy(requiredFeatures = listOf("future-feature"))), payload))
        }

    @Test
    fun changedFileDuringExportIsRejected() =
        runBlocking {
            val (library, payload) = fixture()
            try {
                PackageArchive().write(library, ByteArrayOutputStream(), { path ->
                    ByteArrayInputStream(if (path.startsWith("audio/")) audio.copyOf(audio.size - 1) else payload.getValue(path))
                })
                fail("Changed file must not be exported as a complete package")
            } catch (_: IllegalArgumentException) {
            }
        }

    @Test
    fun declaredSizeBoundsAreEnforced() =
        runBlocking {
            val (library, payload) = fixture()
            val assets = library.manifest.assets.map { if (it.kind == "audio") it.copy(size = 1) else it }
            rejects(uncheckedZip(library.copy(manifest = library.manifest.copy(assets = assets)), payload))
        }

    @Test
    fun missingPlaylistReferencesAndRepeatedTracksAreRejected() {
        val (library, _) = fixture()
        listOf(
            listOf(PackagePlaylistEntry("a", "missing")),
            listOf(PackagePlaylistEntry("a", "track-1"), PackagePlaylistEntry("b", "track-1")),
        ).forEach { entries ->
            try {
                library.copy(playlists = listOf(library.playlists.first().copy(entries = entries))).validate()
                fail("Invalid membership must not be silently discarded")
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    private fun uncheckedZip(
        library: LibraryPackage,
        payload: Map<String, ByteArray>,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write(PackageJson.encodeToString(library.manifest).toByteArray())
            zip.closeEntry()
            payload.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private suspend fun rejects(bytes: ByteArray) =
        withArchive(bytes) { file ->
            try {
                PackageArchive().inspect(file)
                fail("Invalid package accepted")
            } catch (_: IllegalArgumentException) {
            }
        }

    private suspend fun withArchive(
        bytes: ByteArray,
        block: suspend (File) -> Unit,
    ) {
        val file = File.createTempFile("spica-test", ".spica")
        try {
            file.writeBytes(bytes)
            block(file)
        } finally {
            file.delete()
        }
    }
}
