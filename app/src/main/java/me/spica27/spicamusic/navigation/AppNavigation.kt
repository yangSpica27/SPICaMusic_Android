package me.spica27.spicamusic.navigation


import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.ui.AddSongScreen
import me.spica27.spicamusic.ui.MainScreen
import me.spica27.spicamusic.ui.PlaylistDetailScreen
import me.spica27.spicamusic.ui.SearchAllScreen
import me.spica27.spicamusic.ui.SplashScreen

/// App的导航
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

//  composable(
//    route = AppScreens.Player.name,
//    arguments = AppScreens.Player.navArguments,
//
//    ) {
//    PlayerScreen()
//  }

  composable(
    route = AppScreens.PlaylistDetail.name,
    arguments = AppScreens.PlaylistDetail.navArguments
  ) {
    PlaylistDetailScreen(navigator = composeNavigator)
  }


  composable(
    route = AppScreens.SearchAll.name,
    arguments = AppScreens.SearchAll.navArguments
  ) {
    SearchAllScreen()
  }
  composable(
    route = AppScreens.AddSongScreen.name,
    arguments = AppScreens.AddSongScreen.navArguments
  ) {
    AddSongScreen(navController = composeNavigator)
  }


}
