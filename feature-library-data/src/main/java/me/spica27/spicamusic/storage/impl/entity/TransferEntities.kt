package me.spica27.spicamusic.storage.impl.entity

import androidx.room.Entity
import androidx.room.Index

/** 保存不依赖设备 MediaStore ID 的歌曲与歌单标识。 */
@Entity(tableName = "TransferIdentity", primaryKeys = ["kind", "stableId"], indices = [Index(value = ["kind", "localId"])])
data class TransferIdentityEntity(
    val kind: String,
    val stableId: String,
    val localId: Long,
    val sourcesJson: String = "[]",
)

/** 与导入数据同事务写入，用于识别已提交的任务。 */
@Entity(tableName = "TransferReceipt", primaryKeys = ["taskId"])
data class TransferReceiptEntity(
    val taskId: String,
    val archiveId: String,
    val songCount: Int,
    val playlistCount: Int,
)
