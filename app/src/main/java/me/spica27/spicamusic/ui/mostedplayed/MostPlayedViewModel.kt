package me.spica27.spicamusic.ui.mostedplayed

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.PlayHistoryUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.ui.listeningstats.TopSongDisplayItem
import timber.log.Timber
import java.util.Calendar

enum class MostPlayedRange(
    val labelRes: Int,
) {
    WEEK(R.string.range_week),
    YEAR(R.string.range_year),
    ALL_TIME(R.string.range_all_time),
}

@Stable
class MostPlayedViewModel(
    private val app: Application,
    private val historyRepository: PlayHistoryUseCases,
    private val songRepository: SongUseCases,
    private val playlistRepository: PlaylistUseCases,
    private val player: PlayerUseCases,
) : AndroidViewModel(app) {
    private val _selectedRange = MutableStateFlow(MostPlayedRange.ALL_TIME)
    val selectedRange: StateFlow<MostPlayedRange> = _selectedRange.asStateFlow()

    private val _songs = MutableStateFlow<List<TopSongDisplayItem>>(emptyList())
    val songs: StateFlow<List<TopSongDisplayItem>> = _songs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 用于 Snackbar / Toast 提示
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedRange.collect { loadSongs(it) }
        }
    }

    fun selectRange(range: MostPlayedRange) {
        _selectedRange.value = range
    }

    fun refresh() {
        viewModelScope.launch { loadSongs(_selectedRange.value) }
    }

    private suspend fun loadSongs(range: MostPlayedRange) {
        _isLoading.value = true
        try {
            val topSongs =
                when (range) {
                    MostPlayedRange.WEEK -> {
                        val (from, to) = weekRange()
                        historyRepository.getTopSongsByDurationInRange(from, to, limit = 50)
                    }
                    MostPlayedRange.YEAR -> {
                        val (from, to) = yearRange()
                        historyRepository.getTopSongsByDurationInRange(from, to, limit = 50)
                    }
                    MostPlayedRange.ALL_TIME -> historyRepository.getTopSongsByDuration(limit = 50)
                }
            val ids = topSongs.map { it.songId }
            val songs = if (ids.isNotEmpty()) songRepository.getSongsByMediaStoreIds(ids) else emptyList()
            val songMap = songs.associateBy { it.mediaStoreId }
            _songs.value =
                topSongs.map { ts ->
                    TopSongDisplayItem(
                        songId = ts.songId,
                        song = songMap[ts.songId],
                        totalDuration = ts.totalDuration,
                        playCount = ts.playCount,
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "加载最常播放失败")
        } finally {
            _isLoading.value = false
        }
    }

    fun playAll() {
        val list = _songs.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            try {
                player.doAction(
                    PlayerAction.UpdateList(
                        mediaIds = list.mapNotNull { it.song?.mediaStoreId?.toString() },
                        start = true,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "播放失败")
            }
        }
    }

    fun playSong(item: TopSongDisplayItem) {
        val list = _songs.value
        if (list.isEmpty()) return
        viewModelScope.launch {
            try {
                player.doAction(
                    PlayerAction.UpdateList(
                        mediaIds = list.mapNotNull { it.song?.mediaStoreId?.toString() },
                        mediaId = item.song?.mediaStoreId?.toString(),
                        start = true,
                    ),
                )
            } catch (e: Exception) {
                Timber.e(e, "播放歌曲失败")
            }
        }
    }

    fun saveAsPlaylist() {
        val list = _songs.value
        if (list.isEmpty()) return
        val name = playlistNameForRange(_selectedRange.value)
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(name)
                val songIds = list.mapNotNull { it.song?.songId }
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                _snackbarMessage.value = app.getString(R.string.saved_as_playlist_format, name)
            } catch (e: Exception) {
                Timber.e(e, "保存歌单失败")
                _snackbarMessage.value = app.getString(R.string.save_failed)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun playlistNameForRange(range: MostPlayedRange) =
        when (range) {
            MostPlayedRange.WEEK -> "本周最常播放"
            MostPlayedRange.YEAR -> "本年最常播放"
            MostPlayedRange.ALL_TIME -> "全部时间最常播放"
        }

    private fun weekRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to now
    }

    private fun yearRange(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis to now
    }
}
