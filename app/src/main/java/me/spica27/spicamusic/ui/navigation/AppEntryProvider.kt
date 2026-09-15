package me.spica27.spicamusic.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import me.spica27.spicamusic.ui.about.AboutScreen
import me.spica27.spicamusic.ui.about.AppLicenseScreen
import me.spica27.spicamusic.ui.about.OpenSourceLicensesScreen
import me.spica27.spicamusic.ui.about.PrivacyPolicyScreen
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScreen
import me.spica27.spicamusic.ui.albumdetail.AlbumMenuDialogContent
import me.spica27.spicamusic.ui.artistdetail.ArtistDetailScreen
import me.spica27.spicamusic.ui.audioeffects.AudioEffectsScreen
import me.spica27.spicamusic.ui.dialog.ConfirmationDialogContent
import me.spica27.spicamusic.ui.dialog.CreatePlaylistForSongDialogContent
import me.spica27.spicamusic.ui.dialog.PlaylistPickerDialogContent
import me.spica27.spicamusic.ui.dialog.SongInfoDialogContent
import me.spica27.spicamusic.ui.dialog.SongMenuDialogContent
import me.spica27.spicamusic.ui.dialog.SortMenuDialogContent
import me.spica27.spicamusic.ui.dialog.TextInputDialogContent
import me.spica27.spicamusic.ui.favorite.FavoriteScreen
import me.spica27.spicamusic.ui.home.HomeScreen
import me.spica27.spicamusic.ui.ignoredsongs.IgnoredSongsScreen
import me.spica27.spicamusic.ui.player.SleepTimerDialogContent
import me.spica27.spicamusic.ui.player.scene.CurrentListDialogContent
import me.spica27.spicamusic.ui.player.scene.LyricScreen
import me.spica27.spicamusic.ui.playlist.AllPlaylistsScreen
import me.spica27.spicamusic.ui.playlist.PlaylistCreatorScreen
import me.spica27.spicamusic.ui.playlistdetail.PlaylistDetailScreen
import me.spica27.spicamusic.ui.playlistdetail.PlaylistOptionsDialogContent
import me.spica27.spicamusic.ui.playlistdetail.SongPickerDialogContent
import me.spica27.spicamusic.ui.scan.ScanFoldersDialogContent
import me.spica27.spicamusic.ui.scan.ScanRulesDialogContent
import me.spica27.spicamusic.ui.scan.ScannerScreen
import me.spica27.spicamusic.ui.search.SearchScreen
import me.spica27.spicamusic.ui.settings.SettingsScreen
import me.spica27.spicamusic.ui.widget.LyricsSourceDialogContent

private val dialogMetadata =
    DialogSceneStrategy.dialog(
        DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    )

