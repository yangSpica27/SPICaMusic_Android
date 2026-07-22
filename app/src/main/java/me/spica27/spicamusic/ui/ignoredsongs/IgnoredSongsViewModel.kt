package me.spica27.spicamusic.ui.ignoredsongs

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.SongUseCases

/**
 * 已忽略歌曲管理页面 ViewModel
 *
 * 已忽略歌曲集合通常很小，使用全量 [Flow]（非分页）即可；
 * 关键词过滤与排序在内存完成，无需为每次键入访问数据库。
 */
@Stable
class IgnoredSongsViewModel(
    private val songRepository: SongUseCases,
) : ViewModel() {
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword

    // 是否处于多选模式
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    // 已选中的歌曲 ID 集合：以 mediaStoreId 为键（非空、稳定），与收藏页选区模式一致
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds

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
     * 已忽略歌曲列表（按关键词内存过滤、按 displayName 升序）。
     * 初值 null 用于区分「未加载」与「已加载但为空」，避免首帧空态闪烁。
     */
    val ignoredSongs: StateFlow<List<Song>?> =
        combine(
            songRepository.getIgnoreSongsFlow(),
            _searchKeyword,
        ) { songs, keyword ->
            songs.matching(keyword).sortedByDisplayName()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun clearSearch() {
        _searchKeyword.value = ""
    }

    fun enterMultiSelectMode(initialSelectedId: Long? = null) {
        _isMultiSelectMode.value = true
        if (initialSelectedId != null) {
            _selectedSongIds.value = setOf(initialSelectedId)
        }
    }

    fun exitMultiSelectMode() {
        _isMultiSelectMode.value = false
        _selectedSongIds.value = emptySet()
    }

    fun toggleSongSelection(mediaStoreId: Long) {
        _selectedSongIds.value =
            if (_selectedSongIds.value.contains(mediaStoreId)) {
                _selectedSongIds.value - mediaStoreId
            } else {
                _selectedSongIds.value + mediaStoreId
            }
    }

    /** 全选当前过滤结果（搜索状态下仅选匹配项） */
    fun selectAll() {
        val allIds = ignoredSongs.value?.map { it.mediaStoreId } ?: emptyList()
        _selectedSongIds.value = allIds.toSet()
    }

    fun deselectAll() {
        _selectedSongIds.value = emptySet()
    }

    /** 单首取消忽略（列表流会自动移除该项） */
    fun unignoreSong(song: Song) {
        viewModelScope.launch {
            songRepository.updateSongIgnoreStatus(song.mediaStoreId, false)
        }
    }

    /** 批量取消忽略选中歌曲 */
    fun unignoreSelectedSongs() {
        // 在 launch 外同步捕获选区：onRemove 之后会立即调用 exitMultiSelectMode() 清空选区，
        // 若在协程内读取，正确性依赖 Dispatchers.Main.immediate 的即时派发，重构后易静默失效。
        val ids = _selectedSongIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            songRepository.ignoreSongsByMediaStoreIds(ids, false)
        }
    }

    private fun List<Song>.matching(keyword: String): List<Song> {
        val kw = keyword.trim()
        if (kw.isEmpty()) return this
        return filter { song ->
            song.displayName.contains(kw, ignoreCase = true) ||
                song.artist.contains(kw, ignoreCase = true) ||
                song.album.contains(kw, ignoreCase = true)
        }
    }

    private fun List<Song>.sortedByDisplayName(): List<Song> = sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayName })
}
