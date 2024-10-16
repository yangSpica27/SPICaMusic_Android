package me.spica27.spicamusic.module

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.spica27.spicamusic.db.AppDatabase
import me.spica27.spicamusic.utils.DataStoreUtil
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

  @Provides
  @Singleton
  fun provideAppDatabase(
    application: Application,
  ): AppDatabase {
    return Room
      .databaseBuilder(
        application, AppDatabase::class.java,
        "spica_music.db"
      )
      .fallbackToDestructiveMigration()
      .build()
  }

  @Provides
  @Singleton
  fun provideDataStoreUtil(application: Application) = DataStoreUtil(application)

  @Provides
  @Singleton
  fun provideSongDao(appDatabase: AppDatabase) = appDatabase.songDao()

  @Provides
  @Singleton
  fun providePlaylistDao(appDatabase: AppDatabase) = appDatabase.playlistDao()

}