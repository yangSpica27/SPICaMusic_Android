package me.spica27.spicamusic.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 应用路由定义 (Navigation 3)
 * 使用 @Serializable 支持类型安全导航
 * 继承 NavKey 以满足 Navigation 3 的类型约束
 */
sealed interface Screen : NavKey {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Player : Screen

    @Serializable
    data object Playlist : Screen

    @Serializable
    data object Settings : Screen

    // 媒体库相关路由
    @Serializable
    data object AllSongs : Screen

    @Serializable
    data object Playlists : Screen

    @Serializable
    data object Albums : Screen

    @Serializable
    data object Artists : Screen

    @Serializable
    data object RecentlyAdded : Screen

    @Serializable
    data object MostPlayed : Screen

    @Serializable
    data object PlayHistory : Screen

    @Serializable
    data object Folders : Screen

    // 歌单详情路由
    @Serializable
    data class PlaylistDetail(
        val playlistId: Long,
    ) : Screen

    // 设置相关路由
    @Serializable
    data object MediaLibrarySource : Screen
}
