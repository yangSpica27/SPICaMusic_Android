package me.spica27.spicamusic.service

import android.app.Service
import androidx.core.app.ServiceCompat
import timber.log.Timber

/**
 * 用来给后台服务绑定前台通知的工具类
 */
class ForegroundManager(private val service: Service) {
  private var isForeground = false


  fun release() {
    tryStopForeground()
  }

  fun tryStartForeground(notification: ForegroundServiceNotification): Boolean {
    if (isForeground) {
      return false
    }

    Timber.d("Starting foreground state")
    service.startForeground(notification.code, notification.build())
    isForeground = true
    return true
  }


  fun tryStopForeground(): Boolean {
    if (!isForeground) {
      // Nothing to do.
      return false
    }

    Timber.d("Stopping foreground state")
    ServiceCompat.stopForeground(service, ServiceCompat.STOP_FOREGROUND_REMOVE)
    isForeground = false
    return true
  }
}