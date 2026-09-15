package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.navigation.AlbumDetailRoute
import me.spica27.spicamusic.ui.navigation.ArtistDetailRoute
import me.spica27.spicamusic.ui.navigation.CreatePlaylistForSongRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.PlaylistCreatorRoute
import me.spica27.spicamusic.ui.navigation.PlaylistPickerRoute
import me.spica27.spicamusic.ui.navigation.SongInfoRoute
import me.spica27.spicamusic.ui.widget.CoverFallback
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SongMenuDialogContent(song: Song) {
    val backStack = LocalBackStack.current
    val viewModel: SongMenuViewModel =
        koinViewModel(
            key = "SongMenuViewModel_${song.mediaStoreId}",
        ) { parametersOf(song) }

    val isLiked by viewModel.isLiked.collectAsStateWithLifecycle()
    val playlists by viewModel.availablePlaylists.collectAsStateWithLifecycle()
    val album by viewModel.albumDetail.collectAsStateWithLifecycle()
    val artist by viewModel.artistDetail.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun closeMenu() {
        backStack.removeLastOrNull()
    }

    fun closeAndNavigate(navigate: () -> Unit) {
        backStack.removeLastOrNull()
        navigate()
    }

    val onPlayNext =
        remember {
            {
                viewModel.addToNext()
                closeMenu()
            }
        }
    val onAddToQueue =
        remember {
            {
                viewModel.addToQueue()
                closeMenu()
            }
        }
    val onToggleLike =
        remember {
            {
                viewModel.toggleLike()
                closeMenu()
            }
        }
    val onShowPlaylistDialog =
        remember {
            {
                backStack.add(PlaylistPickerRoute(song = song))
                Unit
            }
        }
    val onOpenAlbum = remember(album) { { closeAndNavigate { backStack.add(AlbumDetailRoute(album)) } } }
    val onOpenArtist = remember(artist) { { closeAndNavigate { backStack.add(ArtistDetailRoute(artist)) } } }
    val onOpenSongInfo = remember { { closeAndNavigate { backStack.add(SongInfoRoute(song)) } } }
    val onIgnoreSong =
        remember {
            {
                viewModel.ignoreSong()
                closeMenu()
            }
        }

    SongMenuContent(
        song = song,
        isLiked = isLiked,
        onClose = ::closeMenu,
        onPlayNext = onPlayNext,
        onAddToQueue = onAddToQueue,
        onToggleLike = onToggleLike,
        onShowPlaylistDialog = onShowPlaylistDialog,
        onOpenAlbum = onOpenAlbum,
        onOpenArtist = onOpenArtist,
        onOpenSongInfo = onOpenSongInfo,
        onIgnoreSong = onIgnoreSong,
    )
}

@Composable
fun PlaylistPickerDialogContent(song: Song) {
    val backStack = LocalBackStack.current
    val viewModel: SongMenuViewModel =
        koinViewModel(key = "SongMenuViewModel_${song.mediaStoreId}") { parametersOf(song) }
    val playlists by viewModel.availablePlaylists.collectAsStateWithLifecycle()

    PlaylistPickerContent(
        playlists = playlists,
        onDismiss = { backStack.removeLastOrNull() },
        onCreatePlaylist = {
            backStack.removeLastOrNull()
            backStack.add(CreatePlaylistForSongRoute(song))
        },
        onSelectPlaylist = { playlist ->
            val playlistId = playlist.playlistId
            if (playlistId != null) {
                viewModel.addToPlaylist(playlistId)
                // Pop self (PlaylistPicker) and parent (SongMenu)
                backStack.removeLastOrNull()
                backStack.removeLastOrNull()
            } else {
                backStack.removeLastOrNull()
            }
        },
    )
}

@Composable
fun CreatePlaylistForSongDialogContent(song: Song) {
    val backStack = LocalBackStack.current
    val viewModel: SongMenuViewModel =
        koinViewModel(key = "SongMenuViewModel_${song.mediaStoreId}") { parametersOf(song) }

    CreatePlaylistContent(
        onDismiss = { backStack.removeLastOrNull() },
        onConfirm = { name ->
            viewModel.createPlaylistAndAdd(name)
            // Pop self (CreatePlaylist) and parent (SongMenu)
            backStack.removeLastOrNull()
            backStack.removeLastOrNull()
        },
        onOpenFullCreator = {
            // Pop self (CreatePlaylist) and parent (SongMenu)
            backStack.removeLastOrNull()
            backStack.removeLastOrNull()
            backStack.add(PlaylistCreatorRoute)
        },
    )
}

