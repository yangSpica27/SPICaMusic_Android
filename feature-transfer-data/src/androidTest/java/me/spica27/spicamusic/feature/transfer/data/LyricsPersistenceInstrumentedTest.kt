package me.spica27.spicamusic.feature.transfer.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity
import me.spica27.spicamusic.storage.impl.repository.LyricRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LyricsPersistenceInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun suppressionSurvivesReopenAndAutomaticWritesCannotUnlockIt() =
        runBlocking(Dispatchers.IO) {
            val name = "lyric-suppression-${UUID.randomUUID()}.db"
            try {
                val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
                val repo = LyricRepositoryImpl(db.lyricDao())
                repo.saveLyrics(
                    1,
                    "snapshot",
                    sourceName = "local",
                    delay = -200,
                    sourceType = "LOCAL_FILE",
                    isManual = true,
                    sourceUri = "content://lyrics",
                )
                val original = repo.getLyrics(1)!!
                repo.suppressLyrics(1)
                repo.saveLyrics(1, "late automatic", sourceName = "online", delay = 0, sourceType = "ONLINE", isManual = false)
                assertEquals(original.copy(lyricsSuppressed = true), repo.getLyrics(1))
                repo.suppressLyrics(2)
                db.close()

                val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
                try {
                    val restored = LyricRepositoryImpl(reopened.lyricDao())
                    assertEquals(original.copy(lyricsSuppressed = true), restored.getLyrics(1))
                    assertTrue(restored.getLyrics(2)!!.lyricsSuppressed)
                    assertEquals("", restored.getLyrics(2)!!.lyrics)
                    restored.saveLyrics(1, "new choice", sourceName = "online", delay = -200, sourceType = "ONLINE", isManual = true)
                    assertFalse(restored.getLyrics(1)!!.lyricsSuppressed)
                    assertEquals("new choice", restored.getLyrics(1)!!.lyrics)
                } finally {
                    reopened.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }

    @Test
    fun migration21To22PreservesSnapshotAndDefaultsToEnabledLyrics() =
        runBlocking(Dispatchers.IO) {
            val name = "lyric-migration-${UUID.randomUUID()}.db"
            try {
                val original =
                    ExtraInfoEntity(
                        mediaId = 9,
                        lyrics = "saved",
                        delay = -321,
                        sourceType = "LOCAL_FILE",
                        sourceUri = "content://old",
                        isManual = true,
                        restoredSnapshot = true,
                    )
                val db = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
                db.lyricDao().insertLyric(original)
                val stored = db.lyricDao().getLyricWithMediaId(9)!!
                db.close()
                SQLiteDatabase.openDatabase(context.getDatabasePath(name).path, null, SQLiteDatabase.OPEN_READWRITE).use { raw ->
                    raw.execSQL(
                        "CREATE TABLE extra_info_v21 (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaId INTEGER NOT NULL, lyrics TEXT NOT NULL, cover TEXT NOT NULL, delay INTEGER NOT NULL, lyricSourceName TEXT NOT NULL, sourceType TEXT NOT NULL, isManual INTEGER NOT NULL, sourceUri TEXT NOT NULL, restoredSnapshot INTEGER NOT NULL DEFAULT 0)",
                    )
                    raw.execSQL(
                        "INSERT INTO extra_info_v21 SELECT id, mediaId, lyrics, cover, delay, lyricSourceName, sourceType, isManual, sourceUri, restoredSnapshot FROM extra_info",
                    )
                    raw.execSQL("DROP TABLE extra_info")
                    raw.execSQL("ALTER TABLE extra_info_v21 RENAME TO extra_info")
                    raw.execSQL("CREATE UNIQUE INDEX index_extra_info_mediaId ON extra_info(mediaId)")
                    raw.version = 21
                }
                val migrated =
                    Room
                        .databaseBuilder(context, AppDatabase::class.java, name)
                        .addMigrations(AppDatabase.MIGRATION_21_22)
                        .build()
                try {
                    assertEquals(stored, migrated.lyricDao().getLyricWithMediaId(9))
                    assertFalse(migrated.lyricDao().getLyricWithMediaId(9)!!.lyricsSuppressed)
                } finally {
                    migrated.close()
                }
            } finally {
                context.deleteDatabase(name)
            }
        }
}
