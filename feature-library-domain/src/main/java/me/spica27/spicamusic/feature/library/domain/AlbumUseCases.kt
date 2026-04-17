package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IAlbumRepository

class AlbumUseCases(
    private val repository: IAlbumRepository,
) : IAlbumRepository by repository
