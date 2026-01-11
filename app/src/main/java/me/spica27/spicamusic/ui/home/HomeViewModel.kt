package me.spica27.spicamusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.common.entity.SongSortOrder
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository

/**
 * 首页 ViewModel
 * 负责管理首页的数据和业务逻辑
 * 播放器相关操作请使用 PlayerViewModel
 */
class HomeViewModel(
    private val songRepository: ISongRepository,
    private val playlistRepository: IPlaylistRepository,
) : ViewModel() {
    // 排序方式
    private val _sortOrder = MutableStateFlow(SongSortOrder.DEFAULT)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder

    // 筛选条件
    private val _filter = MutableStateFlow(SongFilter.EMPTY)
    val filter: StateFlow<SongFilter> = _filter

    /**
     * 所有歌曲（支持排序和筛选）
     */
    val allSongs: StateFlow<List<Song>> =
        combine(_sortOrder, _filter) { sort, filter ->
            songRepository.getSongsFlow(sort, filter)
        }.flatMapLatest { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 喜欢的歌曲
     */
    val favoriteSongs: StateFlow<List<Song>> =
        songRepository
            .getAllLikeSongsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 常听的歌曲
     */
    val frequentSongs: StateFlow<List<Song>> =
        songRepository
            .getOftenListenSong10Flow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 所有歌单
     */
    val playlists: StateFlow<List<Playlist>> =
        playlistRepository
            .getAllPlaylistsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 更新排序方式
     */
    fun updateSortOrder(order: SongSortOrder) {
        _sortOrder.value = order
    }

    /**
     * 更新筛选条件
     */
    fun updateFilter(newFilter: SongFilter) {
        _filter.value = newFilter
    }

    /**
     * 搜索歌曲
     */
    fun searchSongs(keyword: String): StateFlow<List<Song>> =
        songRepository
            .searchSongsFlow(keyword, _sortOrder.value)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 重置筛选条件
     */
    fun resetFilter() {
        _filter.value = SongFilter.EMPTY
        _sortOrder.value = SongSortOrder.DEFAULT
    }
}
