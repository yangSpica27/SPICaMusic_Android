@file:OptIn(ExperimentalMaterial3Api::class)

package me.spica27.spicamusic.ui.main.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.toCoilUri
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import me.spica27.spicamusic.utils.pressable
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.viewModel.SongViewModel
import me.spica27.spicamusic.widget.InputTextDialog
import me.spica27.spicamusic.widget.PlaylistItem
import me.spica27.spicamusic.widget.SongItemWithCover
import me.spica27.spicamusic.widget.blur.progressiveBlur
import me.spica27.spicamusic.widget.materialSharedAxisXIn
import me.spica27.spicamusic.widget.materialSharedAxisXOut
import me.spica27.spicamusic.wrapper.activityViewModel

// 主页
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    modifier: Modifier = Modifier,
    songViewModel: SongViewModel = activityViewModel(),
    navigator: NavController = LocalNavController.current,
    listState: ScrollState = rememberScrollState(),
    pagerState: PagerState,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
) {
    val oftenListenSongs = songViewModel.oftenListenSongs10.collectAsStateWithLifecycle()

    val playlist = songViewModel.allPlayList.collectAsStateWithLifecycle().value

    val randomSong = songViewModel.randomSongs.collectAsState().value

    val showCreatePlaylistDialog = remember { mutableStateOf(false) }

    val surfaceBlur =
        animateFloatAsState(
            targetValue =
                if (showCreatePlaylistDialog.value) {
                    12f
                } else {
                    0f
                },
        )

    if (showCreatePlaylistDialog.value) {
        InputTextDialog(
            onDismissRequest = {
                showCreatePlaylistDialog.value = false
            },
            title = stringResource(R.string.create_playlist),
            onConfirm = {
                songViewModel.addPlayList(it)
                showCreatePlaylistDialog.value = false
            },
        )
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .blur(
                    surfaceBlur.value.dp,
                    BlurredEdgeTreatment.Unbounded,
                ),
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            state = listState,
                        ).padding(bottom = 64.dp, top = 64.dp)
                        .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 16.dp,
                            ).fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                shape = MaterialTheme.shapes.small,
                            ).clip(MaterialTheme.shapes.small)
                            .clickable {
                                navigator.navigate(Routes.SearchAll)
                            }.padding(
                                horizontal = 16.dp,
                                vertical = 10.dp,
                            ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.more),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Text(
                        stringResource(R.string.search_all_music),
                        style = MaterialTheme.typography.bodyLarge.copy(),
                    )
                }

                Title(
                    stringResource(R.string.recently_listen),
                    right = {
                        Text(
                            stringResource(R.string.see_more),
                            modifier =
                                Modifier.clickableNoRippleWithVibration {
                                    navigator.navigate(Routes.RecentlyList)
                                },
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                                    fontWeight = FontWeight.W600,
                                    fontSize = 15.sp,
                                ),
                        )
                    },
                )
                AnimatedContent(
                    targetState = oftenListenSongs.value,
                    modifier = Modifier.fillMaxWidth(),
                    transitionSpec = {
                        materialSharedAxisXIn(true) togetherWith materialSharedAxisXOut(true)
                    },
                    contentKey = {
                        it.isEmpty()
                    },
                ) { songs ->
                    if (songs.isEmpty()) {
                        OftenListenEmptyContent()
                    } else {
                        OftenListenSongList(songs = songs)
                    }
                }
                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.05f,
                        ),
                    thickness = 2.dp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Title(
                    stringResource(R.string.title_playlist),
                    {
                        Text(
                            stringResource(R.string.create_playlist),
                            modifier =
                                Modifier.clickableNoRippleWithVibration {
                                    showCreatePlaylistDialog.value = true
                                },
                            style =
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = .6f),
                                    fontWeight = FontWeight.W600,
                                    fontSize = 15.sp,
                                ),
                        )
                    },
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PlaylistItem(
                        playlist =
                            Playlist(
                                playlistId = 0,
                                playlistName = App.getInstance().getString(R.string.my_favorites),
                                cover = null,
                                needUpdate = false,
                            ),
                        onClick = {
                            navigator.navigate(Routes.LikeList)
                        },
                        showMenu = false,
                    )

                    playlist.forEach {
                        PlaylistItem(
                            playlist = it,
                            onClick = {
                                navigator.navigate(
                                    Routes.PlaylistDetail(
                                        it.playlistId ?: -1,
                                    ),
                                )
                            },
                            showMenu = false,
                        )
                    }
                }
                HorizontalDivider(
                    color =
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.05f,
                        ),
                    thickness = 2.dp,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                Title(
                    stringResource(R.string.title_recommended),
                )

                if (randomSong.isEmpty()) {
                    Box(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                            ),
                    ) {
                        Text(
                            stringResource(R.string.no_recommendations),
                            style =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f),
                                    fontWeight = FontWeight.W600,
                                    fontSize = 20.sp,
                                ),
                        )
                    }
                } else {
                    Column {
                        randomSong.forEach {
                            SongItemWithCover(
                                song = it,
                                onClick = {
                                    playBackViewModel.play(it, randomSong)
                                },
                                showMenu = false,
                                showPlus = false,
                                showLike = false,
                            )
                        }
                    }
                }
            }
            // 标题
            TitleBar(pagerState = pagerState)
        }
    }
}

