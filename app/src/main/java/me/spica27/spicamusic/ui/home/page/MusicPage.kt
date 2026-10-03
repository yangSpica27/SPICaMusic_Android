@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.home.page

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FormatListNumbered
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.artwork.artwork
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.ui.dialog.SortMenuOption
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.player_bar.GlassNavigationIndicatorState
import me.spica27.spicamusic.ui.navigation.AlbumDetailRoute
import me.spica27.spicamusic.ui.navigation.ArtistDetailRoute
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.navigation.PopupAnchor
import me.spica27.spicamusic.ui.navigation.ScannerRoute
import me.spica27.spicamusic.ui.navigation.SongMenuRoute
import me.spica27.spicamusic.ui.navigation.SortMenuDialogRoute
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.ENTRANCE_GATE_MILLIS
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.theme.ScaleEnterFrom
import me.spica27.spicamusic.ui.theme.ScaleExitTo
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entrance
import me.spica27.spicamusic.ui.widget.AnimatedCursorTextField
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.abs

/** 音乐页：歌曲 / 专辑 / 歌手浏览 */

/** 大标题收缩距离上限 */
private val MastheadCollapseDistance = 140.dp

/** 分段控件高度与滑块内缩 */
private val SegmentedHeight = 44.dp
private val SegmentedInset = 4.dp

/** 搜索胶囊与排序按钮高度 */
private val ControlHeight = 44.dp

/** 列表行封面尺寸 */
private val RowCoverSize = 48.dp

/** 首屏入场槽位：刊头 0、分段 1、搜索行 2，条目从 3 起 */
private const val ENTRANCE_ORDER_ITEM_BASE = 3
private const val ENTRANCE_MAX_ORDER = 10

/** 列表更新只做短淡入淡出，不叠加缩放或逐行延迟。 */
private val ItemFadeInSpec = tween<Float>(durationMillis = 160, easing = LinearOutSlowInEasing)
private val ItemFadeOutSpec = tween<Float>(durationMillis = 100, easing = LinearEasing)

/** 条目重排平稳归位，不越过目标位置或回弹。 */
private val ItemPlacementSpringSpec =
    spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
        visibilityThreshold = IntOffset.VisibilityThreshold,
    )

@Immutable
private enum class MusicBrowserTab(
    @param:StringRes val titleRes: Int,
    @param:StringRes val searchHintRes: Int,
    @param:StringRes val foundRes: Int,
) {
    Songs(
        titleRes = R.string.music_tab_songs,
        searchHintRes = R.string.music_search_songs_hint,
        foundRes = R.string.music_found_songs,
    ),
    Albums(
        titleRes = R.string.music_tab_albums,
        searchHintRes = R.string.music_search_albums_hint,
        foundRes = R.string.music_found_albums,
    ),
    Artists(
        titleRes = R.string.music_tab_artists,
        searchHintRes = R.string.music_search_artists_hint,
        foundRes = R.string.music_found_artists,
    ),
}

