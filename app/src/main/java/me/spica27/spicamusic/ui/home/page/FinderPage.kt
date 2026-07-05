package me.spica27.spicamusic.ui.home.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.favorite.FavoriteScene
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.model.PlaylistWithCover
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.playlistdetail.PlaylistDetailScene
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.search.SearchScene
import me.spica27.spicamusic.ui.settings.SettingsScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinderPage() {
    val path = LocalNavigationPath.current
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val frequentSongs by homeViewModel.frequentSongs.collectAsStateWithLifecycle()
    val favoriteSongs by homeViewModel.favoriteSongs.collectAsStateWithLifecycle()
    val playlists by homeViewModel.playlists.collectAsStateWithLifecycle()
    val playlistsWithCover by homeViewModel.playlistsWithCover.collectAsStateWithLifecycle()
    val allSongs by homeViewModel.allSongs.collectAsStateWithLifecycle()

    val playerViewModel = LocalPlayerViewModel.current

    val summaryText =
        stringResource(
            R.string.finder_summary_format,
            frequentSongs.size,
            favoriteSongs.size,
            playlists.size,
        )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.finder_title)) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = { path.push(SettingsScene()) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(LocalBottomBarScrollConnection.current)
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding =
                PaddingValues(
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 200.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
        ) {
            item {
                FinderHeroSearchCard(
                    summaryText = summaryText,
                    onClick = { path.push(SearchScene()) },
                )
            }

            // 数据库没有本地音乐时，引导用户前往扫描页面
            if (allSongs.isEmpty()) {
                item {
                    ScanGuideCard(
                        onClick = { path.push(ScannerScene()) },
                    )
                }
            }

            item {
                PrimaryActionGroup(
                    onOpenLibrary = { homeViewModel.navigateToPage(HomePage.Library) },
                    onOpenMusic = { homeViewModel.navigateToPage(HomePage.Music) },
                    onOpenSettings = { path.push(SettingsScene()) },
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.finder_frequent_title),
                    subtitle = stringResource(R.string.songs_count_format, frequentSongs.size),
                )
            }

            item {
                SongRail(
                    songs = frequentSongs,
                    emptyTitle = stringResource(R.string.finder_no_frequent_title),
                    emptySubtitle = stringResource(R.string.finder_no_frequent_subtitle),
                    onSongClick = { song ->
                        playerViewModel.updatePlaylistWithSongs(
                            songs = frequentSongs,
                            startSong = song,
                            autoStart = true,
                        )
                    },
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.my_favorites),
                    subtitle = stringResource(R.string.songs_count_format, favoriteSongs.size),
                    actionLabel = stringResource(R.string.finder_more).takeIf { favoriteSongs.isNotEmpty() },
                    onActionClick = { path.push(FavoriteScene()) }.takeIf { favoriteSongs.isNotEmpty() },
                )
            }

            item {
                SongRail(
                    songs = favoriteSongs.take(10),
                    emptyTitle = stringResource(R.string.finder_no_favorites_title),
                    emptySubtitle = stringResource(R.string.finder_no_favorites_subtitle),
                    onSongClick = { song ->
                        playerViewModel.updatePlaylistWithSongs(
                            songs = favoriteSongs,
                            startSong = song,
                            autoStart = true,
                        )
                    },
                )
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.finder_playlists_overview_title),
                    subtitle = stringResource(R.string.library_summary_playlists, playlists.size),
                )
            }

            item {
                PlaylistRail(
                    playlists = playlistsWithCover,
                    onPlaylistClick = { playlist ->
                        path.push(PlaylistDetailScene(playlist))
                    },
                    onEmptyClick = { homeViewModel.navigateToPage(HomePage.Library) },
                )
            }
        }
    }
}

@Composable
private fun FinderHeroSearchCard(
    summaryText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .padding(top = Spacing.Small)
                .clip(Shapes.ExtraLarge1CornerBasedShape)
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.colorScheme.tertiaryContainer,
                            ),
                    ),
                ).clickable(onClick = onClick)
                .padding(Spacing.ExtraLarge),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.finder_search_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.finder_search_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * 本地音乐为空时的引导卡片：提示用户前往扫描页面导入歌曲
 */
