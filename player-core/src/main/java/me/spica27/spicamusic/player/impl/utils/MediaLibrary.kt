package me.spica27.spicamusic.player.impl.utils

import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.storage.api.ISongRepository
import org.koin.java.KoinJavaComponent.getKoin

object MediaLibrary {
    const val ROOT = "root"
    const val ALL_SONGS = "all_songs"

    private val songRepository = getKoin().get<ISongRepository>()

    @WorkerThread
    fun getItem(mediaId: String): MediaItem? = runBlocking {
        songRepository.getSongByMediaStoreId(mediaId.toLongOrNull() ?: -1)?.toMediaItem()
    }

    fun mediaIdToMediaItems(mediaIds: List<String>): List<MediaItem> = runBlocking {
        mediaIds.mapNotNull {
            songRepository.getSongByMediaStoreId(it.toLongOrNull() ?: -1)?.toMediaItem()
        }
    }

    fun getChildren(parentId: String): List<MediaItem> = runBlocking {
        if (parentId == ALL_SONGS) {
            return@runBlocking songRepository.getAllSongs().map { it.toMediaItem() }
        }
        emptyList()
    }
}
