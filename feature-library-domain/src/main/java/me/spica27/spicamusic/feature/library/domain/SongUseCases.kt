package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.ISongRepository

class SongUseCases(
    private val repository: ISongRepository,
) : ISongRepository by repository
