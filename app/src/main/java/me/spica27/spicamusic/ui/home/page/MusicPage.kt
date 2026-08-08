@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.popup.PopupMenuAnchorState
import me.spica27.navkit.popup.popupMenuAnchor
import me.spica27.navkit.popup.rememberPopupMenuAnchorState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.artistdetail.ArtistDetailScene
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.dialog.SortMenuOption
import me.spica27.spicamusic.ui.dialog.SortMenuScene
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.theme.ENTRANCE_GATE_MILLIS
import me.spica27.spicamusic.ui.theme.ENTRANCE_STAGGER_MILLIS
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.ScaleEnterFrom
import me.spica27.spicamusic.ui.theme.ScaleExitTo
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entranceGraphics
import me.spica27.spicamusic.ui.theme.rememberEntrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.concurrent.TimeUnit

private val MastheadCollapseDistance = 140.dp

@Immutable
private enum class MusicBrowserTab(
    val titleRes: Int,
    val countRes: Int,
    val searchHintRes: Int,
    val icon: ImageVector,
) {
    Songs(
        titleRes = R.string.music_tab_songs,
        countRes = R.string.music_tab_songs_count,
        searchHintRes = R.string.music_search_songs_hint,
        icon = Icons.Default.MusicNote,
    ),
    Albums(
        titleRes = R.string.music_tab_albums,
        countRes = R.string.music_tab_albums_count,
        searchHintRes = R.string.music_search_albums_hint,
        icon = Icons.Default.Album,
    ),
    Artists(
        titleRes = R.string.music_tab_artists,
        countRes = R.string.music_tab_artists_count,
        searchHintRes = R.string.music_search_artists_hint,
        icon = Icons.Default.Person,
    ),
}

// ──────────────────────────────────────────────────────────────────────────
// 各 Tab 的排序方式
// ──────────────────────────────────────────────────────────────────────────

@Immutable
private enum class SongSortMode(
    val option: SortMenuOption,
    val comparator: Comparator<Song>,
) {
    TitleAsc(
        SortMenuOption("title_asc", R.string.sort_song_title_az, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName },
    ),
    TitleDesc(
        SortMenuOption("title_desc", R.string.sort_song_title_za, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER, Song::displayName).reversed(),
    ),
    ArtistAsc(
        SortMenuOption("artist_asc", R.string.sort_song_artist_az, Icons.Default.Person),
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist },
    ),
    ArtistDesc(
        SortMenuOption("artist_desc", R.string.sort_song_artist_za, Icons.Default.Person),
        compareBy(String.CASE_INSENSITIVE_ORDER, Song::artist).reversed(),
    ),
    DurationAsc(
        SortMenuOption("duration_asc", R.string.sort_song_duration_asc, Icons.Default.Schedule),
        compareBy { it.duration },
    ),
    DurationDesc(
        SortMenuOption("duration_desc", R.string.sort_song_duration_desc, Icons.Default.Schedule),
        compareByDescending { it.duration },
    ),
}

@Immutable
private enum class AlbumSortMode(
    val option: SortMenuOption,
    val comparator: Comparator<Album>,
) {
    TitleAsc(
        SortMenuOption("title_asc", R.string.sort_album_title_az, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.title },
    ),
    TitleDesc(
        SortMenuOption("title_desc", R.string.sort_album_title_za, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER, Album::title).reversed(),
    ),
    ArtistAsc(
        SortMenuOption("artist_asc", R.string.sort_album_artist_az, Icons.Default.Person),
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist },
    ),
    ArtistDesc(
        SortMenuOption("artist_desc", R.string.sort_album_artist_za, Icons.Default.Person),
        compareBy(String.CASE_INSENSITIVE_ORDER, Album::artist).reversed(),
    ),
    CountDesc(
        SortMenuOption("count_desc", R.string.sort_album_count_desc, Icons.Default.FormatListNumbered),
        compareByDescending { it.numberOfSongs },
    ),
    CountAsc(
        SortMenuOption("count_asc", R.string.sort_album_count_asc, Icons.Default.FormatListNumbered),
        compareBy { it.numberOfSongs },
    ),
}

