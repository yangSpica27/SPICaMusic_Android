package me.spica27.spicamusic.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.IMusicScanService

class MusicScanUseCases(
    private val scanService: IMusicScanService,
) {
    suspend fun scanMediaStore(): ScanResult = scanService.scanMediaStore().toDomain()

    suspend fun scanExtraFolders(): ScanResult = scanService.scanExtraFolders().toDomain()

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
