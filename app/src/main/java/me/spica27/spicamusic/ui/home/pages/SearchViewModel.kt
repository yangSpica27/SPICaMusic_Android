package me.spica27.spicamusic.ui.home.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.SongSortOrder
import me.spica27.spicamusic.storage.api.ISongRepository

/**
 * 搜索页面 ViewModel
 */
class SearchViewModel(
    private val songRepository: ISongRepository,
) : ViewModel() {
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    // 搜索结果（按首字母拼音排序）
    @OptIn(FlowPreview::class)
    val searchResults: StateFlow<List<Song>> =
        searchKeyword
            .debounce(300) // 防抖：300ms 后才执行搜索
            .flatMapLatest { keyword ->
                if (keyword.isBlank()) {
                    // 空搜索显示所有歌曲
                    songRepository.getSongsFlow(sortOrder = SongSortOrder.DISPLAY_NAME_ASC)
                } else {
                    songRepository.searchSongsFlow(
                        keyword = keyword,
                        sortOrder = SongSortOrder.DISPLAY_NAME_ASC,
                    )
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 更新搜索关键词
     */
    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    /**
     * 清空搜索关键词
     */
    fun clearSearch() {
        _searchKeyword.value = ""
    }
}
