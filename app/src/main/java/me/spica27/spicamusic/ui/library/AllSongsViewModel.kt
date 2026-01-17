package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.ISongRepository

/**
 * 所有歌曲页面 ViewModel
 */
class AllSongsViewModel(
    private val songRepository: ISongRepository,
) : ViewModel() {
    // 原始歌曲列表
    private val allSongs = songRepository.getAllSongsFlow()

    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword

    // 是否处于多选模式
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    // 已选中的歌曲ID集合
    private val _selectedSongIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedSongIds: StateFlow<Set<Long>> = _selectedSongIds

    // 过滤后的歌曲列表
    val filteredSongs: StateFlow<List<Song>> =
        combine(
            allSongs,
            searchKeyword,
        ) { songs, keyword ->
            if (keyword.isBlank()) {
                songs
            } else {
                songs.filter { song ->
                    song.displayName.contains(keyword, ignoreCase = true) ||
                        song.artist.contains(keyword, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
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
     * 全选
     */
    fun selectAll() {
        _selectedSongIds.value = filteredSongs.value.mapNotNull { it.songId }.toSet()
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
}
