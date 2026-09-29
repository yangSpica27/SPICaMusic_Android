package me.spica27.spicamusic.feature.transfer.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.spica27.spicamusic.core.preferences.PreferencesManager
import me.spica27.spicamusic.feature.transfer.domain.LibraryPackage
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.IScanRulesRepository
import me.spica27.spicamusic.storage.api.LibraryAccessGate
import me.spica27.spicamusic.storage.api.LocalLyricFile
import me.spica27.spicamusic.storage.api.ScanRules
import me.spica27.spicamusic.storage.api.SelectedLyricsResolver
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.repository.LyricRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.ScanFolderRepositoryImpl
import me.spica27.spicamusic.storage.impl.scanner.MusicScanService
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CompletePackageInstrumentedTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databases = mutableListOf<AppDatabase>()
    private val sourceUris = mutableListOf<Uri>()
    private val taskIds = mutableListOf<String>()
    private val files = mutableListOf<File>()
    private val embedded = mutableMapOf<Long, String>()
    private val reader =
        object : ILyricSourceReader {
            override suspend fun readEmbedded(mediaStoreId: Long): String? = embedded[mediaStoreId]

            override suspend fun readLocalFile(uri: String): LocalLyricFile? = null
        }

    @After
    fun cleanup() {
        runBlocking(Dispatchers.IO) {
            val db = databases.firstOrNull() ?: newDatabase()
            taskIds.forEach { LibraryPackageImporter(context, db, reader).cleanup(it) }
            sourceUris.forEach { context.contentResolver.delete(it, null, null) }
            files.forEach { it.delete() }
            File(context.filesDir, "library-transfer").deleteRecursively()
            databases.forEach { it.close() }
        }
    }

    @Test
    fun completePackageRestoresAudioFavoritesLyricsOrderAndIsIdempotent() =
        runBlocking(Dispatchers.IO) {
            val (source, file) = fixture()
            val originalIds =
                source
                    .transferDao()
                    .songs()
                    .map { it.mediaStoreId }
                    .toSet()
            sourceUris.forEach { context.contentResolver.delete(it, null, null) }
            sourceUris.clear()
            val destination = newDatabase()
            val importer = LibraryPackageImporter(context, destination, reader)
            val plan = importer.prepare(file) {}
            assertEquals(2, plan.preview.newSongs)
            val task = newTask()
            importer.commit(task, file, plan, false) {}
            val songs = destination.transferDao().songs()
            assertEquals(2, songs.size)
            assertTrue(songs.none { it.mediaStoreId in originalIds })
            assertTrue(songs.first { it.displayName == "手动歌词" }.like)
            assertFalse(songs.first { it.displayName == "内嵌歌词" }.like)
            songs.forEach { song ->
                val bytes = context.contentResolver.openInputStream(mediaUri(song.mediaStoreId))!!.use { it.readBytes() }
                assertArrayEquals(wav(), bytes)
            }
            val lyrics = destination.transferDao().lyrics().associateBy { it.mediaId }
            val manual = lyrics.getValue(songs.first { it.displayName == "手动歌词" }.mediaStoreId)
            assertEquals("[00:01.00]当前选择\n[00:02.00]第二行", manual.lyrics)
            assertEquals(-275L, manual.delay)
            assertTrue(manual.isManual)
            assertTrue(manual.restoredSnapshot)
            val automatic = lyrics.getValue(songs.first { it.displayName == "内嵌歌词" }.mediaStoreId)
            assertEquals("内嵌原文\n第二行", automatic.lyrics)
            assertFalse(automatic.isManual)
            assertEquals("EMBEDDED", automatic.sourceType)
            assertTrue(automatic.restoredSnapshot)
            val playlistId =
                destination
                    .transferDao()
                    .playlists()
                    .single()
                    .playlistId!!
            assertEquals(listOf("内嵌歌词", "手动歌词"), destination.playlistDao().getSongsByPlaylistId(playlistId).map { it.displayName })
            val repeat = importer.prepare(file) {}
            assertEquals(0, repeat.preview.newSongs)
            assertEquals(2, repeat.preview.reusedSongs)
            assertEquals(0, repeat.preview.newPlaylists)
            importer.commit(newTask(), file, repeat, false) {}
            assertEquals(2, destination.transferDao().songs().size)
            assertEquals(1, destination.transferDao().playlists().size)
            assertEquals(2, destination.transferDao().entries().size)
            assertNotNull(destination.transferDao().receipt(task))
        }

    @Test
    fun mergeKeepsLocalLyricsAndFavoritesUntilReplacementIsSelected() =
        runBlocking(Dispatchers.IO) {
            val (database, file) = fixture()
            val favorite = database.transferDao().songs().first { it.like }
            val existing = database.lyricDao().getLyricWithMediaId(favorite.mediaStoreId)!!
            database.lyricDao().insertLyric(existing.copy(lyrics = "本地新歌词", delay = 500))
            val other = database.transferDao().songs().first { !it.like }
            database.songDao().likeSongs(other.songId!!, true)
            val importer = LibraryPackageImporter(context, database, reader)
            val keep = importer.prepare(file) {}
            assertEquals(1, keep.preview.lyricConflicts)
            importer.commit(newTask(), file, keep, false) {}
            assertEquals("本地新歌词", database.lyricDao().getLyricWithMediaId(favorite.mediaStoreId)!!.lyrics)
            assertTrue(database.songDao().getSongWithId(other.songId!!)!!.like)
            importer.commit(newTask(), file, importer.prepare(file) {}, true) {}
            assertEquals(existing.lyrics, database.lyricDao().getLyricWithMediaId(favorite.mediaStoreId)!!.lyrics)
            assertEquals(-275L, database.lyricDao().getLyricWithMediaId(favorite.mediaStoreId)!!.delay)
        }

    @Test
    fun interruptedAudioCopyRollsBackAndCanBeRetried() =
        runBlocking(Dispatchers.IO) {
            val (_, file) = fixture()
            val database = newDatabase()
            val importer = LibraryPackageImporter(context, database, reader)
            val plan = importer.prepare(file) {}
            val task = newTask()
            try {
                importer.commit(task, file, plan, false) { if (it.completed > wav().size) throw CancellationException("test interruption") }
                fail("Expected interruption")
            } catch (_: CancellationException) {
            }
            assertTrue(database.transferDao().songs().isEmpty())
            assertTrue(database.transferDao().playlists().isEmpty())
            assertNull(database.transferDao().receipt(task))
            assertEquals(0, countTaskFiles(task))
            importer.commit(task, file, importer.prepare(file) {}, false) {}
            assertEquals(2, database.transferDao().songs().size)
            assertEquals(2, countTaskFiles(task))
        }

    @Test
    fun migration20To21PreservesUserData() =
        runBlocking(Dispatchers.IO) {
            val name = "migration-${UUID.randomUUID()}.db"
            val first = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            first.lyricDao().insertLyric(ExtraInfoEntity(mediaId = 99, lyrics = "保存我的歌词", delay = -100, isManual = true))
            first.playlistDao().insertPlaylistAndGetId(PlaylistEntity(playlistName = "保存我的歌单"))
            first.close()
            try {
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
                    raw.execSQL(
                        "CREATE TABLE extra_info_old (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaId INTEGER NOT NULL, lyrics TEXT NOT NULL, cover TEXT NOT NULL, delay INTEGER NOT NULL, lyricSourceName TEXT NOT NULL, sourceType TEXT NOT NULL, isManual INTEGER NOT NULL, sourceUri TEXT NOT NULL)",
                    )
                    raw.execSQL(
                        "INSERT INTO extra_info_old SELECT id, mediaId, lyrics, cover, delay, lyricSourceName, sourceType, isManual, sourceUri FROM extra_info",
                    )
                    raw.execSQL("DROP TABLE extra_info")
                    raw.execSQL("ALTER TABLE extra_info_old RENAME TO extra_info")
                    raw.execSQL("CREATE UNIQUE INDEX index_extra_info_mediaId ON extra_info(mediaId)")
                    raw.execSQL("DROP TABLE TransferIdentity")
                    raw.execSQL("DROP TABLE TransferReceipt")
                    raw.version = 20
                }
                val migrated =
                    Room
                        .databaseBuilder(
                            context,
                            AppDatabase::class.java,
                            name,
                        ).addMigrations(AppDatabase.MIGRATION_20_21, AppDatabase.MIGRATION_21_22)
                        .build()
                try {
                    val lyrics = migrated.lyricDao().getLyricWithMediaId(99)!!
                    assertEquals("保存我的歌词", lyrics.lyrics)
                    assertEquals(-100L, lyrics.delay)
                    assertTrue(lyrics.isManual)
                    assertFalse(lyrics.restoredSnapshot)
                    assertEquals(
                        "保存我的歌单",
                        migrated
                            .transferDao()
                            .playlists()
                            .single()
                            .playlistName,
                    )
                    assertTrue(migrated.transferDao().identities().isEmpty())
                } finally {
                    migrated.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }

    @Test
    fun importedSongsSurviveStricterScanRules() =
        runBlocking(Dispatchers.IO) {
            val (_, file) = fixture()
            val database = newDatabase()
            val importer = LibraryPackageImporter(context, database, reader)
            importer.commit(newTask(), file, importer.prepare(file) {}, false) {}
            val ids =
                database
                    .transferDao()
                    .songs()
                    .map { it.mediaStoreId }
                    .toSet()
            val strictRules = ScanRules(60_000, 10_000_000, emptySet())
            val rules =
                object : IScanRulesRepository {
                    override fun getRulesFlow() = flowOf(strictRules)

                    override suspend fun getRulesSync() = strictRules

                    override suspend fun setMinDurationSec(seconds: Int) = Unit

                    override suspend fun setMinFileSizeKb(kb: Int) = Unit

                    override suspend fun setEnabledFormats(keys: Set<String>) = Unit
                }
            val scanner =
                MusicScanService(
                    context,
                    database.songDao(),
                    database.albumDao(),
                    ScanFolderRepositoryImpl(database.scanFolderDao()),
                    rules,
                    PreferencesManager(context),
                    LibraryAccessGate(),
                )
            scanner.scanMediaStore()
            assertEquals(
                ids,
                database
                    .transferDao()
                    .songs()
                    .map { it.mediaStoreId }
                    .toSet(),
            )
            assertEquals(2, database.transferDao().entries().size)
        }

    @Test
    fun stagedPackageAndCommittedReceiptSurviveCoordinatorRecreation() =
        runBlocking(Dispatchers.IO) {
            val (_, file) = fixture()
            val database = newDatabase()
            val gate = LibraryAccessGate()
            val first = LibraryTransferCoordinator(context, database, gate, reader)
            first.execute(TransferAction.INSPECT, Uri.fromFile(file).toString())
            assertTrue(first.state.value.toString(), first.state.value is TransferState.Preview)
            val journal = File(context.filesDir, "library-transfer/job.json")
            val record = journal.readText()
            val task =
                PackageJson
                    .parseToJsonElement(record)
                    .jsonObject
                    .getValue("taskId")
                    .jsonPrimitive.content
            taskIds.add(task)
            assertTrue(file.delete()) // The source document is no longer needed after staging.
            val restarted = LibraryTransferCoordinator(context, database, gate, reader)
            assertTrue(restarted.state.value is TransferState.Interrupted)
            restarted.execute(TransferAction.RESUME)
            assertTrue(restarted.state.value.toString(), restarted.state.value is TransferState.Preview)
            restarted.execute(TransferAction.IMPORT)
            assertTrue(restarted.state.value.toString(), restarted.state.value is TransferState.Success)
            assertEquals(2, database.transferDao().songs().size)
            // 模拟数据库提交后、任务日志清理前进程退出。
            journal.writeText(record.replace("PREVIEW", "IMPORT"))
            val afterCommit = LibraryTransferCoordinator(context, database, gate, reader)
            afterCommit.execute(TransferAction.RESUME)
            assertTrue(afterCommit.state.value.toString(), afterCommit.state.value is TransferState.Success)
            assertEquals(2, countTaskFiles(task))
            assertEquals(2, database.transferDao().songs().size)
        }

    @Test
    fun suppressedSelectionsExportAndRestoreEvenWithoutAnyLyricsText() =
        runBlocking(Dispatchers.IO) {
            val (source, file) = fixture()
            val sourceRepo = LyricRepositoryImpl(source.lyricDao())
            source.transferDao().songs().forEach { sourceRepo.suppressLyrics(it.mediaStoreId) }
            LibraryPackageExporter(context, source, reader).export(Uri.fromFile(file)) {}
            val archive = PackageArchive().inspect(file)
            assertTrue(LibraryPackage.LYRICS_SUPPRESSION in archive.manifest.requiredFeatures)
            assertTrue(archive.tracks.all { it.lyrics?.suppressed == true })
            assertNull(
                archive.tracks
                    .first { it.title == "内嵌歌词" }
                    .lyrics!!
                    .asset,
            )
            assertNotNull(
                archive.tracks
                    .first { it.title == "手动歌词" }
                    .lyrics!!
                    .asset,
            )

            val destination = newDatabase()
            val importer = LibraryPackageImporter(context, destination, reader)
            importer.commit(newTask(), file, importer.prepare(file) {}, false) {}
            val restored = destination.transferDao().lyrics()
            assertEquals(2, restored.size)
            restored.forEach {
                assertTrue(it.lyricsSuppressed)
                assertEquals(
                    it.stored(),
                    SelectedLyricsResolver.resolve(it.mediaId, it.stored()) { error("Suppression must bypass embedded lyrics") },
                )
            }
            assertEquals(-275L, restored.single { it.lyrics.isNotBlank() }.delay)
            val repeat = importer.prepare(file) {}
            assertEquals(0, repeat.preview.lyricConflicts)
            importer.commit(newTask(), file, repeat, false) {}
            assertTrue(destination.transferDao().lyrics().all { it.lyricsSuppressed })
        }

    @Test
    fun suppressionConflictsRespectKeepLocalAndUseImportedInBothDirections() =
        runBlocking(Dispatchers.IO) {
            val (source, file) = fixture()
            val destination = newDatabase()
            val importer = LibraryPackageImporter(context, destination, reader)
            importer.commit(newTask(), file, importer.prepare(file) {}, false) {}
            val localSong = destination.transferDao().songs().single { it.like }
            val localRepo = LyricRepositoryImpl(destination.lyricDao())
            localRepo.suppressLyrics(localSong.mediaStoreId)
            var preview = importer.prepare(file) {}
            assertEquals(1, preview.preview.lyricConflicts)
            importer.commit(newTask(), file, preview, false) {}
            assertTrue(localRepo.getLyrics(localSong.mediaStoreId)!!.lyricsSuppressed)
            importer.commit(newTask(), file, importer.prepare(file) {}, true) {}
            assertFalse(localRepo.getLyrics(localSong.mediaStoreId)!!.lyricsSuppressed)

            val sourceSong = source.transferDao().songs().single { it.like }
            LyricRepositoryImpl(source.lyricDao()).suppressLyrics(sourceSong.mediaStoreId)
            LibraryPackageExporter(context, source, reader).export(Uri.fromFile(file)) {}
            preview = importer.prepare(file) {}
            assertEquals(1, preview.preview.lyricConflicts)
            importer.commit(newTask(), file, preview, false) {}
            assertFalse(localRepo.getLyrics(localSong.mediaStoreId)!!.lyricsSuppressed)
            importer.commit(newTask(), file, importer.prepare(file) {}, true) {}
            assertTrue(localRepo.getLyrics(localSong.mediaStoreId)!!.lyricsSuppressed)
        }

    private suspend fun fixture(): Pair<AppDatabase, File> {
        val database = newDatabase()
        val first = createSong("手动歌词", true)
        val second = createSong("内嵌歌词", false)
        database.transferDao().insertSong(first)
        database.transferDao().insertSong(second)
        embedded[second.mediaStoreId] = "内嵌原文\n第二行"
        database.lyricDao().insertLyric(
            ExtraInfoEntity(
                mediaId = first.mediaStoreId,
                lyrics = "[00:01.00]当前选择\n[00:02.00]第二行",
                delay = -275,
                lyricSourceName = "选择的歌词",
                isManual = true,
            ),
        )
        val id = database.playlistDao().insertPlaylistAndGetId(PlaylistEntity(playlistName = "顺序测试"))
        database.playlistDao().insertListItems(
            listOf(
                PlaylistSongCrossRefEntity(id, second.mediaStoreId, sortOrder = 20),
                PlaylistSongCrossRefEntity(id, first.mediaStoreId, sortOrder = 10),
            ),
        )
        val file = File.createTempFile("complete-library", ".spica", context.cacheDir).also(files::add)
        LibraryPackageExporter(context, database, reader).export(Uri.fromFile(file)) {}
        return database to file
    }

    private fun newDatabase() = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().also(databases::add)

    private fun newTask() = UUID.randomUUID().toString().also(taskIds::add)

    private fun createSong(
        title: String,
        favorite: Boolean,
    ): SongEntity {
        val name = "${UUID.randomUUID()}.wav"
        val uri =
            context.contentResolver
                .insert(
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    ContentValues().apply {
                        put(MediaStore.Audio.Media.DISPLAY_NAME, name)
                        put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/SPICaTransferTests/")
                        put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                        put(MediaStore.Audio.Media.IS_PENDING, 1)
                    },
                )!!
                .also(sourceUris::add)
        context.contentResolver.openOutputStream(uri)!!.use { it.write(wav()) }
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
        return SongEntity(
            mediaStoreId = ContentUris.parseId(uri),
            path = "/$name",
            displayName = title,
            artist = "测试歌手",
            size = wav().size.toLong(),
            like = favorite,
            duration = 1000,
            sort = 0,
            sortName = title,
            mimeType = "audio/wav",
            albumId = 0,
            album = "测试专辑",
            sampleRate = 8000,
            bitRate = 128000,
            channels = 1,
            digit = 16,
            isIgnore = false,
            codec = "pcm",
        )
    }

    @Suppress("DEPRECATION")
    private fun countTaskFiles(task: String): Int =
        context.contentResolver
            .query(
                MediaStore.setIncludePending(MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)),
                arrayOf(MediaStore.Audio.Media._ID),
                "relative_path = ?",
                arrayOf("Music/SPICaMusic/Imports/$task/"),
                null,
            )!!
            .use { it.count }

    private fun wav(): ByteArray =
        ByteBuffer
            .allocate(16044)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray())
                putInt(16036)
                put("WAVEfmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(8000)
                putInt(16000)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(16000)
                repeat(8000) { putShort((it % 100).toShort()) }
            }.array()
}
