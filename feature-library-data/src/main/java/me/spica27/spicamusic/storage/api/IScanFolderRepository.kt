package me.spica27.spicamusic.storage.api

import kotlinx.coroutines.flow.Flow

/** 文件夹用途类型 */
enum class FolderType(val value: Int) {
    /** 额外扫描：遍历此文件夹并注册未收录的音频 */
    EXTRA(0),
    /** 忽略：此路径下的音频在所有扫描中均跳过 */
    IGNORE(1),
}

data class ScanFolder(
    val id: Long,
    val uriString: String,
    val displayName: String,
    val folderType: FolderType,
    /** 解析出的绝对路径，用于 MediaStore DATA 列前缀匹配；null 表示无法解析（SD 卡等） */
    val pathPrefix: String?,
    val addedAt: Long,
    /** SAF 权限是否有效；仅 EXTRA 有意义 */
    val isAccessible: Boolean,
)

interface IScanFolderRepository {
    fun getExtraFoldersFlow(): Flow<List<ScanFolder>>
    fun getIgnoreFoldersFlow(): Flow<List<ScanFolder>>
    suspend fun getExtraFoldersSync(): List<ScanFolder>
    suspend fun getIgnoreFoldersSync(): List<ScanFolder>
    suspend fun addFolder(
        uriString: String,
        displayName: String,
        folderType: FolderType,
        pathPrefix: String?,
    )
    suspend fun removeFolder(id: Long)
    suspend fun markInaccessible(id: Long)
    suspend fun reAuthorize(id: Long, newUriString: String)
}
