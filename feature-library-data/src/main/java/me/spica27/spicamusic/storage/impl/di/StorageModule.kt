package me.spica27.spicamusic.storage.impl.di

import android.app.Application
import androidx.room.Room
import me.spica27.spicamusic.storage.api.IAlbumRepository
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.IScanFolderRepository
import me.spica27.spicamusic.storage.api.IScanRulesRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.repository.AlbumRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.LyricRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.PlayHistoryRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.PlaylistRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.ScanFolderRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.ScanRulesRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.SongRepositoryImpl
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
            AppDatabase.MIGRATION_5_6,
            AppDatabase.MIGRATION_9_10,
            AppDatabase.MIGRATION_12_13,
            AppDatabase.MIGRATION_13_14,
            AppDatabase.MIGRATION_14_15,
            AppDatabase.MIGRATION_15_16,
            AppDatabase.MIGRATION_16_17,
            AppDatabase.MIGRATION_17_18,
            AppDatabase.MIGRATION_18_19,
        )
            // 版本链在 6→9、10→12 之间仍有缺口（那几版没留下 Migration，
            // 原始表结构已无从考证），只能保留破坏性回退兜底，
            // 否则停留在那些版本的设备升级时会直接崩溃而不是重扫。
            // 注意：Room 只在找不到迁移路径时才回退，
            // 因此 14→15→16 的正常升级路径不受影响、用户数据会被保留。
            .fallbackToDestructiveMigration(false)
            .build()
    }

    // DAOs
    single { get<AppDatabase>().songDao() }
    single { get<AppDatabase>().playlistDao() }
    single { get<AppDatabase>().lyricDao() }
    single { get<AppDatabase>().playHistoryDao() }
    single { get<AppDatabase>().albumDao() }
    single { get<AppDatabase>().scanFolderDao() }

    // Repositories - 通过接口暴露
    single<ISongRepository> { SongRepositoryImpl(get()) }
    single<IPlaylistRepository> { PlaylistRepositoryImpl(get(), get()) }
    single<IPlayHistoryRepository> { PlayHistoryRepositoryImpl(get(), get()) }
    single<IAlbumRepository> { AlbumRepositoryImpl(get()) }
    single<ILyricRepository> { LyricRepositoryImpl(get()) }
    single<IScanFolderRepository> { ScanFolderRepositoryImpl(get()) }
    single<IScanRulesRepository> { ScanRulesRepositoryImpl(get()) }

    // 扫描服务
    single<IMusicScanService> { MusicScanService(get(), get(), get(), get(), get(), get()) }
}
