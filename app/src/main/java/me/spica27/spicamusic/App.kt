package me.spica27.spicamusic

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import me.spcia.lyric_core.di.extraInfoModule
import me.spica27.spicamusic.di.AppModule
import me.spica27.spicamusic.player.impl.SpicaPlayer
import me.spica27.spicamusic.service.PlaybackService
import me.spica27.spicamusic.storage.impl.di.storageModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

/**
 * 应用程序类
 * 负责初始化 Koin 依赖注入和其他全局配置
 */
class App : Application() {
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 初始化 Koin 依赖注入
        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                storageModule, // 存储模块 (storage-core)
                SpicaPlayer.createModule(PlaybackService::class.java), // 播放器模块 (player-core)
                AppModule.appModule, // 应用模块
                extraInfoModule,
            )
        }
    }

    companion object {
        private lateinit var instance: App

        fun getInstance(): App = instance
    }
}
