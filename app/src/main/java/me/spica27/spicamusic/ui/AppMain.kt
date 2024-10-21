package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.compose.AppTheme
import me.spica27.spicamusic.navigation.AppNavHost
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.utils.DataStoreUtil


@Composable
fun AppMain(
  composeNavigator: AppComposeNavigator,
) {
  val darkTheme = DataStoreUtil(LocalContext.current)
    .getForceDarkTheme.collectAsStateWithLifecycle(false)
    .value
  AppTheme(
    darkTheme = darkTheme
  ) {
    val navHostController = rememberNavController()
    LaunchedEffect(Unit) {
      composeNavigator.handleNavigationCommands(navHostController)
    }

    AppNavHost(navHostController = navHostController, composeNavigator = composeNavigator)
  }
}
