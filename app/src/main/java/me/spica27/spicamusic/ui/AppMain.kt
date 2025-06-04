package me.spica27.spicamusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import me.spica27.spicamusic.navigator.AppScreens
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.utils.DataStoreUtil


@Composable
fun AppMain(

) {
  val context = LocalContext.current
  val systemIsDark = DataStoreUtil(context).isForceDarkTheme
  val darkTheme = DataStoreUtil(context)
    .getForceDarkTheme.collectAsStateWithLifecycle(systemIsDark)
    .value
  val backStack = rememberNavBackStack(AppScreens.Splash)
  AppTheme(
    darkTheme = darkTheme,
    dynamicColor = false
  ) {
    NavDisplay(
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider = entryProvider {
        entry<AppScreens.AddSong> { key ->
          AddSongScreen(navigator = backStack, playlistId = key.playlistId)
        }
        entry<AppScreens.PlaylistDetail> { PlaylistDetailScreen(navigator = backStack, playlistId = it.playlistId) }
        entry<AppScreens.Main> { MainScreen(navigator = backStack) }
        entry<AppScreens.Splash> { SplashScreen(navigator = backStack) }
        entry<AppScreens.SearchAll> { SearchAllScreen() }
      }

    )
  }
}
