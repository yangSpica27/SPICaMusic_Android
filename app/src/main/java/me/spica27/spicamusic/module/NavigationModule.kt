package me.spica27.spicamusic.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.ComposeNavigator
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NavigationModule {

  @Binds
  @Singleton
  abstract fun provideComposeNavigator(
    composeNavigator: ComposeNavigator
  ): AppComposeNavigator
}
