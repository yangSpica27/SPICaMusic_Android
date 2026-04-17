package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IMusicScanService

class MusicScanUseCases(
    private val scanService: IMusicScanService,
) : IMusicScanService by scanService
