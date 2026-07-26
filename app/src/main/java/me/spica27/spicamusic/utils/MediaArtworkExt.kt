package me.spica27.spicamusic.utils

import android.net.Uri
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata

/**
 * 从 MediaMetadata extras 里的 albumId 推导专辑封面 URI。
 * artworkUri 是按歌曲的（只含该文件内嵌图），本体无内嵌封面时用专辑图兜底，
 * 与列表侧 AudioCover 的 fallbackUri 链保持一致。
 */
fun MediaMetadata.albumCoverFallbackUri(): Uri? {
    val albumId = extras?.getLong("albumId", 0L) ?: 0L
    return if (albumId > 0) {
        "content://media/external/audio/albumart/$albumId".toUri()
    } else {
        null
    }
}
