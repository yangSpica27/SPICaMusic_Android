package me.spica27.spicamusic.navigator

import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.navArgument

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
    navArguments = listOf(
      navArgument(song_Id) { type = NavType.StringType },
      navArgument(song_name) { type = NavType.StringType }
    )
  ) {
    fun createRoute(songId: String, songName: String) =
      name.replace("{${navArguments[0].name}}", songId)
        .replace("{${navArguments[1].name}}", songName)
  }


  companion object {
    const val song_Id = "songId"
    const val song_name = "songName"
  }
}

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
