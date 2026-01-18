package me.spica27.spicamusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository
import timber.log.Timber

/**
 * 当前播放列表面板 ViewModel
 */
class CurrentPlaylistPanelViewModel(
    private val playlistRepository: IPlaylistRepository,
    private val songRepository: ISongRepository,
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
                val songIds =
                    mediaIds.mapNotNull { mediaId ->
                        val mediaStoreId = mediaId.toLongOrNull() ?: return@mapNotNull null
                        songRepository.getSongByMediaStoreId(mediaStoreId)?.songId
                    }
                if (songIds.isEmpty()) {
                    onComplete(false)
                    return@launch
                }
                val playlistId = playlistRepository.createPlaylist(name.trim())
                playlistRepository.addSongsToPlaylist(playlistId, songIds)
                onComplete(true)
            } catch (e: Exception) {
                Timber.e(e, "创建歌单失败")
                onComplete(false)
            }
        }
    }
}