/**
 * 最近常听占位空
 */
@Composable
private fun OftenListenEmptyContent(
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavController.current
    Box(
        modifier =
            Modifier
                .then(modifier)
                .padding(
                    horizontal = 16.dp,
                ).width(120.dp)
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                    MaterialTheme.shapes.small,
                ).padding(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ).clip(MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.empty),
                style =
                    MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black,
                    ),
            )
            Spacer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .height(4.dp),
            )
            TextButton(
                onClick = {
                    navigator.navigate(Routes.Scanner)
                },
            ) {
                Text(stringResource(R.string.scan_local_music))
            }
        }
    }
}

/**
 * 最近常听列表
 */
@Composable
private fun OftenListenSongList(
    songs: List<Song> = emptyList(),
    playBackViewModel: PlayBackViewModel = activityViewModel(),
) {
    HorizontalMultiBrowseCarousel(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(180.dp),
        state =
            rememberCarouselState(
                initialItem = 0,
                itemCount = { songs.size },
            ),
        itemSpacing = 8.dp,
        preferredItemWidth = 150.dp,
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) { index ->
        val item = songs[index]
        val coverPainter =
            rememberAsyncImagePainter(
                model = item.getCoverUri().toCoilUri(),
            )
        val coverState = coverPainter.state.collectAsState().value

        Box(
            modifier =
                Modifier
                    .width(150.dp)
                    .height(180.dp)
                    .background(
                        MaterialTheme.colorScheme.surface,
                    ).clip(
                        MaterialTheme.shapes.small,
                    ).pressable()
                    .clickableNoRippleWithVibration {
                        playBackViewModel.play(item, songs)
                    },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainer,
                            MaterialTheme.shapes.small,
                        ).clip(MaterialTheme.shapes.small)
                        .progressiveBlur(),
            ) {
                if (coverState is AsyncImagePainter.State.Success) {
                    AsyncImage(
                        model = item.getCoverUri().toCoilUri(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(
                        modifier = Modifier.rotate(45f),
                        text = item.displayName,
                        style =
                            MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontWeight = FontWeight.W900,
                            ),
                    )
                }
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        ),
                                ),
                        ).padding(
                            horizontal = 16.dp,
                            vertical = 12.dp,
                        ),
                verticalArrangement = Arrangement.Bottom,
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp),
                )
                Text(
                    text = item.displayName,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    style =
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                )
                Spacer(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                )
                Text(
                    text = item.artist,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1,
                    style =
                        MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Normal,
                            color =
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.6f,
                                ),
                        ),
                )
            }
        }
    }

//  LazyRow(
//    modifier =
//      Modifier
//        .fillMaxWidth()
//        .overScrollHorizontal(),
//    horizontalArrangement = Arrangement.spacedBy(12.dp),
//    state = listState,
//    flingBehavior =
//      rememberSnapFlingBehavior(
//        lazyListState = listState,
//        snapPosition = SnapPosition.Center,
//      ),
//  ) {
//    item {
//      Spacer(
//        modifier =
//          Modifier
//            .width(4.dp),
//      )
//    }
//    items(songs, key = {
//      it.songId?.toString() ?: UUID.randomUUID().toString()
//    }) {
//
//    }
//    item {
//      Spacer(
//        modifier =
//          Modifier
//            .width(16.dp),
//      )
//    }
//  }
}

// 标题
@Composable
private fun Title(
    title: String,
    right: @Composable () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style =
                MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.W600,
                    fontSize = 20.sp,
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
        right()
    }
}

/**
 * 顶部标题栏
 */
@Composable
private fun TitleBar(pagerState: PagerState) {
    val coroutineScope = rememberCoroutineScope()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_page_title),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                ),
            modifier = Modifier.align(Alignment.Center),
        )

        IconButton(
            onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
