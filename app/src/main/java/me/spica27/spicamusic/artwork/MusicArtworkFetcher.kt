package me.spica27.spicamusic.artwork

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import com.kyant.taglib.TagLib
import com.skydoves.landscapist.core.ImageRequest
import com.skydoves.landscapist.core.model.DataSource
import com.skydoves.landscapist.core.network.FetchResult
import com.skydoves.landscapist.core.network.ImageFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException

/** 用户授权的额外扫描目录：SAF 树 URI 与其对应的绝对路径前缀（无尾部斜杠） */
data class ArtworkTreeRoot(
    val treeUri: Uri,
    val pathPrefix: String,
)

/**
 * 本地曲库封面解析：内嵌图（TagLib，兼容全部格式）→ 同目录图片（File，再经 SAF 树权限）→ MediaStore 缩略图。
 *
 * 一次请求内完成全部回退，UI 只发一个请求；解析失败的键记入负缓存，避免每次滚入都重试。
 */
class MusicArtworkFetcher(
    context: Context,
    private val treeRoots: suspend () -> List<ArtworkTreeRoot>,
) : ImageFetcher {
    private val context = context.applicationContext
    private val resolver = this.context.contentResolver

    /** 近期确认无封面的键（按访问顺序淘汰） */
    private val misses =
        object : LinkedHashMap<String, Unit>(256, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Unit>?): Boolean = size > MAX_MISSES
        }

    override fun canHandle(model: Any?): Boolean = model is MusicArtwork

    override suspend fun fetch(request: ImageRequest): FetchResult {
        val artwork =
            request.model as? MusicArtwork
                ?: return FetchResult.Error(IllegalArgumentException("Model is not a MusicArtwork"))
        val key = artwork.toString()
        if (synchronized(misses) { misses.containsKey(key) }) {
            return FetchResult.Error(NoArtworkException(key))
        }
        return withContext(Dispatchers.IO) {
            val resolved =
                when {
                    artwork.mediaStoreId <= 0L -> resolveAlbumSong(artwork)
                    artwork.path.isBlank() -> resolveSongFile(artwork)
                    else -> artwork
                }
            ensureActive()
            val bytes =
                embeddedPicture(resolved)
                    ?: run {
                        ensureActive()
                        folderImage(resolved)
                    }
                    ?: run {
                        ensureActive()
                        mediaStoreThumbnail(resolved)
                    }
            if (bytes == null) {
                synchronized(misses) { misses[key] = Unit }
                FetchResult.Error(NoArtworkException(key))
            } else {
                FetchResult.Success(data = bytes, mimeType = null, dataSource = DataSource.LOCAL)
            }
        }
    }

    /** 只有专辑 id 时取该专辑曲目号最小的一首作代表 */
    @Suppress("DEPRECATION")
    private fun resolveAlbumSong(artwork: MusicArtwork): MusicArtwork {
        if (artwork.albumId <= 0L) return artwork
        val projection =
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
            )
        return runCatching {
            resolver
                .query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.ALBUM_ID} = ?",
                    arrayOf(artwork.albumId.toString()),
                    "${MediaStore.Audio.Media.TRACK} ASC",
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    artwork.copy(
                        mediaStoreId = cursor.getLong(0),
                        path = cursor.getString(1).orEmpty(),
                        size = cursor.getLong(2),
                    )
                }
        }.getOrNull() ?: artwork
    }

    /** 只有歌曲 id（如歌手代表曲）时补全路径，让目录图回退与歌曲行一致 */
    @Suppress("DEPRECATION")
    private fun resolveSongFile(artwork: MusicArtwork): MusicArtwork =
        runCatching {
            resolver
                .query(
                    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, artwork.mediaStoreId),
                    arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    artwork.copy(
                        path = cursor.getString(0).orEmpty(),
                        size = cursor.getLong(1),
                    )
                }
        }.getOrNull() ?: artwork

    /** 内嵌图：TagLib 优先（FLAC / Ogg / APE 等平台解码器读不到的格式），平台解码器兜底 */
    private fun embeddedPicture(artwork: MusicArtwork): ByteArray? {
        if (artwork.mediaStoreId <= 0L) return null
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, artwork.mediaStoreId)
        return taglibPicture(uri) ?: retrieverPicture(uri)
    }

    private fun taglibPicture(uri: Uri): ByteArray? =
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                // detachFd 后所有权归 TagLib，由其关闭
                TagLib.getFrontCover(pfd.detachFd())?.data
            }
        } catch (e: Exception) {
            Timber.d(e, "TagLib 读取封面失败: %s", uri)
            null
        } catch (e: LinkageError) {
            Timber.w(e, "TagLib 不可用")
            null
        }?.takeIf { it.isNotEmpty() }

    private fun retrieverPicture(uri: Uri): ByteArray? =
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.embeddedPicture
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** 同目录图片：先直接读文件系统，受分区存储限制读不到时再经额外目录的 SAF 树权限读 */
    private suspend fun folderImage(artwork: MusicArtwork): ByteArray? {
        if (artwork.path.isBlank()) return null
        val dir = File(artwork.path).parentFile ?: return null
        val minScore = if (dir.isMediaRoot()) MIN_SCORE_IN_MEDIA_ROOT else MIN_SCORE_ANYWHERE
        return folderImageFromFile(dir, minScore) ?: folderImageFromTree(dir, minScore)
    }

    private fun folderImageFromFile(
        dir: File,
        minScore: Int,
    ): ByteArray? =
        try {
            dir
                .listFiles()
                ?.asSequence()
                ?.filter { it.isFile && it.extension.lowercase() in IMAGE_EXTENSIONS }
                ?.filter { it.length() in 1..MAX_FOLDER_IMAGE_BYTES }
                ?.mapNotNull { file -> folderImageScore(file.nameWithoutExtension, minScore)?.let { it to file } }
                ?.maxWithOrNull(compareBy<Pair<Int, File>>({ it.first }, { it.second.length() }))
                ?.second
                ?.readBytes()
        } catch (e: Exception) {
            null
        }

    private suspend fun folderImageFromTree(
        dir: File,
        minScore: Int,
    ): ByteArray? {
        val dirPath = dir.absolutePath
        val root =
            treeRoots()
                .filter { dirPath == it.pathPrefix || dirPath.startsWith(it.pathPrefix + "/") }
                .maxByOrNull { it.pathPrefix.length }
                ?: return null
        return try {
            val treeDocId = DocumentsContract.getTreeDocumentId(root.treeUri)
            val relative = dirPath.removePrefix(root.pathPrefix).trimStart('/')
            val dirDocId =
                when {
                    relative.isEmpty() -> treeDocId
                    treeDocId.endsWith(":") -> treeDocId + relative
                    else -> "$treeDocId/$relative"
                }
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(root.treeUri, dirDocId)
            val projection =
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                )
            val best =
                resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                    var bestScore = -1
                    var bestSize = -1L
                    var bestDocId: String? = null
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(1) ?: continue
                        val mime = cursor.getString(2).orEmpty()
                        val isImage = mime.startsWith("image/") || name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS
                        if (!isImage) continue
                        val score = folderImageScore(name.substringBeforeLast('.'), minScore) ?: continue
                        val size = cursor.getLong(3)
                        if (size !in 1..MAX_FOLDER_IMAGE_BYTES) continue
                        if (score > bestScore || (score == bestScore && size > bestSize)) {
                            bestScore = score
                            bestSize = size
                            bestDocId = cursor.getString(0)
                        }
                    }
                    bestDocId
                } ?: return null
            val fileUri = DocumentsContract.buildDocumentUriUsingTree(root.treeUri, best)
            resolver.openInputStream(fileUri)?.use { it.readBytes() }?.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            Timber.d(e, "SAF 读取目录封面失败: %s", dirPath)
            null
        }
    }

    /**
     * 目录图打分；低于 [minScore] 或命中排除名返回 null。
     * WMP 留下的 AlbumArt_{GUID}_* 与 *Small 只有几十到两百像素，一律排除。
     */
    private fun folderImageScore(
        stem: String,
        minScore: Int,
    ): Int? {
        val name = stem.lowercase()
        val score =
            when {
                name.startsWith("albumart_") || name.endsWith("small") -> return null
                name == "cover" -> 5
                name == "folder" -> 4
                name == "front" -> 3
                name == "album" || name == "albumart" -> 2
                else -> 1
            }
        return score.takeIf { it >= minScore }
    }

    /** Music / Download 根目录杂物多，只认规范命名的封面 */
    private fun File.isMediaRoot(): Boolean =
        name.equals(Environment.DIRECTORY_MUSIC, ignoreCase = true) ||
            name.equals(Environment.DIRECTORY_DOWNLOADS, ignoreCase = true)

    /** MediaStore 缩略图：先按歌曲再按专辑，都是 provider 生成的固定尺寸图 */
    private fun mediaStoreThumbnail(artwork: MusicArtwork): ByteArray? {
        val candidates =
            buildList {
                if (artwork.mediaStoreId > 0L) {
                    add("content://media/external/audio/media/${artwork.mediaStoreId}/albumart".toUri())
                }
                if (artwork.albumId > 0L) {
                    add("content://media/external/audio/albumart/${artwork.albumId}".toUri())
                }
            }
        for (uri in candidates) {
            val bytes = runCatching { resolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            if (bytes != null && bytes.isNotEmpty()) return bytes
        }
        return null
    }

    private companion object {
        const val MAX_MISSES = 4096
        const val MAX_FOLDER_IMAGE_BYTES = 20L * 1024 * 1024
        const val MIN_SCORE_ANYWHERE = 1
        const val MIN_SCORE_IN_MEDIA_ROOT = 2
        val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}

class NoArtworkException(
    key: String,
) : IOException("No artwork for $key")
