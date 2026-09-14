package me.spica27.spicamusic.ui.library

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.ui.model.PlaylistWithCover

@Stable
class LibraryPageViewModel(
    private val playlistRepositoryImpl: PlaylistUseCases,
) : ViewModel() {
    val playlists =
        playlistRepositoryImpl.getAllPlaylistsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistsWithCover: StateFlow<List<PlaylistWithCover>> =
        playlists
            .flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        list.map { playlist ->
                            val id = playlist.playlistId ?: 0L
                            combine(
                                playlistRepositoryImpl.getPlaylistCoverAlbumIds(id),
                                playlistRepositoryImpl.getSongSizeInPlaylist(id),
                            ) { albumIds, size ->
                                PlaylistWithCover(playlist, ImmutableList.copyOf(albumIds), size)
                            }
                        },
                    ) { it.toList() }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
}
