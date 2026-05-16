package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Artist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.album.AlbumViewModel
import me.spica27.spicamusic.ui.albumdetail.AlbumDetailScene
import me.spica27.spicamusic.ui.artist.ArtistViewModel
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.player.LocalPlayerViewModel
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.widget.AnimateOnEnter
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import org.koin.compose.viewmodel.koinActivityViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPage() {
    val path = LocalNavigationPath.current

    var selectTab by remember { mutableStateOf(MusicTab.SONG) }

    val tabs =
        remember {
            MusicTab.entries
        }

    val pagerState =
        rememberPagerState {
            tabs.size
        }

    LaunchedEffect(selectTab) {
        val index = tabs.indexOf(selectTab)
        if (index != pagerState.targetPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.targetPage) {
        val page = tabs[pagerState.targetPage]
        if (page != selectTab) {
            selectTab = page
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Music")
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                ),
        ) {
            TopTabs(
                modifier = Modifier,
                tabs = tabs,
                selectTab = selectTab,
                onSelectTab = {
                    selectTab = it
                },
            )

            HorizontalPager(
                state = pagerState,
            ) {
                val page = tabs[it]
                when (page) {
                    MusicTab.SONG -> AllSongPage(Modifier)
                    MusicTab.ALBUM -> AlbumPage(Modifier)
                    MusicTab.ARTIST -> ArtistsPage(Modifier)
                }
            }
        }
    }
}

@Composable
private fun TopTabs(
    modifier: Modifier = Modifier,
    tabs: List<MusicTab>,
    selectTab: MusicTab,
    onSelectTab: (MusicTab) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tabs.forEach { tab ->
            TopTabItem(
                selectTab = selectTab,
                onSelectTab = onSelectTab,
                bandTab = tab,
            )
        }
    }
}

@Immutable
private enum class MusicTab(
    val title: String,
) {
    SONG("全部歌曲"),
    ALBUM("专辑"),
    ARTIST("歌手"),
}

@Composable
private fun TopTabItem(
    modifier: Modifier = Modifier,
    selectTab: MusicTab,
    onSelectTab: (MusicTab) -> Unit,
    extraText: String? = "1000首",
    bandTab: MusicTab,
) {
    val isSelected =
        remember(bandTab, selectTab) {
            selectTab == bandTab
        }

    val indicatorColor =
        animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ).value

    val textColor =
        animateColorAsState(
            if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ).value

    Row(
        modifier =
            modifier
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    CircleShape,
                ).clickable {
                    onSelectTab(bandTab)
                },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = bandTab.title,
            modifier =
                Modifier
                    .background(indicatorColor, CircleShape)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            color = textColor,
        )
        AnimatedVisibility(isSelected) {
            Text(
                text = extraText ?: "",
                modifier =
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun AllSongPage(modifier: Modifier = Modifier) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val allSong = homeViewModel.allSongs.collectAsStateWithLifecycle().value
    val density = LocalDensity.current
    val lazyListState = rememberLazyListState()

    val path = LocalNavigationPath.current
    val playerViewMode = LocalPlayerViewModel.current

    val collapseDistancePx = with(density) { 72.dp.toPx() }
    val rawProgress by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) {
                1f
            } else {
                // quadratic ease-in: header lingers at top then collapses quickly
                val x = (lazyListState.firstVisibleItemScrollOffset / collapseDistancePx).coerceIn(0f, 1f)
                1f - (1f - x) * (1f - x)
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "music_header_progress",
    )

    Column(modifier) {
        // Collapsible header: Modifier.layout reports the collapsed height to the Column so
        // the LazyColumn below naturally moves up, while graphicsLayer handles visual transforms
        // in the Draw phase without triggering extra recomposition.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val reportedH = (placeable.height * (1f - animatedProgress)).toInt().coerceAtLeast(0)
                        layout(placeable.width, reportedH) {
                            placeable.place(0, 0)
                        }
                    }.clipToBounds()
                    .graphicsLayer {
                        alpha = 1f - animatedProgress * 0.92f
                        translationY = -size.height * animatedProgress * 0.3f
                        scaleX = 1f - animatedProgress * 0.03f
                        scaleY = 1f - animatedProgress * 0.05f
                    }.background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.01f),
                            ),
                        ),
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 播放全部按钮
            ElevatedButton(onClick = { }) {
                Text(text = "播放全部")
            }

            // 排序方式切换按钮
            IconButton(onClick = {}) {
                Image(Icons.Default.Sort, contentDescription = null)
            }
        }

        LazyColumn(
            state = lazyListState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(LocalBottomBarScrollConnection.current),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            items(allSong, key = { it.mediaStoreId }) { song ->
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
                                        allSong.map { it.mediaStoreId.toString() },
                                        song.mediaStoreId.toString(),
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
            modifier = Modifier.size(64.dp),
            success = { _, painter ->
                ShowOnIdleContent(true) {
                    Image(
                        contentScale = ContentScale.Crop,
                        painter = painter,
                        contentDescription = null,
                        modifier =
                            Modifier
                                .clip(clip)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                ).fillMaxSize(),
                    )
                }
            },
            failure = {
                ShowOnIdleContent(true) {
                    Image(
                        contentScale = ContentScale.Crop,
                        painter = painterResource(R.drawable.default_cover),
                        contentDescription = null,
                        modifier =
                            Modifier
                                .clip(clip)
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                ).fillMaxSize(),
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
            Image(
                Icons.Default.MoreVert,
                contentDescription = null,
            )
        }
    }
}

@Composable
fun AlbumPage(modifier: Modifier = Modifier) {
    val path = LocalNavigationPath.current
    val viewModel: AlbumViewModel = koinViewModel()
    val albums = viewModel.filteredAlbums.collectAsLazyPagingItems()

    LazyVerticalGrid(
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
                onClick = { path.push(AlbumDetailScene(album)) },
            )
        }
    }
}

@Composable
private fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClick = onClick)
                .padding(bottom = 12.dp),
    ) {
        LandscapistImage(
            imageModel = { album.getCoverUri() },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium),
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
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
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.title,
            modifier = Modifier.padding(horizontal = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
fun ArtistsPage(modifier: Modifier = Modifier) {
    val viewModel: ArtistViewModel = koinViewModel()
    val artists = viewModel.filteredArtists.collectAsLazyPagingItems()

    LazyColumn(
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
            ArtistRow(artist = artist)
        }
    }
}

@Composable
private fun ArtistRow(
    artist: Artist,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = {})
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
            success = { _, painter ->
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            },
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
            )
            Text(
                text = "${artist.songCount} 首歌曲",
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
