package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IPlayHistoryRepository

class PlayHistoryUseCases(
    private val repository: IPlayHistoryRepository,
) : IPlayHistoryRepository by repository
