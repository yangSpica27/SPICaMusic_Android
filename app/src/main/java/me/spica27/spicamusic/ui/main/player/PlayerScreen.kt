package me.spica27.spicamusic.ui.main.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    onBackClick: () -> Unit,
    navigator: NavController? = null,
) {
    val nowPlayingSongs =
        playBackViewModel.player.currentTimelineItems
            .collectAsState()
            .value

    val isPlaying = playBackViewModel.isPlaying.collectAsState(false).value

    val pageState = rememberPagerState(pageCount = { 2 })

    LaunchedEffect(nowPlayingSongs) {
        Timber.e("nowPlayingSongs: $nowPlayingSongs")
    }

    LaunchedEffect(isPlaying) {
        Timber.e("isPlaying: $isPlaying")
    }

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize(),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onBackClick.invoke()
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        if (isPlaying) {
                            "Now Playing"
                        } else {
                            "Now Pause"
                        },
                        style =
                            MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                fontWeight = FontWeight.ExtraBold,
                            ),
                    )
                },
            )
        },
    ) { paddingValues ->

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (nowPlayingSongs.isEmpty()) {
                // 播放列表为空
                EmptyPage(navigator)
            } else {
                // 播放列表不为空
                VerticalPager(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    state = pageState,
                    key = { it },
                    flingBehavior =
                        PagerDefaults.flingBehavior(state = pageState, snapPositionalThreshold = .2f),
                ) {
                    when (it) {
                        0 -> {
                            PlayerPage(
                                navigator = navigator,
                            )
                        }

                        1 -> {
                            CurrentListPage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPage(navigator: NavController? = null) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("没有播放中的音乐", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                navigator?.navigate(Routes.SearchAll)
            },
        ) {
            Text("选取音乐")
        }
    }
}

@Composable
private fun Title(
    modifier: Modifier = Modifier,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
) {
    val indexState = playBackViewModel.playlistCurrentIndex.collectAsStateWithLifecycle()

    val playlistSizeState = playBackViewModel.nowPlayingListSize.collectAsStateWithLifecycle()

    Row(
        modifier = modifier,
    ) {
        Text(
            modifier =
                Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.small,
                    ).padding(vertical = 4.dp, horizontal = 8.dp),
            text = "循环播放",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                ),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            modifier =
                Modifier
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.shapes.small,
                    ).padding(vertical = 4.dp, horizontal = 8.dp),
            text = "第 ${indexState.value + 1} / ${playlistSizeState.value} 首",
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                ),
        )
    }
}
