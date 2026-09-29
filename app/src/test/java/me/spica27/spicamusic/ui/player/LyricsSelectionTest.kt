package me.spica27.spicamusic.ui.player

import androidx.lifecycle.ViewModelStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import me.spcia.lyric_core.entity.SongLyrics
import me.spica27.spicamusic.common.entity.LyricSource
import me.spica27.spicamusic.feature.lyrics.domain.LyricsUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.LocalLyricFile
import me.spica27.spicamusic.storage.api.StoredLyrics
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class LyricsSelectionTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val media = MutableStateFlow<MediaItem?>(null)
    private val repository = MemoryLyrics()
    private var readEmbedded: suspend (Long) -> String? = { null }
    private var readLocal: suspend () -> LocalLyricFile? = { LocalLyricFile("[00:01.00]imported", "song.lrc") }
    private var search: suspend (String) -> List<SongLyrics> = { emptyList() }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    private fun model(): LyricsViewModel {
        val player =
            Proxy.newProxyInstance(IMusicPlayer::class.java.classLoader, arrayOf(IMusicPlayer::class.java)) { _, method, _ ->
                when (method.name) {
                    "getCurrentMediaItem" -> media
                    "getCurrentPosition" -> 0L
                    else -> error("Unexpected player call: ${method.name}")
                }
            } as IMusicPlayer
        val reader =
            object : ILyricSourceReader {
                override suspend fun readEmbedded(mediaStoreId: Long) = readEmbedded.invoke(mediaStoreId)

                override suspend fun readLocalFile(uri: String) = readLocal()
            }
        return LyricsViewModel(
            PlayerUseCases(player),
            LyricsUseCases(repository, reader) { search(it) },
            dispatcher,
            dispatcher,
        ).also { store.put("lyrics", it) }
    }

    private fun TestScope.play(id: Long = 1) {
        media.value =
            MediaItem
                .Builder()
                .setMediaId(id.toString())
                .setMediaMetadata(MediaMetadata.Builder().setTitle("song $id").build())
                .build()
        runCurrent()
    }

    @Test
    fun suppressionSurvivesRecreationAndRetainsLocalSnapshotAndOffset() =
        runTest(dispatcher) {
            repository.rows[1] = cached().copy(sourceType = "LOCAL_FILE", sourceUri = "content://lyrics", delay = -275)
            val first = model()
            play()
            first.selectNoMatchingLyrics()
            assertNull(first.uiState.value.displayed)
            runCurrent()
            assertEquals("previous", repository.rows[1]!!.lyrics)
            readEmbedded = { error("Suppressed loading must skip embedded lyrics") }
            search = { error("Suppressed loading must skip online search") }
            val recreated = model()
            runCurrent()
            assertTrue(recreated.uiState.value.lyricsSuppressed)
            assertFalse(recreated.uiState.value.isLoading)
            assertEquals(-275, recreated.uiState.value.lyricsOffsetMs)
            assertEquals(
                "previous",
                recreated.uiState.value.localSource!!
                    .rawLyrics,
            )
            recreated.updateOffset(999)
            runCurrent()
            assertEquals(-275, repository.rows[1]!!.delay)
            recreated.selectSource(recreated.uiState.value.localSource!!)
            runCurrent()
            assertFalse(recreated.uiState.value.lyricsSuppressed)
            assertFalse(repository.rows[1]!!.lyricsSuppressed)
            assertNotNull(recreated.uiState.value.displayed)
        }

    @Test
    fun lateNetworkResultCannotUndoSuppression() =
        runTest(dispatcher) {
            val response = CompletableDeferred<List<SongLyrics>>()
            search = { withContext(NonCancellable) { response.await() } }
            val vm = model()
            play()
            assertTrue(vm.uiState.value.isLoading)
            vm.selectNoMatchingLyrics()
            runCurrent()
            response.complete(listOf(SongLyrics(1, "wrong", "artist", "", "", 0, "wrong lyrics")))
            runCurrent()
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertEquals("", repository.rows[1]!!.lyrics)
            assertTrue(vm.uiState.value.lyricsSuppressed)
            assertNull(vm.uiState.value.displayed)
            assertNull(vm.uiState.value.errorMessage)
        }

    @Test
    fun cancelledLocalReadDoesNotSaveAfterSuppression() =
        runTest(dispatcher) {
            repository.rows[1] = cached()
            val response = CompletableDeferred<LocalLyricFile>()
            readLocal = { withContext(NonCancellable) { response.await() } }
            val vm = model()
            play()
            vm.importLocalFile("content://slow")
            runCurrent()
            vm.selectNoMatchingLyrics()
            runCurrent()
            response.complete(LocalLyricFile("[00:01.00]late import", "song.lrc"))
            runCurrent()
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertEquals("previous", repository.rows[1]!!.lyrics)
            assertNull(vm.uiState.value.displayed)
        }

    @Test
    fun alreadyRunningWriteFinishesBeforeTheNewSuppressionIsPersisted() =
        runTest(dispatcher) {
            repository.rows[1] = cached()
            val release = CompletableDeferred<Unit>()
            repository.beforeSave = { withContext(NonCancellable) { release.await() } }
            val vm = model()
            play()
            vm.selectSource(LyricSource.Embedded("replacement"))
            runCurrent()
            vm.selectNoMatchingLyrics()
            runCurrent()
            release.complete(Unit)
            runCurrent()
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertTrue(vm.uiState.value.lyricsSuppressed)
            assertNull(vm.uiState.value.displayed)
        }

    @Test
    fun switchingSongsKeepsTheChoiceOnTheOriginalSong() =
        runTest(dispatcher) {
            repository.rows[1] = cached()
            repository.rows[2] = cached().copy(mediaId = 2, lyrics = "second song")
            val vm = model()
            play()
            vm.selectNoMatchingLyrics()
            play(2)
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertFalse(repository.rows[2]!!.lyricsSuppressed)
            assertEquals("second song", vm.uiState.value.displayedRawText)
            play(1)
            assertTrue(vm.uiState.value.lyricsSuppressed)
        }

    @Test
    fun failedOrInvalidReplacementKeepsSuppressionAndSuccessfulImportUnlocksIt() =
        runTest(dispatcher) {
            repository.rows[1] = cached().copy(lyricsSuppressed = true)
            val vm = model()
            play()
            vm.selectSource(LyricSource.Embedded(" "))
            runCurrent()
            assertTrue(vm.uiState.value.lyricsSuppressed)
            assertNotNull(vm.uiState.value.errorMessage)
            repository.beforeSave = { error("Disk full") }
            vm.selectSource(LyricSource.Embedded("new"))
            runCurrent()
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertTrue(vm.uiState.value.lyricsSuppressed)
            repository.beforeSave = {}
            vm.importLocalFile("content://valid")
            runCurrent()
            assertFalse(vm.uiState.value.lyricsSuppressed)
            assertFalse(repository.rows[1]!!.lyricsSuppressed)
            assertEquals("[00:01.00]imported", vm.uiState.value.displayedRawText)
        }

    @Test
    fun suppressionSaveFailureIsVisibleWithoutRedisplayingWrongLyrics() =
        runTest(dispatcher) {
            repository.rows[1] = cached()
            repository.failSuppression = true
            val vm = model()
            play()
            vm.selectNoMatchingLyrics()
            runCurrent()
            assertNull(vm.uiState.value.displayed)
            assertNotNull(vm.uiState.value.errorMessage)
            assertFalse(repository.rows[1]!!.lyricsSuppressed)
        }

    @Test
    fun invalidReplacementDoesNotCancelAPendingSuppressionSave() =
        runTest(dispatcher) {
            repository.rows[1] = cached()
            val vm = model()
            play()
            vm.selectNoMatchingLyrics()
            vm.selectSource(LyricSource.Embedded(" "))
            runCurrent()
            assertTrue(repository.rows[1]!!.lyricsSuppressed)
            assertTrue(vm.uiState.value.lyricsSuppressed)
            assertNull(vm.uiState.value.displayed)
        }

    @Test
    fun panelSearchKeepsSuppressionAndCachedOnlineLyricsCanBeReselectedOffline() =
        runTest(dispatcher) {
            repository.rows[1] = cached().copy(lyricsSuppressed = true)
            val vm = model()
            play()
            val saved = vm.uiState.value.cachedOnlineSource!!
            assertEquals("previous", saved.rawLyrics)
            val response = CompletableDeferred<List<SongLyrics>>()
            search = { response.await() }
            vm.openPanel()
            runCurrent()
            assertTrue(vm.uiState.value.onlineLoading)
            assertTrue(vm.uiState.value.lyricsSuppressed)
            vm.selectSource(saved)
            runCurrent()
            assertFalse(repository.rows[1]!!.lyricsSuppressed)
            assertEquals("previous", vm.uiState.value.displayedRawText)
            response.complete(listOf(SongLyrics(1, "other", "artist", "", "", 0, "other lyrics")))
            runCurrent()
            assertEquals("previous", vm.uiState.value.displayedRawText)
        }

    private fun cached() = StoredLyrics(1, "previous", "", 0, "saved", "ONLINE", true, "")

    private class MemoryLyrics : ILyricRepository {
        val rows = mutableMapOf<Long, StoredLyrics>()
        var beforeSave: suspend () -> Unit = {}
        var failSuppression = false

        override suspend fun getLyrics(mediaId: Long) = rows[mediaId]

        override suspend fun updateDelay(
            mediaId: Long,
            delay: Long,
        ) {
            rows[mediaId]?.let { rows[mediaId] = it.copy(delay = delay) }
        }

        override suspend fun suppressLyrics(mediaId: Long) {
            if (failSuppression) error("Disk full")
            val row = rows[mediaId] ?: StoredLyrics(mediaId, "", "", 0, "", "ONLINE", false, "")
            rows[mediaId] = row.copy(lyricsSuppressed = true)
        }

        override suspend fun saveLyrics(
            mediaId: Long,
            lyrics: String,
            cover: String,
            sourceName: String,
            delay: Long,
            sourceType: String,
            isManual: Boolean,
            sourceUri: String,
        ) {
            beforeSave()
            rows[mediaId] = StoredLyrics(mediaId, lyrics, cover, delay, sourceName, sourceType, isManual, sourceUri)
        }
    }
}
