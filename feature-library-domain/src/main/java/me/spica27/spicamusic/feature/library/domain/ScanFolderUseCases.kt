package me.spica27.spicamusic.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.IScanFolderRepository

class ScanFolderUseCases(
    private val repository: IScanFolderRepository,
) {
    fun getExtraFoldersFlow(): Flow<List<ScanFolder>> = repository.getExtraFoldersFlow().map { folders -> folders.map { it.toDomain() } }

    fun getIgnoreFoldersFlow(): Flow<List<ScanFolder>> = repository.getIgnoreFoldersFlow().map { folders -> folders.map { it.toDomain() } }

    suspend fun addFolder(
        uriString: String,
        displayName: String,
        folderType: FolderType,
        pathPrefix: String?,
    ) {
        repository.addFolder(uriString, displayName, folderType.toData(), pathPrefix)
    }

    suspend fun removeFolder(id: Long) {
        repository.removeFolder(id)
    }

    suspend fun reAuthorize(
        id: Long,
        newUriString: String,
    ) {
        repository.reAuthorize(id, newUriString)
    }
}
