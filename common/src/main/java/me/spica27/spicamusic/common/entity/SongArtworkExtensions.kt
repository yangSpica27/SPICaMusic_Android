package me.spica27.spicamusic.common.entity

import android.net.Uri
import androidx.core.net.toUri

/**
 * 歌曲封面：优先按歌曲本体取封面（audio/media/{id}/albumart 直接读取该文件的内嵌图），
 * 规避 MediaStore 专辑分组不可靠导致的串图 —— 尤其是额外目录扫描注册进 MediaStore 的歌曲：
 * 扁平下载目录里同名/空专辑会被并进同一 albumId，albumart/{albumId} 会显示成别的歌的封面。
 * 没有 mediaStoreId 时回退按专辑取图；两者都无效返回 null（UI 显示占位图）。
 */
fun Song.getCoverUri(): Uri? =
    when {
        mediaStoreId > 0 -> "content://media/external/audio/media/$mediaStoreId/albumart".toUri()
        albumId > 0 -> "content://media/external/audio/albumart/$albumId".toUri()
        else -> null
    }

/** 歌曲所属 MediaStore 专辑的封面：本体无内嵌封面时的兜底图源 */
fun Song.getAlbumCoverUri(): Uri? =
    if (albumId > 0) "content://media/external/audio/albumart/$albumId".toUri() else null

fun Album.getCoverUri(): Uri? =
    id.toLongOrNull()?.takeIf { it > 0 }?.let { "content://media/external/audio/albumart/$it".toUri() }

fun Artist.getCoverUri(): Uri? =
    if (coverAlbumId > 0) "content://media/external/audio/albumart/$coverAlbumId".toUri() else null
