package me.spica27.spicamusic.ui.navigation

import android.net.Uri
import androidx.compose.ui.graphics.vector.ImageVector
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.dialog.SortMenuOption

sealed interface Route

// ──────────────────────────────────────────────────────────────────────────
// Stack Routes（全屏页面）
// ──────────────────────────────────────────────────────────────────────────

data object HomeRoute : Route

data class AlbumDetailRoute(
    val album: Album,
) : Route

data class ArtistDetailRoute(
    val artist: Artist,
) : Route

data class PlaylistDetailRoute(
    val playlist: Playlist,
) : Route

data object AllPlaylistsRoute : Route

data object PlaylistCreatorRoute : Route

data object SearchRoute : Route

data object SettingsRoute : Route

data object AboutRoute : Route

data object AppLicenseRoute : Route

data object OpenSourceLicensesRoute : Route

data object PrivacyPolicyRoute : Route

data object AudioEffectsRoute : Route

data object FavoriteRoute : Route

data object IgnoredSongsRoute : Route

data object ScannerRoute : Route

data class LyricRoute(
    val heroArtworkUri: Uri? = null,
) : Route

// ──────────────────────────────────────────────────────────────────────────
// Dialog Routes（对话框 / 菜单）
// ──────────────────────────────────────────────────────────────────────────

data object CurrentListRoute : Route

data object SleepTimerRoute : Route

data object ScanFoldersRoute : Route

data object ScanRulesRoute : Route

data object LyricsSourceRoute : Route

data class SongMenuRoute(
    val song: Song,
) : Route

data class SongInfoRoute(
    val song: Song,
) : Route

data class AlbumMenuRoute(
    val album: Album,
) : Route

data class PlaylistPickerRoute(
    val songMediaStoreId: Long,
) : Route

data class CreatePlaylistForSongRoute(
    val songMediaStoreId: Long,
) : Route

data class SongPickerRoute(
    val playlistId: Long,
) : Route

data class PlaylistOptionsRoute(
    val playlistName: String,
    val isMultiSelectMode: Boolean,
    val isPlaylistEmpty: Boolean,
    val playlistId: Long,
) : Route

class SortMenuDialogRoute(
    val anchorIcon: ImageVector,
    val options: List<SortMenuOption>,
    val selectedId: String,
    val onSelect: (String) -> Unit,
) : Route

class TextInputDialogRoute(
    val title: String,
    val initialValue: String,
    val label: String,
    val confirmLabel: String,
    val dismissLabel: String,
    val onConfirm: (String, dismiss: () -> Unit) -> Unit,
) : Route

class ConfirmationDialogRoute(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val dismissLabel: String? = null,
    val icon: ImageVector? = null,
    val destructive: Boolean = false,
    val onConfirm: (dismiss: () -> Unit) -> Unit = { dismiss -> dismiss() },
) : Route
