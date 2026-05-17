package me.spica27.spicamusic.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.widget.materialSharedAxisXIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
import org.koin.androidx.compose.koinViewModel

class SearchScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val searchViewModel = koinViewModel<SearchViewModel>()
        val searchKey = searchViewModel.searchKeyword.collectAsState().value
        val searchResult = searchViewModel.searchPagingResults.collectAsLazyPagingItems()
        Scaffold(
            topBar = {
                TextField(
                    searchKey,
                    onValueChange = { searchViewModel.updateSearchKeyword(it) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp),
                    placeholder = {
                        Text(
                            "根据关键词搜索歌曲",
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    colors =
                        TextFieldDefaults.colors().copy(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            errorContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    singleLine = true,
                    leadingIcon = {
                        Image(
                            Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            searchKey.isNotEmpty(),
                            enter = materialSharedAxisXIn(true),
                            exit = materialSharedAxisZOut(true),
                        ) {
                            IconButton(onClick = {
                                searchViewModel.clearSearch()
                            }) {
                                Image(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier.padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (searchKey.isEmpty()) {
                    item {
                        Empty()
                    }
                } else {
                    items(searchResult.itemCount) { index ->
                        val item = searchResult[index]
                        if (item != null) {
                            when (item) {
                                is SearchListItem.Header -> {
                                    Text(
                                        item.title,
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(
                                                    horizontal = 22.dp,
                                                    vertical = 8.dp,
                                                ),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }

                                is SearchListItem.SongItem -> {
                                    SongItem(
                                        modifier =
                                            Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 4.dp,
                                            ),
                                        song = item.song,
                                    )
                                }
                            }
                        }
                    }
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
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    vertical = 8.dp,
                    horizontal = 12.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LandscapistImage(
            imageModel = { song.getCoverUri() },
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
            )
            Text(
                "${song.album} - ${song.artist}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        IconButton(
            onClick = {},
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun Empty(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(top = 145.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Image(
            painterResource(R.drawable.load_error),
            contentDescription = null,
        )
    }
}
