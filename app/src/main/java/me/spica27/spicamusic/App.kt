package me.spica27.spicamusic

import android.app.Application
import com.lzx.starrysky.StarrySkyInstall
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Timber.plant(Timber.DebugTree())
    StarrySkyInstall.init(this)
      .setAutoManagerFocus(true)
      .startForegroundByWorkManager(true)
      .setNotificationSwitch(true)
      .apply()
  }

}