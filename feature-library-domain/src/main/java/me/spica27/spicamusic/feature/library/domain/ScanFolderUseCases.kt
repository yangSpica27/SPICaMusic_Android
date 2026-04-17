package me.spica27.spicamusic.feature.library.domain

import me.spica27.spicamusic.storage.api.IScanFolderRepository

class ScanFolderUseCases(
    private val repository: IScanFolderRepository,
) : IScanFolderRepository by repository
