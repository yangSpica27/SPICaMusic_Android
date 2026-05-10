package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.widget.AnimateOnEnter
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.highLightClickable
import org.koin.compose.viewmodel.koinActivityViewModel
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
            pagerState.scrollToPage(index)
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
                Modifier
                    .padding(paddingValues),
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
                    MusicTab.SONG -> AllSongPage()
                    MusicTab.ALBUM -> AllSongPage()
                    MusicTab.ARTIST -> AllSongPage()
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
                ).highLightClickable {
                    onSelectTab(bandTab)
                },
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
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
    var headerHeight by remember {
        mutableIntStateOf(0)
    }
    val lazyListState = rememberLazyListState()

    val showHeader by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0
        }
    }

    Box(
        modifier,
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top =
                        with(density) {
                            headerHeight.toDp()
                        },
                ),
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
                        modifier =
                            Modifier
                                .graphicsLayer {
                                    translationY = (1 - progress) * 20f
                                    scaleX = 0.75f + 0.25f * progress
                                    scaleY = 0.75f + 0.25f * progress
                                    transformOrigin = TransformOrigin(0.5f, 0f)
                                }.animateItem()
                                .fillMaxWidth(),
                        song = song,
                    )
                }
            }
        }
        // Header
        AnimatedVisibility(
            showHeader,
            enter =
                expandIn(
                    initialSize = { IntSize(it.width, 0) },
                ) + fadeIn(),
            exit =
                fadeOut() +
                    shrinkOut {
                        IntSize(it.width, 0)
                    },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            headerHeight = it.size.height
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
                ElevatedButton(
                    onClick = { },
                ) {
                    Text(text = "播放全部")
                }

                // 排序方式切换按钮
                IconButton(
                    onClick = {},
                ) {
                    Image(
                        Icons.Default.Sort,
                        contentDescription = null,
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
                .highLightClickable {
                }.padding(horizontal = 16.dp, vertical = 8.dp),
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
            onClick = { /* TODO */ },
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
}

@Composable
fun ArtistsPage(modifier: Modifier = Modifier) {
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
