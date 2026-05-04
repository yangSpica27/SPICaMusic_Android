package me.spica27.spicamusic.feature.library.domain

import androidx.compose.runtime.Immutable
import me.spica27.spicamusic.storage.api.FolderType as StorageFolderType
import me.spica27.spicamusic.storage.api.ScanFolder as StorageScanFolder
import me.spica27.spicamusic.storage.api.ScanProgress as StorageScanProgress
import me.spica27.spicamusic.storage.api.ScanResult as StorageScanResult

enum class FolderType {
    EXTRA,
    IGNORE,
}

@Immutable
data class ScanFolder(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val folderType: FolderType,
    val pathPrefix: String?,
    val addedAt: Long,
    val isAccessible: Boolean,
)

@Immutable
data class ScanResult(
    val totalScanned: Int,
    val newAdded: Int,
    val updated: Int,
    val removed: Int,
)

@Immutable
data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
)

internal fun StorageFolderType.toDomain(): FolderType =
    when (this) {
        StorageFolderType.EXTRA -> FolderType.EXTRA
        StorageFolderType.IGNORE -> FolderType.IGNORE
    }

internal fun FolderType.toData(): StorageFolderType =
    when (this) {
        FolderType.EXTRA -> StorageFolderType.EXTRA
        FolderType.IGNORE -> StorageFolderType.IGNORE
    }

internal fun StorageScanFolder.toDomain(): ScanFolder =
    ScanFolder(
        id = id,
        uriString = uriString,
        displayName = displayName,
        folderType = folderType.toDomain(),
        pathPrefix = pathPrefix,
        addedAt = addedAt,
        isAccessible = isAccessible,
    )

internal fun StorageScanResult.toDomain(): ScanResult =
    ScanResult(
        totalScanned = totalScanned,
        newAdded = newAdded,
        updated = updated,
        removed = removed,
    )

internal fun StorageScanProgress.toDomain(): ScanProgress =
    ScanProgress(
        current = current,
        total = total,
        currentFile = currentFile,
    )
