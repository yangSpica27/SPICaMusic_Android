package me.spica27.spicamusic.feature.library.domain

import androidx.compose.runtime.Immutable
import me.spica27.spicamusic.storage.api.FolderType as StorageFolderType
import me.spica27.spicamusic.storage.api.ScanFolder as StorageScanFolder
import me.spica27.spicamusic.storage.api.ScanFormat as StorageScanFormat
import me.spica27.spicamusic.storage.api.ScanProgress as StorageScanProgress
import me.spica27.spicamusic.storage.api.ScanResult as StorageScanResult
import me.spica27.spicamusic.storage.api.ScanRules as StorageScanRules

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

/** 扫描规则：决定哪些音频会被收录进曲库 */
@Immutable
data class ScanRules(
    val minDurationMs: Long,
    val minFileSizeBytes: Long,
    val enabledFormatKeys: Set<String>,
) {
    val minDurationSec: Int get() = (minDurationMs / 1000L).toInt()

    val minFileSizeKb: Int get() = (minFileSizeBytes / 1024L).toInt()

    val allFormatsEnabled: Boolean get() = enabledFormatKeys.containsAll(ScanFormats.allKeys)

    companion object {
        val DEFAULT =
            ScanRules(
                minDurationMs = StorageScanRules.DEFAULT.minDurationMs,
                minFileSizeBytes = StorageScanRules.DEFAULT.minFileSizeBytes,
                enabledFormatKeys = StorageScanRules.DEFAULT.enabledFormatKeys,
            )
    }
}

/** 可选音频格式目录（key 与 data 层 ScanFormat 一致，label 供 UI 展示） */
@Immutable
data class ScanFormatOption(
    val key: String,
    val label: String,
)

object ScanFormats {
    val all: List<ScanFormatOption> =
        StorageScanFormat.entries.map { ScanFormatOption(key = it.key, label = it.name) }

    val allKeys: Set<String> = StorageScanFormat.allKeys
}

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

internal fun StorageScanRules.toDomain(): ScanRules =
    ScanRules(
        minDurationMs = minDurationMs,
        minFileSizeBytes = minFileSizeBytes,
        enabledFormatKeys = enabledFormatKeys,
    )
