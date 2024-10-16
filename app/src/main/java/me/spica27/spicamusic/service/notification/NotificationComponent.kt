package me.spica27.spicamusic.service.notification

import android.annotation.SuppressLint
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import me.spica27.spicamusic.R
import me.spica27.spicamusic.playback.RepeatMode
import me.spica27.spicamusic.service.ForegroundServiceNotification
import me.spica27.spicamusic.service.MusicService
import me.spica27.spicamusic.utils.newBroadcastPendingIntent
import me.spica27.spicamusic.utils.newMainPendingIntent

@SuppressLint("RestrictedApi")
class NotificationComponent(private val context: Context, sessionToken: MediaSessionCompat.Token) :
  ForegroundServiceNotification(context, CHANNEL_INFO) {


  override val code: Int
    get() = 0x102030

  init {
    setSmallIcon(R.drawable.ic_app)
    setCategory(NotificationCompat.CATEGORY_SERVICE)
    setShowWhen(false)
    setSilent(true)
    setContentIntent(context.newMainPendingIntent())
    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

    addAction(buildAction(context, MusicService.ACTION_SKIP_PREV, R.drawable.ic_pre))
    addAction(buildPlayPauseAction(context, true))
    addAction(buildAction(context, MusicService.ACTION_SKIP_NEXT, R.drawable.ic_next))

    setStyle(
      androidx.media.app
        .NotificationCompat
        .MediaStyle()
        .setMediaSession(sessionToken)
        .setShowActionsInCompactView(0, 1, 2)
    )
  }


  fun updateMetadata(metadata: MediaMetadataCompat) {
    setLargeIcon(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART))
    setContentTitle(metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
    setContentText(metadata.getText(MediaMetadataCompat.METADATA_KEY_ARTIST))
    setSubText(metadata.getText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
  }

  private fun buildPlayPauseAction(
    context: Context,
    isPlaying: Boolean
  ): NotificationCompat.Action {
    val drawableRes =
      if (isPlaying) {
        R.drawable.ic_pause
      } else {
        R.drawable.ic_play
      }
    return buildAction(context, MusicService.ACTION_PLAY_PAUSE, drawableRes)
  }

  private fun buildRepeatAction(
    context: Context,
    repeatMode: RepeatMode
  ): NotificationCompat.Action {
    return buildAction(context, MusicService.ACTION_INC_REPEAT_MODE, repeatMode.icon)
  }

  private fun buildAction(context: Context, actionName: String, @DrawableRes iconRes: Int) =
    NotificationCompat.Action.Builder(
      iconRes, actionName, context.newBroadcastPendingIntent(actionName)
    )
      .build()


  fun updatePlaying(isPlaying: Boolean) {
    mActions[1] = buildPlayPauseAction(context, isPlaying)
  }


  private companion object {
    val CHANNEL_INFO =
      ChannelInfo(
        id = "me.spica27.spicamusic" + ".channel.PLAYBACK",
        nameRes = R.string.playing
      )
  }
}