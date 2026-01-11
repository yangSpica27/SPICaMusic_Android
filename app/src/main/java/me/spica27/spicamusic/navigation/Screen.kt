package me.spica27.spicamusic.navigation

/**
 * 应用路由定义
 */
sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object Player : Screen("player")

    data object Playlist : Screen("playlist")

    data object Settings : Screen("settings")

    // 媒体库相关路由
    data object AllSongs : Screen("all_songs")

    data object Playlists : Screen("playlists")

    data object Albums : Screen("albums")

    data object Artists : Screen("artists")

    data object RecentlyAdded : Screen("recently_added")

    data object MostPlayed : Screen("most_played")

    data object PlayHistory : Screen("play_history")

    data object Folders : Screen("folders")
}
