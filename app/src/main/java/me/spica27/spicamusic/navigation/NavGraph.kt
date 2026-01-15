package me.spica27.spicamusic.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import me.spica27.spicamusic.ui.home.HomeScreen
import me.spica27.spicamusic.ui.library.AlbumsScreen
import me.spica27.spicamusic.ui.library.AllSongsScreen
import me.spica27.spicamusic.ui.library.ArtistsScreen
import me.spica27.spicamusic.ui.library.FoldersScreen
import me.spica27.spicamusic.ui.library.MostPlayedScreen
import me.spica27.spicamusic.ui.library.PlayHistoryScreen
import me.spica27.spicamusic.ui.library.PlaylistsScreen
import me.spica27.spicamusic.ui.library.RecentlyAddedScreen
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceScreen

/**
 * 应用导航图 (Navigation 3)
 */
@Composable
fun AppNavGraph(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<Screen.Home> {
                    HomeScreen()
                }

                // 媒体库相关路由
                entry<Screen.AllSongs> {
                    AllSongsScreen()
                }

                entry<Screen.Playlists> {
                    PlaylistsScreen()
                }

                entry<Screen.Albums> {
                    AlbumsScreen()
                }

                entry<Screen.Artists> {
                    ArtistsScreen()
                }

                entry<Screen.RecentlyAdded> {
                    RecentlyAddedScreen()
                }

                entry<Screen.MostPlayed> {
                    MostPlayedScreen()
                }

                entry<Screen.PlayHistory> {
                    PlayHistoryScreen()
                }

                entry<Screen.Folders> {
                    FoldersScreen()
                }

                // 设置相关路由
                entry<Screen.MediaLibrarySource> {
                    MediaLibrarySourceScreen()
                }
            },
    )
}
