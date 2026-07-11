@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.home.page

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Search
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
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.artistdetail.ArtistDetailScene
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel
import java.util.concurrent.TimeUnit

private val MastheadCollapseDistance = 140.dp

private const val ENTRANCE_STAGGER_MILLIS = 55L

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
    var playEntrance by remember { mutableStateOf(true) }
    var playlistEntrance by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        if (playEntrance) {
            delay(1400)
            playEntrance = false
        }
    }

    LaunchedEffect(playlistEntrance) {
        if (playlistEntrance) {
            delay(55)
            playlistEntrance = false
        }
    }

    val filteredSongs =
        remember(allSongs, searchQuery) {
            allSongs
                .filterSongsBy(searchQuery)
                .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
        }
    val filteredAlbums =
        remember(albums, searchQuery) {
            albums.filterAlbumsBy(searchQuery)
        }
    val filteredArtists =
        remember(artists, searchQuery) {
            artists.filterArtistsBy(searchQuery)
        }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
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
                                val enter = entrance.value
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = (1f - t) * enter
                                translationY = -t * 16.dp.toPx() + (1f - enter) * 28.dp.toPx()
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
                                    tween(
                                        durationMillis = 240,
                                        easing = FastOutSlowInEasing,
                                    ),
                                placementSpec = null,
                                fadeOutSpec = tween(durationMillis = 160),
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
                                    tween(
                                        durationMillis = 240,
                                        easing = FastOutSlowInEasing,
                                    ),
                                placementSpec = null,
                                fadeOutSpec = tween(durationMillis = 160),
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
                    modifier =
                        Modifier.animateItem(
                            fadeInSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                            placementSpec =
                                spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                ),
                            fadeOutSpec = tween(durationMillis = 160),
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
                                                tween(
                                                    durationMillis = 240,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = tween(durationMillis = 160),
                                        ).graphicsLayer {
                                            val enter = entrance.value
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = (1f - enter) * 28.dp.toPx()
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
                            MusicAlbumRow(
                                album = album,
                                onClick = { path.push(AlbumDetailScene(album)) },
                                modifier =
                                    Modifier
                                        .animateItem(
                                            fadeInSpec =
                                                tween(
                                                    durationMillis = 240,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = tween(durationMillis = 160),
                                        ).graphicsLayer {
                                            val enter = entrance.value
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = (1f - enter) * 28.dp.toPx()
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
                                                tween(
                                                    durationMillis = 240,
                                                    easing = FastOutSlowInEasing,
                                                ),
                                            placementSpec =
                                                spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                                ),
                                            fadeOutSpec = tween(durationMillis = 160),
                                        ).graphicsLayer {
                                            val enter = entrance.value
                                            transformOrigin = TransformOrigin(0f, 0f)
                                            alpha = enter
                                            translationY = (1f - enter) * 28.dp.toPx()
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
private fun rememberEntrance(
    order: Int,
    play: Boolean,
): Animatable<Float, AnimationVector1D> {
    val entrance = remember { Animatable(if (play) 0f else 1f) }
    Log.e("yangweizhi", "rememberEntrance: $order, play: $play, entrance: ${entrance.value}")
    LaunchedEffect(Unit) {
        if (entrance.value < 1f) {
            delay(order * ENTRANCE_STAGGER_MILLIS)
            entrance.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = 380f,
                    ),
            )
        }
    }
    return entrance
}

private fun Modifier.entranceGraphics(entrance: Animatable<Float, AnimationVector1D>): Modifier =
    graphicsLayer {
        val enter = entrance.value
        alpha = enter
        translationY = (1f - enter) * 28.dp.toPx()
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
                        .graphicsLayer { alpha = mastheadCollapse(listState) },
            )
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
        Icon(
            imageVector = tab.icon,
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
private fun MusicSongRow(
    index: Int,
    song: Song,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                ).clickHighlight(onClick = onClick)
                .padding(Spacing.Small),
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
