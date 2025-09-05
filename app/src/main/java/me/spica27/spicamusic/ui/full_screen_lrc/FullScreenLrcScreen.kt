package me.spica27.spicamusic.ui.full_screen_lrc

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.ui.player.LocalPlayerWidgetState
import me.spica27.spicamusic.ui.player.PlayerOverlyState
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.LyricsView
import me.spica27.spicamusic.widget.MusicEffectBackground
import me.spica27.spicamusic.wrapper.activityViewModel

@Composable
fun FullScreenLrcScreen() {
    val playBackViewModel = activityViewModel<PlayBackViewModel>()
    // 当前播放的歌曲
    val currentPlayingSong =
        playBackViewModel.currentSongFlow
            .collectAsState()
            .value

    val currentTime = playBackViewModel.positionSec.collectAsStateWithLifecycle().value

    val overlyState = LocalPlayerWidgetState.current

    LaunchedEffect(Unit) {
        overlyState.value = PlayerOverlyState.BOTTOM
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    MaterialTheme.colorScheme.surfaceContainer,
                ),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            MusicEffectBackground(
                modifier = Modifier.fillMaxSize(),
            )
//      TunEffectBackground(
//        modifier = Modifier.fillMaxSize()
//      )
        }
        if (currentPlayingSong == null) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(
                            MaterialTheme.shapes.medium,
                        ),
                contentAlignment = Center,
            ) {
                Text(
                    "未在播放",
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color =
                                MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.8f,
                                ),
                        ),
                )
            }
        } else {
            Box {
                LyricsView(
                    modifier = Modifier.fillMaxSize(),
                    currentTime = currentTime * 1000,
                    song = currentPlayingSong,
                    onScroll = {
                        playBackViewModel.seekTo(it.toLong())
                    },
                    placeHolder = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(
                                        MaterialTheme.shapes.medium,
                                    ),
                            contentAlignment = Center,
                        ) {
                            Text(
                                "暂无歌词",
                                style =
                                    MaterialTheme.typography.titleLarge.copy(
                                        color =
                                            MaterialTheme.colorScheme.onSurface.copy(
                                                alpha = 0.8f,
                                            ),
                                    ),
                            )
                        }
                    },
                )
            }
        }
    }
}
