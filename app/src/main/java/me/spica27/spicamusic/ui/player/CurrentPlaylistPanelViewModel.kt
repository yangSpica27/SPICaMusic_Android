package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import timber.log.Timber

/**
 * 当前播放列表面板 ViewModel
 */
@Stable
class CurrentPlaylistPanelViewModel(
    private val playlistRepository: PlaylistUseCases,
) : ViewModel() {
    /**
     * 根据 MediaItem 的 mediaId 列表创建歌单并添加歌曲
     */
    fun createPlaylistWithMediaIds(
        name: String,
        mediaIds: List<String>,
        onComplete: (Boolean) -> Unit = {},
    ) {
        if (name.isBlank()) {
            onComplete(false)
            return
        }
        if (mediaIds.isEmpty()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            try {
                val mediaIds = mediaIds.mapNotNull { it.toLongOrNull() }
                if (mediaIds.isEmpty()) {
                    onComplete(false)
                    return@launch
                }
                val playlistId = playlistRepository.createPlaylist(name.trim())
                playlistRepository.addSongsToPlaylist(playlistId, mediaIds)
                onComplete(true)
            } catch (e: Exception) {
                Timber.e(e, "创建歌单失败")
                onComplete(false)
            }
        }
    }
}
