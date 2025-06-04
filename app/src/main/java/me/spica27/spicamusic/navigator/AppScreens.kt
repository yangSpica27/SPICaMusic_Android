package me.spica27.spicamusic.navigator

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/// App的导航

object AppScreens {


  @Serializable
  data object Splash : NavKey

  @Serializable
  data object Main : NavKey

  @Serializable
  data class AddSong(val playlistId: Long) : NavKey

  @Serializable
  data class PlaylistDetail(val playlistId: Long) : NavKey

  @Serializable
  data object SearchAll : NavKey

}


