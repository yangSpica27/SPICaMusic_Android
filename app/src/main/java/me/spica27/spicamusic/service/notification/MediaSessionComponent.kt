package me.spica27.spicamusic.service.notification

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.collection.LruCache
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import coil.target.Target
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.playback.PlaybackStateManager
import me.spica27.spicamusic.player.Queue
import me.spica27.spicamusic.service.MusicService

class MediaSessionComponent(
  private val context: Context,
  private val listener: Listener
) :
  MediaSessionCompat.Callback(),
  PlaybackStateManager.Listener {

  private val coverCache: LruCache<String, Bitmap> by lazy {
    object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
      override fun sizeOf(key: String, value: Bitmap): Int {
        return value.allocationByteCount
      }
    }
  }

  private val mediaSession =
    MediaSessionCompat(context, context.packageName).apply {
      isActive = true
      setQueueTitle(context.getString(R.string.queue))
    }

  private val playbackManager = PlaybackStateManager.getInstance()

  private val notification = NotificationComponent(context, mediaSession.sessionToken)


  init {
    playbackManager.addListener(this)
    mediaSession.setCallback(this)
  }

  fun handleMediaButtonIntent(intent: Intent) {
    MediaButtonReceiver.handleIntent(mediaSession, intent)
  }

  interface Listener {
    fun onPostNotification(notification: NotificationComponent)
  }

  fun release() {
    playbackManager.removeListener(this)
    mediaSession.apply {
      isActive = false
      release()
    }
  }

  override fun onIndexMoved(queue: Queue) {
    updateMediaMetadata(queue.currentSong())
  }

  override fun onNewListLoad(queue: Queue) {
    super.onNewListLoad(queue)
    updateMediaMetadata(queue.currentSong())
  }

  override fun onStateChanged(isPlaying: Boolean) {
    super.onStateChanged(isPlaying)
    notification.updatePlaying(playbackManager.playerState.isPlaying)
    listener.onPostNotification(notification)
    updatePlaybackState()
  }

  override fun onPlay() {
    playbackManager.setPlaying(true)
    notification.updatePlaying(true)
    listener.onPostNotification(notification)
  }

  override fun onPause() {
    playbackManager.setPlaying(false)
    notification.updatePlaying(false)
    listener.onPostNotification(notification)
  }


  override fun onSkipToNext() {
    playbackManager.playNext()
  }

  override fun onSkipToPrevious() {
    playbackManager.playPre()
  }

  override fun onSeekTo(position: Long) {
    playbackManager.seekTo(position)
    updatePlaybackState()
  }

  override fun onCustomAction(action: String?, extras: Bundle?) {
    super.onCustomAction(action, extras)
    context.sendBroadcast(Intent(action))
  }


  override fun onStop() {
    context.sendBroadcast(Intent(MusicService.ACTION_EXIT))
  }


  private fun updateMediaMetadata(song: Song?) {
    if (song == null) {
      mediaSession.setMetadata(emptyMetadata)
      return
    }
    val title = song.displayName
    val artist = song.artist
    val builder =
      MediaMetadataCompat.Builder()
        .putText(MediaMetadataCompat.METADATA_KEY_TITLE, title)
        .putText(MediaMetadataCompat.METADATA_KEY_ALBUM, song.displayName)
        .putText(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
        .putText(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST, song.artist)
        .putText(MediaMetadataCompat.METADATA_KEY_AUTHOR, artist)
        .putText(MediaMetadataCompat.METADATA_KEY_COMPOSER, artist)
        .putText(MediaMetadataCompat.METADATA_KEY_WRITER, artist)
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
        .putText(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.duration)
        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, song.mediaStoreId.toString())

    coverCache["${song.songId}-${song.mediaStoreId}"].let { cache ->
      if (cache != null) {
        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, cache)
      } else {
        val reqBitmap = ImageRequest.Builder(context)
          .data(song.getCoverUri())
          .size(Size.ORIGINAL)
          .target(object : Target {
            override fun onError(error: Drawable?) {
              super.onError(error)
              val coverBitmap =
                ContextCompat.getDrawable(context, R.mipmap.default_cover)!!.toBitmap()
              coverCache.put("${song.songId}-${song.mediaStoreId}", coverBitmap)
              builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, coverBitmap)
              val metadata = builder.build()
              mediaSession.setMetadata(metadata)
            }

            override fun onSuccess(result: Drawable) {
              super.onSuccess(result)
              val coverBitmap = result.toBitmap()
              coverCache.put("${song.songId}-${song.mediaStoreId}", coverBitmap)
              builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, coverBitmap)
              val metadata = builder.build()
              mediaSession.setMetadata(metadata)
            }
          }).build()

        ImageLoader(context).enqueue(reqBitmap)
      }
    }


    val metadata = builder.build()
    mediaSession.setMetadata(metadata)
    notification.updateMetadata(metadata)
    listener.onPostNotification(notification)
  }

  private fun updatePlaybackState() {
    val builder = PlaybackStateCompat.Builder()
      .setActions(ACTIONS)
      .setState(
        if (playbackManager.playerState.isPlaying) PlaybackStateCompat.STATE_PLAYING
        else PlaybackStateCompat.STATE_PAUSED,
        playbackManager.playerState.currentPositionMs,
        1f
      )
    mediaSession.setPlaybackState(builder.build())
  }


  companion object {
    const val METADATA_KEY_PARENT = "me.spica27.spicamusic" + ".metadata.PARENT"
    private val emptyMetadata = MediaMetadataCompat.Builder().build()
    private const val ACTIONS =
      PlaybackStateCompat.ACTION_PLAY or
          PlaybackStateCompat.ACTION_PAUSE or
          PlaybackStateCompat.ACTION_PLAY_PAUSE or
          PlaybackStateCompat.ACTION_SET_REPEAT_MODE or
          PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE or
          PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
          PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
          PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or
          PlaybackStateCompat.ACTION_SEEK_TO or
          PlaybackStateCompat.ACTION_STOP
  }
}