package me.spica27.spicamusic.ui.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.common.entity.SongSortOrder
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases

/**
 * 首页 ViewModel
 * 负责管理首页的数据和业务逻辑
 * 播放器相关操作请使用 PlayerViewModel
 */
@Stable
class HomeViewModel(
    private val songRepository: SongUseCases,
    private val playlistRepository: PlaylistUseCases,
) : ViewModel() {
    // 排序方式
    private val _sortOrder = MutableStateFlow(SongSortOrder.DEFAULT)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder

    private val _currentPage = MutableStateFlow(HomePage.Library)
    val currentPage: StateFlow<HomePage> = _currentPage

    // 播放器展开进度 0f = 最小化, 1f = 全屏
    private val _playerExpandProgress = MutableStateFlow(0f)
    val playerExpandProgress: StateFlow<Float> = _playerExpandProgress

    private val _showCreateMenu = MutableStateFlow(false)

    val showCreateMenu: StateFlow<Boolean> = _showCreateMenu

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
            .flowOn(Dispatchers.IO)
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

    /**
     * 导航到指定页面
     */
    fun navigateToPage(page: HomePage) {
        _currentPage.value = page
    }

    /**
     * 显示或隐藏创建菜单
     */
    fun toggleCreateMenu() {
        _showCreateMenu.value = !_showCreateMenu.value
    }

    /**
     * 更新播放器展开进度
     */
    fun updatePlayerExpandProgress(progress: Float) {
        _playerExpandProgress.value = progress.coerceIn(0f, 1f)
    }

    /**
     * 展开播放器到全屏
     */
    fun expandPlayer() {
        _playerExpandProgress.value = 1f
    }

    /**
     * 收起播放器到迷你模式
     */
    fun collapsePlayer() {
        _playerExpandProgress.value = 0f
    }
}
