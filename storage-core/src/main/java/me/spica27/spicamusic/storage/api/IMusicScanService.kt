package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow

/**
 * 音乐扫描结果
 */
data class ScanResult(
    val totalScanned: Int,
    val newAdded: Int,
    val updated: Int,
    val removed: Int,
)

/**
 * 扫描进度
 */
data class ScanProgress(
    val current: Int,
    val total: Int,
    val currentFile: String,
)

/**
 * 音乐扫描服务接口 - 支持多种扫描来源
 */
interface IMusicScanService {
    /**
     * 扫描媒体库（默认使用 MediaStore）
     * @return 扫描结果
     */
    suspend fun scanMediaStore(): ScanResult

    /**
     * 扫描指定文件夹（预留接口，用于后续扩展）
     * @param folderPath 文件夹路径
     * @return 扫描结果
     */
    suspend fun scanFolder(folderPath: String): ScanResult

    /**
     * 扫描多个文件夹
     * @param folderPaths 文件夹路径列表
     * @return 扫描结果
     */
    suspend fun scanFolders(folderPaths: List<String>): ScanResult

    /**
     * 获取扫描进度 Flow
     */
    fun getScanProgress(): Flow<ScanProgress?>

    /**
     * 是否正在扫描
     */
    fun isScanning(): Flow<Boolean>

    /**
     * 取消当前扫描
     */
    fun cancelScan()
}
