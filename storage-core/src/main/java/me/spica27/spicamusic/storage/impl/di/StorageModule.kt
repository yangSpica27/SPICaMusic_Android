package me.spica27.spicamusic.storage.impl.di

import android.app.Application
import androidx.room.Room
import me.spica27.spicamusic.storage.api.*
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.repository.*
import me.spica27.spicamusic.storage.impl.scanner.MusicScanService
import org.koin.dsl.module

/**
 * 存储模块的 Koin 依赖注入配置
 */
val storageModule = module {
    // Database
    single<AppDatabase> {
        Room.databaseBuilder(
            get<Application>(),
            AppDatabase::class.java,
            "spica_music.db",
        ).addMigrations(
            AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_16_17,
        ).fallbackToDestructiveMigration(false)
            .build()
    }

    // DAOs
    single { get<AppDatabase>().songDao() }
    single { get<AppDatabase>().playlistDao() }
    single { get<AppDatabase>().lyricDao() }
    single { get<AppDatabase>().playHistoryDao() }

    // Repositories - 通过接口暴露
    single<ISongRepository> { SongRepositoryImpl(get()) }
    single<IPlaylistRepository> { PlaylistRepositoryImpl(get()) }
    single<ILyricRepository> { LyricRepositoryImpl(get()) }
    single<IPlayHistoryRepository> { PlayHistoryRepositoryImpl(get()) }

    // 扫描服务
    single<IMusicScanService> { MusicScanService(get(), get()) }
}
