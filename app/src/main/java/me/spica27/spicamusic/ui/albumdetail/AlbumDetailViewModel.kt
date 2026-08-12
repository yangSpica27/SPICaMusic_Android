package me.spica27.spicamusic.ui.albumdetail

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Album
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.feature.library.domain.AlbumUseCases
import me.spica27.spicamusic.feature.library.domain.PlaylistUseCases
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import timber.log.Timber

@Stable
class AlbumDetailViewModel(
    private val albumId: String,
    private val albumRepository: AlbumUseCases,
    private val playlistRepository: PlaylistUseCases,
    private val player: PlayerUseCases,
) : ViewModel() {
    val songs: StateFlow<List<Song>> =
        albumRepository.getAlbumSongsFlow(albumId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /**
     * 同一歌手的其他专辑
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val otherAlbums: StateFlow<List<Album>> =
        songs
            .map { it.firstOrNull()?.artist.orEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { artist ->
                if (artist.isBlank()) {
                    flowOf(emptyList())
                } else {
                    albumRepository
                        .getAlbumsByArtistFlow(artist)
                        .map { albums -> albums.filter { it.id != albumId } }
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /** 全部歌单：专辑"存为歌单"面板的选择目标，随歌单增删自动刷新 */
    val playlists: StateFlow<List<Playlist>> =
        playlistRepository.getAllPlaylistsFlow().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    /** 面板内的勾选集合（媒体库 ID），跨重组保留 */
    private val _pickerSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val pickerSelectedIds: StateFlow<Set<Long>> = _pickerSelectedIds.asStateFlow()

    /** 一次性提示消息（保存成功/失败 toast），消费后需调用 [clearToast] */
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun togglePickerSelection(playlistId: Long) {
        _pickerSelectedIds.value =
            if (playlistId in _pickerSelectedIds.value) {
                _pickerSelectedIds.value - playlistId
            } else {
                _pickerSelectedIds.value + playlistId
            }
    }

    fun clearPickerSelection() {
        _pickerSelectedIds.value = emptySet()
    }

    fun playAll() {
        val songList = songs.value
        if (songList.isEmpty()) return
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    start = true,
                ),
            )
        }
    }

    /** 当前正在播放本专辑时切换播放/暂停，否则从头播放整张专辑 */
    fun togglePlayPause() {
        val songList = songs.value
        if (songList.isEmpty()) return
        viewModelScope.launch {
            player.doAction(PlayerAction.PlayOrPause)
        }
    }

    fun playSongInList(song: Song) {
        val songList = songs.value
        viewModelScope.launch {
            player.doAction(
                PlayerAction.UpdateList(
                    songList.map { it.mediaStoreId.toString() },
                    mediaId = song.mediaStoreId.toString(),
                    start = true,
                ),
            )
        }
    }

    /** 将整张专辑写入新建歌单 */
    fun saveAsNewPlaylist(name: String) {
        val trimmed = name.trim()
        val songList = songs.value
        if (trimmed.isBlank() || songList.isEmpty()) return
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(trimmed)
                playlistRepository.addSongsToPlaylist(
                    playlistId = playlistId,
                    mediaIds = songList.map { it.mediaStoreId },
                )
                _toastMessage.value = App.getInstance().getString(R.string.saved_as_playlist_format, trimmed)
            } catch (e: Exception) {
                Timber.e(e, "专辑存为歌单失败")
                _toastMessage.value = App.getInstance().getString(R.string.save_failed)
            }
        }
    }

    /** 将整张专辑追加到已有歌单 */
    fun addToPlaylist(playlistId: Long) {
        val songList = songs.value
        if (songList.isEmpty()) return
        val playlistName =
            playlists.value
                .firstOrNull { it.playlistId == playlistId }
                ?.playlistName
                .orEmpty()
        viewModelScope.launch {
            try {
                playlistRepository.addSongsToPlaylist(
                    playlistId = playlistId,
                    mediaIds = songList.map { it.mediaStoreId },
                )
                _toastMessage.value =
                    App.getInstance().getString(R.string.added_to_playlist_format, playlistName)
            } catch (e: Exception) {
                Timber.e(e, "专辑写入歌单失败")
                _toastMessage.value = App.getInstance().getString(R.string.save_failed)
            }
        }
    }

    /** 将整张专辑批量写入勾选的多个歌单 */
    fun addToSelectedPlaylists() {
        val ids = _pickerSelectedIds.value
        val songList = songs.value
        if (ids.isEmpty() || songList.isEmpty()) return
        viewModelScope.launch {
            try {
                ids.forEach { playlistId ->
                    playlistRepository.addSongsToPlaylist(
                        playlistId = playlistId,
                        mediaIds = songList.map { it.mediaStoreId },
                    )
                }
                _toastMessage.value =
                    App.getInstance().getString(R.string.added_to_playlists_count_format, ids.size)
            } catch (e: Exception) {
                Timber.e(e, "专辑批量写入歌单失败")
                _toastMessage.value = App.getInstance().getString(R.string.save_failed)
            } finally {
                _pickerSelectedIds.value = emptySet()
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
