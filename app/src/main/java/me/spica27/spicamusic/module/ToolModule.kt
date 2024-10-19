package me.spica27.spicamusic.module

import android.app.Application
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import linc.com.amplituda.Amplituda
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object ToolModule {


  @Provides
  @Singleton
  fun provideAmplituda(
    application: Application,
  ): Amplituda {
    return Amplituda(application)
  }


}