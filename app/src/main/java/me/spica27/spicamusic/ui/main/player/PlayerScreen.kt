package me.spica27.spicamusic.ui.main.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.ui.current_list.CurrentListPage
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.BottomSheet
import me.spica27.spicamusic.widget.COLLAPSED_ANCHOR
import me.spica27.spicamusic.widget.EXPANDED_ANCHOR
import me.spica27.spicamusic.widget.rememberBottomSheetState
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerScreen(
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    onBackClick: () -> Unit,
) {
    val nowPlayingSongs =
        playBackViewModel.player.currentTimelineItems
            .collectAsState()
            .value

    val isPlaying = playBackViewModel.isPlaying.collectAsState(false).value

    LaunchedEffect(nowPlayingSongs) {
        Timber.e("nowPlayingSongs: $nowPlayingSongs")
    }

    LaunchedEffect(isPlaying) {
        Timber.e("isPlaying: $isPlaying")
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (nowPlayingSongs.isEmpty()) {
            EmptyPage()
        } else {
            var anchor by remember { mutableIntStateOf(COLLAPSED_ANCHOR) }
            val currentListBottomSheetState =
                rememberBottomSheetState(
                    collapsedBound = 0.dp,
                    expandedBound = maxHeight,
                    initialAnchor = 1,
                    onAnchorChanged = {
                        anchor = it
                    },
                )

            BackHandler(anchor == EXPANDED_ANCHOR) {
                currentListBottomSheetState.collapseSoft()
            }

            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                onBackClick.invoke()
                            },
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
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
                PlayerPage(
                    currentListBottomSheetState = currentListBottomSheetState,
                )
            }
            BottomSheet(
                state = currentListBottomSheetState,
                collapsedContent = {},
                content = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                    ) {
                        CurrentListPage(nestedScrollConnection = currentListBottomSheetState.preUpPostDownNestedScrollConnection)
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
            )
        }
    }
}

@Composable
private fun EmptyPage() {
    val navigator = LocalNavController.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("没有播放中的音乐", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                navigator.navigate(Routes.SearchAll)
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
