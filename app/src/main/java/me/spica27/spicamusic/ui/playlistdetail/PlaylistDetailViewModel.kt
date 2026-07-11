package me.spica27.spicamusic.ui.playlistdetail

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

/**
 * 歌单详情页面 ViewModel
 */
@Stable
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val playlistRepository: PlaylistUseCases,
    private val player: PlayerUseCases,
    private val songRepository: SongUseCases,
) : ViewModel() {
    companion object {
        const val SORT_MODE_FULL_LIST_LIMIT = 5000
    }

    // 歌单信息
    val playlist: StateFlow<Playlist?> =
        playlistRepository.getPlaylistByIdFlow(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    // 歌单封面所需的专辑 ID（最多 4 个，用于马赛克封面）
    val coverAlbumIds: StateFlow<List<Long>> =
        playlistRepository.getPlaylistCoverAlbumIds(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // 歌单内歌曲数量
    val songCount: StateFlow<Int> =
        playlistRepository.getSongSizeInPlaylist(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0,
        )

    // ===== 搜索功能 =====
    private val _isSearchMode = MutableStateFlow(false)
    val isSearchMode = _isSearchMode.asStateFlow()

    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword = _searchKeyword.asStateFlow()

    private val _isSortMode = MutableStateFlow(false)
    val isSortMode = _isSortMode.asStateFlow()

    private val _sortModeSongs = MutableStateFlow<List<Song>>(emptyList())
    val sortModeSongs = _sortModeSongs.asStateFlow()

    private val _sortModeLimitExceeded = MutableStateFlow(false)
    val sortModeLimitExceeded = _sortModeLimitExceeded.asStateFlow()

    /**
     * 浏览态主列表：keyword 恒为空，Pager 整个生命周期只创建一次，
     * 搜索输入不会触及它 —— 主列表滚动位置与内容因此永不跳变。
     */
    val browseSongs: Flow<PagingData<Song>> =
        playlistRepository
            .getSongsByPlaylistIdFlow(playlistId, "")
            .cachedIn(viewModelScope)

    /**
     * 防抖后的搜索关键字。UI 用它做两件事：
     * 1. 判定加载态（输入 != 防抖值 → 展示骨架，杜绝防抖窗口内旧结果停留）
     * 2. 结果高亮（保证高亮词与实际查询一致）
     */
    @OptIn(FlowPreview::class)
    val debouncedKeyword: StateFlow<String> =
        _searchKeyword
            .debounce(300)
            .map { it.trim() }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = "",
            )

    /**
     * 搜索结果流（与主列表完全独立）。
     * 空关键字发射停驻 Loading 的空 PagingData，
     * 使 UI 在防抖窗口内显示骨架而非"无结果"闪现。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: Flow<PagingData<Song>> =
        debouncedKeyword
            .flatMapLatest { keyword ->
                if (keyword.isBlank()) {
                    flowOf(
                        PagingData.empty(
                            sourceLoadStates =
                                LoadStates(
                                    refresh = LoadState.Loading,
                                    prepend = LoadState.NotLoading(endOfPaginationReached = true),
                                    append = LoadState.NotLoading(endOfPaginationReached = true),
                                ),
                        ),
                    )
                } else {
                    playlistRepository.getSongsByPlaylistIdFlow(playlistId, keyword)
                }
            }.cachedIn(viewModelScope)

    fun enterSearchMode() {
        // 多选中不可进入搜索（selectAll 的全歌单语义与过滤视图冲突）
        if (_isMultiSelectMode.value) return
        cancelSortMode()
        _isSearchMode.value = true
    }

    /**
     * 退出搜索模式。注意：这里不清空关键字 ——
     * 覆盖层退场动画期间仍需渲染旧结果，避免闪现空态；
     * 关键字由 UI 在退场动画结束后调用 [clearSearchKeyword] 清理。
     */
    fun exitSearchMode() {
        _isSearchMode.value = false
    }

    /** 覆盖层完全退场后清空关键字，下次进入搜索从全新 Idle 开始 */
    fun clearSearchKeyword() {
        _searchKeyword.value = ""
    }

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    // ===== 歌曲选择器（SongPickerSheet）分页支持 =====

    /** SongPickerSheet 内部的搜索关键词 */
    private val _pickerKeyword = MutableStateFlow("")

    /** 更新选择器搜索关键词 */
    fun updatePickerKeyword(keyword: String) {
        _pickerKeyword.value = keyword
    }

    /** 分页获取不在当前歌单中的歌曲（支持关键词过滤） */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pickerSongsPaging: Flow<PagingData<Song>> =
        _pickerKeyword
            .flatMapLatest { keyword ->
                songRepository.getSongsNotInPlaylistPagingFlow(
                    playlistId = playlistId,
                    keyword = keyword.ifBlank { null },
                )
            }.cachedIn(viewModelScope)

    /** 不在歌单中的符合条件歌曲总数 */
    @OptIn(ExperimentalCoroutinesApi::class)
    val pickerSongCount: StateFlow<Int> =
        _pickerKeyword
            .flatMapLatest { keyword ->
                songRepository.countSongsNotInPlaylistFlow(
                    playlistId = playlistId,
                    keyword = keyword.ifBlank { null },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    /** 获取不在歌单中的所有符合条件歌曲 ID（用于全选） */
    suspend fun getPickerSongIds(): List<Long> {
        val keyword = _pickerKeyword.value.ifBlank { null }
        return songRepository.getSongIdsNotInPlaylist(playlistId, keyword)
    }

    // 多选模式
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode = _isMultiSelectMode.asStateFlow()

    // 已选中的歌曲
    private val _selectedSongs = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongs = _selectedSongs.asStateFlow()

    // 是否显示重命名对话框
    private val _showRenameDialog = MutableStateFlow(false)
    val showRenameDialog = _showRenameDialog.asStateFlow()

    private val _showDeleteConfirmDialog = MutableStateFlow(false)

    val showDeleteConfirmDialog = _showDeleteConfirmDialog.asStateFlow()

    private val _playlistDeleted = MutableStateFlow(false)
    val playlistDeleted = _playlistDeleted.asStateFlow()

    // 是否显示添加歌曲选择器
    private val _showAddSongsSheet = MutableStateFlow(false)

    val showAddSongsSheet = _showAddSongsSheet.asStateFlow()

    // 是否显示更多选项菜单
    private val _showMoreOptionsMenu = MutableStateFlow(false)

    val showMoreOptionsMenu = _showMoreOptionsMenu.asStateFlow()

    /**
     * 播放歌单所有歌曲
     */
    fun playAll() {
        viewModelScope.launch(Dispatchers.Main) {
            val ids =
                async(Dispatchers.IO) { playlistRepository.getMediaIdsInPlaylist(playlistId) }.await()
            if (ids.isEmpty()) {
                Timber.w("歌单中没有歌曲，无法播放")
                return@launch
            }
            try {
                // 增加播放次数
                playlistRepository.incrementPlaylistPlayTime(playlistId)
                // 发送播放指令
                player.doAction(
                    PlayerAction.UpdateList(
                        ids.map { it.toString() },
                        start = true,
                    ),
                )
                Timber.d("开始播放歌单")
            } catch (e: Exception) {
                Timber.e(e, "播放歌单失败")
            }
        }
    }

    /**
     * 播放指定歌曲
     */
    fun playSongInList(song: Song) {
        viewModelScope.launch {
            val ids =
                async(Dispatchers.IO) { playlistRepository.getMediaIdsInPlaylist(playlistId) }.await()
            if (ids.isEmpty()) {
                Timber.w("歌单中没有歌曲，无法播放")
                return@launch
            }
            try {
                player.doAction(
                    PlayerAction.UpdateList(
                        ids.map { it.toString() },
                        mediaId = song.mediaStoreId.toString(),
                        start = true,
                    ),
                )
                Timber.d("播放歌曲: ${song.displayName}")
            } catch (e: Exception) {
                Timber.e(e, "播放歌曲失败")
            }
        }
    }

    /**
     * 切换多选模式
     */
    fun toggleMultiSelectMode() {
        val shouldEnter = !_isMultiSelectMode.value
        if (shouldEnter) {
            cancelSortMode()
            exitSearchMode()
        }
        _isMultiSelectMode.value = shouldEnter
        if (!_isMultiSelectMode.value) {
            // 退出多选模式时清空选择
            _selectedSongs.value = emptySet()
        }
    }

    /**
     * 切换歌曲选中状态
     */
    fun toggleSongSelection(mediaId: Long?) {
        if (mediaId == null) return
        val current = _selectedSongs.value.toMutableSet()
        if (current.contains(mediaId)) {
            current.remove(mediaId)
        } else {
            current.add(mediaId)
        }
        _selectedSongs.value = current
    }

    /**
     * 全选
     */
    fun selectAll() {
        viewModelScope.launch(Dispatchers.IO) {
            val ids = playlistRepository.getMediaIdsInPlaylist(playlistId)
            if (ids.isEmpty()) {
                Timber.w("歌单中没有歌曲，无法全选")
                return@launch
            }
            _selectedSongs.value = ids.toSet()
        }
    }

    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedSongs.value = emptySet()
    }

    fun enterSortMode() {
        exitSearchMode()
        _isMultiSelectMode.value = false
        _selectedSongs.value = emptySet()
        _showMoreOptionsMenu.value = false

        viewModelScope.launch {
            try {
                val count = playlistRepository.getSongSizeInPlaylistOnce(playlistId)
                if (count > SORT_MODE_FULL_LIST_LIMIT) {
                    _sortModeLimitExceeded.value = true
                    Timber.w("歌单歌曲数量 $count 超过排序模式上限 $SORT_MODE_FULL_LIST_LIMIT")
                    return@launch
                }

                _sortModeSongs.value = playlistRepository.getSongsByPlaylistIdList(playlistId)
                _isSortMode.value = true
            } catch (e: Exception) {
                Timber.e(e, "进入排序模式失败")
            }
        }
    }

    fun cancelSortMode() {
        _isSortMode.value = false
        _sortModeSongs.value = emptyList()
    }

    fun finishSortMode() {
        val mediaIds = _sortModeSongs.value.map { it.mediaStoreId }
        if (!_isSortMode.value || mediaIds.isEmpty()) {
            cancelSortMode()
            return
        }

        viewModelScope.launch {
            try {
                playlistRepository.reorderPlaylistSongs(playlistId, mediaIds)
                Timber.d("保存歌单歌曲顺序成功: ${mediaIds.size} 首")
                cancelSortMode()
            } catch (e: Exception) {
                Timber.e(e, "保存歌单歌曲顺序失败")
            }
        }
    }

    fun moveSortModeSong(
        fromMediaId: Long,
        toMediaId: Long,
        insertAfterTarget: Boolean,
    ) {
        if (fromMediaId == toMediaId || !_isSortMode.value) return

        val songs = _sortModeSongs.value.toMutableList()
        val fromIndex = songs.indexOfFirst { it.mediaStoreId == fromMediaId }
        if (fromIndex < 0) {
            Timber.w("排序源歌曲不在本地列表中: $fromMediaId")
            return
        }

        val movedSong = songs.removeAt(fromIndex)
        val targetIndex = songs.indexOfFirst { it.mediaStoreId == toMediaId }
        if (targetIndex < 0) {
            Timber.w("排序目标歌曲不在本地列表中: $toMediaId")
            return
        }

        val insertIndex = if (insertAfterTarget) targetIndex + 1 else targetIndex
        songs.add(insertIndex.coerceIn(0, songs.size), movedSong)
        _sortModeSongs.value = songs
    }

    fun hideSortModeLimitExceeded() {
        _sortModeLimitExceeded.value = false
    }

    fun showDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = true
    }

    fun hideDeleteConfirmDialog() {
        _showDeleteConfirmDialog.value = false
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            try {
                playlistRepository.deletePlaylist(playlistId)
                _playlistDeleted.value = true
                hideDeleteConfirmDialog()
                Timber.d("删除歌单成功: $playlistId")
            } catch (e: Exception) {
                Timber.e(e, "删除歌单失败")
            }
        }
    }

    /**
     * 从歌单中移除选中的歌曲
     */
    fun removeSelectedSongs() {
        val selected = _selectedSongs.value
        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                // 单事务批量移除：只触发一次 Room 失效，列表只做一次退场动画
                playlistRepository.removeSongsFromPlaylist(playlistId, selected.toList())
                Timber.d("从歌单移除 ${selected.size} 首歌曲")
                // 退出多选模式
                _isMultiSelectMode.value = false
                _selectedSongs.value = emptySet()
            } catch (e: Exception) {
                Timber.e(e, "移除歌曲失败")
            }
        }
    }

    /**
     * 显示重命名对话框
     */
    fun showRenameDialog() {
        _showRenameDialog.value = true
    }

    /**
     * 隐藏重命名对话框
     */
    fun hideRenameDialog() {
        _showRenameDialog.value = false
    }

    /**
     * 重命名歌单
     */
    fun renamePlaylist(newName: String) {
        if (newName.isBlank()) {
            Timber.w("歌单名称不能为空")
            return
        }

        viewModelScope.launch {
            try {
                playlistRepository.renamePlaylist(playlistId, newName.trim())
                Timber.d("重命名歌单成功: $newName")
                hideRenameDialog()
            } catch (e: Exception) {
                Timber.e(e, "重命名歌单失败")
            }
        }
    }

    /**
     * 显示添加歌曲选择器
     */
    fun showAddSongsSheet() {
        _showAddSongsSheet.value = true
    }

    /**
     * 隐藏添加歌曲选择器
     */
    fun hideAddSongsSheet() {
        _showAddSongsSheet.value = false
    }

    /**
     * 显示更多选项菜单
     */
    fun showMoreOptionsMenu() {
        _showMoreOptionsMenu.value = true
    }

    /**
     * 隐藏更多选项菜单
     */
    fun hideMoreOptionsMenu() {
        _showMoreOptionsMenu.value = false
    }

    /**
     * 批量添加歌曲到歌单
     */
    fun addSongsToPlaylist(mediaIds: List<Long>) {
        if (mediaIds.isEmpty()) return

        viewModelScope.launch {
            try {
                playlistRepository.addSongsToPlaylist(playlistId, mediaIds)
                Timber.d("成功添加 ${mediaIds.size} 首歌曲到歌单")
                hideAddSongsSheet()
            } catch (e: Exception) {
                Timber.e(e, "添加歌曲到歌单失败")
            }
        }
    }
}