@Composable
private fun ScanGuideCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .clip(Shapes.ExtraLarge1CornerBasedShape)
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                    ),
                ).clickable(onClick = onClick)
                .padding(Spacing.ExtraLarge),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Scanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.finder_no_local_music_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.finder_no_local_music_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            Row(
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = stringResource(R.string.finder_go_to_scan),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun PrimaryActionGroup(
    onOpenLibrary: () -> Unit,
    onOpenMusic: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        FinderFeatureCard(
            title = stringResource(R.string.finder_library_title),
            subtitle = stringResource(R.string.finder_library_subtitle),
            icon = Icons.Default.LibraryMusic,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenLibrary,
            accent =
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    ),
                ),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            FinderFeatureCard(
                title = stringResource(R.string.finder_all_music_title),
                subtitle = stringResource(R.string.finder_all_music_subtitle),
                icon = Icons.Default.MusicNote,
                modifier = Modifier.weight(1f),
                onClick = onOpenMusic,
            )
            FinderFeatureCard(
                title = stringResource(R.string.finder_settings_title),
                subtitle = stringResource(R.string.finder_settings_subtitle),
                icon = Icons.Default.Settings,
                modifier =
                    Modifier
                        .weight(1f),
                onClick = onOpenSettings,
            )
        }
    }
}

@Composable
private fun FinderFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Brush =
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.surfaceContainer,
            ),
        ),
) {
    Box(
        modifier =
            modifier
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(accent)
                .clickable(onClick = onClick)
                .padding(Spacing.Large),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SongRail(
    songs: List<Song>,
    emptyTitle: String,
    emptySubtitle: String,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (songs.isEmpty()) {
        EmptyFinderCard(
            title = emptyTitle,
            subtitle = emptySubtitle,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        )
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Horizontal),
        ) {
            items(
                items = songs,
                key = { it.mediaStoreId },
            ) { song ->
                FinderSongCard(
                    song = song,
                    onClick = { onSongClick(song) },
                )
            }
        }
    }
}

@Composable
private fun FinderSongCard(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(160.dp)
                .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Card(
            modifier =
                Modifier
                    .size(160.dp)
                    .clip(Shapes.ExtraLargeCornerBasedShape),
        ) {
            SongCover(song)
        }
        Text(
            text = song.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PlaylistRail(
    playlists: List<PlaylistWithCover>,
    onPlaylistClick: (Playlist) -> Unit,
    onEmptyClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (playlists.isEmpty()) {
        EmptyFinderCard(
            title = stringResource(R.string.no_playlists_yet),
            subtitle = stringResource(R.string.finder_no_playlists_subtitle),
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            onClick = onEmptyClick,
        )
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Horizontal),
        ) {
            items(
                items = playlists,
                key = {
                    it.playlist.playlistId ?: it.playlist.playlistName
                        .hashCode()
                        .toLong()
                },
            ) { item ->
                FinderPlaylistCard(
                    item = item,
                    onClick = { onPlaylistClick(item.playlist) },
                )
            }
        }
    }
}

@Composable
private fun FinderPlaylistCard(
    item: PlaylistWithCover,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(168.dp)
                .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        PlaylistCoverView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(Shapes.ExtraLargeCornerBasedShape),
            albumIds = item.coverAlbumIds,
        )
        Text(
            text = item.playlist.playlistName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.songs_count, item.songCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyFinderCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .padding(Spacing.Large),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier =
                    Modifier
                        .padding(start = Spacing.Small)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.ExtraSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SongCover(song: Song) {
    val coverUri =
        remember(song) {
            song.getCoverUri()
        }
    LandscapistImage(
        imageModel = { coverUri },
        modifier = Modifier.fillMaxSize(),
        failure = {
            Image(
                painter = painterResource(R.drawable.default_cover),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        },
    )
}

@Immutable
private class Shortcut(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)
