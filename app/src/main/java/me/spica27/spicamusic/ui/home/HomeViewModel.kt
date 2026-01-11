package me.spica27.spicamusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import me.spica27.spicamusic.common.entity.Playlist
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository

/**
 * 首页 ViewModel
 * 负责管理首页的数据和业务逻辑
 * 播放器相关操作请使用 PlayerViewModel
 */
class HomeViewModel(
    private val songRepository: ISongRepository,
    private val playlistRepository: IPlaylistRepository,
) : ViewModel() {
    /**
     * 所有歌曲
     */
    val allSongs: StateFlow<List<Song>> =
        songRepository
            .getAllSongsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 喜欢的歌曲
     */
    val favoriteSongs: StateFlow<List<Song>> =
        songRepository
            .getAllLikeSongsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 常听的歌曲
     */
    val frequentSongs: StateFlow<List<Song>> =
        songRepository
            .getOftenListenSong10Flow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    /**
     * 所有歌单
     */
    val playlists: StateFlow<List<Playlist>> =
        playlistRepository
            .getAllPlaylistsFlow()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
}