@Immutable
private enum class ArtistSortMode(
    val option: SortMenuOption,
    val comparator: Comparator<Artist>,
) {
    NameAsc(
        SortMenuOption("name_asc", R.string.sort_artist_name_az, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER) { it.name },
    ),
    NameDesc(
        SortMenuOption("name_desc", R.string.sort_artist_name_za, Icons.Default.SortByAlpha),
        compareBy(String.CASE_INSENSITIVE_ORDER, Artist::name).reversed(),
    ),
    CountDesc(
        SortMenuOption("count_desc", R.string.sort_artist_count_desc, Icons.Default.FormatListNumbered),
        compareByDescending { it.songCount },
    ),
    CountAsc(
        SortMenuOption("count_asc", R.string.sort_artist_count_asc, Icons.Default.FormatListNumbered),
        compareBy { it.songCount },
    ),
}

@Composable
fun MusicPage() {
    val path = LocalNavigationPath.current
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current

    val allSongs by homeViewModel.allSongs.collectAsStateWithLifecycle()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()

    val unknownAlbum = stringResource(R.string.unknown_album)
    val unknownArtist = stringResource(R.string.unknown_artist)

    val albums =
        remember(allSongs, unknownAlbum, unknownArtist) {
            allSongs.toAlbums(unknownAlbum = unknownAlbum, unknownArtist = unknownArtist)
        }
    val artists =
        remember(allSongs, unknownArtist) {
            allSongs.toArtists(unknownArtist = unknownArtist)
        }

    var selectedTab by rememberSaveable { mutableStateOf(MusicBrowserTab.Songs) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var songSortMode by rememberSaveable { mutableStateOf(SongSortMode.TitleAsc) }
    var albumSortMode by rememberSaveable { mutableStateOf(AlbumSortMode.TitleAsc) }
    var artistSortMode by rememberSaveable { mutableStateOf(ArtistSortMode.NameAsc) }
    var playEntrance by remember { mutableStateOf(true) }
    var playlistEntrance by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            delay(ENTRANCE_GATE_MILLIS)
            playEntrance = false
        }
    }

    LaunchedEffect(playlistEntrance) {
        if (playlistEntrance) {
            delay(ENTRANCE_STAGGER_MILLIS)
            playlistEntrance = false
        }
    }

    val filteredSongs =
        remember(allSongs, searchQuery, songSortMode) {
            allSongs
                .filterSongsBy(searchQuery)
                .sortedWith(songSortMode.comparator)
        }
    val filteredAlbums =
        remember(albums, searchQuery, albumSortMode) {
            albums
                .filterAlbumsBy(searchQuery)
                .sortedWith(albumSortMode.comparator)
        }
    val filteredArtists =
        remember(artists, searchQuery, artistSortMode) {
            artists
                .filterArtistsBy(searchQuery)
                .sortedWith(artistSortMode.comparator)
        }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    // 排序菜单锚点：挂在页面作用域，锚点图标本身在 Lazy item 内
    val sortAnchor = rememberPopupMenuAnchorState()

    fun openSortMenu() {
        if (sortAnchor.isOpen) return
        val scene =
            when (selectedTab) {
                MusicBrowserTab.Songs ->
                    SortMenuScene(
                        anchorState = sortAnchor,
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = SongSortMode.entries.map { it.option },
                        selectedId = songSortMode.option.id,
                        onSelect = { id ->
                            SongSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { songSortMode = it }
                        },
                    )

                MusicBrowserTab.Albums ->
                    SortMenuScene(
                        anchorState = sortAnchor,
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = AlbumSortMode.entries.map { it.option },
                        selectedId = albumSortMode.option.id,
                        onSelect = { id ->
                            AlbumSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { albumSortMode = it }
                        },
                    )

                MusicBrowserTab.Artists ->
                    SortMenuScene(
                        anchorState = sortAnchor,
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = ArtistSortMode.entries.map { it.option },
                        selectedId = artistSortMode.option.id,
                        onSelect = { id ->
                            ArtistSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { artistSortMode = it }
                        },
                    )
            }
        path.push(scene)
    }
    // 用户开始滚动结果时自动收起键盘，把屏幕还给内容
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
    }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(LocalBottomBarScrollConnection.current),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + 56.dp,
                    bottom = 200.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "masthead", contentType = "masthead") {
                val entrance = rememberEntrance(order = 0, play = playEntrance)
                MusicMasthead(
                    songsCount = allSongs.size,
                    albumsCount = albums.size,
                    artistsCount = artists.size,
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .graphicsLayer {
                                val t = mastheadCollapse(listState)
                                val enter = entrance.alpha
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = (1f - t) * enter
                                translationY = -t * 16.dp.toPx() + entrance.translateFraction * 28.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }
            item(key = "tabs", contentType = "tabs") {
                val entrance = rememberEntrance(order = 2, play = playEntrance)
                MusicTabStrip(
                    selectedTab = selectedTab,
                    songsCount = allSongs.size,
                    albumsCount = albums.size,
                    artistsCount = artists.size,
                    onSelect = {
                        selectedTab = it
                        searchQuery = ""
                        playlistEntrance = true
                    },
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec =
                                ListItemFadeInSpec,
                                placementSpec = null,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ).entranceGraphics(entrance),
                )
            }

            item(key = "search", contentType = "search") {
                val entrance = rememberEntrance(order = 3, play = playEntrance)
                MusicSearchBar(
                    query = searchQuery,
                    hint = stringResource(selectedTab.searchHintRes),
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = "" },
                    modifier =
                        Modifier
                            .animateItem(
                                fadeInSpec =
                                ListItemFadeInSpec,
                                placementSpec = null,
                                fadeOutSpec = ListItemFadeOutSpec,
                            ).entranceGraphics(entrance),
                )
            }

            item(key = "section_header", contentType = "section_header") {
                MusicSectionHeader(
                    tab = selectedTab,
                    count =
                        when (selectedTab) {
                            MusicBrowserTab.Songs -> filteredSongs.size
                            MusicBrowserTab.Albums -> filteredAlbums.size
                            MusicBrowserTab.Artists -> filteredArtists.size
                        },
                    sortAnchor = sortAnchor,
                    onSortClick = ::openSortMenu,
                    modifier =
                        Modifier.animateItem(
                            fadeInSpec = ListItemFadeInSpec,
                            placementSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                ),
                            fadeOutSpec = ListItemFadeOutSpec,
                        ),
                )
            }

            when (selectedTab) {
                MusicBrowserTab.Songs -> {
                    if (filteredSongs.isEmpty()) {
                        item(key = "songs_empty", contentType = "empty") {
                            MusicEmptyState(
                                title =
                                    stringResource(
                                        if (allSongs.isEmpty()) {
                                            R.string.music_no_songs_title
                                        } else {
                                            R.string.music_empty_songs_title
                                        },
                                    ),
                                subtitle =
                                    stringResource(
                                        if (allSongs.isEmpty()) {
                                            R.string.music_no_songs_subtitle
                                        } else {
                                            R.string.music_empty_songs_subtitle
                                        },
                                    ),
                                actionLabel = stringResource(R.string.scan_local_music).takeIf { allSongs.isEmpty() },
                                onActionClick = { path.push(ScannerScene()) }.takeIf { allSongs.isEmpty() },
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _, song -> song.mediaStoreId },
                            contentType = { _, _ -> "song" },
                        ) { index, song ->
                            val entrance =
                                rememberEntrance(
                                    order = minOf(index + 4, 10),
                                    play = playlistEntrance,
                                )
                            MusicSongRow(
                                index = index,
                                song = song,
                                isPlaying = currentMediaItem?.mediaId == song.mediaStoreId.toString(),
                                onLongClick = {
                                    path.push(SongMenuScene(song))
                                },
                                onClick = {
                                    playerViewModel.updatePlaylistWithSongs(
                                        songs = filteredSongs,
                                        startSong = song,
                                        autoStart = true,
                                    )
                                },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec =
                                            ListItemFadeInSpec,
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = ListItemFadeOutSpec,
                                        ).graphicsLayer {
                                            val enter = entrance.alpha
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = entrance.translateFraction * 28.dp.toPx()
                                        },
                            )
                        }
                    }
                }

                MusicBrowserTab.Albums -> {
                    if (filteredAlbums.isEmpty()) {
                        item(key = "albums_empty", contentType = "empty") {
                            MusicEmptyState(
                                title =
                                    stringResource(
                                        if (albums.isEmpty()) {
                                            R.string.music_no_albums_title
                                        } else {
                                            R.string.music_empty_albums_title
                                        },
                                    ),
                                subtitle = stringResource(R.string.music_empty_albums_subtitle),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredAlbums,
                            key = { index, album -> album.id },
                            contentType = { index, _ -> "album" },
                        ) { index, album ->
                            val entrance =
                                rememberEntrance(
                                    order = minOf(index + 4, 10),
                                    play = playlistEntrance,
                                )
                            // 共享元素过渡挂在行级：同一行复用同一对实例，
                            // LazyColumn 条目离屏销毁时自动弃用（点击发生时必然在屏）
                            val albumCoverTransition =
                                remember(album.id) {
                                    GeometryTransition(
                                        key = "album_cover_${album.id}",
                                        sourceClipRadius = 16.dp,
                                        targetClipRadius = 12.dp,
                                    )
                                }
                            MusicAlbumRow(
                                album = album,
                                onClick = {
                                    if (albumCoverTransition.phase.value ==
                                        GeometryTransition.GeometryPhase.Source
                                    ) {
                                        path.push(
                                            AlbumDetailScene(
                                                album,
                                            ),
                                        )
                                    }
                                },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec =
                                            ListItemFadeInSpec,
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = ListItemFadeOutSpec,
                                        ).graphicsLayer {
                                            val enter = entrance.alpha
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = entrance.translateFraction * 28.dp.toPx()
                                        },
                            )
                        }
                    }
                }

                MusicBrowserTab.Artists -> {
                    if (filteredArtists.isEmpty()) {
                        item(key = "artists_empty", contentType = "empty") {
                            MusicEmptyState(
                                title =
                                    stringResource(
                                        if (artists.isEmpty()) {
                                            R.string.music_no_artists_title
                                        } else {
                                            R.string.music_empty_artists_title
                                        },
                                    ),
                                subtitle = stringResource(R.string.music_empty_artists_subtitle),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredArtists,
                            key = { index, artist -> artist.name },
                            contentType = { index, _ -> "artist" },
                        ) { index, artist ->
                            val entrance =
                                rememberEntrance(
                                    order = minOf(index + 4, 10),
                                    play = playlistEntrance,
                                )
                            MusicArtistRow(
                                artist = artist,
                                onClick = { path.push(ArtistDetailScene(artist)) },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec =
                                            ListItemFadeInSpec,
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = ListItemFadeOutSpec,
                                        ).graphicsLayer {
                                            val enter = entrance.alpha
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = entrance.translateFraction * 28.dp.toPx()
                                        },
                            )
                        }
                    }
                }
            }
        }

        MusicTopBar(
            listState = listState,
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

private fun Density.mastheadCollapse(listState: LazyListState): Float {
    if (listState.firstVisibleItemIndex > 0) return 1f
    val layoutInfo = listState.layoutInfo
    val masthead = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
    val scrollOutDistance =
        (masthead.size + layoutInfo.mainAxisItemSpacing)
            .toFloat()
            .coerceIn(1f, MastheadCollapseDistance.toPx())
    return (listState.firstVisibleItemScrollOffset / scrollOutDistance).coerceIn(0f, 1f)
}

@Composable
private fun rememberPressScale(interactionSource: MutableInteractionSource): State<Float> {
    val isPressed by interactionSource.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 1100f,
            ),
        label = "musicPressScale",
    )
}

