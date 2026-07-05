package me.spica27.spicamusic.ui.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.dialog.SongMenuScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.combinedClickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen() {
    val path = LocalNavigationPath.current
    val viewModel: FavoriteViewModel = koinViewModel()
    val songs = viewModel.favoriteSongs.collectAsLazyPagingItems()
    val searchKeyword by viewModel.searchKeyword.collectAsStateWithLifecycle()
    val songCount by viewModel.songCount.collectAsStateWithLifecycle()

    BackHandler { path.popTop() }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topOffset by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f else (listState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
        }
    }
    val headerAlpha by animateFloatAsState(
        targetValue = 1f - topOffset * 0.32f,
        animationSpec = spring(stiffness = 420f),
        label = "favoritesHeaderAlpha",
    )
    val headerShift by animateFloatAsState(
        targetValue = topOffset * -20f,
        animationSpec = spring(stiffness = 420f),
        label = "favoritesHeaderShift",
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.surfaceContainerLow,
                        0.42f to MaterialTheme.colorScheme.surface,
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
    ) {
        FavoriteBackdrop(statusBarTop = statusBarTop)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + 88.dp,
                    bottom = 200.dp,
                ),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            item(key = "favorites_hero") {
                FavoriteHeroCard(
                    songCount = songCount,
                    onPlayAll = { viewModel.playAllSongs() },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .graphicsLayer {
                                alpha = headerAlpha
                                translationY = headerShift
                            },
                )
            }

            item(key = "favorites_search") {
                FavoriteSearchField(
                    keyword = searchKeyword,
                    onKeywordChange = viewModel::updateSearchKeyword,
                    onClear = viewModel::clearSearch,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
                )
            }

            if (songs.loadState.refresh is LoadState.Loading) {
                item(key = "favorites_loading") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.Huge),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (songs.itemCount == 0) {
                item(key = "favorites_empty") {
                    FavoriteEmptyCard(
                        title =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.favorites_empty_title)
                            } else {
                                stringResource(R.string.favorites_search_empty)
                            },
                        subtitle =
                            if (searchKeyword.isBlank()) {
                                stringResource(R.string.favorites_empty_subtitle)
                            } else {
                                searchKeyword
                            },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
                    )
                }
            } else {
                item(key = "favorites_list_caption") {
                    FavoriteSectionHeader(
                        title = stringResource(R.string.favorites_subtitle_count, songCount),
                        modifier =
                            Modifier
                                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                                .padding(top = Spacing.ExtraSmall),
                    )
                }
                items(
                    count = songs.itemCount,
                    key = { index -> songs.peek(index)?.mediaStoreId ?: "favorite_placeholder_$index" },
                    contentType = { "favorite_song" },
                ) { index ->
                    val song = songs[index]
                    if (song != null) {
                        FavoriteSongRow(
                            song = song,
                            onClick = { viewModel.playAllSongs(song.mediaStoreId) },
                            onLongClick = { path.push(SongMenuScene(song)) },
                            onRemoveFavorite = { viewModel.toggleFavorite(song) },
                            onMore = { path.push(SongMenuScene(song)) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            item(key = "favorites_bottom_spacer") {
                Spacer(Modifier.height(Spacing.Huge))
            }
        }

        FavoriteTopBar(
            title = stringResource(R.string.favorites_title),
            alpha = topOffset,
            onBack = { path.popTop() },
            modifier = Modifier.align(Alignment.TopStart),
        )
    }
}

@Composable
private fun FavoriteBackdrop(statusBarTop: Dp) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + 360.dp)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f),
                        0.36f to MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.18f),
                        0.72f to MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                        1f to Color.Transparent,
                    ),
                ),
    )
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(statusBarTop + 180.dp)
                .background(
                    Brush.horizontalGradient(
                        0f to MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        0.52f to Color.Transparent,
                        1f to MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                    ),
                ),
    )
}

