package me.spica27.spicamusic.viewModel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import me.spica27.spicamusic.db.dao.PlaylistDao
import me.spica27.spicamusic.db.dao.SongDao
import me.spica27.spicamusic.db.entity.PlaylistSongCrossRef
import me.spica27.spicamusic.db.entity.Song

class SelectSongViewModel(
    private val songDao: SongDao,
    private val playlistDao: PlaylistDao,
) : ViewModel() {
    private val playlistId = MutableStateFlow(-1L)

    private val _keyword = MutableStateFlow("")

    fun setKeyword(keyword: String) {
        _keyword.value = keyword
    }

    val keyword = _keyword

    private val _selectedSongsIds = MutableStateFlow<List<Long>>(emptyList())

    val selectedSongsIds = _selectedSongsIds

    @OptIn(ExperimentalCoroutinesApi::class)
    val allSongsFlow =
        combine(
            playlistId.flatMapLatest { playlistId ->
                songDao.getSongsNotInPlayListFlow(playlistId)
            },
            _keyword,
        ) { allSongs, keyword ->
            allSongs.filter {
                it.displayName.contains(keyword) || it.artist.contains(keyword)
            }
        }

    fun setPlaylistId(id: Long) {
        playlistId.value = id
    }

    fun clearSelectedSongs() {
        _selectedSongsIds.value = emptyList()
    }

    fun selectSongs(list: List<Song>) {
        _selectedSongsIds.value = list.map { it.songId ?: -1 }
    }

    // 切换歌曲选择状态
    fun toggleSongSelection(songId: Long?) {
        if (songId == null) return
        if (_selectedSongsIds.value.contains(songId)) {
            _selectedSongsIds.value = _selectedSongsIds.value.filter { it != songId }
        } else {
            _selectedSongsIds.value = _selectedSongsIds.value + songId
        }
    }

    // 添加到播放列表
    suspend fun addSongToPlaylist(playlistId: Long?) {
        playlistDao.insertListItems(
            _selectedSongsIds.value.map { songId -> PlaylistSongCrossRef(playlistId ?: 0, songId) },
        )
        playlistDao.setNeedUpdate(playlistId ?: -1)
    }
}
