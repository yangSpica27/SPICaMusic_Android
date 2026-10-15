package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IArtistRepository

class ArtistUseCases(
    private val repository: IArtistRepository,
) : IArtistRepository by repository
