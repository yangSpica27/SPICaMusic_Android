package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import me.spica27.spicamusic.navigation.AppNavHost
import me.spica27.spicamusic.navigator.AppComposeNavigator


@Composable
fun AppMain(
  composeNavigator: AppComposeNavigator,
) {
  AppTheme {
    val navHostController = rememberNavController()

    LaunchedEffect(Unit) {
      composeNavigator.handleNavigationCommands(navHostController)
    }

    AppNavHost(navHostController = navHostController, composeNavigator = composeNavigator)
  }
}
