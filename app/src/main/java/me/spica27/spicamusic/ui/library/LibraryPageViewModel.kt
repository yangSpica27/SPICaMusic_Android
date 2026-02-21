package me.spica27.spicamusic.ui.library

import androidx.lifecycle.ViewModel
import me.spica27.spicamusic.storage.api.IAlbumRepository
import me.spica27.spicamusic.storage.api.IPlayHistoryRepository
import me.spica27.spicamusic.storage.api.IPlaylistRepository
import me.spica27.spicamusic.storage.api.ISongRepository

class LibraryPageViewModel(
    private val songRepositoryImpl: ISongRepository,
    private val albumRepositoryImpl: IAlbumRepository,
    private val playlistRepositoryImpl: IPlaylistRepository,
    private val historyRepository: IPlayHistoryRepository,
) : ViewModel() {
    val albumList = albumRepositoryImpl.getAllPagingFlow()

    val playlists = playlistRepositoryImpl.getAllPlaylistsFlow()
}
