package me.spica27.spicamusic.common.entity

import android.net.Uri
import androidx.core.net.toUri

/**
 * 封面一律按歌曲本体取图（audio/media/{id}/albumart 直接读取该文件的内嵌图）。
 * 专辑、歌手由应用自建，不再使用 MediaStore 的 albumart/{albumId}：
 * 系统按目录名合并的伪专辑会让那条路径显示成别的歌的封面。
 */
private fun songCoverUri(mediaStoreId: Long): Uri? =
    if (mediaStoreId > 0) "content://media/external/audio/media/$mediaStoreId/albumart".toUri() else null

fun Song.getCoverUri(): Uri? = songCoverUri(mediaStoreId)

/** 专辑封面：取代表曲的封面 */
fun Album.getCoverUri(): Uri? = songCoverUri(coverMediaStoreId)

/** 歌手封面：取代表曲的封面 */
fun Artist.getCoverUri(): Uri? = songCoverUri(coverMediaStoreId)
