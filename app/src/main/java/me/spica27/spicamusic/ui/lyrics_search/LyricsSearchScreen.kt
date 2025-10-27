package me.spica27.spicamusic.ui.lyrics_search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.NetworkState
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.network.bean.LyricResponse
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.ui.player.LocalPlayerWidgetState
import me.spica27.spicamusic.ui.player.PlayerOverlyState
import me.spica27.spicamusic.utils.ToastUtils
import me.spica27.spicamusic.viewModel.LyricSearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSearchScreen(
    song: Song,
    lyricSearchViewModel: LyricSearchViewModel = koinViewModel(),
) {
    val data = lyricSearchViewModel.lyricsFlow.collectAsState(initial = emptyList()).value

    val state = lyricSearchViewModel.state.collectAsState(initial = NetworkState.IDLE).value

    val overlyState = LocalPlayerWidgetState.current

    LaunchedEffect(Unit) {
        overlyState.value = PlayerOverlyState.BOTTOM
    }

    val navigator = LocalNavController.current

    Scaffold(
        modifier =
            Modifier
                .fillMaxSize(),
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(
                    onClick = {
                        navigator.popBackStack()
                    },
                ) {
                    Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
                }
            }, title = {
                Text(
                    stringResource(R.string.title_lyrics_search),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold,
                        ),
                )
            })
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                TopPanel(song, lyricSearchViewModel)
                HorizontalDivider()
                ListView(
                    song = song,
                    state = state,
                    modifier = Modifier.weight(1f),
                    data = data,
                    viewModel = lyricSearchViewModel,
                )
            }
        }
    }
}

@Composable
private fun TopPanel(
    song: Song,
    lyricSearchViewModel: LyricSearchViewModel,
) {
    val songName = rememberSaveable { mutableStateOf(song.displayName) }

    val artists = rememberSaveable { mutableStateOf(song.artist) }
    val keyboardController = LocalSoftwareKeyboardController.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EditText(
            leftLabel = stringResource(R.string.song_displayname),
            rightLabel = stringResource(R.string.hint_edit_song_name),
            text = songName.value,
            onValueChange = {
                songName.value = it
            },
        )
        EditText(
            leftLabel = stringResource(R.string.song_artist),
            rightLabel = stringResource(R.string.hint_edit_song_artist),
            text = artists.value,
            onValueChange = {
                artists.value = it
            },
        )
        ElevatedButton(
            shape = MaterialTheme.shapes.small,
            onClick = {
                keyboardController?.hide()
                if (songName.value.isEmpty()) {
                    ToastUtils.showToast(App.getInstance().getString(R.string.hint_edit_song_name))
                } else {
                    lyricSearchViewModel.fetchLyric(songName.value, artists.value)
                }
            },
            modifier =
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
            colors =
                ButtonDefaults.elevatedButtonColors().copy(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        ) {
            Text(stringResource(R.string.search))
        }
    }
}

@Composable
private fun ListView(
    song: Song,
    state: NetworkState,
    modifier: Modifier,
    data: List<LyricResponse>,
    viewModel: LyricSearchViewModel,
) {
    Box(
        modifier = modifier,
    ) {
        when (state) {
            is NetworkState.IDLE -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.empty), style = MaterialTheme.typography.titleMedium)
                }
            }

            is NetworkState.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is NetworkState.ERROR -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(state.message, style = MaterialTheme.typography.titleMedium)
                }
            }

            is NetworkState.SUCCESS -> {
                if (data.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.empty), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(data.size) {
                            Column(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp,
                                    ),
                            ) {
                                LyricItem(lyric = data[it], song = song, lyricSearchViewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricItem(
    lyricSearchViewModel: LyricSearchViewModel,
    song: Song,
    lyric: LyricResponse,
) {
    val showDetailState = rememberSaveable { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.small,
                ).clip(
                    MaterialTheme.shapes.small,
                ).clickable {
                    showDetailState.value = !showDetailState.value
                }.padding(
                    horizontal = 12.dp,
                    vertical = 10.dp,
                ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = lyric.cover,
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(80.dp)
                        .clip(MaterialTheme.shapes.small),
                contentDescription = "background",
                contentScale = ContentScale.Crop,
                onError = {
                },
                placeholder = null,
                error = null,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier =
                    Modifier
                        .height(80.dp)
                        .weight(1f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${lyric.title}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style =
                        MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                        ),
                )
                Text(
                    "${lyric.artist}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        AnimatedVisibility(
            visible = showDetailState.value,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp,
                    ),
            ) {
                ElevatedButton(
                    colors =
                        ButtonDefaults.elevatedButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    shape = MaterialTheme.shapes.extraSmall,
                    onClick = {
                        lyricSearchViewModel.applyLyric(lyric, song)
                        ToastUtils.showToast(App.getInstance().getString(R.string.lrc_apply_success))
                    },
                ) {
                    Text(stringResource(R.string.apply_lrc))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    lyric.lyrics,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun EditText(
    leftLabel: String,
    rightLabel: String,
    text: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            leftLabel,
            modifier = Modifier.width(60.dp),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.W500,
                ),
        )
        TextField(
            value = text,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = { Text(rightLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            shape = MaterialTheme.shapes.small,
            colors =
                TextFieldDefaults.colors().copy(
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
        )
    }
}
