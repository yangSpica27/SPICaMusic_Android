package me.spica27.spicamusic.player.impl.utils

import android.net.Uri
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import me.spica27.spicamusic.common.entity.Song

/**
 * Song 实体的扩展方法 - Player 模块专用
 */

fun Song.getCoverUri(): Uri = "content://media/external/audio/albumart/$albumId".toUri()

fun Song.getSongUri(): Uri = "content://media/external/audio/media/$mediaStoreId".toUri()

fun Song.toMediaItem(): MediaItem =
  MediaItem.Builder().setMediaId(mediaStoreId.toString()).setUri(getSongUri()).setMimeType(mimeType)
    .setRequestMetadata(
      MediaItem.RequestMetadata.Builder()
        .setMediaUri(getSongUri()).build(),
    ).setMediaMetadata(
      MediaMetadata.Builder().setTitle(displayName).setDisplayTitle(displayName).setArtist(artist)
        .setSubtitle(artist)
        .setExtras(Bundle().apply {
          putLong("mediaStoreId", mediaStoreId)
          putLong("albumId", albumId)
          putInt("sampleRate", sampleRate)
          putInt("bitRate", bitRate)
          putInt("channels", channels)
          putInt("digit", digit)
        })
        .setDurationMs(duration).setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
        .setIsPlayable(true).setIsBrowsable(true).setArtworkUri(getCoverUri()).build(),
    )
    .setMimeType(mimeType)
    .build()
