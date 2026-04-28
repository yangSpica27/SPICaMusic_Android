package me.spica27.spicamusic.ui.favorite

import android.app.Application
import androidx.compose.runtime.Stable
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
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

/**
 * 我的收藏页面 ViewModel
 * 使用 Paging3 按需加载收藏歌曲列表
 */
@Stable
class FavoriteViewModel(
    private val app: Application,
    private val songRepository: SongUseCases,
    private val player: PlayerUseCases,
    private val playlistRepository: PlaylistUseCases,
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

    // Snackbar 消息
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    // 分页收藏歌曲列表（关键词变化时自动切换 PagingSource）
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val favoriteSongs: Flow<PagingData<Song>> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest { keyword ->
                songRepository.getLikeSongsPagingFlow(keyword.ifBlank { null })
            }.cachedIn(viewModelScope)

    // 收藏歌曲总数
    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val songCount: StateFlow<Int> =
        _searchKeyword
            .debounce(300)
            .flatMapLatest { keyword ->
                songRepository.countLikeSongsFlow(keyword.ifBlank { null })
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

    fun updateSearchKeyword(keyword: String) {
        _searchKeyword.value = keyword
    }

    fun clearSearch() {
        _searchKeyword.value = ""
    }

    /** 播放所有收藏歌曲，可指定起播的 mediaStoreId */
    fun playAllSongs(mediaId: Long? = null) {
        viewModelScope.launch {
            val keyword = _searchKeyword.value.ifBlank { null }
            val allIds = songRepository.getLikeMediaStoreIds(keyword).map { it.toString() }
            if (allIds.isEmpty()) return@launch
            player.doAction(
                PlayerAction.UpdateList(
                    mediaIds = allIds,
                    mediaId = mediaId?.toString(),
                    start = true,
                ),
            )
        }
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

    fun toggleSongSelection(songId: Long) {
        _selectedSongIds.value =
            if (_selectedSongIds.value.contains(songId)) {
                _selectedSongIds.value - songId
            } else {
                _selectedSongIds.value + songId
            }
    }

    fun selectAll() {
        viewModelScope.launch {
            val keyword = _searchKeyword.value.ifBlank { null }
            val allIds = songRepository.getLikeSongIds(keyword)
            _selectedSongIds.value = allIds.toSet()
        }
    }

    fun deselectAll() {
        _selectedSongIds.value = emptySet()
    }

    /** 批量取消收藏选中歌曲 */
    fun dislikeSelectedSongs() {
        viewModelScope.launch {
            val ids = _selectedSongIds.value.toList()
            if (ids.isNotEmpty()) {
                songRepository.likeSongs(ids, false)
            }
        }
    }

    /** 播放选中的歌曲 */
    fun playSelectedSongs() {
        viewModelScope.launch {
            val ids = _selectedSongIds.value.toList()
            if (ids.isEmpty()) return@launch
            // 根据选中的 songId 找到对应的 mediaStoreId
            val keyword = _searchKeyword.value.ifBlank { null }
            val allMediaIds = songRepository.getLikeMediaStoreIds(keyword)
            val songIdToMediaId =
                songRepository
                    .getLikeSongIds(keyword)
                    .zip(allMediaIds)
                    .toMap()
            val selectedMediaIds = ids.mapNotNull { songIdToMediaId[it]?.toString() }
            if (selectedMediaIds.isNotEmpty()) {
                player.doAction(
                    PlayerAction.UpdateList(
                        mediaIds = selectedMediaIds,
                        mediaId = selectedMediaIds.first(),
                        start = true,
                    ),
                )
            }
        }
    }

    /** 将选中歌曲生成为新歌单 */
    fun createPlaylistFromSelected(playlistName: String) {
        viewModelScope.launch {
            val ids = _selectedSongIds.value.toList()
            if (ids.isEmpty()) return@launch
            try {
                val playlistId = playlistRepository.createPlaylist(playlistName)
                playlistRepository.addSongsToPlaylist(playlistId, ids)
                _snackbarMessage.value =
                    app.getString(R.string.saved_as_playlist_format, playlistName)
            } catch (e: Exception) {
                Timber.e(e, "创建歌单失败")
                _snackbarMessage.value = app.getString(R.string.save_failed)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