@Composable
private fun SongMenuContent(
    song: Song,
    isLiked: Boolean,
    onClose: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onShowPlaylistDialog: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenArtist: () -> Unit,
    onOpenSongInfo: () -> Unit,
    onIgnoreSong: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 18.dp,
                            bottom = 14.dp,
                            start = 20.dp,
                            end = 12.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier =
                        Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    shape = RoundedCornerShape(18.dp),
                    tonalElevation = 3.dp,
                ) {
                    LandscapistImage(
                        imageModel = { song.getCoverUri() },
                        modifier = Modifier.fillMaxSize(),
                        failure = {
                            CoverFallback(
                                fallbackUri = song.getAlbumCoverUri(),
                                modifier = Modifier.fillMaxSize(),
                            )
                        },
                    )
                }
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = song.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        maxLines = 1,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ControlButton(
                    title = stringResource(R.string.play_next),
                    icon = Icons.Default.PlayArrow,
                    modifier = Modifier.weight(1f),
                    onClick = onPlayNext,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                )
                ControlButton(
                    title = stringResource(R.string.add_to_queue),
                    icon = Icons.Default.PlaylistPlay,
                    modifier = Modifier.weight(1f),
                    onClick = onAddToQueue,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                )
                ControlButton(
                    title =
                        if (isLiked) {
                            stringResource(R.string.remove_from_favorites)
                        } else {
                            stringResource(
                                R.string.favorite,
                            )
                        },
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleLike,
                    containerColor =
                        if (isLiked) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    iconTint =
                        if (isLiked) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            ControlItem(
                title = stringResource(R.string.add_to_playlist),
                subtitle = stringResource(R.string.add_to_playlist_subtitle_menu),
                icon = Icons.AutoMirrored.Default.PlaylistAdd,
                onClick = onShowPlaylistDialog,
            )
            ControlItem(
                title = stringResource(R.string.title_add_to_ignore_list),
                subtitle = stringResource(R.string.desc_add_to_ignore_list),
                icon = Icons.Default.MusicOff,
                onClick = onIgnoreSong,
            )
            ControlItem(
                title = stringResource(R.string.view_album),
                subtitle = song.album,
                icon = Icons.Default.Album,
                onClick = onOpenAlbum,
            )
            ControlItem(
                title = stringResource(R.string.view_artist),
                subtitle = song.artist,
                icon = Icons.Default.SportsMartialArts,
                onClick = onOpenArtist,
            )
            ControlItem(
                title = stringResource(R.string.song_info_menu_title),
                subtitle = stringResource(R.string.song_info_menu_subtitle),
                icon = Icons.Default.Info,
                onClick = onOpenSongInfo,
            )
        }
    }
}

@Composable
private fun ControlButton(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier =
            modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = containerColor.copy(alpha = 0.72f),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ControlItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistPickerContent(
    playlists: List<me.spica27.spicamusic.common.entity.Playlist>,
    onDismiss: () -> Unit,
    onCreatePlaylist: () -> Unit,
    onSelectPlaylist: (me.spica27.spicamusic.common.entity.Playlist) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogTitle(
                title = stringResource(R.string.add_to_playlist),
                subtitle = stringResource(R.string.playlist_picker_subtitle),
                icon = Icons.AutoMirrored.Default.PlaylistAdd,
            )
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (playlists.isEmpty()) {
                    EmptyDialogCard(
                        icon = Icons.Default.LibraryMusic,
                        title = stringResource(R.string.no_playlists_yet),
                        subtitle = stringResource(R.string.no_playlists_hint),
                    )
                } else {
                    playlists.forEach { playlist ->
                        PlaylistDialogRow(
                            title = playlist.playlistName,
                            subtitle = stringResource(R.string.tap_to_add_to_playlist),
                            onClick = { onSelectPlaylist(playlist) },
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = onCreatePlaylist,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.create_playlist))
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistContent(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onOpenFullCreator: () -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DialogTitle(
                title = stringResource(R.string.create_playlist),
                subtitle = stringResource(R.string.create_playlist_auto_add_subtitle),
                icon = Icons.Default.Add,
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text(stringResource(R.string.playlist_name_label)) },
                    placeholder = { Text(stringResource(R.string.playlist_name_placeholder)) },
                    supportingText = { Text(stringResource(R.string.playlist_name_supporting)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions =
                        KeyboardActions(onDone = {
                            if (playlistName.isNotBlank()) {
                                onConfirm(playlistName)
                            }
                        }),
                )
                TextButton(onClick = onOpenFullCreator) {
                    Text(stringResource(R.string.create_empty_only))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onConfirm(playlistName) },
                    enabled = playlistName.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(stringResource(R.string.create_and_add))
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(
    title: String,
    subtitle: String,
    icon: ImageVector,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PlaylistDialogRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(15.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.LibraryMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun EmptyDialogCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
