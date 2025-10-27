package me.spica27.spicamusic.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.kyant.backdrop.backdrop
import com.kyant.backdrop.rememberBackdrop
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.ui.add_song.AddSongScreen
import me.spica27.spicamusic.ui.agree_privacy.AgreePrivacyScreen
import me.spica27.spicamusic.ui.current_list.CurrentListScreen
import me.spica27.spicamusic.ui.eq.EqScreen
import me.spica27.spicamusic.ui.full_screen_lrc.FullScreenLrcScreen
import me.spica27.spicamusic.ui.ignore_list.IgnoreListScreen
import me.spica27.spicamusic.ui.like_list.LikeListScreen
import me.spica27.spicamusic.ui.lyrics_search.LyricsSearchScreen
import me.spica27.spicamusic.ui.main.MainScreen
import me.spica27.spicamusic.ui.plady_list_detail.PlaylistDetailScreen
import me.spica27.spicamusic.ui.player.PlayerOverly
import me.spica27.spicamusic.ui.player.PlayerOverlyContent
import me.spica27.spicamusic.ui.rencently_list.RecentlyListScreen
import me.spica27.spicamusic.ui.scanner.ScannerScreen
import me.spica27.spicamusic.ui.search_all.SearchAllScreen
import me.spica27.spicamusic.ui.translate.TranslateScreen
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.widget.BackPress
import me.spica27.spicamusic.widget.BottomSheetMenu
import me.spica27.spicamusic.widget.LocalMenuState
import me.spica27.spicamusic.widget.materialSharedAxisXIn
import me.spica27.spicamusic.widget.materialSharedAxisXOut
import me.spica27.spicamusic.widget.materialSharedAxisYIn
import me.spica27.spicamusic.widget.materialSharedAxisYOut
import kotlin.reflect.typeOf

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppMain() {
    val systemIsDark = DataStoreUtil().isForceDarkTheme
    val darkTheme =
        DataStoreUtil()
            .getForceDarkTheme
            .collectAsStateWithLifecycle(systemIsDark)
            .value

    val navHostController = rememberNavController()

    BackPress()

    val backdrop = rememberBackdrop()

    CompositionLocalProvider(LocalNavController provides navHostController) {
        AppTheme(
            darkTheme = darkTheme,
            dynamicColor = false,
        ) {
            PlayerOverlyContent {
                NavHost(
                    startDestination = Routes.Main,
                    navController = LocalNavController.current,
                    modifier =
                        Modifier
                            .clearAndSetSemantics {}
                            .backdrop(backdrop)
                            .fillMaxSize(),
                    enterTransition = {
                        materialSharedAxisXIn(forward = true)
                    },
                    exitTransition = {
                        materialSharedAxisXOut(forward = true)
                    },
                    popEnterTransition = {
                        materialSharedAxisXIn(forward = true)
                    },
                    popExitTransition = {
                        materialSharedAxisXOut(forward = true)
                    },
                ) {
                    composable<Routes.Main> {
                        MainScreen()
                    }
                    composable<Routes.FullScreenLrc>(
                        enterTransition = { materialSharedAxisYIn(true) },
                        exitTransition = { materialSharedAxisYOut(true) },
                        popEnterTransition = { materialSharedAxisYIn(true) },
                        popExitTransition = { materialSharedAxisYOut(true) },
                    ) {
                        FullScreenLrcScreen()
                    }
                    composable<Routes.AddSong> { key ->
                        val playlistId = key.toRoute<Routes.AddSong>().playlistId
                        AddSongScreen(
                            playlistId = playlistId,
                        )
                    }
                    composable<Routes.PlaylistDetail> { key ->
                        val playlistId = key.toRoute<Routes.PlaylistDetail>().playlistId
                        PlaylistDetailScreen(playlistId = playlistId)
                    }
                    composable<Routes.SearchAll>(
                        enterTransition = { materialSharedAxisYIn(true) },
                        exitTransition = { materialSharedAxisYOut(true) },
                        popEnterTransition = { materialSharedAxisYIn(true) },
                        popExitTransition = { materialSharedAxisYOut(true) },
                    ) {
                        SearchAllScreen()
                    }
                    composable<Routes.EQ> {
                        EqScreen()
                    }
                    composable<Routes.Scanner> {
                        ScannerScreen()
                    }
                    composable<Routes.AgreePrivacy> {
                        AgreePrivacyScreen()
                    }
                    composable<Routes.CurrentList> {
                        CurrentListScreen()
                    }
                    composable<Routes.Translate>(
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                    ) { key ->
                        val route = key.toRoute<Routes.Translate>()
                        TranslateScreen(
                            pointX = route.pointX,
                            pointY = route.pointY,
                            fromLight = route.fromLight,
                        )
                    }
                    composable<Routes.LikeList> { LikeListScreen() }
                    composable<Routes.RecentlyList> { RecentlyListScreen() }
                    composable<Routes.IgnoreList> {
                        IgnoreListScreen()
                    }
                    composable<Routes.LyricsSearch>(
                        typeMap =
                            mapOf(
                                typeOf<Song>() to Routes.parcelableType<Song>(),
                            ),
                        enterTransition = {
                            slideInVertically { it }
                        },
                        exitTransition = {
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(350),
                            )
                        },
                    ) { key ->
                        val song = key.toRoute<Routes.LyricsSearch>().song
                        LyricsSearchScreen(
                            song = song,
                        )
                    }
                }
                PlayerOverly(
                    backdrop,
                )
                BottomSheetMenu(
                    state = LocalMenuState.current,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
