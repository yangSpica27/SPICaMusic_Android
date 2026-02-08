package me.spica27.spicamusic.ui.home.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.ISongRepository

/**
 * SearchPage 列表项的密封类：分组头 or 歌曲
 */
sealed class SearchListItem {
    data class Header(
        val title: String,
    ) : SearchListItem()

    data class SongItem(
        val song: Song,
    ) : SearchListItem()
}

/**
 * 搜索页面 ViewModel
 * 空关键词不加载数据（WelcomeHolder），有关键词时使用 Paging 3 + InsertSeparators 实现分组
 */
class SearchViewModel(
    private val songRepository: ISongRepository,
) : ViewModel() {
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")
    val searchKeyword: StateFlow<String> = _searchKeyword.asStateFlow()

    /**
     * 分页搜索结果（带分组头）
     * 空关键词时返回空的 PagingData，不做任何查询
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchPagingResults: Flow<PagingData<SearchListItem>> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest<String, PagingData<SearchListItem>> { keyword ->
                if (keyword.isBlank()) {
                    // 空关键词 → 不查询，返回空 PagingData
                    flowOf(PagingData.empty<SearchListItem>())
                } else {
                    songRepository
                        .getSongsBySortNamePagingFlow(keyword)
                        .map<PagingData<Song>, PagingData<SearchListItem>> { pagingData ->
                            pagingData
                                .map<Song, SearchListItem> { song -> SearchListItem.SongItem(song) }
                                .insertSeparators<SearchListItem, SearchListItem> { before, after ->
                                    val beforeSort = (before as? SearchListItem.SongItem)?.song?.sortName
                                    val afterSort = (after as? SearchListItem.SongItem)?.song?.sortName
                                    if (afterSort != null && beforeSort != afterSort) {
                                        SearchListItem.Header(afterSort)
                                    } else {
                                        null
                                    }
                                }
                        }
                }
            }.cachedIn(viewModelScope)

    /**
     * 搜索结果总数（用于判断是否显示空状态）
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val searchResultCount: StateFlow<Int> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest { keyword ->
                if (keyword.isBlank()) {
                    flowOf(0)
                } else {
                    songRepository.countFilteredSongsFlow(keyword)
                }
            }.stateIn(
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
     * 清空搜索关键词
     */
    fun clearSearch() {
        _searchKeyword.value = ""
    }
}
