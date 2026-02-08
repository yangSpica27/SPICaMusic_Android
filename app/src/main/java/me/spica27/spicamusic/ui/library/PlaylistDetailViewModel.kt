package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.player.api.IMusicPlayer
import me.spica27.spicamusic.player.api.PlayerAction
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import timber.log.Timber

/**
 * 歌单详情页面 ViewModel
 */
class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val playlistRepository: IPlaylistRepository,
    private val player: IMusicPlayer,
    private val songRepository: ISongRepository,
) : ViewModel() {
    // 歌单信息
    val playlist: StateFlow<Playlist?> =
        playlistRepository.getPlaylistByIdFlow(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null,
        )

    // 歌单中的歌曲列表
    val songs: StateFlow<List<Song>> =
        playlistRepository.getSongsByPlaylistIdFlow(playlistId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

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

    // 是否显示添加歌曲选择器
    private val _showAddSongsSheet = MutableStateFlow(false)
    val showAddSongsSheet = _showAddSongsSheet.asStateFlow()

    /**
     * 播放歌单所有歌曲
     */
    fun playAll() {
        val songList = songs.value
        if (songList.isEmpty()) {
            Timber.w("歌单为空，无法播放")
            return
        }

        viewModelScope.launch {
            try {
                // 增加播放次数
                playlistRepository.incrementPlaylistPlayTime(playlistId)
                // 播放第一首歌曲
                player.doAction(PlayerAction.PlayById(songList.first().mediaStoreId.toString()))
                Timber.d("开始播放歌单")
            } catch (e: Exception) {
                Timber.e(e, "播放歌单失败")
            }
        }
    }

    /**
     * 播放指定歌曲
     */
    fun playSong(song: Song) {
        viewModelScope.launch {
            try {
                player.doAction(PlayerAction.PlayById(song.mediaStoreId.toString()))
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
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) {
            // 退出多选模式时清空选择
            _selectedSongs.value = emptySet()
        }
    }

    /**
     * 切换歌曲选中状态
     */
    fun toggleSongSelection(songId: Long?) {
        if (songId == null) return
        val current = _selectedSongs.value.toMutableSet()
        if (current.contains(songId)) {
            current.remove(songId)
        } else {
            current.add(songId)
        }
        _selectedSongs.value = current
    }

    /**
     * 全选
     */
    fun selectAll() {
        _selectedSongs.value = songs.value.map { it.songId ?: -1 }.toSet()
    }

    /**
     * 取消全选
     */
    fun deselectAll() {
        _selectedSongs.value = emptySet()
    }

    /**
     * 从歌单中移除选中的歌曲
     */
    fun removeSelectedSongs() {
        val selected = _selectedSongs.value
        if (selected.isEmpty()) return

        viewModelScope.launch {
            try {
                selected.forEach { songId ->
                    playlistRepository.removeSongFromPlaylist(playlistId, songId)
                }
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
     * 批量添加歌曲到歌单
     */
    fun addSongsToPlaylist(songIds: List<Long>) {
        if (songIds.isEmpty()) return

        viewModelScope.launch {
            try {
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                Timber.d("成功添加 ${songIds.size} 首歌曲到歌单")
                hideAddSongsSheet()
            } catch (e: Exception) {
                Timber.e(e, "添加歌曲到歌单失败")
            }
        }
    }
}
