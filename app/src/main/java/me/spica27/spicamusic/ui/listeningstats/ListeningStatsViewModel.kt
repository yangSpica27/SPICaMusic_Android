package me.spica27.spicamusic.ui.listeningstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.PlayStats
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.PlayHistoryUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases

data class TopSongDisplayItem(
    val songId: Long,
    val song: Song?,
    val totalDuration: Long,
    val playCount: Long,
)

class ListeningStatsViewModel(
    private val historyRepository: PlayHistoryUseCases,
    private val songRepository: SongUseCases,
) : ViewModel() {
    private val _weeklyStats = MutableStateFlow<PlayStats?>(null)
    val weeklyStats: StateFlow<PlayStats?> = _weeklyStats.asStateFlow()

    private val _allTimeStats = MutableStateFlow<PlayStats?>(null)
    val allTimeStats: StateFlow<PlayStats?> = _allTimeStats.asStateFlow()

    private val _topSongs = MutableStateFlow<List<TopSongDisplayItem>>(emptyList())
    val topSongs: StateFlow<List<TopSongDisplayItem>> = _topSongs.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                _weeklyStats.value = historyRepository.getWeeklyStats()
                _allTimeStats.value = historyRepository.getAllTimeStats()

                val top = historyRepository.getTopSongsByDuration(limit = 20)
                val ids = top.map { it.songId }
                val songs =
                    if (ids.isNotEmpty()) {
                        songRepository.getSongsByMediaStoreIds(ids)
                    } else {
                        emptyList()
                    }
                val songMap = songs.associateBy { it.mediaStoreId }
                _topSongs.value =
                    top.map { ts ->
                        TopSongDisplayItem(
                            songId = ts.songId,
                            song = songMap[ts.songId],
                            totalDuration = ts.totalDuration,
                            playCount = ts.playCount,
                        )
                    }
            } catch (_: Exception) {
            }
        }
    }
}
