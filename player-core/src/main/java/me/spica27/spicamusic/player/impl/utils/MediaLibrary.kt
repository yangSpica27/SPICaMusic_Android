package me.spica27.spicamusic.player.impl.utils

import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import me.spica27.spicamusic.storage.api.ISongRepository
import org.koin.java.KoinJavaComponent.getKoin
import timber.log.Timber

object MediaLibrary {
    private const val TAG = "MediaLibrary"
    const val ROOT = "root"
    const val ALL_SONGS = "all_songs"

    private val songRepository = getKoin().get<ISongRepository>()

    @WorkerThread
    fun getItem(mediaId: String): MediaItem? = runBlocking {
        val mediaStoreId = mediaId.toLongOrNull()
        if (mediaStoreId == null) {
            Timber.tag(TAG).e("Invalid mediaId: $mediaId (not a valid Long)")
            return@runBlocking null
        }
        
        Timber.tag(TAG).d("getItem: mediaId=$mediaId, mediaStoreId=$mediaStoreId")
        
        val song = songRepository.getSongByMediaStoreId(mediaStoreId)
        if (song == null) {
            Timber.tag(TAG).e("Song not found for mediaStoreId=$mediaStoreId")
            // 调试：检查数据库中是否有歌曲
            val allSongs = songRepository.getAllSongs()
            Timber.tag(TAG).e("Total songs in DB: ${allSongs.size}")
            if (allSongs.isNotEmpty()) {
                Timber.tag(TAG).e("Sample mediaStoreIds: ${allSongs.take(3).map { it.mediaStoreId }}")
            }
        } else {
            Timber.tag(TAG).d("✓ Found: ${song.displayName}")
        }
        
        song?.toMediaItem()
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
