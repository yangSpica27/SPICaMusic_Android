package me.spica27.spicamusic.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import me.spica27.spicamusic.ui.add_song.AddSongScreen
import me.spica27.spicamusic.ui.agree_privacy.AgreePrivacyScreen
import me.spica27.spicamusic.ui.eq.EqScreen
import me.spica27.spicamusic.ui.like_list.LikeListScreen
import me.spica27.spicamusic.ui.lyrics_search.LyricsSearchScreen
import me.spica27.spicamusic.ui.main.MainScreen
import me.spica27.spicamusic.ui.plady_list_detail.PlaylistDetailScreen
import me.spica27.spicamusic.ui.rencently_list.RecentlyListScreen
import me.spica27.spicamusic.ui.scanner.ScannerScreen
import me.spica27.spicamusic.ui.search_all.SearchAllScreen
import me.spica27.spicamusic.ui.splash.SplashScreen
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.sliderFromBottomRouteAnim


@OptIn(ExperimentalSharedTransitionApi::class)
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
    SharedTransitionLayout {
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
          entry<Routes.Main> { key ->

            MainScreen(
              navigator = backStack,
              sharedTransitionScope = this@SharedTransitionLayout,
            )
          }
          entry<Routes.Splash> { SplashScreen(navigator = backStack) }
          entry<Routes.SearchAll>(
            metadata = sliderFromBottomRouteAnim()
          ) { SearchAllScreen(navigator = backStack) }
          entry<Routes.EQ> {
            EqScreen(navigator = backStack)
          }
          entry<Routes.Scanner> {
            ScannerScreen(navigator = backStack)
          }
          entry<Routes.AgreePrivacy> {
            AgreePrivacyScreen(navigator = backStack)
          }
          entry<Routes.LikeList> { LikeListScreen(navigator = backStack) }
          entry<Routes.RecentlyList> { RecentlyListScreen(navigator = backStack) }
          entry<Routes.LyricsSearch>(
            metadata = sliderFromBottomRouteAnim()
          ) { key ->
            LyricsSearchScreen(
              song = key.song
            )
          }
        },
        transitionSpec = {
          scaleIn(
            initialScale = 1.2f,
          ) + fadeIn(
            animationSpec = tween(250)
          ) togetherWith
              scaleOut(
                targetScale = 1.2f,
              ) + fadeOut(
            animationSpec = tween(250)
          )
        },
        popTransitionSpec = {
          scaleIn(
            initialScale = 1.2f,
          ) + fadeIn() togetherWith
              scaleOut(
                targetScale = 1.2f,

                ) + fadeOut(
            animationSpec = tween(125)
          )
        },
        predictivePopTransitionSpec = {
          scaleIn(
            initialScale = 1.2f,
          ) + fadeIn(
            animationSpec = tween(250)
          ) togetherWith
              scaleOut(
                targetScale = 1.2f,
              ) + fadeOut(
            animationSpec = tween(250)
          )
        },
        sizeTransform = SizeTransform(
          clip = true
        )
      )
    }
  }
}
