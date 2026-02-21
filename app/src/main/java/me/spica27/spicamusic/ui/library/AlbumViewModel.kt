package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import me.spica27.spicamusic.storage.api.IAlbumRepository

class AlbumViewModel(
    private val albumRepository: IAlbumRepository,
) : ViewModel() {
    // 搜索关键词
    private val _searchKeyword = MutableStateFlow("")

    /**
     * 搜索关键词的 StateFlow，直接暴露给 UI 层使用，UI 层可以通过 collectAsState() 订阅它的变化
     */
    val searchKeyword: StateFlow<String> = _searchKeyword

    /**
     * 根据搜索关键词获取过滤后的专辑列表 Flow，使用 debounce 和 flatMapLatest 实现搜索节流和自动切换数据源
     */
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredAlbums =
        _searchKeyword
            .debounce(500)
            .flatMapLatest { keyword ->
                if (keyword.isNotEmpty()) {
                    albumRepository.getFilterAlbumsFlow(
                        keyword,
                    )
                } else {
                    albumRepository.getAllPagingFlow()
                }
            }.cachedIn(viewModelScope)

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
}
