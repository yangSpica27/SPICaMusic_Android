package me.spica27.spicamusic.storage.impl.repository

import android.content.Context
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.LocalLyricFile
import timber.log.Timber

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
        withContext(Dispatchers.IO) {
            try {
                val parsedUri = uri.toUri()
                val text =
                    context.contentResolver.openInputStream(parsedUri)?.use { input ->
                        input.readBytes().toString(Charsets.UTF_8)
                    }?.takeIf { it.isNotBlank() } ?: return@withContext null
                LocalLyricFile(text = text, displayName = queryDisplayName(parsedUri) ?: uri.substringAfterLast('/'))
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "读取本地歌词文件失败 uri=$uri")
                null
            }
        }

    /** 查询 SAF 文档显示名，用于面板展示与 sourceUri 记录 */
    private fun queryDisplayName(uri: android.net.Uri): String? =
        try {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx) else null
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "查询本地文件名失败")
            null
        }

    companion object {
        private const val TAG = "LyricSourceReader"

        /** TagLib propertyMap 中可能承载歌词的键，大写；按优先级排列 */
        private val LYRIC_KEYS = listOf("LYRICS", "UNSYNCEDLYRICS", "USLT")
    }
}
