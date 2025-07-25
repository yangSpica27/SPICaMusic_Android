package me.spica27.spicamusic.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.sliderFromBottomRouteAnim


@Composable
fun AppMain() {
  val systemIsDark = DataStoreUtil().isForceDarkTheme
  val darkTheme = DataStoreUtil()
    .getForceDarkTheme.collectAsStateWithLifecycle(systemIsDark)
    .value
  val backStack = rememberNavBackStack(Routes.Splash)
  AppTheme(
    darkTheme = darkTheme,
    dynamicColor = false
  ) {
    NavDisplay(
      entryDecorators = listOf(
        rememberSceneSetupNavEntryDecorator(),
        rememberSavedStateNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
      ),
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider = entryProvider {
        entry<Routes.AddSong> { key ->
          AddSongScreen(navigator = backStack, playlistId = key.playlistId)
        }
        entry<Routes.PlaylistDetail> {
          PlaylistDetailScreen(
            navigator = backStack,
            playlistId = it.playlistId
          )
        }
        entry<Routes.Main> { MainScreen(navigator = backStack) }
        entry<Routes.Splash> { SplashScreen(navigator = backStack) }
        entry<Routes.SearchAll>(
          metadata = sliderFromBottomRouteAnim()
        ) { SearchAllScreen() }
        entry<Routes.EQ> {
          EqScreen(navigator = backStack)
        }
        entry<Routes.PlayListItemDetail>(
          metadata = sliderFromBottomRouteAnim()
        ) { key ->
          PlayListItemDetailScreen(
            playlistId = key.playlistId,
            songId = key.songId,
            navigator = backStack
          )
        }
      },
      transitionSpec = {
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
      },
      popTransitionSpec = {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
      },
      predictivePopTransitionSpec = {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
      }
    )
  }
}