@Composable
fun appEntryProvider(): (Route) -> NavEntry<Route> =
    remember {
        { key: Route ->
            when (key) {
                // ── Stack Routes ─────────────────────────────────────────
                is HomeRoute -> NavEntry(key) { HomeScreen() }
                is AlbumDetailRoute -> NavEntry(key) { AlbumDetailScreen(album = key.album) }
                is ArtistDetailRoute -> NavEntry(key) { ArtistDetailScreen(artist = key.artist) }
                is PlaylistDetailRoute -> NavEntry(key) { PlaylistDetailScreen(playlist = key.playlist) }
                is AllPlaylistsRoute -> NavEntry(key) { AllPlaylistsScreen() }
                is PlaylistCreatorRoute -> NavEntry(key) { PlaylistCreatorScreen() }
                is SearchRoute -> NavEntry(key) { SearchScreen() }
                is SettingsRoute -> NavEntry(key) { SettingsScreen() }
                is AboutRoute -> NavEntry(key) { AboutScreen() }
                is AppLicenseRoute -> NavEntry(key) { AppLicenseScreen() }
                is OpenSourceLicensesRoute -> NavEntry(key) { OpenSourceLicensesScreen() }
                is PrivacyPolicyRoute -> NavEntry(key) { PrivacyPolicyScreen() }
                is AudioEffectsRoute -> NavEntry(key) { AudioEffectsScreen() }
                is FavoriteRoute -> NavEntry(key) { FavoriteScreen() }
                is IgnoredSongsRoute -> NavEntry(key) { IgnoredSongsScreen() }
                is ScannerRoute -> NavEntry(key) { ScannerScreen() }
                is LyricRoute ->
                    NavEntry(key) {
                        LyricScreen(heroArtworkUri = key.heroArtworkUri?.let(Uri::parse))
                    }

                // ── Dialog Routes ────────────────────────────────────────
                is CurrentListRoute -> NavEntry(key, metadata = dialogMetadata) { CurrentListDialogContent() }
                is SleepTimerRoute -> NavEntry(key, metadata = dialogMetadata) { SleepTimerDialogContent() }
                is ScanFoldersRoute -> NavEntry(key, metadata = dialogMetadata) { ScanFoldersDialogContent() }
                is ScanRulesRoute -> NavEntry(key, metadata = dialogMetadata) { ScanRulesDialogContent() }
                is LyricsSourceRoute -> NavEntry(key, metadata = dialogMetadata) { LyricsSourceDialogContent() }
                is SongMenuRoute -> NavEntry(key, metadata = dialogMetadata) { SongMenuDialogContent(song = key.song) }
                is SongInfoRoute -> NavEntry(key, metadata = dialogMetadata) { SongInfoDialogContent(song = key.song) }
                is AlbumMenuRoute -> NavEntry(key, metadata = dialogMetadata) { AlbumMenuDialogContent(album = key.album) }
                is PlaylistPickerRoute ->
                    NavEntry(
                        key,
                        metadata = dialogMetadata,
                    ) { PlaylistPickerDialogContent(song = key.song) }
                is CreatePlaylistForSongRoute ->
                    NavEntry(key, metadata = dialogMetadata) {
                        CreatePlaylistForSongDialogContent(song = key.song)
                    }
                is SongPickerRoute -> NavEntry(key, metadata = dialogMetadata) { SongPickerDialogContent(playlistId = key.playlistId) }
                is PlaylistOptionsRoute ->
                    NavEntry(key, metadata = dialogMetadata) {
                        PlaylistOptionsDialogContent(
                            playlistName = key.playlistName,
                            isMultiSelectMode = key.isMultiSelectMode,
                            isPlaylistEmpty = key.isPlaylistEmpty,
                            playlistId = key.playlistId,
                            onSelectAll = key.onSelectAll,
                            onDeselectAll = key.onDeselectAll,
                            onEnterSortMode = key.onEnterSortMode,
                            onToggleMultiSelectMode = key.onToggleMultiSelectMode,
                            onRename = key.onRename,
                            onDelete = key.onDelete,
                        )
                    }
                is SortMenuDialogRoute ->
                    NavEntry(key, metadata = dialogMetadata) {
                        SortMenuDialogContent(
                            options = key.options,
                            selectedId = key.selectedId,
                            onSelect = key.onSelect,
                        )
                    }
                is TextInputDialogRoute ->
                    NavEntry(key, metadata = dialogMetadata) {
                        TextInputDialogContent(
                            title = key.title,
                            initialValue = key.initialValue,
                            label = key.label,
                            confirmLabel = key.confirmLabel,
                            dismissLabel = key.dismissLabel,
                            onConfirm = key.onConfirm,
                        )
                    }
                is ConfirmationDialogRoute ->
                    NavEntry(key, metadata = dialogMetadata) {
                        ConfirmationDialogContent(
                            title = key.title,
                            message = key.message,
                            confirmLabel = key.confirmLabel,
                            dismissLabel = key.dismissLabel,
                            icon = key.icon,
                            destructive = key.destructive,
                            onConfirm = key.onConfirm,
                        )
                    }
            }
        }
    }