@Composable
private fun FavoriteTopBar(
    title: String,
    alpha: Float,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 64.dp),
        color = MaterialTheme.colorScheme.background.copy(alpha = alpha),
        tonalElevation = 0.dp,
        border =
            if (alpha > 0.3f) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f))
            } else {
                null
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .padding(horizontal = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.graphicsLayer { this.alpha = alpha },
            )
        }
    }
}

@Composable
private fun FavoriteHeroCard(
    songCount: Int,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.ExtraLarge2CornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                                MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.46f),
                            ),
                        ),
                    ).padding(Spacing.ExtraLarge),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(Shapes.LargeCornerBasedShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.primaryContainer,
                                        ),
                                    ),
                                    shape = Shapes.LargeCornerBasedShape,
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                    ) {
                        Text(
                            text = stringResource(R.string.favorites_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.favorites_subtitle_count, songCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = Shapes.LargeCornerBasedShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.48f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        FluentCommandButton(
                            text = stringResource(R.string.favorites_play_all),
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            enabled = songCount > 0,
                            emphasized = true,
                            onClick = onPlayAll,
                        )
                        Spacer(Modifier.weight(1f))
                        FavoriteCountPill(songCount = songCount)
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCountPill(songCount: Int) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)),
    ) {
        Text(
            text = stringResource(R.string.favorites_subtitle_count, songCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        )
    }
}

@Composable
private fun FluentCommandButton(
    text: String,
    icon: @Composable () -> Unit,
    enabled: Boolean,
    emphasized: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (emphasized) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        }
    val content =
        if (emphasized) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Surface(
        modifier =
            modifier
                .clip(Shapes.MediumCornerBasedShape)
                .combinedClickHighlight(
                    enabled = enabled,
                    onClick = onClick,
                ),
        shape = Shapes.MediumCornerBasedShape,
        color = if (enabled) container else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.52f),
        border =
            if (emphasized) {
                null
            } else {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Box(
                modifier = Modifier.graphicsLayer { alpha = if (enabled) 1f else 0.42f },
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = content.copy(alpha = if (enabled) 1f else 0.42f),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteSearchField(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)),
    ) {
        TextField(
            value = keyword,
            onValueChange = onKeywordChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(stringResource(R.string.favorites_search_hint))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = {
                AnimatedContent(
                    targetState = keyword.isNotBlank(),
                    transitionSpec = { materialSharedAxisZ(forward = true) },
                    label = "favoriteSearchClear",
                ) { hasKeyword ->
                    if (hasKeyword) {
                        IconButton(onClick = onClear) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                            )
                        }
                    }
                }
            },
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
            shape = Shapes.LargeCornerBasedShape,
        )
    }
}

@Composable
private fun FavoriteSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f),
        )
    }
}

@Composable
private fun FavoriteSongRow(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var liked by remember(song.mediaStoreId) { mutableStateOf(true) }
    val favoriteScale by animateFloatAsState(
        targetValue = if (liked) 1f else 0.78f,
        animationSpec = spring(stiffness = 520f),
        label = "favoriteIconScale",
    )
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(Shapes.LargeCornerBasedShape)
                    .combinedClickHighlight(
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ).padding(horizontal = Spacing.Small, vertical = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            FavoriteSongCover(song = song)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    liked = false
                    onRemoveFavorite()
                },
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = favoriteScale
                        scaleY = favoriteScale
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.remove_from_favorites),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onMore) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 78.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f),
        )
    }
}

@Composable
private fun FavoriteSongCover(song: Song) {
    LandscapistImage(
        imageModel = { song.getCoverUri() },
        modifier =
            Modifier
                .size(58.dp)
                .clip(Shapes.LargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        success = { _, painter ->
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
        failure = {
            Image(
                painter = painterResource(R.drawable.default_cover),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun FavoriteEmptyCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.ExtraLarge1CornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.86f),
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f)),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
