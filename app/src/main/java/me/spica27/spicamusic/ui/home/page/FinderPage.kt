package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.Scene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.home.HomeViewModel
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
import me.spica27.spicamusic.ui.scan.ScannerScene
import me.spica27.spicamusic.ui.settings.SettingsScene
import me.spica27.spicamusic.ui.widget.AnimateOnEnter
import me.spica27.spicamusic.ui.widget.ShowOnIdleContent
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationStyleApi::class)
@Composable
fun FinderPage() {
    val path = LocalNavigationPath.current

    val scrollBehavior =
        androidx.compose.material3.TopAppBarDefaults
            .enterAlwaysScrollBehavior()

    val homeViewModel: HomeViewModel = koinActivityViewModel()

    val frequentSongs = homeViewModel.frequentSongs.collectAsStateWithLifecycle().value

    val shortcuts =
        remember {
            listOf(
                Shortcut(
                    title = "我的歌单",
                    icon = Icons.Default.PlaylistPlay,
                    scene = SettingsScene(),
                ),
                Shortcut(
                    title = "设置中心",
                    icon = Icons.Default.Settings,
                ),
                Shortcut(
                    title = "听歌统计",
                    icon = Icons.Default.Hexagon,
                ),
                Shortcut(
                    title = "音乐扫描",
                    icon = Icons.Default.Scanner,
                    scene = ScannerScene(),
                ),
            )
        }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("发现") },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyVerticalGrid(
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
            horizontalArrangement =
                Arrangement
                    .spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            columns =
                androidx.compose.foundation.lazy.grid.GridCells
                    .Fixed(2),
            overscrollEffect =
                rememberIOSOverScrollEffect(
                    orientation = Orientation.Vertical,
                ),
        ) {
            // ========= 快捷入口START =========
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Title("快捷入口")
                }
            }
            itemsIndexed(shortcuts, key = { index, shortcut ->
                "shortcut-$index-${shortcut.title}"
            }) { index, shortcut ->
                val delayMillis = remember { Random.nextInt(0, 280) }
                val durationMillis = remember { Random.nextInt(100, 700) }
                AnimateOnEnter(
                    delayMillis = delayMillis,
                    animationSpec =
                        tween(
                            durationMillis = durationMillis,
                            delayMillis = delayMillis,
                            easing = EaseOutCubic,
                        ),
                ) { progress, _ ->
                    ShortcutItem(
                        index = index,
                        modifier =
                            Modifier
                                .graphicsLayer {
                                    this.alpha = progress
                                    scaleX = 1.0f * progress
                                    scaleY = 1.0f * progress
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                }.fillMaxSize(),
                        title = shortcut.title,
                        scene = shortcut.scene,
                        icon = shortcut.icon,
                    )
                }
            }
            // ========= 快捷入口END =========

            // ========= 推荐歌单START =========
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Title("推荐歌曲")
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    overscrollEffect =
                        rememberIOSOverScrollEffect(
                            orientation = Orientation.Horizontal,
                        ),
                ) {
                    items(frequentSongs) { song ->
                        SongItem(song)
                    }
                }
            }

            // ========= 推荐歌单END =========

            // =========== 统计START ============

            // =========== 统计END ============
        }
    }
}

@Composable
private fun Title(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W600,
            ),
    )
}

@Composable
private fun ShortcutItem(
    index: Int,
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    scene: Scene? = null,
) {
    val path = LocalNavigationPath.current

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    end = ((index % 2) * 16).dp,
                    start = ((1 - index % 2) * 16).dp,
                ).height(80.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                ).clickable {
                    if (scene != null) {
                        path.push(scene)
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            Text(
                text = title,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            16.dp,
                        ),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterEnd)
                        .size(64.dp)
                        .rotate(45f)
                        .graphicsLayer {
                            translationX = 16.dp.toPx()
                            translationY = 10.dp.toPx()
                        }.background(
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primaryFixed,
                                        MaterialTheme.colorScheme.primaryFixedDim,
                                        MaterialTheme.colorScheme.inversePrimary,
                                    ),
                            ),
                            shape = MaterialTheme.shapes.extraSmall,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier =
                        Modifier
                            .size(32.dp),
                )
            }
        }
    }
}

@Composable
private fun SongItem(song: Song) {
    Column(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.medium)
                .width(160.dp)
                .clickable {
                },
    ) {
        Card(
            modifier =
                Modifier
                    .size(160.dp, 160.dp),
        ) {
            SongCover(song)
        }
        Spacer(
            modifier = Modifier.size(8.dp),
        )
        Text(
            song.displayName,
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
            maxLines = 1,
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
        )
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
        imageOptions =
            ImageOptions(
                requestSize = IntSize(200, 200),
            ),
        success = { state, painter ->
            ShowOnIdleContent(true) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        },
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
class Shortcut(
    val title: String,
    val icon: ImageVector,
    val scene: Scene? = null,
)
