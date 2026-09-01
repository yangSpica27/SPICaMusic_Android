package me.spica27.spicamusic.storage.api

/**
 * 歌词来源读取器：负责从"歌词已选缓存(extra_info)"之外的位置读取原始歌词文本。
 * - 内嵌：解析音频文件自带的歌词标签（LYRICS/UNSYNCEDLYRICS/USLT 等）。
 * - 本地文件：读取用户通过 SAF 选择的歌词文本内容。
 *
 * 返回的均为**原始文本**，解析（YRC/LRC/纯文本兜底）由上层统一完成。
 */
interface ILyricSourceReader {
    /**
     * 读取音频文件内嵌歌词。
     * @param mediaStoreId MediaStore 音频 id
     * @return 原始歌词文本；无内嵌歌词或读取失败返回 null
     */
    suspend fun readEmbedded(mediaStoreId: Long): String?

    /**
     * 读取本地歌词文件内容（用于导入时快照入库）。
     * @param uri SAF 文档 URI 字符串
     * @return 文件文本与显示名；读取失败返回 null
     */
    suspend fun readLocalFile(uri: String): LocalLyricFile?

    suspend fun readLocalFileResult(uri: String): LocalLyricReadResult =
        readLocalFile(uri)?.let(LocalLyricReadResult::Success)
            ?: LocalLyricReadResult.Failure(LocalLyricReadResult.FailureReason.READ_FAILED)
}

sealed interface LocalLyricReadResult {
    data class Success(val file: LocalLyricFile) : LocalLyricReadResult

    enum class FailureReason {
        UNSUPPORTED_FILE,
        FILE_TOO_LARGE,
        BINARY_FILE,
        EMPTY_FILE,
        READ_FAILED,
    }

    data class Failure(val reason: FailureReason) : LocalLyricReadResult
}

/** 本地歌词文件读取结果：正文快照 + 用于展示的文件名。 */
data class LocalLyricFile(
    val text: String,
    val displayName: String,
    /** Provider 返回的 MIME；部分 provider 可能返回 null 或 application/octet-stream。 */
    val mimeType: String? = null,
    /** Provider 返回的大小；未知时为 null。 */
    val sizeBytes: Long? = null,
)
