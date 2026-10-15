package me.spica27.spicamusic.artwork

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaMetadata
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song

/**
 * 封面请求模型，由 [MusicArtworkFetcher] 按「内嵌图 → 目录图 → MediaStore 缩略图」解析。
 * 专辑、歌手、歌单都按代表曲本体取图；只有 mediaStoreId 时 fetcher 自行补全路径。
 */
@Immutable
data class MusicArtwork(
    val mediaStoreId: Long = 0L,
    val path: String = "",
    val size: Long = 0L,
) {
    val isEmpty: Boolean get() = mediaStoreId <= 0L

    /** Landscapist 以 toString 作缓存键；path 不参与，文件改动靠 size 体现，额外目录变化靠 generation 体现 */
    override fun toString(): String = "music-artwork:${ArtworkLoader.generation}:$mediaStoreId:$size"
}

fun Song.artwork(): MusicArtwork =
    MusicArtwork(
        mediaStoreId = mediaStoreId,
        path = path,
        size = size,
    )

fun Album.artwork(): MusicArtwork? = songArtwork(coverMediaStoreId)

fun Artist.artwork(): MusicArtwork? = songArtwork(coverMediaStoreId)

/** 按代表曲取图（路径由 fetcher 补全） */
fun songArtwork(mediaStoreId: Long): MusicArtwork? = if (mediaStoreId > 0L) MusicArtwork(mediaStoreId = mediaStoreId) else null

/** 从 Song.toMediaItem 写入的 extras 还原 */
fun MediaMetadata.artwork(): MusicArtwork? {
    val extras = extras ?: return null
    return MusicArtwork(
        mediaStoreId = extras.getLong(EXTRA_MEDIA_STORE_ID, 0L),
        path = extras.getString(EXTRA_PATH).orEmpty(),
        size = extras.getLong(EXTRA_SIZE, 0L),
    ).takeUnless { it.isEmpty }
}

private const val EXTRA_MEDIA_STORE_ID = "mediaStoreId"
private const val EXTRA_PATH = "path"
private const val EXTRA_SIZE = "size"
