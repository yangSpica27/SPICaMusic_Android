package me.spica27.spicamusic.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.dialog.SortMenuOption

sealed interface Route

@Serializable
sealed interface ScreenRoute :
    Route,
    NavKey

sealed interface DialogRoute : Route

// ──────────────────────────────────────────────────────────────────────────
// Stack Routes（全屏页面）
// ──────────────────────────────────────────────────────────────────────────

@Serializable
data object HomeRoute : ScreenRoute

@Serializable
data class AlbumDetailRoute(
    val album: Album,
) : ScreenRoute

@Serializable
data class ArtistDetailRoute(
    val artist: Artist,
) : ScreenRoute

@Serializable
data class PlaylistDetailRoute(
    val playlist: Playlist,
) : ScreenRoute

@Serializable
data object AllPlaylistsRoute : ScreenRoute

@Serializable
data object PlaylistCreatorRoute : ScreenRoute

@Serializable
data object SearchRoute : ScreenRoute

@Serializable
data object SettingsRoute : ScreenRoute

@Serializable
data object AboutRoute : ScreenRoute

@Serializable
data object AppLicenseRoute : ScreenRoute

@Serializable
data object OpenSourceLicensesRoute : ScreenRoute

@Serializable
data object PrivacyPolicyRoute : ScreenRoute

@Serializable
data object AudioEffectsRoute : ScreenRoute

@Serializable
data object FavoriteRoute : ScreenRoute

@Serializable
data object IgnoredSongsRoute : ScreenRoute

@Serializable
data object ScannerRoute : ScreenRoute

@Serializable
data class LyricRoute(
    val heroArtworkUri: String? = null,
) : ScreenRoute

// ──────────────────────────────────────────────────────────────────────────
// Dialog Routes（对话框 / 菜单）
// ──────────────────────────────────────────────────────────────────────────

data object CurrentListRoute : DialogRoute

data object SleepTimerRoute : DialogRoute

data object ScanFoldersRoute : DialogRoute

data object ScanRulesRoute : DialogRoute

data object LyricsSourceRoute : DialogRoute

data class SongMenuRoute(
    val song: Song,
) : DialogRoute

data class SongInfoRoute(
    val song: Song,
) : DialogRoute

data class AlbumMenuRoute(
    val album: Album,
) : DialogRoute

data class PlaylistPickerRoute(
    val song: Song,
) : DialogRoute

data class CreatePlaylistForSongRoute(
    val song: Song,
) : DialogRoute

data class SongPickerRoute(
    val playlistId: Long,
) : DialogRoute

class PlaylistOptionsRoute(
    val playlistName: String,
    val isMultiSelectMode: Boolean,
    val isPlaylistEmpty: Boolean,
    val playlistId: Long,
    val onSelectAll: () -> Unit,
    val onDeselectAll: () -> Unit,
    val onEnterSortMode: () -> Unit,
    val onToggleMultiSelectMode: () -> Unit,
    val onRename: (String, onSuccess: () -> Unit) -> Unit,
    val onDelete: (onSuccess: () -> Unit) -> Unit,
) : DialogRoute

class SortMenuDialogRoute(
    val anchorIcon: ImageVector,
    val options: List<SortMenuOption>,
    val selectedId: String,
    val onSelect: (String) -> Unit,
) : DialogRoute

class TextInputDialogRoute(
    val title: String,
    val initialValue: String,
    val label: String,
    val confirmLabel: String,
    val dismissLabel: String,
    val onConfirm: (String, dismiss: () -> Unit) -> Unit,
) : DialogRoute

class ConfirmationDialogRoute(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String? = null,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val onConfirm: (dismiss: () -> Unit) -> Unit = { dismiss -> dismiss() },
) : DialogRoute
