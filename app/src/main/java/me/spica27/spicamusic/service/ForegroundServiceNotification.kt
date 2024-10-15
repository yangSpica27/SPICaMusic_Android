package me.spica27.spicamusic.service

import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

abstract class ForegroundServiceNotification(context: Context, info: ChannelInfo) :
  NotificationCompat.Builder(context, info.id) {
  private val notificationManager = NotificationManagerCompat.from(context)

  init {
    val channel =
      NotificationChannelCompat.Builder(info.id, NotificationManagerCompat.IMPORTANCE_LOW)
        .setName(context.getString(info.nameRes))
        .setLightsEnabled(false)
        .setVibrationEnabled(false)
        .setShowBadge(false)
        .build()
    notificationManager.createNotificationChannel(channel)
  }


  abstract val code: Int


  fun post() {
    @Suppress("MissingPermission") notificationManager.notify(code, build())
  }

  data class ChannelInfo(val id: String, @StringRes val nameRes: Int)
}