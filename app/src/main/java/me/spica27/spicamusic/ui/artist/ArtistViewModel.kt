package me.spica27.spicamusic.ui.artist

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import me.spica27.spicamusic.feature.library.domain.SongUseCases

@Stable
class ArtistViewModel(
    private val songUseCases: SongUseCases,
) : ViewModel() {
    private val _searchKeyword = MutableStateFlow("")

    val searchKeyword: StateFlow<String> = _searchKeyword

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val filteredArtists =
        _searchKeyword
            .debounce(500)
            .flatMapLatest { keyword ->
                songUseCases.getArtistsPagingFlow(keyword.ifEmpty { null })
            }.cachedIn(viewModelScope)

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun clearSearch() {
        _searchKeyword.value = ""
    }
}
