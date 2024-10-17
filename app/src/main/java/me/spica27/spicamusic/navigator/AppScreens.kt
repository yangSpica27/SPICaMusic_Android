package me.spica27.spicamusic.navigator

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

/// App的导航
sealed class AppScreens(
  val route: String,
  val index: Int? = null,
  val navArguments: List<NamedNavArgument> = emptyList()
) {
  val name: String = route.appendArguments(navArguments)

  // splash screen
  data object Splash : AppScreens("splash")

  // main screen
  data object Main : AppScreens(
    route = "main"
  )

  // player screen
  data object Player : AppScreens(
    route = "player",
  )


  // search all screen
  data object SearchAll : AppScreens(
    route = "searchAll",
  )

  data object AddSongScreen : AppScreens(
    route = "addSong",
    navArguments = listOf(
      navArgument(playlist_id) {
        type = NavType.LongType
      }
    )
  ) {
    fun createRoute(playlistId: Long) =
      name.replace("{${navArguments[0].name}}", playlistId.toString())
  }

  data object PlaylistDetail : AppScreens(
    route = "playlistDetail",
    navArguments = listOf(
      navArgument(playlist_id) {
        type = NavType.LongType
      }
    )
  ) {
    fun createRoute(playlistId: Long) =
      name.replace("{${navArguments[0].name}}", playlistId.toString())
  }


  companion object {
    // arguments 歌单id
    const val playlist_id = "playlist_id"
  }
}

/// 拼接参数
private fun String.appendArguments(navArguments: List<NamedNavArgument>): String {
  val mandatoryArguments = navArguments.filter { it.argument.defaultValue == null }
    .takeIf { it.isNotEmpty() }
    ?.joinToString(separator = "/", prefix = "/") { "{${it.name}}" }
    .orEmpty()
  val optionalArguments = navArguments.filter { it.argument.defaultValue != null }
    .takeIf { it.isNotEmpty() }
    ?.joinToString(separator = "&", prefix = "?") { "${it.name}={${it.name}}" }
    .orEmpty()
  return "$this$mandatoryArguments$optionalArguments"
}
