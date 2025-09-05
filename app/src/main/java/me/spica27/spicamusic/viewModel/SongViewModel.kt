package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.db.entity.Playlist
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.PlaylistRepository
import me.spica27.spicamusic.repository.SongRepository
import timber.log.Timber

class SongViewModel(
    private val songRepository: SongRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {
    fun getSongFlow(id: Long) = songRepository.songFlowWithId(id)

    // 所有收藏的歌曲
    val allLikeSong: StateFlow<List<Song>> =
        songRepository
            .allLikeSongFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 所有歌单
    val allPlayList: StateFlow<List<Playlist>> =
        playlistRepository
            .getAllPlaylistFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val oftenListenSongs10: StateFlow<List<Song>> =
        songRepository
            .oftenListenSong10Flow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val oftenListenSongs: StateFlow<List<Song>> =
        songRepository
            .oftenListenSongFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val randomSongs: StateFlow<List<Song>> =
        songRepository
            .randomSongFlow()
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val ignoreSongs: StateFlow<List<Song>> =
        songRepository.ignoreSongFlow().stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            emptyList(),
        )

    // 切换喜欢状态
    fun toggleFavorite(id: Long) {
        Timber.e("toggleFavorite: $id")
        viewModelScope.launch(Dispatchers.IO) {
            songRepository.toggleLike(id)
        }
    }

    fun songLikeFlow(id: Long) = songRepository.songLikeFlowWithId(id).distinctUntilChanged()

    // 忽略歌曲
    fun ignore(
        id: Long,
        isIgnore: Boolean,
    ) {
        viewModelScope.launch {
            songRepository.ignore(id, isIgnore)
        }
    }

    // 添加歌单
    fun addPlayList(value: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(value)
        }
    }
}
