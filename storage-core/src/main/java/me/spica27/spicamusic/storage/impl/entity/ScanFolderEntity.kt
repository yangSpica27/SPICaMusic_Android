package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ScanFolder")
data class ScanFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    /** SAF tree URI 字符串，通过 takePersistableUriPermission 持久化 */
    val uriString: String,
    /** 展示名称 */
    val displayName: String,
    /** 文件夹类型：0 = EXTRA（额外扫描），1 = IGNORE（忽略） */
    val folderType: Int,
    /** 解析出的绝对路径前缀，用于 MediaStore DATA 列过滤；SD 卡等无法解析时为 null */
    val pathPrefix: String?,
    val addedAt: Long = System.currentTimeMillis(),
    /** SAF 读权限是否仍然有效，仅对 EXTRA 类型有意义 */
    val isAccessible: Boolean = true,
)
