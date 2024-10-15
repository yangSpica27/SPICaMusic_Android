package me.spica27.spicamusic

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import okhttp3.internal.platform.Jdk9Platform.Companion.isAvailable
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
  override fun onCreate() {
    super.onCreate()
    Timber.plant(Timber.DebugTree())
    Timber.tag("SpicaMusic").e("FlacLibrary isAvailable: $isAvailable")
  }

}