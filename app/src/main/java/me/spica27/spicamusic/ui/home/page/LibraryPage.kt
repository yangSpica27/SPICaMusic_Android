package me.spica27.spicamusic.ui.home.page

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.ui.home.LocalBottomBarScrollConnection
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

    val pagerState = rememberPagerState { tabs.size }

    LaunchedEffect(selectTab) {
        val index = tabs.indexOf(selectTab)
        if (index != pagerState.targetPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.targetPage) {
        val tab = tabs[pagerState.targetPage]
        if (tab != selectTab) {
            selectTab = tab
        }
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
                    .fillMaxSize()
                    .padding(
                        top = paddingValues.calculateTopPadding(),
                    ),
        ) {
            TopTabBar(
                tabs = tabs,
                selectTab = selectTab,
                onSelectTab = { selectTab = it },
            )
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Horizontal),
            ) {
                PlaylistPage(Modifier.fillMaxSize())
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
private fun PlaylistPage(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        modifier =
            modifier
                .fillMaxWidth()
                .nestedScroll(LocalBottomBarScrollConnection.current),
        columns =
            GridCells
                .Fixed(2),
        contentPadding =
            PaddingValues(
                bottom = 200.dp,
                start = 16.dp,
                end = 16.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
    ) {
        items(
            10,
            key = { it },
            span = { GridItemSpan(1) },
        ) { index ->
            PlaylistItem(
                modifier =
                    Modifier.fillMaxWidth().clickable {
                    },
                Playlist(
                    playlistName = "Playlist ${index + 1}",
                ),
            )
        }
    }
}

@Composable
private fun PlaylistItem(
    modifier: Modifier = Modifier,
    playlist: Playlist,
) {
    Column(
        modifier = modifier,
    ) {
        LandscapistImage(
            imageModel = {},
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer,
                    ),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.playlistName,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = "10 songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
