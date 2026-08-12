package me.spica27.spicamusic.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.spica27.spicamusic.storage.api.IMusicScanService

class MusicScanUseCases(
    private val scanService: IMusicScanService,
) {
    suspend fun scanMediaStore(): ScanResult = scanService.scanMediaStore().toDomain()

    suspend fun scanExtraFolders(): ScanResult = scanService.scanExtraFolders().toDomain()

    /** 扫描 schema 版本升级时的启动静默补扫（无权限/版本已是最新则内部直接返回） */
    suspend fun syncIfSchemaVersionChanged() {
        scanService.syncIfSchemaVersionChanged()
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
