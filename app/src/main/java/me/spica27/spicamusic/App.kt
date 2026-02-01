package me.spica27.spicamusic

import android.app.Application
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import me.spcia.lyric_core.di.extraInfoModule
import me.spica27.spicamusic.di.AppModule
import me.spica27.spicamusic.player.impl.SpicaPlayer
import me.spica27.spicamusic.service.PlaybackService
import me.spica27.spicamusic.storage.impl.di.storageModule
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

/**
 * 应用程序类
 * 负责初始化 Koin 依赖注入、ImageLoader 和其他全局配置
 */
class App :
    Application(),
    SingletonImageLoader.Factory {
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

    /**
     * 创建自定义 ImageLoader 配置
     * 优化图片缓存策略，提高加载性能
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    // 磁盘缓存 50MB
                    .directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }.crossfade(true) // 启用淡入淡出效果
            .build()

    companion object {
        private lateinit var instance: App

        fun getInstance(): App = instance
    }
}
