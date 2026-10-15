package me.spica27.spicamusic.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.IMusicScanService

class MusicScanUseCases(
    private val scanService: IMusicScanService,
) {
    suspend fun scanMediaStore(): ScanResult = scanService.scanMediaStore().toDomain()

    suspend fun scanExtraFolders(): ScanResult = scanService.scanExtraFolders().toDomain()

    /** 启动检查：需要时重建曲库或艺人 / 专辑关系（无权限或已是最新则内部直接返回） */
    suspend fun ensureLibraryUpToDate() {
        scanService.ensureLibraryUpToDate()
    }

    fun getScanProgress(): Flow<ScanProgress?> = scanService.getScanProgress().map { it?.toDomain() }

    fun cancelScan() {
        scanService.cancelScan()
    }

    fun startMediaStoreObserver() {
        scanService.startMediaStoreObserver()
    }

    fun stopMediaStoreObserver() {
        scanService.stopMediaStoreObserver()
    }
}
