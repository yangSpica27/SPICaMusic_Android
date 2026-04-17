package me.spica27.spicamusic.ui.library

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.PlayHistoryUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases

class LibraryPageViewModel(
    private val songRepositoryImpl: SongUseCases,
    private val albumRepositoryImpl: AlbumUseCases,
    private val playlistRepositoryImpl: PlaylistUseCases,
    private val historyRepository: PlayHistoryUseCases,
) : ViewModel() {
    val albumList = albumRepositoryImpl.getAllPagingFlow()
    val playlists = playlistRepositoryImpl.getAllPlaylistsFlow()
    private val _weeklyStats = MutableStateFlow<PlayStats?>(null)
    val weeklyStats: StateFlow<PlayStats?> = _weeklyStats.asStateFlow()
    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs.asStateFlow()
    val lazyGridState = LazyGridState()

    init {
        refreshWeeklyStats()
        refreshRecommendations()
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

    fun refreshRecommendations() {
        viewModelScope.launch {
            try {
                // 基于历史最常听的歌曲 + 数据库随机补全（不再全量加载歌曲到内存）
                val top = historyRepository.getTopSongsByDuration(limit = 10)
                val topIds = top.map { it.songId }

                val topSongs =
                    if (topIds.isNotEmpty()) {
                        songRepositoryImpl.getSongsByMediaStoreIds(topIds)
                    } else {
                        emptyList()
                    }

                val need = 15 - topSongs.size
                val filler =
                    if (need > 0) {
                        // 直接在数据库层 ORDER BY RANDOM() LIMIT need，避免全量加载
                        songRepositoryImpl.getRandomSongsExcluding(topIds, need)
                    } else {
                        emptyList()
                    }

                // 混合并限制到 15
                val mixed = (topSongs + filler).toMutableList()
                if (mixed.size > 1) mixed.shuffle()
                _recommendedSongs.value = mixed.take(15)
            } catch (e: Exception) {
                // ignore for now
            }
        }
    }
}
