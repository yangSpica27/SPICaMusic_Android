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

    // TODO: 添加更多路由定义
}
