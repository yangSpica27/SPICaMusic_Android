package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.ui.LocalNavSharedTransitionScope
import me.spica27.spicamusic.ui.widget.AudioCover
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import java.util.Locale

@Composable
fun AlbumDetailScreen(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    val album = (backStack.lastOrNull() as? Screen.AlbumDetail)?.album ?: return

    val viewModel: AlbumDetailViewModel =
        koinViewModel(key = "AlbumDetailViewModel_${album.id}") {
            parametersOf(album.id)
        }

    val songs by viewModel.songs.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()

    val localNavSharedTransitionScope = LocalNavSharedTransitionScope.current

    val localNavAnimatedContentScope = LocalNavAnimatedContentScope.current

    with(localNavSharedTransitionScope) {
        Scaffold(
            modifier =
                modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(album),
                        animatedVisibilityScope = localNavAnimatedContentScope,
                        exit =
                            scaleOut(
                                animationSpec = tween(durationMillis = 850),
                            ) + fadeOut(),
                    ),
            topBar = {
                TopAppBar(
                    title = "专辑详情",
                    largeTitle = "",
                    color = Color.Transparent,
                    navigationIcon = {
                        IconButton(
                            modifier =
                                Modifier
                                    .padding(start = 16.dp)
                                    .background(
                                        MiuixTheme.colorScheme.surface,
                                        shape = RoundedCornerShape(50),
                                    ),
                            onClick = {
                                backStack.removeLastOrNull()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "返回",
                            )
                        }
                    },
                )
            },
        ) { _ ->

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .scrollEndHaptic()
                        .overScrollVertical(),
            ) {
                item {
                    Header(album, viewModel)
                }
                items(count = songs.size) { index ->
                    val song = songs[index]
                    Column {
                        if (index == 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                        }
                        SongItemCard(
                            song = song,
                            onClick = { viewModel.playSongInList(song) },
                            index = index,
                        )

                        if (index != songs.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        } else {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 10.dp),
                            )
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun Header(
    album: Album,
    viewModel: AlbumDetailViewModel,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.78f),
    ) {
        AudioCover(
            uri = album.artworkUri.toString().toUri(),
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    ).hazeEffect {
                        progressive =
                            HazeProgressive.verticalGradient(
                                startY = .5f,
                                startIntensity = 0f,
                                endIntensity = .8f,
                            )
                    },
            placeHolder = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                brush =
                                    Brush.verticalGradient(
                                        colors =
                                            listOf(
                                                MiuixTheme.colorScheme.surface,
                                                MiuixTheme.colorScheme.primaryVariant.copy(alpha = 0.8f),
                                            ),
                                    ),
                            ),
                ) {
                    Text(
                        text = album.title,
                        style = MiuixTheme.textStyles.headline1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        fontSize = 88.sp,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .rotate(-45f),
                    )
                }
            },
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MiuixTheme.colorScheme.surface.copy(alpha = 0.0f),
                                        MiuixTheme.colorScheme.surface.copy(alpha = 0.3f),
                                        MiuixTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        MiuixTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        MiuixTheme.colorScheme.surface.copy(alpha = 1f),
                                    ),
                            ),
                    ).padding(
                        horizontal = 16.dp,
                        vertical = 12.dp,
                    ).padding(top = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = album.title,
                style = MiuixTheme.textStyles.headline1,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.W600,
            )
            Text(
                text = album.artist,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal,
            )
            Text(
                text = "${album.year} · ${album.numberOfSongs}首",
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
            )
            Button(
                onClick = {
                    viewModel.playAll()
                },
                minWidth = 120.dp,
                colors =
                    ButtonColors(
                        MiuixTheme.colorScheme.onSurface,
                        MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.1f),
                    ),
                insideMargin =
                    PaddingValues(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = stringResource(R.string.play),
                    tint = MiuixTheme.colorScheme.surface,
                )
                Spacer(
                    modifier = Modifier.width(4.dp),
                )
                Text(
                    text = stringResource(R.string.play),
                    color = MiuixTheme.colorScheme.surface,
                    style = MiuixTheme.textStyles.button,
                )
            }
        }
    }
}

@Composable
private fun SongItemCard(
    song: Song,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${index + 1}",
                modifier = Modifier.width(30.dp),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = song.displayName,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.W600,
            )
            Text(
                text = formatDuration(song.duration),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurface.copy(alpha = .6f),
                modifier = Modifier,
                fontWeight = FontWeight.W500,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
}
