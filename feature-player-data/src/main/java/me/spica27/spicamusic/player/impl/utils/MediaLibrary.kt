package me.spica27.spicamusic.player.impl.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.ISongRepository
import org.koin.java.KoinJavaComponent.getKoin
import timber.log.Timber

/**
 * 媒体库工具类
 * 提供歌曲 ID 到 MediaItem 的转换功能
 * 
 * 注意：所有方法都是 suspend 函数，必须在协程中调用
 */
object MediaLibrary {
    private const val TAG = "MediaLibrary"
    const val ROOT = "root"
    const val ALL_SONGS = "all_songs"

    private val songRepository = getKoin().get<ISongRepository>()

    /**
     * 构建可浏览的文件夹节点（供 Android Auto 等媒体浏览器展示目录）
     */
    private fun browsableFolder(
        mediaId: String,
        title: String,
    ): MediaItem =
        MediaItem
            .Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build(),
            ).build()

    /**
     * 根据 mediaId 获取单个 MediaItem
     */
    suspend fun getItem(mediaId: String): MediaItem? = withContext(Dispatchers.IO) {
        // 文件夹节点直接返回可浏览项
        when (mediaId) {
            ROOT -> return@withContext browsableFolder(ROOT, "SPICaMusic")
            ALL_SONGS -> return@withContext browsableFolder(ALL_SONGS, "所有歌曲")
        }

        val mediaStoreId = mediaId.toLongOrNull()
        if (mediaStoreId == null) {
            Timber.tag(TAG).e("Invalid mediaId: $mediaId (not a valid Long)")
            return@withContext null
        }

        Timber.tag(TAG).d("getItem: mediaId=$mediaId, mediaStoreId=$mediaStoreId")
        
        val song = songRepository.getSongByMediaStoreId(mediaStoreId)
        if (song == null) {
            Timber.tag(TAG).e("Song not found for mediaStoreId=$mediaStoreId")
        } else {
            Timber.tag(TAG).d("✓ Found: ${song.displayName}")
        }
        
        song?.toMediaItem()
    }

    /**
     * 批量将 mediaId 转换为 MediaItem
     * 使用批量查询优化，避免 N+1 问题
     */
    suspend fun mediaIdToMediaItems(mediaIds: List<String>): List<MediaItem> = withContext(Dispatchers.IO) {
        if (mediaIds.isEmpty()) return@withContext emptyList()
        
        val ids = mediaIds.mapNotNull { it.toLongOrNull() }
        if (ids.isEmpty()) return@withContext emptyList()
        
        // 使用批量查询而非循环单独查询
        val songs = songRepository.getSongsByMediaStoreIds(ids)
        
        // 按原始顺序返回结果
        val songMap = songs.associateBy { it.mediaStoreId }
        ids.mapNotNull { id -> songMap[id]?.toMediaItem() }
    }

    /**
     * 获取指定父节点的子项
     */
    suspend fun getChildren(parentId: String): List<MediaItem> = withContext(Dispatchers.IO) {
        when (parentId) {
            // 根节点展示可浏览的分类目录
            ROOT -> listOf(browsableFolder(ALL_SONGS, "所有歌曲"))
            ALL_SONGS -> songRepository.getAllSongs().map { it.toMediaItem() }
            else -> emptyList()
        }
    }
}
