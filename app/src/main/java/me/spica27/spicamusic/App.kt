package me.spica27.spicamusic

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.crash.CrashHandler
import me.spica27.spicamusic.media.SpicaPlayer
import me.spica27.spicamusic.module.InjectModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber


@OptIn(UnstableApi::class)
class App : Application() {


  override fun onCreate() {
    super.onCreate()
    instance = this
    Timber.plant(Timber.DebugTree())
    CrashHandler.init(this)
    startKoin {
      androidLogger()
      androidContext(this@App)
      modules(
        InjectModules.networkModule,
        InjectModules.persistenceModule,
        InjectModules.utilsModule,
        InjectModules.repositoryModule,
        InjectModules.viewModelModule,
        SpicaPlayer.module
      )
    }
//    SingletonImageLoader.setSafe(factory = {
//      ImageLoader.Builder(this)
//        .crossfade(true)
//        .serviceLoaderEnabled(true)
//        .logger(DebugLogger(Logger.Level.Error))
//        .components {
//
//        }
//        .build()
//    })
  }

  companion object {
    private lateinit var instance: App

    fun getInstance(): App {
      return instance
    }
  }


}