package me.spica27.spicamusic

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.serviceLoaderEnabled
import coil3.util.DebugLogger
import coil3.util.Logger
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class App : Application() {
  override fun onCreate() {
    super.onCreate()
    instance = this
    Timber.plant(Timber.DebugTree())
    SingletonImageLoader.setSafe(factory = {
      ImageLoader.Builder(this)
        .crossfade(true)
        .serviceLoaderEnabled(true)
        .logger(DebugLogger(Logger.Level.Error))
        .components {

        }
        .build()
    })
  }

  companion object {
    private lateinit var instance: App

    fun getInstance(): App {
      return instance
    }
  }



}