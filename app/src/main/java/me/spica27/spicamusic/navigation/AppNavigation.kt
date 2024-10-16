package me.spica27.spicamusic.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.ui.MainScreen
import me.spica27.spicamusic.ui.PlayerScreen
import me.spica27.spicamusic.ui.SplashScreen

fun NavGraphBuilder.appHomeNavigation(
  composeNavigator: AppComposeNavigator
) {

  composable(route = AppScreens.Splash.name) {
    SplashScreen(
      navigator = composeNavigator
    )
  }

  composable(
    route = AppScreens.Main.name,
    arguments = AppScreens.Main.navArguments
  ) {
    MainScreen(
      navigator = composeNavigator
    )
  }

  composable(
    route = AppScreens.Player.name,
    arguments = AppScreens.Player.navArguments,

    ) {
    PlayerScreen()
  }
}
