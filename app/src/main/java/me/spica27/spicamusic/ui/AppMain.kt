package me.spica27.spicamusic.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
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
      entryDecorators = listOf(
        rememberSceneSetupNavEntryDecorator(),
        rememberSavedStateNavEntryDecorator(),
        rememberViewModelStoreNavEntryDecorator()
      ),
      backStack = backStack,
      onBack = { backStack.removeLastOrNull() },
      entryProvider = entryProvider {
        entry<AppScreens.AddSong> { key ->
          AddSongScreen(navigator = backStack, playlistId = key.playlistId)
        }
        entry<AppScreens.PlaylistDetail> {
          PlaylistDetailScreen(
            navigator = backStack,
            playlistId = it.playlistId
          )
        }
        entry<AppScreens.Main> { MainScreen(navigator = backStack) }
        entry<AppScreens.Splash> { SplashScreen(navigator = backStack) }
        entry<AppScreens.SearchAll>(
          metadata = NavDisplay.transitionSpec {
            slideInVertically(
              initialOffsetY = { it },
              animationSpec = tween(450)
            ) togetherWith ExitTransition.KeepUntilTransitionsFinished
          } + NavDisplay.popTransitionSpec {
            EnterTransition.None togetherWith
                slideOutVertically(
                  targetOffsetY = { it },
                  animationSpec = tween(450)
                )
          } + NavDisplay.predictivePopTransitionSpec {
            EnterTransition.None togetherWith
                slideOutVertically(
                  targetOffsetY = { it },
                  animationSpec = tween(450)
                )
          }
        ) { SearchAllScreen() }
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
