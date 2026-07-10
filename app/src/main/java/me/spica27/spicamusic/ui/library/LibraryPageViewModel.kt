package me.spica27.spicamusic.ui.library

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.feature.library.domain.PlayHistoryUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.feature.library.domain.ScanFolderUseCases
import me.spica27.spicamusic.ui.model.PlaylistWithCover

@Stable
class LibraryPageViewModel(
    private val playlistRepositoryImpl: PlaylistUseCases,
    private val historyRepository: PlayHistoryUseCases,
    private val scanFolderUseCases: ScanFolderUseCases,
) : ViewModel() {
    val playlists =
        playlistRepositoryImpl.getAllPlaylistsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * 歌单 + 封面/歌曲数聚合。封面与歌曲数的 Flow 订阅集中在此处管理，
     * 列表项只做纯展示，避免滚动时每项各自订阅 DB 造成的抖动。
     */
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
                                PlaylistWithCover(playlist, albumIds, size)
                            }
                        },
                    ) { it.toList() }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
    val extraFolders: StateFlow<List<ScanFolder>> =
        scanFolderUseCases.getExtraFoldersFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
    val ignoreFolders: StateFlow<List<ScanFolder>> =
        scanFolderUseCases.getIgnoreFoldersFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )
    private val _weeklyStats = MutableStateFlow<PlayStats?>(null)
    val weeklyStats: StateFlow<PlayStats?> = _weeklyStats.asStateFlow()

    init {
        refreshWeeklyStats()
    }

    fun refreshWeeklyStats() {
        viewModelScope.launch {
            try {
                _weeklyStats.value = historyRepository.getWeeklyStats()
            } catch (e: Exception) {
                // ignore errors for now
            }
        }
    }
}
