package me.spica27.spicamusic.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.navigator.AppScreens

@Composable
fun AppNavHost(
  navHostController: NavHostController,
  composeNavigator: AppComposeNavigator
) {
  NavHost(
    navController = navHostController,
    startDestination = AppScreens.Splash.route
  ) {
    appHomeNavigation(
      composeNavigator = composeNavigator
    )
  }
}
