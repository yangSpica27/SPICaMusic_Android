package me.spica27.spicamusic.storage.impl.di

import android.app.Application
import androidx.room.Room
import me.spica27.spicamusic.storage.api.ILyricRepository
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.repository.LyricRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.PlayHistoryRepositoryImpl
import me.spica27.spicamusic.storage.impl.repository.PlaylistRepositoryImpl
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
    )
      .fallbackToDestructiveMigration(false)
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
  single<IMusicScanService> { MusicScanService(get(), get())}
}
