package me.spica27.spicamusic.ui.home

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongFilter
import me.spica27.spicamusic.common.entity.SongSortOrder
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.ui.model.PlaylistWithCover
import timber.log.Timber

/**
 * 首页 ViewModel
 * 负责管理首页的数据和业务逻辑
 * 播放器相关操作请使用 PlayerViewModel
 */
@Stable
class HomeViewModel(
    private val app: Application,
    private val songRepository: SongUseCases,
    private val playlistRepository: PlaylistUseCases,
) : ViewModel() {
    // 排序方式
    private val _sortOrder = MutableStateFlow(SongSortOrder.DEFAULT)
    val sortOrder: StateFlow<SongSortOrder> = _sortOrder

    private val _currentPage = MutableStateFlow(HomePage.Finder)
    val currentPage: StateFlow<HomePage> = _currentPage

    // 播放器展开进度 0f = 最小化, 1f = 全屏
    private val _playerExpandProgress = MutableStateFlow(0f)
    val playerExpandProgress: StateFlow<Float> = _playerExpandProgress

    private val _showCreateMenu = MutableStateFlow(false)

    val showCreateMenu: StateFlow<Boolean> = _showCreateMenu

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

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
     * 首页"发现"页用的歌单 + 封面/歌曲数聚合。
     * 发现页只展示前 8 个，这里同样只为前 8 个订阅封面/数量，避免无谓订阅。
     * 订阅集中在 ViewModel，列表项纯展示，消除滚动时的 DB 订阅抖动。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val playlistsWithCover: StateFlow<List<PlaylistWithCover>> =
        playlists
            .flatMapLatest { list ->
                val limited = list.take(8)
                if (limited.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        limited.map { playlist ->
                            val id = playlist.playlistId ?: 0L
                            combine(
                                playlistRepository.getPlaylistCoverAlbumIds(id),
                                playlistRepository.getSongSizeInPlaylist(id),
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

    fun createPlaylistFromSongs(
        songs: List<Song>,
        playlistName: String,
    ) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(playlistName)
                playlistRepository.addSongsToPlaylist(
                    playlistId = playlistId,
                    mediaIds = songs.map { it.mediaStoreId },
                )
                _snackbarMessage.value = app.getString(R.string.saved_as_playlist_format, playlistName)
            } catch (e: Exception) {
                Timber.e(e, "创建常听歌曲歌单失败")
                _snackbarMessage.value = app.getString(R.string.save_failed)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
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
