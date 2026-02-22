package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IAlbumRepository
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository

class LibraryPageViewModel(
    private val songRepositoryImpl: ISongRepository,
    private val albumRepositoryImpl: IAlbumRepository,
    private val playlistRepositoryImpl: IPlaylistRepository,
    private val historyRepository: IPlayHistoryRepository,
) : ViewModel() {
    val albumList = albumRepositoryImpl.getAllPagingFlow()

    val playlists = playlistRepositoryImpl.getAllPlaylistsFlow()

    private val _weeklyStats = MutableStateFlow<PlayStats?>(null)
    val weeklyStats: StateFlow<PlayStats?> = _weeklyStats.asStateFlow()

    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs: StateFlow<List<Song>> = _recommendedSongs.asStateFlow()

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
                // 基于历史最常听的歌曲 + 随机补全
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
                        val all = songRepositoryImpl.getAllSongs()
                        all.filter { s -> topIds.none { it == s.mediaStoreId } }.shuffled().take(need)
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
