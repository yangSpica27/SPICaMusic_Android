package me.spica27.spicamusic.media.utils

import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import me.spica27.spicamusic.db.dao.SongDao
import org.koin.java.KoinJavaComponent.getKoin

object MediaLibrary {
    const val ROOT = "root"
    const val ALL_SONGS = "all_songs"

    private val songDao = getKoin().get<SongDao>()

    private val songFlow =
        songDao
            .getAll()
            .flowOn(Dispatchers.IO)

    @WorkerThread
    fun getItem(mediaId: String): MediaItem? = songDao.getSongWithMediaStoreId(mediaId.toLongOrNull() ?: -1)?.toMediaItem()

    fun mediaIdToMediaItems(mediaIds: List<String>): List<MediaItem> =
        mediaIds.mapNotNull {
            songDao.getSongWithMediaStoreId(it.toLongOrNull() ?: -1)?.toMediaItem()
        }

    fun getChildren(parentId: String): List<MediaItem> {
        if (parentId == ALL_SONGS) {
            return songDao.getAllSync().map { it.toMediaItem() }
        }
        return emptyList()
    }
}
