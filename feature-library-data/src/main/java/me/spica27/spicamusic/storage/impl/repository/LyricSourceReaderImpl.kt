package me.spica27.spicamusic.storage.impl.repository

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.kyant.taglib.TagLib
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.LocalLyricFile
import me.spica27.spicamusic.storage.api.LocalLyricReadResult
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

/**
 * [ILyricSourceReader] 实现。
 *
 * 内嵌歌词读取复用 [me.spica27.spicamusic.storage.impl.scanner.MusicScanService] 里的 TagLib FD 范式：
 * 只打开一次 FileDescriptor，detachFd() 将所有权转移给 TagLib。
 */
class LyricSourceReaderImpl(
    private val context: Context,
) : ILyricSourceReader {

    override suspend fun readEmbedded(mediaStoreId: Long): String? =
        withContext(Dispatchers.IO) {
            try {
                val uri = "content://media/external/audio/media/$mediaStoreId".toUri()
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    // detachFd() 将 fd 所有权转移给 TagLib，TagLib 用完后负责关闭
                    val fd = pfd.detachFd()
                    val metadata = TagLib.getMetadata(fd = fd, readPictures = false)
                    val map = metadata?.propertyMap ?: return@use null
                    // 不同封装/标签写法的歌词键，按常见优先级探测
                    LYRIC_KEYS
                        .asSequence()
                        .mapNotNull { key -> map[key]?.firstOrNull()?.takeIf { it.isNotBlank() } }
                        .firstOrNull()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "读取内嵌歌词失败 mediaStoreId=$mediaStoreId")
                null
            }
        }

    override suspend fun readLocalFile(uri: String): LocalLyricFile? =
        (readLocalFileResult(uri) as? LocalLyricReadResult.Success)?.file

    override suspend fun readLocalFileResult(uri: String): LocalLyricReadResult =
        withContext(Dispatchers.IO) {
            try {
                val parsedUri = uri.toUri()
                val metadata = queryMetadata(parsedUri)
                val displayName = metadata.displayName
                    ?: uri.substringAfterLast('/').ifBlank { "lyrics" }
                if (!isSupportedFile(displayName, metadata.mimeType)) {
                    Timber.tag(TAG).i("拒绝非歌词文件 name=$displayName mime=${metadata.mimeType}")
                    return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.UNSUPPORTED_FILE,
                    )
                }
                if (metadata.sizeBytes != null && metadata.sizeBytes > MAX_LOCAL_LYRIC_BYTES) {
                    Timber.tag(TAG).i("拒绝过大的歌词文件 name=$displayName size=${metadata.sizeBytes}")
                    return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.FILE_TOO_LARGE,
                    )
                }

                val input = context.contentResolver.openInputStream(parsedUri)
                    ?: return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.READ_FAILED,
                    )
                val bytes = input.use { it.readAtMost(MAX_LOCAL_LYRIC_BYTES) }
                    ?: return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.FILE_TOO_LARGE,
                    )
                val decoded = decodeText(bytes)
                    ?: return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.BINARY_FILE,
                    )
                val text = decoded.takeIf { it.isNotBlank() }
                    ?: return@withContext LocalLyricReadResult.Failure(
                        LocalLyricReadResult.FailureReason.EMPTY_FILE,
                    )
                LocalLyricReadResult.Success(
                    LocalLyricFile(
                        text = text,
                        displayName = displayName,
                        mimeType = metadata.mimeType,
                        sizeBytes = metadata.sizeBytes ?: bytes.size.toLong(),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "读取本地歌词文件失败 uri=$uri")
                LocalLyricReadResult.Failure(LocalLyricReadResult.FailureReason.READ_FAILED)
            }
        }

    private data class FileMetadata(
        val displayName: String?,
        val sizeBytes: Long?,
        val mimeType: String?,
    )

    /** 查询 SAF 元信息。部分 provider 不提供 SIZE，因此读取时仍需流式限制大小。 */
    private fun queryMetadata(uri: android.net.Uri): FileMetadata {
        return try {
            val resolver = context.contentResolver
            var displayName: String? = null
            var sizeBytes: Long? = null
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    displayName = nameIndex.takeIf { it >= 0 }?.let(cursor::getString)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    sizeBytes = sizeIndex.takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let(cursor::getLong)
                        ?.takeIf { it >= 0L }
                }
            }
            FileMetadata(displayName, sizeBytes, resolver.getType(uri)?.lowercase())
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "查询本地文件元信息失败")
            FileMetadata(null, null, context.contentResolver.getType(uri)?.lowercase())
        }
    }

    private fun isSupportedFile(displayName: String, mimeType: String?): Boolean {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        // 有扩展名时必须是歌词扩展名；无扩展名文件仅依赖安全的文本 MIME。
        if (extension.isNotEmpty() && extension !in SUPPORTED_EXTENSIONS) return false
        // 已知歌词扩展名优先于 provider 的 MIME（部分 DocumentsProvider 会误报 MIME）。
        // 内容仍会经过严格解码、二进制检测和格式解析，伪装的媒体文件不会因此放行。
        if (extension in SUPPORTED_EXTENSIONS) return true
        val mime = mimeType?.substringBefore(';')?.trim()?.lowercase()
        if (mime == null || mime == "application/octet-stream") return extension.isEmpty()
        if (mime.startsWith("audio/") || mime.startsWith("video/") || mime.startsWith("image/")) return false
        return mime.startsWith("text/") || mime == "application/xml" || mime.endsWith("+xml")
    }

    private fun InputStream.readAtMost(limit: Long): ByteArray? {
        val output = ByteArrayOutputStream(minOf(limit, 8192L).toInt())
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            if (count == 0) {
                val single = read()
                if (single < 0) break
                total++
                if (total > limit) return null
                output.write(single)
                continue
            }
            total += count
            if (total > limit) return null
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun decodeText(bytes: ByteArray): String? {
        val (charset, offset) = when {
            bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte() ->
                Charsets.UTF_8 to 3
            bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() ->
                Charsets.UTF_16LE to 2
            bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() ->
                Charsets.UTF_16BE to 2
            else -> Charsets.UTF_8 to 0
        }
        val payload = bytes.copyOfRange(offset, bytes.size)
        val decoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(payload)).toString()
        } catch (_: Exception) {
            return null
        }
        if (text.any { it == '\u0000' || (it < ' ' && it != '\n' && it != '\r' && it != '\t') }) {
            return null
        }
        return text.removePrefix("\uFEFF")
    }

    companion object {
        private const val TAG = "LyricSourceReader"
        private const val MAX_LOCAL_LYRIC_BYTES = 4L * 1024L * 1024L

        private val SUPPORTED_EXTENSIONS = setOf("lrc", "ttml", "ttml2", "yrc", "txt", "xml")

        /** TagLib propertyMap 中可能承载歌词的键，大写；按优先级排列 */
        private val LYRIC_KEYS = listOf("LYRICS", "UNSYNCEDLYRICS", "USLT")
    }
}
