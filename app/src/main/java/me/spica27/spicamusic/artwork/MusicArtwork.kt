package me.spica27.spicamusic.artwork

import androidx.compose.runtime.Immutable
import androidx.media3.common.MediaMetadata
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song

/**
 * 封面请求模型，由 [MusicArtworkFetcher] 按「内嵌图 → 目录图 → MediaStore 缩略图」解析。
 * 只有专辑 id 时 fetcher 自行挑一首代表曲。
 */
@Immutable
data class MusicArtwork(
    val mediaStoreId: Long = 0L,
    val albumId: Long = 0L,
    val path: String = "",
    val size: Long = 0L,
) {
    val isEmpty: Boolean get() = mediaStoreId <= 0L && albumId <= 0L

    /** Landscapist 以 toString 作缓存键；path 不参与，文件改动靠 size 体现，额外目录变化靠 generation 体现 */
    override fun toString(): String = "music-artwork:${ArtworkLoader.generation}:$mediaStoreId:$albumId:$size"
}

fun Song.artwork(): MusicArtwork =
    MusicArtwork(
        mediaStoreId = mediaStoreId,
        albumId = albumId,
        path = path,
        size = size,
    )

fun Album.artwork(): MusicArtwork? = albumArtwork(id.toLongOrNull() ?: 0L)

/** 按代表曲本体取图（路径由 fetcher 补全）；旧数据没有代表曲时才退回按专辑 */
fun Artist.artwork(): MusicArtwork? =
    if (coverMediaStoreId > 0L) {
        MusicArtwork(mediaStoreId = coverMediaStoreId, albumId = coverAlbumId)
    } else {
        albumArtwork(coverAlbumId)
    }

fun albumArtwork(albumId: Long): MusicArtwork? = if (albumId > 0L) MusicArtwork(albumId = albumId) else null

/** 从 Song.toMediaItem 写入的 extras 还原 */
fun MediaMetadata.artwork(): MusicArtwork? {
    val extras = extras ?: return null
    return MusicArtwork(
        mediaStoreId = extras.getLong(EXTRA_MEDIA_STORE_ID, 0L),
        albumId = extras.getLong(EXTRA_ALBUM_ID, 0L),
        path = extras.getString(EXTRA_PATH).orEmpty(),
        size = extras.getLong(EXTRA_SIZE, 0L),
    ).takeUnless { it.isEmpty }
}

private const val EXTRA_MEDIA_STORE_ID = "mediaStoreId"
private const val EXTRA_ALBUM_ID = "albumId"
private const val EXTRA_PATH = "path"
private const val EXTRA_SIZE = "size"
