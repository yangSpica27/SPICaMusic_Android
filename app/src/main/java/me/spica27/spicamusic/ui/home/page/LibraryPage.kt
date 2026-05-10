package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.ui.widget.highLightClickable
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryPage() {
    val path = LocalNavigationPath.current

    var selectTab by remember { mutableStateOf(LibraryPageTab.Playlist) }

    val tabs =
        remember {
            LibraryPageTab.entries
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Library")
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues),
        ) {
            TopTabBar(
                tabs = tabs,
                selectTab = selectTab,
                onSelectTab = { selectTab = it },
            )

            when (selectTab) {
                LibraryPageTab.Playlist -> PlaylistPage()
                LibraryPageTab.Folder -> PlaylistPage()
            }
        }
    }
}

@Composable
private fun TopTabBar(
    modifier: Modifier = Modifier,
    tabs: List<LibraryPageTab>,
    selectTab: LibraryPageTab,
    onSelectTab: (LibraryPageTab) -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        tabs.forEach { tab ->
            TopTabItem(
                selectTab = selectTab,
                onSelectTab = { onSelectTab(it) },
                bandTab = tab,
            )
        }
    }
}

@Immutable
enum class LibraryPageTab(
    val title: String,
) {
    Playlist("Playlist"),
    Folder("Folder"),
}

@Composable
private fun TopTabItem(
    modifier: Modifier = Modifier,
    selectTab: LibraryPageTab,
    onSelectTab: (LibraryPageTab) -> Unit,
    extraText: String? = "10个",
    bandTab: LibraryPageTab,
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
private fun PlaylistPage(modifier: Modifier = Modifier) {
    val colors =
        remember {
            listOf(
                Color(0xff08979c),
                Color(0xff389e0d),
                Color(0xff531dab),
            )
        }
    LazyVerticalGrid(
        modifier = modifier.fillMaxSize(),
        columns =
            GridCells
                .Fixed(2),
        contentPadding =
            PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
    ) {
        items(
            10,
            key = { it },
            span = { GridItemSpan(2) },
        ) { index ->
            PlaylistItem(
                modifier =
                    Modifier.fillMaxWidth(),
                Playlist(
                    playlistName = "Playlist ${index + 1}",
                ),
                colors[index % colors.size],
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    color: Color,
) {
    Box {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .shadow(4.dp, MaterialTheme.shapes.medium)
                    .background(
                        color,
                        MaterialTheme.shapes.medium,
                    ).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row {
                LandscapistImage(
                    imageModel = {
                        R.drawable.default_cover
                    },
                    modifier =
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .size(66.dp),
                    success = { state, painter ->
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    },
                )
                Column(
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp)
                            .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = playlist.playlistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                        color = Color.White,
                    )
                    Text(
                        text = "10 songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(.6f),
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
            Text(
                text = "50首歌曲 · 40分钟",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(.6f),
            )
            Row {
                Text(
                    text = "创建于 2024-06-01",
                    color = Color.White.copy(.6f),
                    fontWeight = FontWeight.W600,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        IconButton(
            onClick = { },
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black,
                        CircleShape,
                    ).innerShadow(
                        CircleShape,
                        Shadow(
                            color = Color.White.copy(.5f),
                            radius = 15.dp,
                        ),
                    ),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
