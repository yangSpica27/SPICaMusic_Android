package me.spica27.spicamusic.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.ui.main.player.PlayerScreen
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.widget.BottomSheet
import me.spica27.spicamusic.widget.COLLAPSED_ANCHOR
import me.spica27.spicamusic.widget.CoverWidget
import me.spica27.spicamusic.widget.DISMISSED_ANCHOR
import me.spica27.spicamusic.widget.EXPANDED_ANCHOR
import me.spica27.spicamusic.widget.PlayerBar
import me.spica27.spicamusic.widget.rememberBottomSheetState
import me.spica27.spicamusic.wrapper.activityViewModel
import timber.log.Timber

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlayerOverly() {
    val navHostController = LocalNavController.current

    val playbackViewModel: PlayBackViewModel = activityViewModel()

    val isPlaying = playbackViewModel.isPlaying.collectAsStateWithLifecycle(false).value

    val nowPlayingSize = playbackViewModel.nowPlayingListSize.collectAsState().value

    val overlyState = LocalPlayerWidgetState.current

    BackHandler(overlyState.value == PlayerOverlyState.PLAYER) {
        overlyState.value = PlayerOverlyState.BOTTOM
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying && overlyState.value == PlayerOverlyState.HIDE) {
            overlyState.value = PlayerOverlyState.BOTTOM
        } else if (nowPlayingSize == 0 && overlyState.value == PlayerOverlyState.BOTTOM) {
            overlyState.value = PlayerOverlyState.HIDE
        } else {
            Timber.tag("PlayerOverly").d("都不符合 isPlay =$isPlaying overlyState = $overlyState")
        }
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val playerBottomSheetState =
            rememberBottomSheetState(
                dismissedBound = 0.dp,
                collapsedBound = 80.dp,
                expandedBound = maxHeight,
                onAnchorChanged = {
                    Timber.tag("PlayerOverly").d("onAnchorChanged = $it")
                    if (it == EXPANDED_ANCHOR) {
                        overlyState.value = PlayerOverlyState.PLAYER
                    }
                    if (it == COLLAPSED_ANCHOR) {
                        overlyState.value = PlayerOverlyState.BOTTOM
                    }
                    if (it == DISMISSED_ANCHOR) {
                        overlyState.value = PlayerOverlyState.HIDE
                    }
                },
            )

        LaunchedEffect(overlyState.value) {
            when (overlyState.value) {
                PlayerOverlyState.HIDE -> {
                    if (!playerBottomSheetState.isDismissed) {
                        playerBottomSheetState.dismiss()
                    }
                }

                PlayerOverlyState.BOTTOM -> {
                    if (!playerBottomSheetState.isCollapsed) {
                        playerBottomSheetState.collapseSoft()
                    }
                }

                PlayerOverlyState.PLAYER -> {
                    if (!playerBottomSheetState.isExpanded) {
                        playerBottomSheetState.expandSoft()
                    }
                }
            }
        }

        BottomSheet(
            state = playerBottomSheetState,
            collapsedContent = {
                Bottom()
            },
            content = {
                PlayerScreen(
                    onBackClick = {
                        overlyState.value = PlayerOverlyState.BOTTOM
                    },
                )
            },
            modifier = Modifier.fillMaxSize(),
            backgroundColor = MaterialTheme.colorScheme.background,
        )
    }
}

@Composable
private fun Bottom() {
    PlayerBar()
}

@Composable
private fun Mimi(song: Song) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinite")
    val rotateState =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(10000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "",
        )
    AnimatedContent(
        song,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        },
        contentKey = { "${it.songId}" },
    ) { song ->
        CoverWidget(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(CircleShape)
                    .rotate(rotateState.value),
            song = song,
        )
    }
}

@Composable
fun PlayerOverlyContent(content: @Composable BoxScope.() -> Unit) {
    val overlyState = rememberSaveable { mutableStateOf(PlayerOverlyState.HIDE) }
    CompositionLocalProvider(LocalPlayerWidgetState provides overlyState) {
        Box(
            content = content,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

internal val LocalPlayerWidgetState =
    staticCompositionLocalOf<MutableState<PlayerOverlyState>> {
        error("CompositionLocal LocalPlayerWidgetState not present")
    }

enum class PlayerOverlyState {
    //  MINI, // 右边悬浮模式
    HIDE, // 隐藏模式
    BOTTOM, // 底栏模式
    PLAYER, // 播放器模式
}
