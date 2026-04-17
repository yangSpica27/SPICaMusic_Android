package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IPlaylistRepository

class PlaylistUseCases(
    private val repository: IPlaylistRepository,
) : IPlaylistRepository by repository
