package me.spica27.spicamusic.storage.impl.scanner

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.net.toUri
import com.ibm.icu.text.Transliterator
import com.kyant.taglib.AudioPropertiesReadStyle
import com.kyant.taglib.TagLib
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IScanFolderRepository
import me.spica27.spicamusic.storage.api.MediaStoreChangeEvent
import me.spica27.spicamusic.storage.api.MediaStoreChangeType
import me.spica27.spicamusic.storage.api.ScanProgress
import me.spica27.spicamusic.storage.api.ScanResult
import me.spica27.spicamusic.storage.impl.dao.AlbumDao
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * 音乐扫描服务实现 - 基于 MediaStore 和文件系统扫描
 * 支持自动监听系统媒体库变更
 */
class MusicScanService(
    private val context: Context,
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val scanFolderRepository: IScanFolderRepository,
) : IMusicScanService {

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    private val _isScanning = MutableStateFlow(false)
    private var isCancelled = false

    // MediaStore 变更事件流
    private val _mediaStoreChanges =
        MutableSharedFlow<MediaStoreChangeEvent>(replay = 0, extraBufferCapacity = 10)

    // 协程作用域用于处理去抖动扫描
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 去抖动任务
    private var debounceJob: Job? = null

    // MediaStore 内容观察者
    private var mediaStoreObserver: ContentObserver? = null

    // 去抖动延迟时间（毫秒）- 避免频繁变更触发多次扫描
    private val debounceDelayMs = 2000L

    companion object {
        private const val TAG = "MusicScanService"
        private const val MAX_FOLDER_DEPTH = 20

        // 支持的音频格式
        private val SUPPORTED_MIME_TYPES = setOf(
            "audio/mpeg",      // MP3
            "audio/mp4",       // M4A
            "audio/flac",      // FLAC
            "audio/ogg",       // OGG
            "audio/wav",       // WAV
            "audio/x-wav",     // WAV
            "audio/x-flac",    // FLAC
            "audio/aac",       // AAC
            "audio/opus",      // OPUS
        )

        // 文件系统遍历时的音频扩展名过滤
        private val SUPPORTED_EXTENSIONS = setOf(
            "mp3", "m4a", "flac", "ogg", "wav", "aac", "opus"
        )
    }

    override fun getScanProgress(): Flow<ScanProgress?> = _scanProgress.asStateFlow()

    override fun isScanning(): Flow<Boolean> = _isScanning.asStateFlow()

    override fun cancelScan() {
        isCancelled = true
    }

    override fun getMediaStoreChanges(): Flow<MediaStoreChangeEvent> =
        _mediaStoreChanges.asSharedFlow()

    /**
     * 启动 MediaStore 变更监听
     * 监听音频文件的新增、修改、删除事件
     */
    override fun startMediaStoreObserver() {
        if (mediaStoreObserver != null) {
            Timber.tag(TAG).w("MediaStore 观察者已在运行中")
            return
        }

        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        mediaStoreObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange(selfChange, null)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                onChange(selfChange, uri, 0)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
                Timber.tag(TAG)
                    .d("MediaStore 变更检测: uri=$uri, flags=$flags, selfChange=$selfChange")

                // 确定变更类型
                val changeType = when {
                    flags and ContentResolver.NOTIFY_DELETE != 0 -> MediaStoreChangeType.CONTENT_DELETED
                    flags and ContentResolver.NOTIFY_INSERT != 0 -> MediaStoreChangeType.CONTENT_CHANGED
                    flags and ContentResolver.NOTIFY_UPDATE != 0 -> MediaStoreChangeType.CONTENT_CHANGED
                    else -> MediaStoreChangeType.UNKNOWN
                }

                // 发送变更事件
                serviceScope.launch {
                    _mediaStoreChanges.emit(MediaStoreChangeEvent(changeType))
                }

                // 去抖动：取消之前的任务，延迟执行扫描
                debounceJob?.cancel()
                debounceJob = serviceScope.launch {
                    delay(debounceDelayMs)
                    Timber.tag(TAG).i("去抖动完成，开始增量扫描...")
                    scanMediaStore()
                }
            }
        }

        // 注册观察者，notifyForDescendants=true 监听子路径变化
        context.contentResolver.registerContentObserver(
            uri,
            true, // notifyForDescendants
            mediaStoreObserver!!
        )

        Timber.tag(TAG).i("MediaStore 观察者已启动，监听: $uri")
    }

    /**
     * 停止 MediaStore 变更监听
     */
    override fun stopMediaStoreObserver() {
        mediaStoreObserver?.let {
            context.contentResolver.unregisterContentObserver(it)
            mediaStoreObserver = null
            Timber.tag(TAG).i("MediaStore 观察者已停止")
        }
        debounceJob?.cancel()
        debounceJob = null
    }

    override suspend fun scanMediaStore(): ScanResult = withContext(Dispatchers.IO) {
        if (_isScanning.value) {
            Timber.tag(TAG).w("扫描已在进行中")
            return@withContext ScanResult(0, 0, 0, 0)
        }

        _isScanning.value = true
        isCancelled = false
        var totalScanned = 0
        var newAdded = 0
        var updated = 0

        // 加载忽略文件夹路径前缀（MediaStore 扫描时过滤用）
        val ignorePrefixes: List<String> = try {
            scanFolderRepository.getIgnoreFoldersSync()
                .mapNotNull { it.pathPrefix }
                .map { if (it.endsWith("/")) it else "$it/" }
        } catch (e: Exception) {
            emptyList()
        }

        try {
            val albums = loadAlbums()
            albumDao.replaceAll(albums)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "扫描失败")
            _isScanning.value = false
            return@withContext ScanResult(0, 0, 0, 0)
        }

        try {
            val contentResolver = context.contentResolver

            // 1. 从 DB 加载现有歌曲的摘要信息，构建 HashMap（O(1) 查找）
            val existingScanInfoMap: Map<Long, SongDao.SongScanInfo> =
                songDao.getAllScanInfo().associateBy { it.mediaStoreId }

            // 2. 查询 MediaStore（包含 DATE_MODIFIED 用于增量判断）
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_MODIFIED,
            )

            val selection =
                "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 10000"
            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            }

            // 需要 TagLib 深度扫描的歌曲列表（新增或修改的）
            val songsToScan = mutableListOf<SongEntity>()
            // 本次 MediaStore 中所有可见的 mediaStoreId
            val currentMediaStoreIds = mutableSetOf<Long>()

            contentResolver.query(
                uri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                val totalCount = cursor.count
                Timber.tag(TAG).d("开始增量扫描 MediaStore，共 $totalCount 个音频文件")

                while (cursor.moveToNext() && !isCancelled) {
                    val mediaStoreId = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn) ?: "Unknown"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getLong(durationColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val albumId = cursor.getLong(albumIdColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val dateModified = cursor.getLong(dateModifiedColumn)

                    // 过滤不支持的格式
                    if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
                        continue
                    }

                    // 过滤太短的音频（小于 10 秒）
                    if (duration < 10000) {
                        continue
                    }

                    // 跳过忽略文件夹中的文件
                    if (path.isNotEmpty() && ignorePrefixes.any { path.startsWith(it) }) {
                        continue
                    }

                    totalScanned++
                    currentMediaStoreIds.add(mediaStoreId)

                    // 更新进度
                    _scanProgress.value = ScanProgress(
                        current = totalScanned,
                        total = totalCount,
                        currentFile = displayName
                    )

                    val existingInfo = existingScanInfoMap[mediaStoreId]

                    if (existingInfo != null && existingInfo.dateModified == dateModified && dateModified != 0L) {
                        // 文件未变更 → 跳过 TagLib 扫描，不做任何处理
                        continue
                    }

                    // 需要 TagLib 扫描：新增歌曲或文件已修改
                    val audioInfo = extractAudioInfoWithTaglib(
                        contentResolver = contentResolver,
                        mediaStoreId = mediaStoreId,
                        fallbackDuration = duration
                    )

                    val finalDisplayName = audioInfo.title ?: displayName
                    val finalArtist = audioInfo.artist ?: artist
                    val sortName = generateSortName(finalDisplayName)


                    var codec = mimeType.let {
                        when {
                            it.contains("mp3", ignoreCase = true) -> "MP3"
                            it.contains("aac", ignoreCase = true) -> "AAC"
                            it.contains("flac", ignoreCase = true) -> "FLAC"
                            it.contains("alac", ignoreCase = true) -> "ALAC"
                            it.contains("opus", ignoreCase = true) -> "Opus"
                            it.contains("vorbis", ignoreCase = true) -> "Vorbis"
                            it.contains("wav", ignoreCase = true) -> "WAV"
                            it.contains("m4a", ignoreCase = true) -> "M4A"
                            else -> it.substringAfter("/").uppercase()
                        }
                    }
                    if (codec == "M4A") {
                        codec = if (audioInfo.bitRate >= 700000) "ALAC" else "AAC"
                    }

                    val song = SongEntity(
                        songId = existingInfo?.songId, // 保留已有主键，Upsert 会更新而非插入
                        mediaStoreId = mediaStoreId,
                        path = path,
                        displayName = finalDisplayName,
                        artist = finalArtist,
                        size = size,
                        like = existingInfo?.like ?: false,
                        duration = audioInfo.duration.takeIf { it > 0 } ?: duration,
                        sort = existingInfo?.sort ?: 0,
                        mimeType = mimeType,
                        albumId = albumId,
                        sampleRate = audioInfo.sampleRate,
                        bitRate = audioInfo.bitRate,
                        channels = audioInfo.channels,
                        digit = audioInfo.digit,
                        isIgnore = existingInfo?.isIgnore ?: false,
                        sortName = sortName,
                        dateModified = dateModified,
                        codec = codec,
                    )
                    songsToScan.add(song)
                    if (existingInfo != null) {
                        updated++
                    } else {
                        newAdded++
                    }
                }
            }

            if (isCancelled) {
                Timber.tag(TAG).w("扫描已取消")
                return@withContext ScanResult(totalScanned, 0, 0, 0)
            }

            // 3. 计算已从 MediaStore 中移除的歌曲
            val removedMediaStoreIds = existingScanInfoMap.keys - currentMediaStoreIds

            // 4. 一次事务完成：删除已移除 + upsert 变更
            songDao.incrementalUpdateSongs(
                removedMediaStoreIds = removedMediaStoreIds.toList(),
                changedSongs = songsToScan,
            )

            val removed = removedMediaStoreIds.size

            Timber.tag(TAG).i(
                "增量扫描完成: 总计=$totalScanned, 新增=$newAdded, 更新=$updated, 删除=$removed, " +
                        "跳过=${totalScanned - newAdded - updated}"
            )

            ScanResult(
                totalScanned = totalScanned,
                newAdded = newAdded,
                updated = updated,
                removed = removed.coerceAtLeast(0)
            )
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "扫描失败")
            ScanResult(0, 0, 0, 0)
        } finally {
            _isScanning.value = false
            _scanProgress.value = null
        }
    }

    override suspend fun scanFolder(folderPath: String): ScanResult {
        Timber.tag(TAG).w("scanFolder 已废弃，请使用 scanExtraFolders()")
        return ScanResult(0, 0, 0, 0)
    }

    override suspend fun scanFolders(folderPaths: List<String>): ScanResult {
        Timber.tag(TAG).w("scanFolders 已废弃，请使用 scanExtraFolders()")
        return ScanResult(0, 0, 0, 0)
    }

    override suspend fun scanExtraFolders(): ScanResult = withContext(Dispatchers.IO) {
        val extraFolders = try {
            scanFolderRepository.getExtraFoldersSync()
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "加载额外扫描文件夹失败")
            return@withContext ScanResult(0, 0, 0, 0)
        }
        if (extraFolders.isEmpty()) return@withContext ScanResult(0, 0, 0, 0)

        // 加载忽略路径前缀
        val ignorePrefixes: List<String> = try {
            scanFolderRepository.getIgnoreFoldersSync()
                .mapNotNull { it.pathPrefix }
                .map { if (it.endsWith("/")) it else "$it/" }
        } catch (e: Exception) {
            emptyList()
        }

        // 第一层防御：预校验 persistedUriPermissions
        val validUris = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }
            .map { it.uri }
            .toHashSet()

        val (foldersToScan, lostFolders) = extraFolders.partition { Uri.parse(it.uriString) in validUris }
        lostFolders.forEach {
            Timber.tag(TAG).w("SAF 权限预校验失效: ${it.displayName}")
            try { scanFolderRepository.markInaccessible(it.id) } catch (_: Exception) {}
        }

        // 构建现有路径集合（避免重复注册进 MediaStore）
        val existingPaths = buildExistingPathsSet()

        var totalScanned = 0
        var newAdded = 0

        for (folder in foldersToScan) {
            if (isCancelled) break
            try {
                // 第二层防御：每个文件夹独立 try-catch
                val treeUri = Uri.parse(folder.uriString)
                val audioFiles = walkDocumentTree(treeUri, ignorePrefixes)

                for (fileInfo in audioFiles) {
                    if (isCancelled) break
                    totalScanned++

                    // 已在 MediaStore 中则跳过注册
                    val filePath = fileInfo.absolutePath ?: continue
                    if (existingPaths.contains(filePath)) continue

                    _scanProgress.value = ScanProgress(
                        current = totalScanned,
                        total = totalScanned,
                        currentFile = fileInfo.displayName
                    )

                    // 注册到 MediaStore，获取 mediaId
                    val mediaId = registerFileInMediaStore(filePath) ?: continue

                    // 使用 TagLib 读取元数据
                    val audioInfo = extractAudioInfoWithTaglib(
                        contentResolver = context.contentResolver,
                        mediaStoreId = mediaId,
                        fallbackDuration = fileInfo.duration,
                    )

                    val finalDisplayName = audioInfo.title ?: fileInfo.displayName
                    val finalArtist = audioInfo.artist ?: "Unknown Artist"
                    val sortName = generateSortName(finalDisplayName)
                    val mimeType = fileInfo.mimeType
                    var codec = when {
                        mimeType.contains("mp3", ignoreCase = true) -> "MP3"
                        mimeType.contains("aac", ignoreCase = true) -> "AAC"
                        mimeType.contains("flac", ignoreCase = true) -> "FLAC"
                        mimeType.contains("alac", ignoreCase = true) -> "ALAC"
                        mimeType.contains("opus", ignoreCase = true) -> "Opus"
                        mimeType.contains("vorbis", ignoreCase = true) -> "Vorbis"
                        mimeType.contains("wav", ignoreCase = true) -> "WAV"
                        mimeType.contains("m4a", ignoreCase = true) -> "M4A"
                        else -> mimeType.substringAfter("/").uppercase()
                    }
                    if (codec == "M4A" && audioInfo.bitRate >= 700000) codec = "ALAC"

                    val song = SongEntity(
                        mediaStoreId = mediaId,
                        path = filePath,
                        displayName = finalDisplayName,
                        artist = finalArtist,
                        size = fileInfo.size,
                        like = false,
                        duration = audioInfo.duration.takeIf { it > 0 } ?: fileInfo.duration,
                        sort = 0,
                        sortName = sortName,
                        mimeType = mimeType,
                        albumId = 0L,
                        sampleRate = audioInfo.sampleRate,
                        bitRate = audioInfo.bitRate,
                        channels = audioInfo.channels,
                        digit = audioInfo.digit,
                        isIgnore = false,
                        codec = codec,
                        dateModified = 0L,
                    )
                    songDao.upsertSongs(listOf(song))
                    existingPaths.add(filePath) // 防止同次扫描重复处理
                    newAdded++
                }
            } catch (e: SecurityException) {
                // 第二层防御：权限在遍历中被撤销
                Timber.tag(TAG).w("SAF 权限在扫描中失效: ${folder.displayName}")
                try { scanFolderRepository.markInaccessible(folder.id) } catch (_: Exception) {}
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "额外文件夹扫描失败: ${folder.displayName}")
            }
        }

        Timber.tag(TAG).i("额外文件夹扫描完成: 总计=$totalScanned, 新增=$newAdded")
        ScanResult(totalScanned = totalScanned, newAdded = newAdded, updated = 0, removed = 0)
    }

    /** 从 MediaStore 构建现有文件路径的可变集合 */
    private suspend fun buildExistingPathsSet(): MutableSet<String> = withContext(Dispatchers.IO) {
        val paths = mutableSetOf<String>()
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.DATA),
            null, null, null
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            if (dataCol >= 0) {
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol)
                    if (!path.isNullOrEmpty()) paths.add(path)
                }
            }
        }
        paths
    }

    /**
     * 递归遍历 SAF 目录树，收集所有音频文件信息
     * @param treeUri  SAF tree URI
     * @param ignorePrefixes 忽略文件夹的绝对路径前缀列表（以 "/" 结尾）
     * @param parentDocId  当前递归层级的 document ID，首次调用传 null 使用 tree root
     * @param depth  当前递归深度，超过 MAX_FOLDER_DEPTH 时停止
     */
    private fun walkDocumentTree(
        treeUri: Uri,
        ignorePrefixes: List<String>,
        parentDocId: String? = null,
        depth: Int = 0,
    ): List<DocumentFileInfo> {
        if (depth > MAX_FOLDER_DEPTH) return emptyList()

        val docId = parentDocId ?: DocumentsContract.getTreeDocumentId(treeUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)

        val results = mutableListOf<DocumentFileInfo>()

        context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            ),
            null, null, null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext() && !isCancelled) {
                val childDocId = cursor.getString(idCol) ?: continue
                val displayName = cursor.getString(nameCol) ?: continue
                val mimeType = cursor.getString(mimeCol) ?: continue
                val size = cursor.getLong(sizeCol)

                // 解析绝对路径（用于忽略文件夹匹配）
                val absolutePath = resolveAbsolutePath(childDocId)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    // 是目录：检查是否需要忽略，再递归
                    val folderPath = if (absolutePath != null) {
                        if (absolutePath.endsWith("/")) absolutePath else "$absolutePath/"
                    } else null

                    if (folderPath != null && ignorePrefixes.any { folderPath.startsWith(it) || it.startsWith(folderPath) }) {
                        continue // 跳过此子目录整棵树
                    }
                    results.addAll(walkDocumentTree(treeUri, ignorePrefixes, childDocId, depth + 1))
                } else {
                    // 是文件：按扩展名过滤
                    val ext = displayName.substringAfterLast('.', "").lowercase()
                    if (ext !in SUPPORTED_EXTENSIONS) continue

                    // 忽略文件夹过滤
                    if (absolutePath != null && ignorePrefixes.any { absolutePath.startsWith(it) }) continue

                    results.add(
                        DocumentFileInfo(
                            displayName = displayName,
                            mimeType = if (mimeType == "application/octet-stream") "audio/$ext" else mimeType,
                            size = size,
                            absolutePath = absolutePath,
                            duration = 0L,
                        )
                    )
                }
            }
        }
        return results
    }

    /**
     * 从 SAF document ID 解析绝对文件路径
     * - 主存储：`primary:relative/path` → `/storage/emulated/0/relative/path`
     * - 外置存储：`XXXX-XXXX:relative/path` → `/storage/XXXX-XXXX/relative/path`
     */
    private fun resolveAbsolutePath(documentId: String): String? {
        val parts = documentId.split(":", limit = 2)
        if (parts.size != 2) return null
        val (volume, relativePath) = parts
        return if (volume.equals("primary", ignoreCase = true)) {
            "${Environment.getExternalStorageDirectory()}/$relativePath"
        } else {
            "/storage/$volume/$relativePath"
        }
    }

    /**
     * 通过 MediaScannerConnection 将文件注册进 MediaStore，返回获得的 mediaStoreId
     * 回调在主线程触发，通过 suspendCancellableCoroutine 桥接到协程
     */
    private suspend fun registerFileInMediaStore(filePath: String): Long? =
        suspendCancellableCoroutine { cont ->
            MediaScannerConnection.scanFile(
                context,
                arrayOf(filePath),
                null
            ) { _, uri ->
                if (uri != null) {
                    val mediaId = uri.lastPathSegment?.toLongOrNull()
                    cont.resume(mediaId)
                } else {
                    cont.resume(null)
                }
            }
        }

    /** SAF 文件信息 */
    private data class DocumentFileInfo(
        val displayName: String,
        val mimeType: String,
        val size: Long,
        val absolutePath: String?,
        val duration: Long,
    )

    /**
     * 使用 Taglib 提取音频详细信息（优先使用）
     * 优化：只打开一次 FileDescriptor，通过 dup() 复制给两个 TagLib 调用
     */
    private fun extractAudioInfoWithTaglib(
        contentResolver: ContentResolver,
        mediaStoreId: Long,
        fallbackDuration: Long,
    ): AudioInfo {
        var title: String? = null
        var artist: String? = null
        var duration = 0L
        var sampleRate = 0
        var bitRate = 0
        var channels = 0
        var digit = 0

        try {
            val uri = "content://media/external/audio/media/$mediaStoreId".toUri()

            // 只打开一次 FileDescriptor，通过 dup() 复制给两个 TagLib 调用
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                try {
                    val dupPfd = pfd.dup()
                    val fdForMetadata = pfd.detachFd()      // int fd，所有权转移给 TagLib
                    val fdForAudioProps = dupPfd.detachFd()  // int fd（复制），所有权转移给 TagLib

                    // 读取元数据（标题和艺术家）
                    try {
                        val metadata = TagLib.getMetadata(
                            fd = fdForMetadata,
                            readPictures = false
                        )
                        if (metadata != null) {
                            title = metadata.propertyMap["TITLE"]?.firstOrNull()
                            artist = metadata.propertyMap["ARTIST"]?.firstOrNull()
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Taglib 元数据读取失败")
                    }

                    // 读取音频属性（采样率、比特率等）
                    try {
                        TagLib.getAudioProperties(
                            fdForAudioProps,
                            readStyle = AudioPropertiesReadStyle.Accurate
                        )?.let {
                            duration = it.length.toLong()
                            sampleRate = it.sampleRate
                            bitRate = it.bitrate * 1000 // kbps 转 bps
                            channels = it.channels
                        }
                    } catch (e: Exception) {
                        Timber.tag(TAG).w(e, "Taglib 音频属性读取失败")
                    }
                } catch (e: Exception) {
                    Timber.tag(TAG).w(e, "Taglib FD 操作失败，回退到默认值")
                }
            }

            // 如果 Taglib 未能获取 duration，使用回退值
            if (duration == 0L) {
                duration = fallbackDuration
            }

            // 从 MediaStore 获取 bitrate（作为补充）
            if (bitRate == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.query(
                    Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaStoreId.toString()
                    ),
                    arrayOf(MediaStore.Audio.Media.BITRATE),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val bitRateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                        if (bitRateColumn != -1) {
                            bitRate = cursor.getInt(bitRateColumn)
                        }
                    }
                }
            }

            // 使用默认值填充缺失的信息
            if (sampleRate == 0) sampleRate = 44100
            if (channels == 0) channels = 2
            if (digit == 0) digit = 16

        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "提取音频信息失败: $mediaStoreId")
            duration = fallbackDuration
            sampleRate = 44100
            channels = 2
            digit = 16
        }

        return AudioInfo(
            title = title,
            artist = artist,
            duration = duration,
            sampleRate = sampleRate,
            bitRate = bitRate,
            channels = channels,
            digit = digit
        )
    }

    /**
     * 根据歌曲名称生成排序用的首字符
     * - 英文：直接使用首字符（大写）
     * - 中文：转换为拼音首字母
     * - 日文：转换为罗马音首字母
     * - 其他：返回 "#"
     */
    private fun generateSortName(displayName: String): String {
        if (displayName.isEmpty()) return "#"

        val firstChar = displayName.first()

        return try {
            when {
                // 英文字母（A-Z, a-z）
                firstChar.isLetter() && firstChar.code in 0x41..0x7A -> {
                    firstChar.uppercaseChar().toString()
                }

                // 中文（CJK 统一表意文字）
                firstChar.code in 0x4E00..0x9FFF -> {
                    val transliterator = Transliterator.getInstance("Han-Latin; Latin-ASCII")
                    val pinyin = transliterator.transliterate(firstChar.toString())
                    pinyin.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                }

                // 日文平假名（ひらがな）
                firstChar.code in 0x3040..0x309F -> {
                    val transliterator = Transliterator.getInstance("Hiragana-Latin")
                    val romaji = transliterator.transliterate(firstChar.toString())
                    romaji.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                }

                // 日文片假名（カタカナ）
                firstChar.code in 0x30A0..0x30FF -> {
                    val transliterator = Transliterator.getInstance("Katakana-Latin")
                    val romaji = transliterator.transliterate(firstChar.toString())
                    romaji.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
                }

                // 数字
                firstChar.isDigit() -> "#"

                // 其他字符
                else -> "#"
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "生成排序名称失败: $displayName")
            "#"
        }
    }

    /**
     * 音频信息数据类（扩展版本，包含元数据）
     */
    private data class AudioInfo(
        val title: String? = null,
        val artist: String? = null,
        val duration: Long,
        val sampleRate: Int,
        val bitRate: Int,
        val channels: Int,
        val digit: Int,
    )


    private suspend fun loadAlbums(): List<AlbumEntity> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<AlbumEntity>()
        val collection = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            // Cache column indices
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songsCountColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(albumColumn) ?: continue // Skip if null
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val songsCount = cursor.getInt(songsCountColumn)
                val year = cursor.getInt(yearColumn)
                val albumArtUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    id
                ).toString()
                val album = AlbumEntity(
                    id = id.toString(),
                    title = title,
                    artist = artist,
                    artworkUri = albumArtUri,
                    year = year,
                    numberOfSongs = songsCount,
                )
                albums.add(album)
            }
        }

        albums
    }

}
