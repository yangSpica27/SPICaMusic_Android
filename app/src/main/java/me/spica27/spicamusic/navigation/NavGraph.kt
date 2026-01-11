package me.spica27.spicamusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import me.spica27.spicamusic.ui.home.HomeScreen
import me.spica27.spicamusic.ui.library.AlbumsScreen
import me.spica27.spicamusic.ui.library.AllSongsScreen
import me.spica27.spicamusic.ui.library.ArtistsScreen
import me.spica27.spicamusic.ui.library.FoldersScreen
import me.spica27.spicamusic.ui.library.MostPlayedScreen
import me.spica27.spicamusic.ui.library.PlayHistoryScreen
import me.spica27.spicamusic.ui.library.PlaylistsScreen
import me.spica27.spicamusic.ui.library.RecentlyAddedScreen

/**
 * 应用导航图
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        // 媒体库相关路由
        composable(Screen.AllSongs.route) {
            AllSongsScreen()
        }

        composable(Screen.Playlists.route) {
            PlaylistsScreen()
        }

        composable(Screen.Albums.route) {
            AlbumsScreen()
        }

        composable(Screen.Artists.route) {
            ArtistsScreen()
        }

        composable(Screen.RecentlyAdded.route) {
            RecentlyAddedScreen()
        }

        composable(Screen.MostPlayed.route) {
            MostPlayedScreen()
        }

        composable(Screen.PlayHistory.route) {
            PlayHistoryScreen()
        }

        composable(Screen.Folders.route) {
            FoldersScreen()
        }
    }
}
