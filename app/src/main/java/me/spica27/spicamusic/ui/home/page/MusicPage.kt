package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.common.collect.ImmutableList
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.geometry.GeometryTransition
import me.spica27.navkit.geometry.geometrySource
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.album.AlbumViewModel
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.artist.ArtistViewModel
import me.spica27.spicamusic.ui.artistdetail.ArtistDetailScene
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.AnimateOnEnter
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max

@Composable
fun MusicPage() {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val allSongs by homeViewModel.allSongs.collectAsStateWithLifecycle()
    val density = LocalDensity.current

    val songCount = allSongs.size
    val albumCount = remember(allSongs) { allSongs.distinctBy { it.albumId }.size }
    val artistCount = remember(allSongs) { allSongs.distinctBy { it.artist }.size }

    var selectTab by remember { mutableStateOf(MusicTab.SONG) }

    val tabs =
        remember {
            MusicTab.entries
        }
    val allSongListState = rememberLazyListState()
    val albumGridState = rememberLazyGridState()
    val artistListState = rememberLazyListState()

    val pagerState =
        rememberPagerState {
            tabs.size
        }

    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(selectTab) {
        val index = tabs.indexOf(selectTab)
        if (index != pagerState.targetPage) {
            allSongListState.requestScrollToItem(0)
            albumGridState.requestScrollToItem(0)
            artistListState.requestScrollToItem(0)
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.targetPage) {
        val page = tabs[pagerState.targetPage]
        if (page != selectTab) {
            selectTab = page
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    val summaryText = stringResource(R.string.music_summary_format, songCount, albumCount, artistCount)
    val songCountText = stringResource(R.string.music_tab_songs_count, songCount)
    val albumCountText = stringResource(R.string.music_tab_albums_count, albumCount)
    val artistCountText = stringResource(R.string.music_tab_artists_count, artistCount)
    val headerFollowDistancePx = with(density) { LayoutTokens.PageHeaderFollowDistance.toPx() }
    val headerProgress by remember(headerFollowDistancePx) {
        derivedStateOf {
            when (selectTab) {
                MusicTab.SONG -> headerFollowProgress(allSongListState, headerFollowDistancePx)
                MusicTab.ALBUM -> headerFollowProgress(albumGridState, headerFollowDistancePx)
                MusicTab.ARTIST -> headerFollowProgress(artistListState, headerFollowDistancePx)
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        MusicPageHeader(
            summaryText = summaryText,
            tabs = ImmutableList.copyOf(tabs),
            selectTab = selectTab,
            progress = headerProgress,
            onSelectTab = { selectTab = it },
            extraText = { tab ->
                when (tab) {
                    MusicTab.SONG -> if (songCount > 0) songCountText else null
                    MusicTab.ALBUM -> if (albumCount > 0) albumCountText else null
                    MusicTab.ARTIST -> if (artistCount > 0) artistCountText else null
                }
            },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) {
            val page = tabs[it]
            when (page) {
                MusicTab.SONG -> AllSongPage(Modifier, allSongListState, allSongs)
                MusicTab.ALBUM -> AlbumPage(Modifier, albumGridState)
                MusicTab.ARTIST -> ArtistsPage(Modifier, artistListState)
            }
        }
    }
}

@Composable
private fun MusicPageHeader(
    modifier: Modifier = Modifier,
    summaryText: String,
    tabs: ImmutableList<MusicTab>,
    selectTab: MusicTab,
    progress: Float,
    onSelectTab: (MusicTab) -> Unit,
    extraText: (MusicTab) -> String? = { null },
) {
    val heroAlpha = (1f - progress * 1.35f).coerceIn(0f, 1f)
    val backgroundColor =
        animateColorAsState(
            if (progress < 1f) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        )

    val tabPositions = remember { mutableStateMapOf<MusicTab, Dp>() }
    val tabWidths = remember { mutableStateMapOf<MusicTab, Dp>() }
    val tabHeight = remember { mutableStateMapOf<MusicTab, Dp>() }

    val indicatorOffset by animateDpAsState(
        targetValue = tabPositions.getOrElse(selectTab) { 0.dp },
        label = "",
    )
    val indicatorWidth by animateDpAsState(
        targetValue = tabWidths.getOrElse(selectTab) { 0.dp },
        label = "",
    )
    val indicatorHeight by animateDpAsState(
        targetValue = tabHeight.getOrElse(selectTab) { 0.dp },
        label = "",
    )

    val indicatorColor =
        animateColorAsState(
            targetValue = if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "",
        ).value

    val density = LocalDensity.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    backgroundColor.value,
                ).statusBarsPadding()
                .padding(
                    bottom = 12.dp,
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                    .padding(top = LayoutTokens.MusicHeaderTopPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Column(
                modifier =
                    Modifier
                        .animateContentSize()
                        .followHeaderSection(progress)
                        .graphicsLayer {
                            alpha = heroAlpha
                            translationY = -28f * progress
                        },
                verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.music_page_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(Shapes.ExtraLarge1CornerBasedShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .animateContentSize()
                        .padding(LayoutTokens.MusicTabContainerPadding)
                        .drawWithCache {
                            onDrawWithContent {
                                drawRoundRect(
                                    color = indicatorColor,
                                    topLeft = Offset(indicatorOffset.toPx(), 0f),
                                    size = Size(indicatorWidth.toPx(), indicatorHeight.toPx()),
                                    cornerRadius =
                                        CornerRadius(
                                            12.dp.toPx(),
                                            12.dp.toPx(),
                                        ),
                                )
                                drawContent()
                            }
                        },
                horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                tabs.forEach { tab ->
                    TopTabItem(
                        modifier =
                            Modifier
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInParent()
                                    val size = coordinates.size.toSize()
                                    tabPositions[tab] = with(density) { position.x.toDp() }
                                    tabWidths[tab] = with(density) { size.width.toDp() }
                                    tabHeight[tab] = with(density) { size.height.toDp() }
                                }.weight(1f),
                        progress = progress,
                        selectTab = selectTab,
                        onSelectTab = onSelectTab,
                        bandTab = tab,
                        extraText = extraText(tab),
                    )
                }
            }
        }
    }
}

@Immutable
private enum class MusicTab(
    val titleRes: Int,
) {
    SONG(R.string.music_tab_songs),
    ALBUM(R.string.music_tab_albums),
    ARTIST(R.string.music_tab_artists),
}

@Composable
private fun TopTabItem(
    modifier: Modifier = Modifier,
    progress: Float,
    selectTab: MusicTab,
    onSelectTab: (MusicTab) -> Unit,
    extraText: String? = null,
    bandTab: MusicTab,
) {
    val isSelected =
        remember(bandTab, selectTab) {
            selectTab == bandTab
        }

    val textColor =
        animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ).value

    val secondaryTextColor =
        animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ).value

    Column(
        modifier =
            modifier
                .animateContentSize()
                .clickable {
                    onSelectTab(bandTab)
                }.padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(bandTab.titleRes),
            color = textColor,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (progress < 0.5f && extraText != null) {
            Text(
                text = extraText,
                color = secondaryTextColor,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AllSongPage(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    allSongs: List<Song>,
) {
    val path = LocalNavigationPath.current
    val playerViewMode = LocalPlayerViewModel.current

    val playlistIds = remember(allSongs) { allSongs.map { it.mediaStoreId.toString() } }

    Column(modifier) {
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(LocalBottomBarScrollConnection.current),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            items(allSongs, key = { it.mediaStoreId }, contentType = { 1 }) { song ->
                AnimateOnEnter(
                    delayMillis = 150,
                    animationSpec =
                        tween(
                            durationMillis = 300,
                            easing = EaseInOutCubic,
                        ),
                ) { progress, _ ->
                    SongItem(
                        menuClick = {
                            path.push(SongMenuScene(song))
                        },
                        modifier =
                            Modifier
                                .graphicsLayer {
                                    translationY = (1 - progress) * 20f
                                    scaleX = 0.75f + 0.25f * progress
                                    scaleY = 0.75f + 0.25f * progress
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }.animateItem()
                                .clickable {
                                    playerViewMode.updatePlaylist(
                                        playlistIds,
                                        song.mediaStoreId.toString(),
                                        autoStart = true,
                                    )
                                }.fillMaxWidth(),
                        song = song,
                    )
                }
            }
        }
    }
}

@Composable
private fun SongItem(
    modifier: Modifier = Modifier,
    song: Song,
    menuClick: () -> Unit,
) {
    val coverUri =
        remember(song) {
            song.getCoverUri()
        }

    val hexagon =
        remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(0.2f),
            )
        }
    val clip =
        remember(hexagon) {
            RoundedPolygonShape(polygon = hexagon)
        }

    Row(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LandscapistImage(
            imageModel = {
                coverUri
            },
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(clip)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
            failure = {
                ShowOnIdleContent(true, delayMillis = 225) {
                    Image(
                        contentScale = ContentScale.Crop,
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .fillMaxSize(),
                    )
                }
            },
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(
            onClick = {
                menuClick.invoke()
            },
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun AlbumPage(
    modifier: Modifier = Modifier,
    gridState: LazyGridState,
) {
    val path = LocalNavigationPath.current
    val viewModel: AlbumViewModel = koinViewModel()
    val albums = viewModel.filteredAlbums.collectAsLazyPagingItems()

    LazyVerticalGrid(
        state = gridState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(LocalBottomBarScrollConnection.current),
        columns = GridCells.Fixed(2),
        contentPadding =
            PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 200.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            count = albums.itemCount,
            key = albums.itemKey { it.id },
        ) { index ->
            val album = albums[index] ?: return@items
            AlbumGridItem(
                album = album,
                onClick = { transition ->
                    path.push(AlbumDetailScene(album, transition))
                },
                modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun AlbumGridItem(
    album: Album,
    onClick: (GeometryTransition) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 每个专辑维护独立的 GeometryTransition，geometrySource 持续记录封面在屏幕中的位置
    val transition = remember(album.id) { GeometryTransition("album_cover_${album.id}") }
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { onClick(transition) }
                .padding(bottom = 12.dp),
    ) {
        LandscapistImage(
            imageModel = { album.getCoverUri() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    // 持续记录此封面的屏幕坐标，作为飞行动画的起始位置
                    .geometrySource(transition)
                    // 正向动画起始的几个百分点继续保留源封面，避免 overlay 首次接管时闪空
                    .graphicsLayer {
                        alpha = if (transition.shouldShowSource()) 1f else 0f
                    },
            failure = {
                Box(
                    Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.title,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = album.artist,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistsPage(
    modifier: Modifier = Modifier,
    listState: LazyListState,
) {
    val viewModel: ArtistViewModel = koinViewModel()
    val artists = viewModel.filteredArtists.collectAsLazyPagingItems()
    val path = LocalNavigationPath.current

    LazyColumn(
        state = listState,
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(LocalBottomBarScrollConnection.current),
        contentPadding = PaddingValues(bottom = 200.dp),
    ) {
        items(
            count = artists.itemCount,
            key = artists.itemKey { it.name },
        ) { index ->
            val artist = artists[index] ?: return@items
            ArtistRow(
                artist = artist,
                modifier = modifier.animateItem(),
                onClick = { path.push(ArtistDetailScene(artist)) },
            )
        }
    }
}

fun Modifier.followHeaderSection(progress: Float): Modifier =
    this.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val collapseProgress = 1f - (1f - progress) * (1f - progress)
        val reportedHeight = (placeable.height * (1f - collapseProgress)).toInt().coerceAtLeast(0)
        layout(placeable.width, reportedHeight) {
            placeable.place(0, 0)
        }
    }

fun headerFollowProgress(
    state: LazyListState,
    collapseDistancePx: Float,
): Float =
    if (state.firstVisibleItemIndex > 0) {
        1f
    } else {
        (state.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
    }

fun headerFollowProgress(
    state: LazyGridState,
    collapseDistancePx: Float,
): Float =
    if (state.firstVisibleItemIndex > 0) {
        1f
    } else {
        (state.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
    }

@Composable
private fun ArtistRow(
    artist: Artist,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LandscapistImage(
            imageModel = { artist.getCoverUri() },
            modifier =
                Modifier
                    .size(52.dp)
                    .clip(MaterialTheme.shapes.medium),
            failure = {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W600,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.songs_count, artist.songCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun RoundedPolygon.getBounds() = calculateBounds().let { Rect(it[0], it[1], it[2], it[3]) }

class RoundedPolygonShape(
    private val polygon: RoundedPolygon,
    private var matrix: Matrix = Matrix(),
) : Shape {
    private var path = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        path.rewind()
        path = polygon.toPath().asComposePath()
        matrix.reset()
        val bounds = polygon.getBounds()
        val maxDimension = max(bounds.width, bounds.height)
        matrix.scale(size.width / maxDimension, size.height / maxDimension)
        matrix.translate(-bounds.left, -bounds.top)
        matrix.rotateZ(45f)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
