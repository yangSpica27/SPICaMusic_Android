package me.spica27.spicamusic.storage.impl

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kyant.taglib.TagLib
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.common.entity.ArtistKind
import me.spica27.spicamusic.common.entity.ArtistRole
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.PlaylistEntity
import me.spica27.spicamusic.storage.impl.entity.PlaylistSongCrossRefEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.entity.TagStatus
import me.spica27.spicamusic.storage.impl.index.IndexPolicy
import me.spica27.spicamusic.storage.impl.index.TagJson
import me.spica27.spicamusic.storage.impl.scanner.AndroidIndexText
import me.spica27.spicamusic.storage.impl.scanner.ExtractedTags
import me.spica27.spicamusic.storage.impl.scanner.LibraryWriter
import me.spica27.spicamusic.storage.impl.scanner.MediaFileRow
import me.spica27.spicamusic.storage.impl.scanner.SongRowBuilder
import me.spica27.spicamusic.storage.impl.scanner.TagExtractor
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/** v23 曲库：迁移、重建收尾、关系重建与查询在 Android SQLite 上的行为 */
@RunWith(AndroidJUnit4::class)
class LibraryV23InstrumentedTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databases = mutableListOf<AppDatabase>()
    private val names = mutableListOf<String>()
    private val files = mutableListOf<File>()
    private val off = IndexPolicy(splitArtists = false)
    private val on = IndexPolicy(splitArtists = true)

    @After
    fun cleanup() {
        databases.forEach { it.close() }
        names.forEach { context.deleteDatabase(it) }
        files.forEach { it.delete() }
    }

    // ===== 迁移 =====

    @Test
    fun migration22To23CarriesMarksAndLeavesUserTablesUntouched() =
        runBlocking {
            val db = migratedFromV22()
            assertEquals(0, count(db, "Song"))
            assertEquals(
                listOf(Triple(101L, true, false), Triple(103L, true, false), Triple(104L, false, true), Triple(105L, true, false)),
                rows(db, "SELECT mediaStoreId, isLiked, isImported FROM LegacySongCarryOver ORDER BY mediaStoreId") {
                    Triple(it.getLong(0), it.getInt(1) == 1, it.getInt(2) == 1)
                },
            )
            assertEquals(listOf("playlist"), rows(db, "SELECT kind FROM TransferIdentity") { it.getString(0) })
            assertTrue(db.libraryDao().state()!!.pendingRebuild)
            // 重建前不动歌单、歌词、统计
            assertEquals(4, count(db, "PlaylistSongCrossRef"))
            assertEquals(1, count(db, "extra_info"))
            assertEquals(1, count(db, "SongPlayStat"))
            assertEquals(1, count(db, "PlayHistory"))
        }

    @Test
    fun rebuildRestoresMarksAndPrunesOnlyMissingPlaylistEntries() =
        runBlocking {
            val db = migratedFromV22()
            val writer = LibraryWriter(db)
            // 第一批：101、102
            writer.commit(listOf(song(101), song(102)), policy = off, reindex = true)
            assertEquals(4, count(db, "PlaylistSongCrossRef"))
            // 最后一批：103、104；105 再也没出现
            writer.commit(listOf(song(103), song(104)), policy = off, reindex = true, finalizeRebuild = true)

            val marks =
                rows(db, "SELECT mediaStoreId, `like`, isImported FROM Song ORDER BY mediaStoreId") {
                    Triple(it.getLong(0), it.getInt(1) == 1, it.getInt(2) == 1)
                }
            assertEquals(
                listOf(Triple(101L, true, false), Triple(102L, false, false), Triple(103L, true, false), Triple(104L, false, true)),
                marks,
            )
            assertEquals(
                listOf(101L, 103L),
                rows(db, "SELECT mediaId FROM PlaylistSongCrossRef WHERE playlistId = 1 ORDER BY sortOrder DESC") { it.getLong(0) },
            )
            assertEquals(
                listOf(1 to true, 2 to false),
                rows(db, "SELECT playlistId, needUpdate FROM Playlist ORDER BY playlistId") { it.getInt(0) to (it.getInt(1) == 1) },
            )
            assertEquals(0, count(db, "LegacySongCarryOver"))
            assertFalse(db.libraryDao().state()!!.pendingRebuild)
            // 歌词、统计按 mediaStoreId 重新对上
            assertEquals(
                "[00:01]x",
                rows(db, "SELECT e.lyrics FROM extra_info e JOIN Song s ON s.mediaStoreId = e.mediaId") { it.getString(0) }.single(),
            )
        }

    // ===== 关系重建 =====

    @Test
    fun reindexBuildsRelationsAndToggleOnlyChangesAffectedRows() =
        runBlocking {
            val db = newDatabase()
            val writer = LibraryWriter(db)
            writer.commit(
                listOf(
                    song(1, title = "a", artists = listOf("周杰倫/费玉清"), album = "Duet", albumArtists = listOf("周杰伦"), dir = "duet"),
                    song(2, title = "b", artists = listOf("周杰伦"), album = "Duet", albumArtists = listOf("周杰伦"), dir = "duet"),
                    song(3, title = "c", artists = listOf("X"), composers = listOf("周杰伦"), album = "Other", dir = "other"),
                ),
                policy = off,
                reindex = true,
            )
            val albumsBefore = db.albumDao().getAllAlbumsFlow().first()
            assertEquals(2, albumsBefore.size)
            // 拆分关闭：整串算一位艺人
            val jay = db.libraryDao().artists().single { it.name == "周杰伦" }
            assertEquals(setOf("周杰倫/费玉清", "周杰伦", "X"), db.libraryDao().artists().map { it.name }.toSet())
            val composed = db.artistDao().getSongsByRoleFlow(jay.artistId, ArtistRole.COMPOSER.name).first()
            assertEquals(listOf(3L), composed.map { it.mediaStoreId })

            // 拆分开启：拆出的 周杰倫 经设备上的繁转简并入 周杰伦，ID 不变
            writer.reindex(on)
            val names = db.libraryDao().artists().map { it.name }.toSet()
            assertTrue(names.containsAll(setOf("周杰伦", "费玉清", "X")))
            assertFalse("周杰倫/费玉清" in names)
            val albumsAfter = db.albumDao().getAllAlbumsFlow().first()
            assertEquals(albumsBefore.map { it.album.albumId }, albumsAfter.map { it.album.albumId })
            assertEquals(jay.artistId, db.libraryDao().artists().single { it.name == "周杰伦" }.artistId)

            // 对同一输入再重建：关系表没有任何变化
            val snapshot = relationSnapshot(db)
            writer.reindex(on)
            assertEquals(snapshot, relationSnapshot(db))
        }

    @Test
    fun upsertKeepsCreditsAndDeletingSongCleansOrphans() =
        runBlocking {
            val db = newDatabase()
            val writer = LibraryWriter(db)
            writer.commit(listOf(song(1, artists = listOf("A"), album = "Solo", dir = "solo")), policy = off, reindex = true)
            val existing = db.songDao().getAllScanInfo().single()
            // 按扫描的方式重建同一首（沿用已有行）再 upsert，不重建关系：
            // 不能像 REPLACE 那样级联删掉署名，也不能清掉 albumId
            val rescanned =
                SongRowBuilder.build(
                    MediaFileRow(1, "/storage/emulated/0/Music/solo/1.mp3", "1.mp3", 1, "audio/mpeg", 2, 1000, null, null, 0),
                    ExtractedTags(mapOf("ARTIST" to listOf("A"), "ALBUM" to listOf("Solo")), audio = null),
                    existing = existing,
                    text = AndroidIndexText.instance,
                )
            writer.commit(listOf(rescanned), policy = off, reindex = false)
            assertEquals(1, db.libraryDao().songArtists().size)
            assertNotNull(db.songDao().getAllScanInfo().single().albumId)

            writer.commit(emptyList(), removedMediaStoreIds = listOf(1L), policy = off, reindex = true)
            assertEquals(0, count(db, "Album"))
            assertEquals(0, count(db, "Artist"))
            assertEquals(0, count(db, "SongArtist"))
        }

    @Test
    fun songsCommittedWithoutReindexAreDetectedAsUnindexed() =
        runBlocking {
            val db = newDatabase()
            val writer = LibraryWriter(db)
            // 模拟分批提交后中断：歌曲已入库，关系重建没跑到
            writer.commit(listOf(song(1, artists = listOf("A"), album = "X", dir = "x")), policy = off, reindex = false)
            assertTrue(db.libraryDao().hasUnindexedSongs())
            writer.reindex(off)
            assertFalse(db.libraryDao().hasUnindexedSongs())
            // 没有任何艺人标签的歌也会关联未知艺人，不会被一直当作未建关系
            writer.commit(listOf(song(2)), policy = off, reindex = true)
            assertFalse(db.libraryDao().hasUnindexedSongs())
        }

    @Test
    fun queriesHonourIgnoredSongsAppearancesAndRelatedAlbums() =
        runBlocking {
            val db = newDatabase()
            val writer = LibraryWriter(db)
            writer.commit(
                listOf(
                    song(1, artists = listOf("Alice"), album = "First", albumArtists = listOf("Alice"), dir = "first", year = 2001),
                    song(2, artists = listOf("Alice", "Bob"), album = "First", albumArtists = listOf("Alice"), dir = "first"),
                    song(3, artists = listOf("Alice"), album = "Second", albumArtists = listOf("Alice"), dir = "second", year = 2005),
                    song(4, artists = listOf("Carol"), album = "Mix", compilation = true, dir = "mix"),
                ),
                policy = off,
                reindex = true,
            )
            val artists = db.libraryDao().artists().associateBy { it.name }
            val bob = artists.getValue("Bob").artistId
            val alice = artists.getValue("Alice").artistId
            val first = db.libraryDao().albums().single { it.title == "First" }.albumId

            assertEquals(listOf("First"), db.albumDao().getAppearancesFlow(bob).first().map { it.album.title })
            assertEquals(listOf("Second", "First"), db.albumDao().getAlbumsByArtistFlow(alice).first().map { it.album.title })
            assertEquals(listOf("Second"), db.albumDao().getRelatedAlbumsFlow(first).first().map { it.album.title })
            val list = db.artistDao().getAllArtistsFlow().first()
            assertEquals(listOf(ArtistKind.NORMAL, ArtistKind.NORMAL, ArtistKind.NORMAL, ArtistKind.VARIOUS), list.map { it.artist.kind })
            assertEquals(3, list.single { it.artist.name == "Alice" }.songCount)
            assertEquals(1, list.single { it.artist.kind == ArtistKind.VARIOUS }.songCount)

            // 隐藏 Bob 唯一参与的歌：参与作品与艺人列表都不再出现 Bob，专辑歌曲数同步减少
            db.songDao().ignoreByMediaStoreId(2, true)
            assertTrue(db.albumDao().getAppearancesFlow(bob).first().isEmpty())
            assertNull(db.artistDao().getAllArtistsFlow().first().firstOrNull { it.artist.name == "Bob" })
            assertEquals(1, db.albumDao().getAllAlbumsFlow().first().single { it.album.albumId == first }.songCount)

            val credits = db.artistDao().getSongCredits(2)
            assertEquals(listOf("Alice", "Bob"), credits.map { it.artist.name })
        }

    @Test
    fun playlistCoverPicksOneSongPerAlbumLatestFirst() =
        runBlocking {
            val db = newDatabase()
            LibraryWriter(db).commit(
                listOf(
                    song(1, artists = listOf("A"), album = "X", dir = "x"),
                    song(2, artists = listOf("A"), album = "X", dir = "x"),
                    song(3, artists = listOf("B"), album = "Y", dir = "y"),
                    song(4, artists = listOf("C")),
                ),
                policy = off,
                reindex = true,
            )
            val playlistId = db.playlistDao().insertPlaylistAndGetId(PlaylistEntity(playlistName = "p"))
            db.playlistDao().insertListItems(
                listOf(1L to 100L, 2L to 400L, 3L to 300L, 4L to 200L).map { (id, order) ->
                    PlaylistSongCrossRefEntity(playlistId, id, sortOrder = order)
                },
            )
            assertEquals(listOf(2L, 3L, 4L), db.playlistDao().getCoverMediaStoreIds(playlistId).first())
        }

    @Test
    fun countOfSongsNotInPlaylistMatchesMediaStoreIds() =
        runBlocking {
            val db = newDatabase()
            // songId 与 mediaStoreId 取值不同，按 songId 比较会算错
            LibraryWriter(db).commit((101L..104L).map { song(it) }, policy = off, reindex = true)
            val playlistId = db.playlistDao().insertPlaylistAndGetId(PlaylistEntity(playlistName = "p"))
            db.playlistDao().insertListItems(listOf(101L, 103L).map { PlaylistSongCrossRefEntity(playlistId, it) })
            assertEquals(2, db.songDao().countSongsNotInPlaylist(playlistId).first())
            assertEquals(listOf(102L, 104L), db.songDao().getSongIdsNotInPlaylist(playlistId).sorted())
        }

    // ===== 标签读取 =====

    @Test
    fun tagExtractorReadsPropertiesWrittenByTagLib() {
        val file = File(context.cacheDir, "${UUID.randomUUID()}.wav").also(files::add)
        file.writeBytes(wav())
        val written =
            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_WRITE).use { pfd ->
                TagLib.savePropertyMap(
                    pfd.detachFd(),
                    hashMapOf(
                        "TITLE" to arrayOf("测试标题"),
                        "ARTIST" to arrayOf("甲", "乙"),
                        "ALBUM" to arrayOf("测试专辑 (Disc 2)"),
                        "TRACKNUMBER" to arrayOf("3/9"),
                    ),
                )
            }
        assertTrue(written)
        val extracted = TagExtractor(context.contentResolver).extract(Uri.fromFile(file))
        val properties = requireNotNull(extracted.properties)
        val row =
            SongRowBuilder.build(
                MediaFileRow(1, file.path, file.name, file.length(), "audio/wav", 1, 0, null, null, 0),
                extracted,
                existing = null,
                text = AndroidIndexText.instance,
            )
        assertEquals("测试标题", row.displayName)
        assertEquals("测试专辑 (Disc 2)", row.album)
        assertEquals(3, row.trackNumber)
        assertEquals(2, row.discNumber)
        assertEquals(TagStatus.TAGLIB, row.tagStatus)
        assertTrue(requireNotNull(extracted.audio).durationMs >= 0)
        // WAV 的标签容器是否保留多值取决于 TagLib；无论哪种，名单都不能丢人
        val artists = TagJson.decode(row.artistTags)
        assertTrue("ARTIST=${properties["ARTIST"]}", artists == listOf("甲", "乙") || artists.single().let { "甲" in it && "乙" in it })
    }

    // ===== 工具 =====

    private fun newDatabase(): AppDatabase = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build().also(databases::add)

    private fun song(
        mediaStoreId: Long,
        title: String = "t$mediaStoreId",
        artists: List<String> = emptyList(),
        album: String = "",
        albumArtists: List<String> = emptyList(),
        composers: List<String> = emptyList(),
        dir: String? = null,
        year: Int = 0,
        compilation: Boolean = false,
    ) = SongEntity(
        mediaStoreId = mediaStoreId,
        path = if (dir == null) "/storage/emulated/0/Download/$mediaStoreId.mp3" else "/storage/emulated/0/Music/$dir/$mediaStoreId.mp3",
        size = 1,
        mimeType = "audio/mpeg",
        dateModified = 1,
        displayName = title,
        artist = artists.joinToString(" / "),
        album = album,
        artistTags = TagJson.encode(artists),
        albumArtistTags = TagJson.encode(albumArtists),
        composerTags = TagJson.encode(composers),
        year = year,
        isCompilation = compilation,
        duration = 1000,
        codec = "MP3",
        tagStatus = TagStatus.TAGLIB,
        tagVersion = SongRowBuilder.TAG_VERSION,
        sortName = "T",
    )

    private suspend fun relationSnapshot(db: AppDatabase) =
        listOf(
            db.libraryDao().artists().sortedBy { it.artistId },
            db.libraryDao().albums().sortedBy { it.albumId },
            db.libraryDao().songArtists().sortedWith(compareBy({ it.songId }, { it.role }, { it.artistId })),
            db.libraryDao().albumArtists().sortedWith(compareBy({ it.albumId }, { it.artistId })),
            db.songDao().getAllScanInfo().map { it.mediaStoreId to it.albumId }.sortedBy { it.first },
        )

    private fun count(
        db: AppDatabase,
        table: String,
    ): Int = rows(db, "SELECT COUNT(*) FROM $table") { it.getInt(0) }.single()

    private fun <T> rows(
        db: AppDatabase,
        sql: String,
        read: (android.database.Cursor) -> T,
    ): List<T> =
        db.openHelper.readableDatabase.query(SimpleSQLiteQuery(sql)).use { cursor ->
            buildList { while (cursor.moveToNext()) add(read(cursor)) }
        }

    /** 按 22.json 建出 v22 数据库并写入样本数据，再由 Room 执行迁移并校验 v23 结构 */
    private fun migratedFromV22(): AppDatabase {
        val name = "v22-${UUID.randomUUID()}.db".also(names::add)
        val schema =
            InstrumentationRegistry
                .getInstrumentation()
                .context.assets
                .open("me.spica27.spicamusic.storage.impl.db.AppDatabase/22.json")
                .bufferedReader()
                .use { JSONObject(it.readText()).getJSONObject("database") }
        val path = context.getDatabasePath(name).apply { parentFile?.mkdirs() }
        SQLiteDatabase.openOrCreateDatabase(path, null).use { raw ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                val table = entity.getString("tableName")
                raw.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                val indices = entity.optJSONArray("indices") ?: continue
                for (j in 0 until indices.length()) {
                    raw.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) raw.execSQL(setup.getString(i))

            listOf(Triple(1, 101, 1), Triple(2, 102, 0), Triple(3, 103, 1), Triple(4, 104, 0), Triple(5, 105, 1)).forEach { (id, media, liked) ->
                raw.execSQL(
                    "INSERT INTO Song (songId, mediaStoreId, path, displayName, artist, size, `like`, duration, sort, sortName, mimeType, albumId, album, sampleRate, bitRate, channels, digit, isIgnore, dateModified, codec, waveformData, trackNumber) " +
                        "VALUES ($id, $media, '/m/$id.mp3', 't', 'a', 1, $liked, 1000, 0, 'T', 'audio/mpeg', 9, 'al', 44100, 320000, 2, 16, 0, 1, 'MP3', 'wave', 1)",
                )
            }
            raw.execSQL("INSERT INTO AlbumEntity (id, title, artist, artworkUri, year, numberOfSongs, dateModified) VALUES ('9', 'al', 'a', NULL, 0, 5, 0)")
            raw.execSQL("INSERT INTO Playlist (playlistId, playlistName, cover, createTimestamp, playTimes, needUpdate) VALUES (1, 'P1', NULL, 0, 0, 0)")
            raw.execSQL("INSERT INTO Playlist (playlistId, playlistName, cover, createTimestamp, playTimes, needUpdate) VALUES (2, 'P2', NULL, 0, 0, 0)")
            listOf(101 to 300, 105 to 200, 103 to 100).forEach { (media, order) ->
                raw.execSQL("INSERT INTO PlaylistSongCrossRef (playlistId, mediaId, insertTime, sortOrder) VALUES (1, $media, 0, $order)")
            }
            raw.execSQL("INSERT INTO PlaylistSongCrossRef (playlistId, mediaId, insertTime, sortOrder) VALUES (2, 102, 0, 100)")
            raw.execSQL("INSERT INTO extra_info (mediaId, lyrics, cover, delay, lyricSourceName, sourceType, isManual, sourceUri) VALUES (102, '[00:01]x', '', 0, '', 'ONLINE', 1, '')")
            raw.execSQL("INSERT INTO PlayHistory (mediaId, title, artist, album, duration, playedDuration, position, actionType, contextType, contextId, sessionId, deviceId, source, isCompleted, extra, time) VALUES (103, 't', 'a', 'al', 0, 0, 0, 0, '', 0, '', '', '', 0, '', 1)")
            raw.execSQL("INSERT INTO SongPlayStat (mediaId, playCount, totalPlayedDuration, completedCount, lastPlayedTime, firstPlayedTime) VALUES (103, 7, 0, 0, 0, 0)")
            raw.execSQL("INSERT INTO TransferIdentity (kind, stableId, localId, sourcesJson) VALUES ('track', 'tA', 1, '[]')")
            raw.execSQL("INSERT INTO TransferIdentity (kind, stableId, localId, sourcesJson) VALUES ('imported_track', 'tB', 4, '[]')")
            raw.execSQL("INSERT INTO TransferIdentity (kind, stableId, localId, sourcesJson) VALUES ('playlist', 'pA', 1, '[]')")
            raw.version = 22
        }
        val db =
            Room
                .databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_22_23)
                .build()
                .also(databases::add)
        // 打开即执行迁移；结构与 v23 Entity 不一致时 Room 在这里抛异常
        db.openHelper.writableDatabase
        return db
    }

    /** 0.1 秒 8kHz 单声道静音 WAV */
    private fun wav(): ByteArray {
        val samples = 800
        val data = samples * 2
        return ByteBuffer
            .allocate(44 + data)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray())
                putInt(36 + data)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(8000)
                putInt(16000)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(data)
            }.array()
    }
}