@Composable
private fun MusicTopBar(
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    val solid by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val scope = rememberCoroutineScope()
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 56.dp)
                .drawBehind {
                    drawRect(color = backgroundColor.copy(alpha = mastheadCollapse(listState)))
                },
    ) {
        if (solid) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        ) {
            Text(
                text = stringResource(R.string.music_page_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .graphicsLayer { alpha = mastheadCollapse(listState) },
            )
            AnimatedVisibility(
                modifier = Modifier.align(Alignment.CenterEnd),
                visible = solid,
                // 高频触发（滚动过阈值即出现）：短时长强 ease-out，不带弹性；
                // 淡入与缩放同时长，时间轴对齐
                enter =
                    scaleIn(
                        animationSpec = tween(durationMillis = 180, easing = EaseOutEmphasized),
                        initialScale = ScaleEnterFrom,
                    ) + fadeIn(tween(durationMillis = 180, easing = EaseOutEmphasized)),
                exit =
                    scaleOut(
                        animationSpec = tween(durationMillis = 140),
                        targetScale = ScaleExitTo,
                    ) + fadeOut(tween(durationMillis = 140)),
            ) {
                Row(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickHighlight(onClick = {
                                scope.launch {
                                    listState.animateScrollToItem(0)
                                }
                            })
                            .padding(horizontal = Spacing.Medium, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.scroll_to_top_hint),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicMasthead(
    songsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.music_page_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        AnimatedContent(
            targetState = Triple(songsCount, albumsCount, artistsCount),
            transitionSpec = {
                val targetSum = targetState.first + targetState.second + targetState.third
                val initialSum = initialState.first + initialState.second + initialState.third
                val direction = if (targetSum >= initialSum) 1 else -1
                (
                    slideInVertically { height -> direction * height / 2 } +
                        fadeIn(
                            tween(
                                durationMillis = 240,
                            ),
                        )
                ) togetherWith (
                    slideOutVertically { height -> -direction * height / 2 } +
                        fadeOut(
                            tween(durationMillis = 160),
                        )
                ) using SizeTransform(clip = false)
            },
            modifier = Modifier.padding(top = 6.dp),
            label = "musicSummaryRoll",
        ) { (songs, albums, artists) ->
            Text(
                text = stringResource(R.string.music_summary_format, songs, albums, artists),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MusicTabStrip(
    selectedTab: MusicBrowserTab,
    songsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    onSelect: (MusicBrowserTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        MusicBrowserTab.entries.forEach { tab ->
            val count =
                when (tab) {
                    MusicBrowserTab.Songs -> songsCount
                    MusicBrowserTab.Albums -> albumsCount
                    MusicBrowserTab.Artists -> artistsCount
                }
            MusicTabChip(
                tab = tab,
                count = count,
                selected = tab == selectedTab,
                onClick = { onSelect(tab) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MusicTabChip(
    tab: MusicBrowserTab,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource)
    val container =
        if (selected) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val content =
        if (selected) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    Column(
        modifier =
            modifier
                .height(LayoutTokens.MusicTabHeight)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }.clip(Shapes.LargeCornerBasedShape)
                .background(container)
                .clickHighlight(interactionSource = interactionSource, onClick = onClick)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(tab.countRes, count),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = content,
            maxLines = 1,
        )
    }
}

@Composable
private fun MusicSearchBar(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .height(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = Spacing.Large, end = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    innerTextField()
                }
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MusicSectionHeader(
    tab: MusicBrowserTab,
    count: Int,
    sortAnchor: PopupMenuAnchorState,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .padding(top = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(tab.titleRes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = stringResource(tab.countRes, count),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        // 排序锚点：点击后图标原地过渡成排序菜单（SortMenuScene）
        Box(
            modifier =
                Modifier
                    .popupMenuAnchor(sortAnchor)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickHighlight(
                        onClickLabel = stringResource(R.string.music_sort_cd),
                        onClick = onSortClick,
                    ).padding(Spacing.Small),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(R.string.music_sort_cd),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun MusicSongRow(
    modifier: Modifier = Modifier,
    index: Int,
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(
                    if (isPlaying) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ).combinedClickHighlight(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color =
                if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Center,
        )
        AudioCover(
            uri = song.getCoverUri(),
            fallbackUri = song.getAlbumCoverUri(),
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(Shapes.LargeCornerBasedShape),
            placeHolder = { MusicCoverPlaceholder() },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedContent(isPlaying) { isPlaying ->
            if (isPlaying) {
                Text(
                    text = stringResource(R.string.playing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier,
                    textAlign = TextAlign.End,
                )
            } else {
                Text(
                    text = song.getFormattedDuration(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(42.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun MusicAlbumRow(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            uri = album.getCoverUri(),
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(Shapes.LargeCornerBasedShape),
            placeHolder = { MusicCoverPlaceholder(Icons.Default.Album) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                Modifier,
            )
            Text(
                text = album.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = stringResource(R.string.songs_count_format, album.numberOfSongs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MusicArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            uri = artist.getCoverUri(),
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape),
            placeHolder = { MusicCoverPlaceholder(Icons.Default.Person) },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.songs_count_format, artist.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = Icons.Default.LibraryMusic,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(Spacing.Small)
                    .size(18.dp),
        )
    }
}

@Composable
private fun MusicEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                .clip(Shapes.ExtraLarge1CornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(Spacing.ExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier =
                    Modifier
                        .padding(top = Spacing.Small)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickHighlight(onClick = onActionClick)
                        .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Icon(
                    imageVector = Icons.Default.Scanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun MusicCoverPlaceholder(
    icon: ImageVector = Icons.Default.MusicNote,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun rememberTotalDurationText(totalDuration: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(totalDuration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDuration) % 60
    return when {
        hours > 0 -> stringResource(R.string.hours_minutes, hours, minutes)
        minutes > 0 -> stringResource(R.string.minutes, minutes)
        else -> stringResource(R.string.less_than_1_minute)
    }
}

private fun List<Song>.toAlbums(
    unknownAlbum: String,
    unknownArtist: String,
): List<Album> =
    groupBy { it.albumId }
        .map { (albumId, songs) ->
            val first = songs.first()
            Album(
                id = albumId.toString(),
                title = first.album.ifBlank { unknownAlbum },
                artist = first.artist.ifBlank { unknownArtist },
                numberOfSongs = songs.size,
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

private fun List<Song>.toArtists(unknownArtist: String): List<Artist> =
    groupBy { it.artist.ifBlank { unknownArtist } }
        .map { (name, songs) ->
            Artist(
                name = name,
                songCount = songs.size,
                coverAlbumId = songs.first().albumId,
            )
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })

private fun List<Song>.filterSongsBy(query: String): List<Song> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { song ->
        song.displayName.contains(normalized, ignoreCase = true) ||
            song.artist.contains(
                normalized,
                ignoreCase = true,
            ) ||
            song.album.contains(normalized, ignoreCase = true)
    }
}

private fun List<Album>.filterAlbumsBy(query: String): List<Album> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { album ->
        album.title.contains(normalized, ignoreCase = true) ||
            album.artist.contains(
                normalized,
                ignoreCase = true,
            )
    }
}

private fun List<Artist>.filterArtistsBy(query: String): List<Artist> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { artist ->
        artist.name.contains(normalized, ignoreCase = true)
    }
}
