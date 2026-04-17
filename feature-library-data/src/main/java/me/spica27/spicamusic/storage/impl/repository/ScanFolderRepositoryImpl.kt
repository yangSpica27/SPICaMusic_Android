package me.spica27.spicamusic.storage.impl.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.FolderType
import me.spica27.spicamusic.storage.api.IScanFolderRepository
import me.spica27.spicamusic.storage.api.ScanFolder
import me.spica27.spicamusic.storage.impl.dao.ScanFolderDao
import me.spica27.spicamusic.storage.impl.entity.ScanFolderEntity

class ScanFolderRepositoryImpl(
    private val scanFolderDao: ScanFolderDao,
) : IScanFolderRepository {

    override fun getExtraFoldersFlow(): Flow<List<ScanFolder>> =
        scanFolderDao.getByType(FolderType.EXTRA.value).map { it.map(ScanFolderEntity::toScanFolder) }

    override fun getIgnoreFoldersFlow(): Flow<List<ScanFolder>> =
        scanFolderDao.getByType(FolderType.IGNORE.value).map { it.map(ScanFolderEntity::toScanFolder) }

    override suspend fun getExtraFoldersSync(): List<ScanFolder> =
        scanFolderDao.getByTypeSync(FolderType.EXTRA.value).map(ScanFolderEntity::toScanFolder)

    override suspend fun getIgnoreFoldersSync(): List<ScanFolder> =
        scanFolderDao.getByTypeSync(FolderType.IGNORE.value).map(ScanFolderEntity::toScanFolder)

    override suspend fun addFolder(
        uriString: String,
        displayName: String,
        folderType: FolderType,
        pathPrefix: String?,
    ) {
        scanFolderDao.insert(
            ScanFolderEntity(
                uriString = uriString,
                displayName = displayName,
                folderType = folderType.value,
                pathPrefix = pathPrefix,
            )
        )
    }

    override suspend fun removeFolder(id: Long) = scanFolderDao.deleteById(id)

    override suspend fun markInaccessible(id: Long) = scanFolderDao.markInaccessible(id)

    override suspend fun reAuthorize(id: Long, newUriString: String) =
        scanFolderDao.reAuthorize(id, newUriString)
}

private fun ScanFolderEntity.toScanFolder() = ScanFolder(
    id = id ?: 0L,
    uriString = uriString,
    displayName = displayName,
    folderType = if (folderType == FolderType.IGNORE.value) FolderType.IGNORE else FolderType.EXTRA,
    pathPrefix = pathPrefix,
    addedAt = addedAt,
    isAccessible = isAccessible,
)
