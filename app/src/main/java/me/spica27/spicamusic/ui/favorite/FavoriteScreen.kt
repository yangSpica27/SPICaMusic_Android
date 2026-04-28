package me.spica27.spicamusic.ui.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.LocalFloatingTabBarScrollConnection
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.widget.AudioQualityBadges
import me.spica27.spicamusic.ui.widget.LibraryActionCard
import me.spica27.spicamusic.ui.widget.SelectionMenuActionItem
import me.spica27.spicamusic.ui.widget.SongListDefaults
import me.spica27.spicamusic.utils.navSharedBounds
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * 我的收藏页面
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun FavoriteScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoriteViewModel = koinViewModel(),
) {
    val favoriteSongs: LazyPagingItems<Song> = viewModel.favoriteSongs.collectAsLazyPagingItems()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsStateWithLifecycle()
    val selectedSongIds by viewModel.selectedSongIds.collectAsStateWithLifecycle()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()
    val hazeSource = rememberHazeState()
    var showMultipleSelectMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    BackHandler(isMultiSelectMode || showMultipleSelectMenu || showCreatePlaylistDialog) {
        when {
            showCreatePlaylistDialog -> showCreatePlaylistDialog = false
            showMultipleSelectMenu -> showMultipleSelectMenu = false
            isMultiSelectMode -> viewModel.exitMultiSelectMode()
        }
    }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            delay(2500)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier =
            modifier
                .navSharedBounds(Screen.Favorite)
                .fillMaxSize(),
        topBar = {
            Column(
                modifier =
                    Modifier
                        .hazeEffect(
                            hazeSource,
                            HazeMaterials.ultraThick(MiuixTheme.colorScheme.surface),
                        ) {
                            progressive =
                                HazeProgressive.verticalGradient(
                                    startIntensity = 1f,
                                    endIntensity = 0f,
                                )
                        }.fillMaxWidth(),
            ) {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    color = Color.Transparent,
                    title =
                        if (isMultiSelectMode) {
                            stringResource(R.string.songs_selected_format, selectedSongIds.size)
                        } else {
                            "${stringResource(R.string.my_favorites)} ($songCount)"
                        },
                    actions = {
                        if (isMultiSelectMode) {
                            IconButton(
                                onClick = {
                                    if (selectedSongIds.size == songCount) {
                                        viewModel.deselectAll()
                                    } else {
                                        viewModel.selectAll()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SelectAll,
                                    contentDescription = stringResource(R.string.select_all),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(
                                onClick = {
                                    if (selectedSongIds.isNotEmpty()) {
                                        showMultipleSelectMenu = true
                                    } else {
                                        viewModel.exitMultiSelectMode()
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.confirm),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    },
                )

                AnimatedVisibility(!isMultiSelectMode) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(12.dp),
                        )
                        TextField(
                            value = searchKeyword,
                            cornerRadius = 12.dp,
                            onValueChange = viewModel::updateSearchKeyword,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                            borderColor = MiuixTheme.colorScheme.surfaceContainer,
                            backgroundColor = MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f),
                            insideMargin = DpSize(22.dp, 16.dp),
                            label = stringResource(R.string.search_song_or_artist),
                            maxLines = 1,
                            useLabelAsPlaceholder = true,
                            leadingIcon = {
                                IconButton(onClick = {}) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search),
                                        tint = MiuixTheme.colorScheme.onSurfaceContainer,
                                    )
                                }
                            },
                            trailingIcon = {
                                if (searchKeyword.isNotEmpty()) {
                                    IconButton(onClick = viewModel::clearSearch) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = null,
                                            tint = MiuixTheme.colorScheme.onSurfaceContainer,
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                        )
                        Row(
                            modifier =
                                Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            LibraryActionCard(
                                icon = Icons.Default.PlayArrow,
                                label = stringResource(R.string.play_all),
                                containerColor = MiuixTheme.colorScheme.primary,
                                contentColor = MiuixTheme.colorScheme.onPrimary,
                                onClick = { viewModel.playAllSongs() },
                                modifier = Modifier.weight(1f),
                            )
                            LibraryActionCard(
                                icon = Icons.Default.CheckCircle,
                                label = stringResource(R.string.multi_select),
                                containerColor = MiuixTheme.colorScheme.tertiaryContainer,
                                contentColor = MiuixTheme.colorScheme.onTertiaryContainer,
                                onClick = { viewModel.enterMultiSelectMode() },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp,
                        vertical = paddingValues.calculateTopPadding(),
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .hazeSource(hazeSource)
                        .nestedScroll(LocalFloatingTabBarScrollConnection.current)
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .fillMaxSize(),
            ) {
                items(
                    count = favoriteSongs.itemCount,
                    key = favoriteSongs.itemKey { it.songId ?: it.mediaStoreId },
                ) { index ->
                    val song = favoriteSongs[index] ?: return@items
                    SongItemCard(
                        song = song,
                        isMultiSelectMode = isMultiSelectMode,
                        isSelected = selectedSongIds.contains(song.songId),
                        onItemClick = {
                            if (isMultiSelectMode) {
                                song.songId?.let(viewModel::toggleSongSelection)
                            } else {
                                viewModel.playAllSongs(song.mediaStoreId)
                            }
                        },
                        onItemLongClick = {
                            if (!isMultiSelectMode) {
                                song.songId?.let(viewModel::enterMultiSelectMode)
                            }
                        },
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }

            AnimatedVisibility(
                visible = showMultipleSelectMenu,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                FavoriteMultiSelectMenu(
                    onUnfavorite = {
                        viewModel.dislikeSelectedSongs()
                        viewModel.exitMultiSelectMode()
                        showMultipleSelectMenu = false
                    },
                    onCreatePlaylist = {
                        showMultipleSelectMenu = false
                        showCreatePlaylistDialog = true
                    },
                    modifier =
                        Modifier
                            .padding(16.dp)
                            .clip(Shapes.SmallCornerBasedShape)
                            .hazeEffect(
                                hazeSource,
                                HazeMaterials.thin(MiuixTheme.colorScheme.primaryContainer),
                            ) {
                                blurRadius = 20.dp
                            }.fillMaxWidth(),
                )
            }

            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = paddingValues.calculateBottomPadding() + 16.dp),
            ) {
                Surface(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    shape = Shapes.ExtraLarge1CornerBasedShape,
                    color = MiuixTheme.colorScheme.onSurface,
                ) {
                    Text(
                        text = snackbarMessage ?: "",
                        color = MiuixTheme.colorScheme.surface,
                        style = MiuixTheme.textStyles.body2,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { playlistName ->
                viewModel.createPlaylistFromSelected(playlistName)
                viewModel.exitMultiSelectMode()
                showCreatePlaylistDialog = false
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongItemCard(
    song: Song,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
) {
    val backgroundColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MiuixTheme.colorScheme.surface
            },
    )

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onItemLongClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        colors = CardDefaults.defaultColors(backgroundColor, backgroundColor),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isMultiSelectMode) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (isSelected) MiuixTheme.colorScheme.primary else Color.Transparent,
                    modifier =
                        Modifier
                            .size(28.dp)
                            .padding(end = 12.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    style = SongListDefaults.songTitleTextStyle,
                    maxLines = 1,
                    color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (song.like) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    AudioQualityBadges(song)
                    Text(
                        text = song.artist,
                        style = SongListDefaults.songMetaTextStyle,
                        color =
                            if (isSelected) {
                                MiuixTheme.colorScheme.primary
                            } else {
                                MiuixTheme.colorScheme.onSurfaceVariantSummary
                            },
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Text(
                text = formatDuration(song.duration),
                style = SongListDefaults.songDurationTextStyle,
                color =
                    if (isSelected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                    },
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FavoriteMultiSelectMenu(
    onUnfavorite: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SelectionMenuActionItem(
            icon = Icons.Default.FavoriteBorder,
            label = stringResource(R.string.remove_from_favorites),
            onClick = onUnfavorite,
            modifier = Modifier.weight(1f),
        )
        SelectionMenuActionItem(
            icon = Icons.AutoMirrored.Filled.PlaylistAdd,
            label = stringResource(R.string.create_playlist),
            onClick = onCreatePlaylist,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var playlistName by remember { mutableStateOf("") }
    var showState by remember { mutableStateOf(true) }

    OverlayDialog(
        title = stringResource(R.string.create_playlist),
        onDismissRequest = onDismiss,
        show = showState,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            TextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = stringResource(R.string.hint_input_playlist_name),
                modifier = Modifier.fillMaxWidth(),
                useLabelAsPlaceholder = true,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                )
                Spacer(modifier = Modifier.size(12.dp))
                Button(
                    onClick = {
                        if (playlistName.isNotBlank()) {
                            onCreate(playlistName.trim())
                        }
                    },
                    enabled = playlistName.isNotBlank(),
                ) {
                    Text(stringResource(R.string.create))
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
