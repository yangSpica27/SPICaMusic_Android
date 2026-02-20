package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.storage.api.ISongRepository
import timber.log.Timber

/**
 * 所有歌曲页面 ViewModel
 * 使用 Paging 3 按需加载歌曲列表，避免全量加载到内存
 */
class AllSongsViewModel(
    private val songRepository: ISongRepository,
    private val player: IMusicPlayer,
) : ViewModel() {
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword

    // 是否处于多选模式
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    // 已选中的歌曲ID集合
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds

    // 分页歌曲列表（关键词变化时自动切换 PagingSource）
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredSongs: Flow<PagingData<Song>> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest { keyword ->
                songRepository.getFilteredSongsPagingFlow(
                    keyword = keyword.ifBlank { null },
                )
            }.cachedIn(viewModelScope)

    // 歌曲总数（用于 TopBar 显示 "所有歌曲 (N)"）
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val songCount: StateFlow<Int> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest { keyword ->
                songRepository.countFilteredSongsFlow(
                    keyword = keyword.ifBlank { null },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    // 已选中的歌曲数量
    val selectedCount: StateFlow<Int> =
        _selectedSongIds
            .map { it.size }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0,
            )

    /**
     * 更新搜索关键词
     */
    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    /**
     * 清空搜索
     */
    fun clearSearch() {
        _searchKeyword.value = ""
    }

    fun playAllSongs(mediaId: Long? = null) {
        viewModelScope.launch {
            val allSongIds =
                songRepository.getFilteredMediaStoreIds(_searchKeyword.value).map {
                    it.toString()
                }
            player.doAction(
                action =
                    PlayerAction.UpdateList(
                        mediaIds = allSongIds,
                        mediaId = mediaId?.toString(),
                        start = true,
                    ),
            )
        }
    }

    /**
     * 进入多选模式
     */
    fun enterMultiSelectMode(initialSelectedId: Long? = null) {
        _isMultiSelectMode.value = true
        if (initialSelectedId != null) {
            _selectedSongIds.value = setOf(initialSelectedId)
        }
    }

    /**
     * 退出多选模式
     */
    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedSongIds.value = emptySet()
    }

    /**
     * 切换歌曲选中状态
     */
    fun toggleSongSelection(songId: Long) {
        _selectedSongIds.value =
            if (_selectedSongIds.value.contains(songId)) {
                _selectedSongIds.value - songId
            } else {
                _selectedSongIds.value + songId
            }
    }

    /**
     * 全选（异步查询所有符合条件的 songId）
     */
    fun selectAll() {
        viewModelScope.launch {
            val keyword = _searchKeyword.value.ifBlank { null }
            val allIds = songRepository.getFilteredSongIds(keyword)
            _selectedSongIds.value = allIds.toSet()
        }
    }

    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedSongIds.value = emptySet()
    }

    /**
     * 检查歌曲是否被选中
     */
    fun isSongSelected(songId: Long): Boolean = _selectedSongIds.value.contains(songId)

    /**
     * 对已选中的歌曲执行喜欢/不喜欢操作（异步更新数据库）
     */
    fun likeSelectedSongs() {
        viewModelScope.launch {
            val selectedIds = _selectedSongIds.value.toList()
            songRepository.likeSongs(selectedIds, true)
        }
    }

    /**
     * 对已选中的歌曲执行喜欢/不喜欢操作（异步更新数据库）
     */
    fun dislikeSelectedSongs() {
        viewModelScope.launch {
            val selectedIds = _selectedSongIds.value.toList()
            songRepository.likeSongs(selectedIds, false)
        }
    }

    /**
     * 对已选中的歌曲执行播放操作
     */
    fun playSelectedSongs() {
        viewModelScope.launch {
//            val selectedIds = _selectedSongIds.value.toList()
//            if (selectedIds.isNotEmpty()) {
//                player.doAction(
//                    action =
//                        PlayerAction.UpdateList(
//                            mediaIds = selectedIds.map { it.toString() },
//                            mediaId = selectedIds.first().toString(),
//                            start = true,
//                        ),
//                )
//            }
        }
    }

    /**
     * 对已选中的歌曲执行添加到队列操作
     */
    fun addToQueueSelectedSongs() {
        viewModelScope.launch {
            val selectedIds = _selectedSongIds.value.toList()
            if (selectedIds.isNotEmpty()) {
                Timber.tag("AllSongsViewModel").d("Adding to queue: $selectedIds 未实现")
            }
        }
    }

    /*
     * 对已选中的歌曲执行隐藏操作（异步更新数据库），隐藏后自动退出多选模式
     */
    fun hideSelectedSongs() {
        viewModelScope.launch {
            val selectedIds = _selectedSongIds.value.toList()
            if (selectedIds.isNotEmpty()) {
                songRepository.ignoreSongs(selectedIds)
            }
        }
    }
}