// 各标签页的排序方式。

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
fun MusicPage(bottomContentPadding: Dp = 0.dp) {
    val backStack = LocalBackStack.current
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val playerViewModel = LocalPlayerViewModel.current
    val reducedMotion = LocalReducedMotion.current
    val itemPlacementSpec = if (reducedMotion) null else ItemPlacementSpringSpec

    val allSongs by homeViewModel.allSongs.collectAsStateWithLifecycle()
    val currentMediaItem by playerViewModel.currentMediaItem.collectAsStateWithLifecycle()
    val playingMediaId = currentMediaItem?.mediaId

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
    var debouncedQuery by rememberSaveable { mutableStateOf("") }
    var songSortMode by rememberSaveable { mutableStateOf(SongSortMode.TitleAsc) }
    var albumSortMode by rememberSaveable { mutableStateOf(AlbumSortMode.TitleAsc) }
    var artistSortMode by rememberSaveable { mutableStateOf(ArtistSortMode.NameAsc) }

    // 首屏入场只播一次
    var playEntrance by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_GATE_MILLIS)
        playEntrance = false
    }

    val selectTab: (MusicBrowserTab) -> Unit = { tab ->
        if (tab != selectedTab) {
            // 首屏尚未播完时切页，也只使用列表更新的淡入淡出。
            playEntrance = false
            selectedTab = tab
        }
    }

    @OptIn(FlowPreview::class)
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(300)
            .collect { debouncedQuery = it }
    }

    val filteredSongs =
        remember(allSongs, debouncedQuery, songSortMode) {
            allSongs
                .filterSongsBy(debouncedQuery)
                .sortedWith(songSortMode.comparator)
        }
    val filteredAlbums =
        remember(albums, debouncedQuery, albumSortMode) {
            albums
                .filterAlbumsBy(debouncedQuery)
                .sortedWith(albumSortMode.comparator)
        }
    val filteredArtists =
        remember(artists, debouncedQuery, artistSortMode) {
            artists
                .filterArtistsBy(debouncedQuery)
                .sortedWith(artistSortMode.comparator)
        }
    val searching = debouncedQuery.isNotBlank()
    val visibleCount =
        when (selectedTab) {
            MusicBrowserTab.Songs -> filteredSongs.size
            MusicBrowserTab.Albums -> filteredAlbums.size
            MusicBrowserTab.Artists -> filteredArtists.size
        }
    val sortIsDefault =
        when (selectedTab) {
            MusicBrowserTab.Songs -> songSortMode == SongSortMode.TitleAsc
            MusicBrowserTab.Albums -> albumSortMode == AlbumSortMode.TitleAsc
            MusicBrowserTab.Artists -> artistSortMode == ArtistSortMode.NameAsc
        }

    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val dismissKeyboard = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    // 排序菜单按触发按钮在窗口中的位置显示。
    fun openSortMenu(anchor: PopupAnchor) {
        dismissKeyboard.invoke()
        val route =
            when (selectedTab) {
                MusicBrowserTab.Songs ->
                    SortMenuDialogRoute(
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = SongSortMode.entries.map { it.option },
                        selectedId = songSortMode.option.id,
                        onSelect = { id ->
                            SongSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { songSortMode = it }
                        },
                        anchor = anchor,
                    )

                MusicBrowserTab.Albums ->
                    SortMenuDialogRoute(
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = AlbumSortMode.entries.map { it.option },
                        selectedId = albumSortMode.option.id,
                        onSelect = { id ->
                            AlbumSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { albumSortMode = it }
                        },
                        anchor = anchor,
                    )

                MusicBrowserTab.Artists ->
                    SortMenuDialogRoute(
                        anchorIcon = Icons.AutoMirrored.Filled.Sort,
                        options = ArtistSortMode.entries.map { it.option },
                        selectedId = artistSortMode.option.id,
                        onSelect = { id ->
                            ArtistSortMode.entries
                                .firstOrNull { it.option.id == id }
                                ?.let { artistSortMode = it }
                        },
                        anchor = anchor,
                    )
            }
        backStack.add(route)
    }

    // 开始滚动时收起键盘
    LaunchedEffect(gridState) {
        snapshotFlow { gridState.isScrollInProgress }
            .filter { it }
            .collect { dismissKeyboard() }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = LayoutTokens.MusicHeaderHorizontalPadding,
                    end = LayoutTokens.MusicHeaderHorizontalPadding,
                    top = statusBarTop + 56.dp,
                    bottom = bottomContentPadding + Spacing.Large,
                ),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "masthead", span = { GridItemSpan(maxLineSpan) }, contentType = "masthead") {
                MusicMasthead(
                    songsCount = allSongs.size,
                    albumsCount = albums.size,
                    artistsCount = artists.size,
                    searching = searching,
                    tab = selectedTab,
                    foundCount = visibleCount,
                    modifier =
                        Modifier
                            .padding(top = Spacing.Large)
                            .entrance(order = 0, play = playEntrance)
                            .graphicsLayer {
                                val t = mastheadCollapse(gridState)
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = 1f - t
                                translationY = -t * 16.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }

            item(key = "tabs", span = { GridItemSpan(maxLineSpan) }, contentType = "tabs") {
                MusicSegmentedTabs(
                    selectedTab = selectedTab,
                    onSelect = selectTab,
                    modifier =
                        Modifier
                            .padding(top = Spacing.Large)
                            .entrance(order = 1, play = playEntrance),
                )
            }

            item(key = "search", span = { GridItemSpan(maxLineSpan) }, contentType = "search") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Small, bottom = Spacing.Small)
                            .entrance(order = 2, play = playEntrance),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    MusicSearchPill(
                        query = searchQuery,
                        hint = stringResource(selectedTab.searchHintRes),
                        onQueryChange = { searchQuery = it },
                        onClear = { searchQuery = "" },
                        onSubmit = dismissKeyboard,
                        modifier = Modifier.weight(1f),
                    )
                    MusicSortButton(
                        active = !sortIsDefault,
                        onClick = ::openSortMenu,
                    )
                }
            }

            when (selectedTab) {
                MusicBrowserTab.Songs -> {
                    if (filteredSongs.isEmpty()) {
                        item(key = "songs_empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
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
                                onActionClick =
                                    {
                                        backStack.add(ScannerRoute)
                                        Unit
                                    }.takeIf { allSongs.isEmpty() },
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = ItemFadeInSpec,
                                        placementSpec = null,
                                        fadeOutSpec = ItemFadeOutSpec,
                                    ),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredSongs,
                            key = { _, song -> "song:${song.mediaStoreId}" },
                            span = { _, _ -> GridItemSpan(maxLineSpan) },
                            contentType = { _, _ -> "song" },
                        ) { index, song ->
                            MusicSongRow(
                                song = song,
                                isPlaying = playingMediaId == song.mediaStoreId.toString(),
                                onClick = {
                                    playerViewModel.updatePlaylistWithSongs(
                                        songs = filteredSongs,
                                        startSong = song,
                                        autoStart = true,
                                    )
                                },
                                onLongClick = { backStack.add(SongMenuRoute(song)) },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec = ItemFadeInSpec,
                                            placementSpec = itemPlacementSpec,
                                            fadeOutSpec = ItemFadeOutSpec,
                                        ).entrance(
                                            order = minOf(index + ENTRANCE_ORDER_ITEM_BASE, ENTRANCE_MAX_ORDER),
                                            play = playEntrance,
                                        ),
                            )
                        }
                    }
                }

                MusicBrowserTab.Albums -> {
                    if (filteredAlbums.isEmpty()) {
                        item(key = "albums_empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
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
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = ItemFadeInSpec,
                                        placementSpec = null,
                                        fadeOutSpec = ItemFadeOutSpec,
                                    ),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredAlbums,
                            key = { _, album -> "album:${album.id}" },
                            contentType = { _, _ -> "album" },
                        ) { index, album ->
                            // 同排两张卡共用一个槽位
                            val row = index / 2
                            MusicAlbumCard(
                                album = album,
                                onClick = { backStack.add(AlbumDetailRoute(album)) },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec = ItemFadeInSpec,
                                            placementSpec = itemPlacementSpec,
                                            fadeOutSpec = ItemFadeOutSpec,
                                        ).entrance(
                                            order = minOf(row + ENTRANCE_ORDER_ITEM_BASE, ENTRANCE_MAX_ORDER),
                                            play = playEntrance,
                                        ),
                            )
                        }
                    }
                }

                MusicBrowserTab.Artists -> {
                    if (filteredArtists.isEmpty()) {
                        item(key = "artists_empty", span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
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
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = ItemFadeInSpec,
                                        placementSpec = null,
                                        fadeOutSpec = ItemFadeOutSpec,
                                    ),
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = filteredArtists,
                            key = { _, artist -> "artist:${artist.name}" },
                            span = { _, _ -> GridItemSpan(maxLineSpan) },
                            contentType = { _, _ -> "artist" },
                        ) { index, artist ->
                            MusicArtistRow(
                                artist = artist,
                                onClick = { backStack.add(ArtistDetailRoute(artist)) },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec = ItemFadeInSpec,
                                            placementSpec = itemPlacementSpec,
                                            fadeOutSpec = ItemFadeOutSpec,
                                        ).entrance(
                                            order = minOf(index + ENTRANCE_ORDER_ITEM_BASE, ENTRANCE_MAX_ORDER),
                                            play = playEntrance,
                                        ),
                            )
                        }
                    }
                }
            }
        }

        MusicTopBar(
            gridState = gridState,
            onScrollToTop = {
                scope.launch {
                    if (gridState.firstVisibleItemIndex > 6) {
                        gridState.scrollToItem(3)
                    }
                    gridState.animateScrollToItem(0)
                }
            },
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

/** 大标题收缩进度：0f 展开，1f 收进顶栏；在绘制阶段读取 */
private fun Density.mastheadCollapse(gridState: LazyGridState): Float {
    if (gridState.firstVisibleItemIndex > 0) return 1f
    val layoutInfo = gridState.layoutInfo
    val masthead = layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
    val scrollOutDistance =
        (masthead.size.height + layoutInfo.mainAxisItemSpacing)
            .toFloat()
            .coerceIn(1f, MastheadCollapseDistance.toPx())
    return (gridState.firstVisibleItemScrollOffset / scrollOutDistance).coerceIn(0f, 1f)
}

/** 让条目越过网格水平内边距通栏铺满，内容由自身 padding 对齐回页边距 */
private fun Modifier.bleedHorizontal(amount: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = amount.roundToPx() * 2
        val placeable =
            measurable.measure(
                constraints.copy(
                    minWidth = constraints.minWidth + extra,
                    maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
                ),
            )
        val width = (placeable.width - extra).coerceIn(constraints.minWidth, constraints.maxWidth)
        layout(width, placeable.height) {
            placeable.place(-extra / 2, 0)
        }
    }

/** 固定顶栏：随刊头收缩显形，收起后弹出「回到顶部」药丸 */
@Composable
private fun MusicTopBar(
    gridState: LazyGridState,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    // 派生状态放在顶栏作用域，翻转只重组顶栏
    val solid by remember { derivedStateOf { gridState.firstVisibleItemIndex > 0 } }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 56.dp)
                .drawBehind {
                    drawRect(color = backgroundColor.copy(alpha = mastheadCollapse(gridState)))
                },
    ) {
        // 全页唯一分隔线：顶栏收起后出现
        if (solid) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
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
                        .weight(1f)
                        .graphicsLayer { alpha = mastheadCollapse(gridState) },
            )
            AnimatedVisibility(
                visible = solid,
                enter =
                    scaleIn(
                        animationSpec =
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        initialScale = ScaleEnterFrom,
                    ) + fadeIn(tween(durationMillis = 160)),
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
                            .clickHighlight(onClick = onScrollToTop)
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

/** 刊头 meta 行：文案与决定滚动方向的计数 */
@Immutable
private data class MusicMeta(
    val text: String,
    val count: Int,
)

/** 刊头：大标题 + meta 行，搜索时切成「找到 N 首」 */
@Composable
private fun MusicMasthead(
    songsCount: Int,
    albumsCount: Int,
    artistsCount: Int,
    searching: Boolean,
    tab: MusicBrowserTab,
    foundCount: Int,
    modifier: Modifier = Modifier,
) {
    val summary = stringResource(R.string.music_summary_format, songsCount, albumsCount, artistsCount)
    val found = stringResource(tab.foundRes, foundCount)
    val meta =
        remember(searching, summary, found, foundCount, songsCount, albumsCount, artistsCount) {
            if (searching) {
                MusicMeta(text = found, count = foundCount)
            } else {
                MusicMeta(text = summary, count = songsCount + albumsCount + artistsCount)
            }
        }
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
            targetState = meta,
            transitionSpec = {
                val direction = if (targetState.count >= initialState.count) 1 else -1
                (
                    slideInVertically { height -> direction * height / 2 } +
                        fadeIn(tween(durationMillis = 240))
                ) togetherWith
                    (
                        slideOutVertically { height -> -direction * height / 2 } +
                            fadeOut(tween(durationMillis = 160))
                    ) using SizeTransform(clip = false)
            },
            modifier = Modifier.padding(top = 6.dp),
            label = "musicMetaRoll",
        ) { state ->
            Text(
                text = state.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 分段切换：胶囊轨道 + 弹性滑块，手势与运动复用底栏指示器状态 */
@Composable
private fun MusicSegmentedTabs(
    selectedTab: MusicBrowserTab,
    onSelect: (MusicBrowserTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = MusicBrowserTab.entries
    val scope = rememberCoroutineScope()
    val indicator = remember(scope) { GlassNavigationIndicatorState(selectedTab.ordinal, tabs.size, scope) }
    val currentSelection by rememberUpdatedState(selectedTab)
    val onSelection by rememberUpdatedState(onSelect)
    val reducedMotion = LocalReducedMotion.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val inset = with(density) { SegmentedInset.toPx() }
    val segmentWidth = ((bounds.width - 2f * inset) / tabs.size).coerceAtLeast(0f)
    val interacting = indicator.isInteracting && !reducedMotion
    val press by animateFloatAsState(
        targetValue = if (interacting) 1f else 0f,
        animationSpec =
            when {
                reducedMotion -> snap()
                interacting -> spring(dampingRatio = 0.85f, stiffness = 900f)
                else -> tween(140)
            },
        label = "musicSegmentPress",
    )

    LaunchedEffect(selectedTab, reducedMotion) { indicator.synchronize(selectedTab.ordinal, reducedMotion) }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(SegmentedHeight)
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh)
                .selectableGroup()
                .onSizeChanged { bounds = it }
                .pointerInput(indicator, segmentWidth, isLtr, reducedMotion) {
                    if (segmentWidth <= 0f) return@pointerInput
                    val direction = if (isLtr) 1f else -1f
                    val lastIndex = tabs.lastIndex
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        indicator.press()
                        try {
                            val dragStart =
                                awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                    // 从滑块当前位置起步跟手
                                    indicator.beginDrag()
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    indicator.dragBy(direction * overSlop / segmentWidth, 0f)
                                    change.consume()
                                }
                            if (dragStart != null) {
                                val completed =
                                    horizontalDrag(dragStart.id) { change ->
                                        tracker.addPosition(change.uptimeMillis, change.position)
                                        indicator.dragBy(
                                            direction * change.positionChange().x / segmentWidth,
                                            direction * tracker.calculateVelocity().x / segmentWidth,
                                        )
                                        change.consume()
                                    }
                                val target =
                                    if (completed) {
                                        tabs[indicator.nearestIndex().coerceIn(0, lastIndex)]
                                    } else {
                                        currentSelection
                                    }
                                indicator.animateTo(target.ordinal, reducedMotion)
                                if (completed && target != currentSelection) onSelection(target)
                            }
                        } finally {
                            indicator.release()
                            if (indicator.isDragging) indicator.animateTo(currentSelection.ordinal, reducedMotion)
                        }
                    }
                },
    ) {
        // 滑块在文字下层，位置在图层阶段读取
        if (bounds.width > 0 && bounds.height > 0 && segmentWidth > 0f) {
            val thumbHeight = (bounds.height - 2f * inset).coerceAtLeast(1f)
            Box(
                modifier =
                    Modifier
                        .size(with(density) { segmentWidth.toDp() }, with(density) { thumbHeight.toDp() })
                        .graphicsLayer {
                            val stretch = (abs(indicator.velocity) / 6f).coerceIn(0f, 1f) * press
                            scaleX = 1f + 0.03f * press + 0.02f * stretch
                            scaleY = 1f + 0.05f * press - 0.02f * stretch
                            translationX =
                                if (isLtr) {
                                    inset + indicator.position * segmentWidth
                                } else {
                                    bounds.width - inset - (indicator.position + 1f) * segmentWidth
                                }
                            translationY = inset
                        }.clip(CircleShape)
                        .background(colors.primaryContainer)
                        .clearAndSetSemantics {},
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(SegmentedInset),
        ) {
            tabs.forEach { tab ->
                val selected = tab == selectedTab
                val labelColor by animateColorAsState(
                    targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
                    label = "musicSegmentLabel",
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    indicator.animateTo(tab.ordinal, reducedMotion)
                                    if (tab != currentSelection) onSelection(tab)
                                },
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(tab.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** 搜索胶囊：占位随分段淡切，有输入时浮出清除键 */
@Composable
private fun MusicSearchPill(
    query: String,
    hint: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(ControlHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = Spacing.Large, end = Spacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Spacing.Small))
        AnimatedCursorTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            cursorColor = MaterialTheme.colorScheme.primary,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onImeAction = onSubmit,
            placeholder = {
                AnimatedContent(
                    targetState = hint,
                    transitionSpec = {
                        fadeIn(tween(durationMillis = 180, easing = EaseOutEmphasized)) togetherWith
                            fadeOut(tween(durationMillis = 120))
                    },
                    label = "musicSearchHint",
                ) { text ->
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            },
        )
        AnimatedContent(
            targetState = query.isNotBlank(),
            transitionSpec = { materialSharedAxisZ(forward = true) },
            label = "musicSearchClear",
        ) { hasKeyword ->
            if (hasKeyword) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

/** 排序按钮：非默认排序时图标转主题色 */
@Composable
private fun MusicSortButton(
    active: Boolean,
    onClick: (PopupAnchor) -> Unit,
    modifier: Modifier = Modifier,
) {
    var anchor by remember { mutableStateOf<PopupAnchor?>(null) }
    val tint by animateColorAsState(
        targetValue =
            if (active) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "musicSortTint",
    )
    val label = stringResource(R.string.music_sort_cd)
    Box(
        modifier =
            modifier
                .size(ControlHeight)
                .onGloballyPositioned { coords ->
                    val pos = coords.positionInWindow()
                    anchor =
                        PopupAnchor(
                            x = pos.x,
                            y = pos.y,
                            width = coords.size.width.toFloat(),
                            height = coords.size.height.toFloat(),
                        )
                }.clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickHighlight(
                    onClickLabel = label,
                    onClick = { anchor?.let(onClick) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Sort,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 通栏歌曲行：播放中标题转主题色、行尾换律动条 */
@Composable
private fun MusicSongRow(
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val highlight by animateColorAsState(
        targetValue = if (isPlaying) colors.primaryContainer.copy(alpha = 0.35f) else colors.primaryContainer.copy(alpha = 0.0f),
        label = "musicSongHighlight",
    )
    val titleColor by animateColorAsState(
        targetValue = if (isPlaying) colors.primary else colors.onSurface,
        label = "musicSongTitle",
    )
    Row(
        modifier =
            modifier
                .bleedHorizontal(LayoutTokens.MusicHeaderHorizontalPadding)
                .fillMaxWidth()
                .drawBehind { drawRect(highlight) }
                .combinedClickHighlight(
                    onClick = onClick,
                    onLongClick = onLongClick,
                ).padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            artwork = song.artwork(),
            modifier =
                Modifier
                    .size(RowCoverSize)
                    .clip(Shapes.MediumCornerBasedShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (
                    fadeIn(tween(durationMillis = 200, easing = EaseOutEmphasized)) +
                        scaleIn(
                            animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
                            initialScale = ScaleEnterFrom,
                        )
                ) togetherWith
                    (
                        fadeOut(tween(durationMillis = 120)) +
                            scaleOut(animationSpec = tween(durationMillis = 120), targetScale = ScaleExitTo)
                    )
            },
            label = "musicSongTrailing",
        ) { playing ->
            if (playing) {
                PlayingBarsIndicator(modifier = Modifier.widthIn(min = 36.dp))
            } else {
                Text(
                    text = song.getFormattedDuration(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.widthIn(min = 36.dp),
                )
            }
        }
    }
}

/** 专辑卡：方形封面 + 标题 + 「歌手 · N 首」 */
@Composable
private fun MusicAlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(bottom = Spacing.Medium)
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .clickHighlight(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        AudioCover(
            artwork = album.artwork(),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(Shapes.ExtraLargeCornerBasedShape),
        )
        Column(
            modifier = Modifier.padding(bottom = Spacing.Small),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = album.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${album.artist} · ${stringResource(R.string.songs_count_format, album.numberOfSongs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 通栏歌手行：圆形封面 + 名称 + 歌曲数 */
@Composable
private fun MusicArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .bleedHorizontal(LayoutTokens.MusicHeaderHorizontalPadding)
                .fillMaxWidth()
                .clickHighlight(onClick = onClick)
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            artwork = artist.artwork(),
            modifier =
                Modifier
                    .size(RowCoverSize)
                    .clip(CircleShape),
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
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 播放中指示：三根错相起伏的竖条，降级动效时静止 */
@Composable
private fun PlayingBarsIndicator(modifier: Modifier = Modifier) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "musicPlayingBars")
    val barHeights =
        List(3) { i ->
            transition.animateFloat(
                initialValue = 4f,
                targetValue = 14f,
                animationSpec =
                    infiniteRepeatable(
                        animation =
                            tween(
                                durationMillis = 420 + i * 130,
                                easing = FastOutSlowInEasing,
                            ),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "bar$i",
            )
        }
    val staticHeights = listOf(8f, 13f, 6f)
    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
        verticalAlignment = Alignment.Bottom,
    ) {
        barHeights.forEachIndexed { index, heightAnim ->
            val barHeight = if (reducedMotion) staticHeights[index] else heightAnim.value
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** 空态：浮动音符 + 文案，可选引导药丸 */
@Composable
private fun MusicEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val reducedMotion = LocalReducedMotion.current
    val floatTransition = rememberInfiniteTransition(label = "musicEmptyFloat")
    val bob by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "musicEmptyBob",
    )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(top = Spacing.ExtraLarge, bottom = Spacing.Huge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .graphicsLayer {
                        if (!reducedMotion) {
                            translationY = bob * 5.dp.toPx()
                            rotationZ = bob * 6f
                        }
                    }.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier =
                    Modifier
                        .padding(top = Spacing.ExtraSmall)
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
            song.artist.contains(normalized, ignoreCase = true) ||
            song.album.contains(normalized, ignoreCase = true)
    }
}

private fun List<Album>.filterAlbumsBy(query: String): List<Album> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { album ->
        album.title.contains(normalized, ignoreCase = true) ||
            album.artist.contains(normalized, ignoreCase = true)
    }
}

private fun List<Artist>.filterArtistsBy(query: String): List<Artist> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return this
    return filter { artist ->
        artist.name.contains(normalized, ignoreCase = true)
    }
}
