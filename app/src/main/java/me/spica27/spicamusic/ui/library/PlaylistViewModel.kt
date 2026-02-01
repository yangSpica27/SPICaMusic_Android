package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import timber.log.Timber

/**
 * 歌单页面 ViewModel
 */
class PlaylistViewModel(
    private val playlistRepository: IPlaylistRepository,
) : ViewModel() {
    // 所有歌单列表
    val playlists: StateFlow<List<Playlist>> =
        playlistRepository.getAllPlaylistsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    // 是否显示创建歌单对话框
    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog = _showCreateDialog.asStateFlow()

    // 是否显示删除确认对话框
    private val _showDeleteDialog = MutableStateFlow<Playlist?>(null)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    /**
     * 显示创建歌单对话框
     */
    fun showCreateDialog() {
        _showCreateDialog.value = true
    }

    /**
     * 隐藏创建歌单对话框
     */
    fun hideCreateDialog() {
        _showCreateDialog.value = false
    }

    /**
     * 创建新歌单
     */
    fun createPlaylist(name: String) {
        if (name.isBlank()) {
            Timber.w("歌单名称不能为空")
            return
        }

        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(name.trim())
                Timber.d("创建歌单成功: $name (ID: $playlistId)")
                hideCreateDialog()
            } catch (e: Exception) {
                Timber.e(e, "创建歌单失败")
            }
        }
    }

    /**
     * 显示删除确认对话框
     */
    fun showDeleteDialog(playlist: Playlist) {
        _showDeleteDialog.value = playlist
    }

    /**
     * 隐藏删除确认对话框
     */
    fun hideDeleteDialog() {
        _showDeleteDialog.value = null
    }

    /**
     * 删除歌单
     */
    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            try {
                playlist.playlistId?.let {
                    playlistRepository.deletePlaylist(it)
                    Timber.d("删除歌单成功: ${playlist.playlistName}")
                }
                hideDeleteDialog()
            } catch (e: Exception) {
                Timber.e(e, "删除歌单失败")
            }
        }
    }

    /**
     * 获取歌单内的歌曲列表（用于封面显示）
     */
    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> = playlistRepository.getSongsByPlaylistIdFlow(playlistId)
}
