package me.spica27.spicamusic.storage.impl.scanner

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.IScanFolderRepository
import me.spica27.spicamusic.storage.api.IScanRulesRepository
import me.spica27.spicamusic.storage.api.LibraryAccessGate
import me.spica27.spicamusic.storage.api.MediaStoreChangeEvent
import me.spica27.spicamusic.storage.api.MediaStoreChangeType
import me.spica27.spicamusic.storage.api.ScanProgress
import me.spica27.spicamusic.storage.api.ScanResult
import me.spica27.spicamusic.storage.api.ScanRules
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.index.IndexPolicy
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * 音乐扫描服务：MediaStore 只提供文件索引，标题、艺人、专辑等分类字段由 TagLib 读取，
 * 艺人与专辑由 [LibraryWriter] 按标签建立。支持自动监听系统媒体库变更。
 *
 * 所有写库操作都在 [LibraryAccessGate] 内串行执行；只有完整、未取消的扫描才删除消失的歌曲，
 * MediaStore 查询失败按失败处理，不当作"文件全被删除"。
 */
class MusicScanService(
    private val context: Context,
    private val database: AppDatabase,
    private val scanFolderRepository: IScanFolderRepository,
    private val scanRulesRepository: IScanRulesRepository,
    private val accessGate: LibraryAccessGate,
) : IMusicScanService {
    private val songDao = database.songDao()
    private val libraryDao = database.libraryDao()
    private val writer = LibraryWriter(database)
    private val extractor = TagExtractor(context.contentResolver)
    private val text = AndroidIndexText.instance

    private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
    private val _isScanning = MutableStateFlow(false)
    private val isCancelled = AtomicBoolean(false)

    // MediaStore 变更事件流
    private val _mediaStoreChanges =
        MutableSharedFlow<MediaStoreChangeEvent>(replay = 0, extraBufferCapacity = 10)

    // 协程作用域用于处理去抖动扫描
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 去抖动任务
    private var debounceJob: Job? = null

    private val pendingMediaStoreSyncLock = Any()
    private val pendingChangedMediaStoreIds = mutableSetOf<Long>()
    private val pendingDeletedMediaStoreIds = mutableSetOf<Long>()
    private var pendingFullMediaStoreRescan = false

    // MediaStore 内容观察者
    private var mediaStoreObserver: ContentObserver? = null

    // 去抖动延迟时间（毫秒）- 避免频繁变更触发多次扫描
    private val debounceDelayMs = 2000L

    /** TagLib 读取的并发数；native 读取在 IO 线程上，按设备实测调整 */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val extractDispatcher = Dispatchers.IO.limitedParallelism(EXTRACT_PARALLELISM)

    companion object {
        private const val TAG = "MusicScanService"
        private const val MAX_FOLDER_DEPTH = 20

        private const val EXTRACT_PARALLELISM = 4

        /** 每批提取的文件数；每批一个事务提交 */
        private const val EXTRACT_BATCH = 64

        /** 分批提交时，关系重建最多每隔这么久执行一次；最后一批必定执行 */
        private const val REINDEX_INTERVAL_MS = 2000L

        private const val QUERY_CHUNK = 300
    }

    /**
     * 把一份扫描规则展开成可高频调用的匹配器（mime/扩展名集合只算一次）
     */
    private class ScanRuleMatcher(
        val rules: ScanRules,
    ) {
        private val allowedMimeTypes = rules.allowedMimeTypes()
        private val allowedExtensions = rules.allowedExtensions()

        /** MediaStore 行：mime 命中即通过；mime 不认识时回退扩展名判断 */
        fun matchesFormat(
            mimeType: String,
            path: String,
        ): Boolean {
            if (mimeType.lowercase() in allowedMimeTypes) return true
            val ext = path.substringAfterLast('.', "").lowercase()
            return ext.isNotEmpty() && ext in allowedExtensions
        }

        /** SAF 文件：只有扩展名可用 */
        fun matchesExtension(fileName: String): Boolean =
            fileName.substringAfterLast('.', "").lowercase() in allowedExtensions

        fun matchesDuration(durationMs: Long): Boolean = durationMs >= rules.minDurationMs

        /** size <= 0 视为未知，放行（后续 MediaStore 行会带真实体积再判） */
        fun matchesSize(sizeBytes: Long): Boolean =
            rules.minFileSizeBytes <= 0 || sizeBytes <= 0 || sizeBytes >= rules.minFileSizeBytes
    }

    /** 一次扫描的结局；只有 Committed 才推进曲库状态 */
    private sealed interface ScanOutcome {
        data class Committed(
            val result: ScanResult,
        ) : ScanOutcome

        data object Cancelled : ScanOutcome

        data class Failed(
            val reason: String,
        ) : ScanOutcome
    }

    private fun ScanOutcome.toResult(): ScanResult = (this as? ScanOutcome.Committed)?.result ?: ScanResult(0, 0, 0, 0)

    private suspend fun loadRules(): ScanRules =
        try {
            scanRulesRepository.getRulesSync()
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "读取扫描规则失败，使用默认规则")
            ScanRules.DEFAULT
        }

    override fun getScanProgress(): Flow<ScanProgress?> = _scanProgress.asStateFlow()

    override fun isScanning(): Flow<Boolean> = _isScanning.asStateFlow()


    override fun cancelScan() {
        isCancelled.set(true)
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

        val uri = audioCollection()

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

                enqueueObservedMediaStoreChange(
                    mediaStoreId = extractObservedMediaStoreId(uri),
                    changeType = changeType,
                )
                scheduleDebouncedMediaStoreSync()
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
        synchronized(pendingMediaStoreSyncLock) {
            pendingChangedMediaStoreIds.clear()
            pendingDeletedMediaStoreIds.clear()
            pendingFullMediaStoreRescan = false
        }
    }

    private data class PendingMediaStoreSync(
        val changedIds: Set<Long>,
        val deletedIds: Set<Long>,
        val requiresFullRescan: Boolean,
    )

    private fun enqueueObservedMediaStoreChange(
        mediaStoreId: Long?,
        changeType: MediaStoreChangeType,
    ) {
        synchronized(pendingMediaStoreSyncLock) {
            when {
                changeType == MediaStoreChangeType.CONTENT_DELETED && mediaStoreId != null -> {
                    pendingDeletedMediaStoreIds.add(mediaStoreId)
                }

                mediaStoreId != null -> {
                    pendingChangedMediaStoreIds.add(mediaStoreId)
                }

                else -> {
                    pendingFullMediaStoreRescan = true
                }
            }
        }
    }

    private fun scheduleDebouncedMediaStoreSync() {
        debounceJob?.cancel()
        debounceJob =
            serviceScope.launch {
                delay(debounceDelayMs)
                flushPendingMediaStoreSync()
            }
    }

    private fun takePendingMediaStoreSync(): PendingMediaStoreSync =
        synchronized(pendingMediaStoreSyncLock) {
            val request =
                PendingMediaStoreSync(
                    changedIds = pendingChangedMediaStoreIds.toSet(),
                    deletedIds = pendingDeletedMediaStoreIds.toSet(),
                    requiresFullRescan = pendingFullMediaStoreRescan,
                )
            pendingChangedMediaStoreIds.clear()
            pendingDeletedMediaStoreIds.clear()
            pendingFullMediaStoreRescan = false
            request
        }

    private fun mergePendingMediaStoreSync(request: PendingMediaStoreSync) {
        synchronized(pendingMediaStoreSyncLock) {
            pendingChangedMediaStoreIds.addAll(request.changedIds)
            pendingDeletedMediaStoreIds.addAll(request.deletedIds)
            pendingFullMediaStoreRescan = pendingFullMediaStoreRescan || request.requiresFullRescan
        }
    }

    private suspend fun flushPendingMediaStoreSync() {
        val request = takePendingMediaStoreSync()
        if (!request.requiresFullRescan && request.changedIds.isEmpty() && request.deletedIds.isEmpty()) {
            return
        }

        if (_isScanning.value) {
            Timber.tag(TAG).i("当前已有扫描任务，合并变更并等待下一轮去抖动同步")
            mergePendingMediaStoreSync(request)
            scheduleDebouncedMediaStoreSync()
            return
        }

        if (request.requiresFullRescan) {
            Timber.tag(TAG).i("监听变更无法定位具体媒体项，回退到全量增量扫描")
            scanMediaStore(forceRescan = false)
            return
        }

        Timber.tag(TAG).i(
            "去抖动完成，开始局部同步 MediaStore：changed=${request.changedIds.size}, deleted=${request.deletedIds.size}",
        )
        scanMediaStoreDelta(
            changedMediaStoreIds = request.changedIds,
            deletedMediaStoreIds = request.deletedIds,
        )
    }

    private fun extractObservedMediaStoreId(uri: Uri?): Long? {
        if (uri == null) return null
        return runCatching { ContentUris.parseId(uri) }.getOrNull()
            ?: uri.lastPathSegment?.toLongOrNull()
    }

    /**
     * 启动检查：v22 升级后的重建、提取版本升级需要完整扫描；
     * 解释规则变化或关系没建完时只重建关系（不读文件，不需要媒体权限）。
     */
    override suspend fun ensureLibraryUpToDate() {
        accessGate.withAccess {
            val state = libraryDao.state()
            val policy = IndexPolicy(loadRules().splitArtistNames)
            when {
                state?.pendingRebuild == true || songDao.hasStaleTags(SongRowBuilder.TAG_VERSION) -> {
                    // 没有权限时不动数据，授权后由授权流程触发扫描
                    if (!hasAudioPermission()) {
                        Timber.tag(TAG).d("未授予媒体权限，推迟曲库重建")
                        return@withAccess
                    }
                    Timber.tag(TAG).i("曲库需要重建（pendingRebuild=${state?.pendingRebuild}），开始完整扫描")
                    val outcome = runExclusive { runFullScan(forceExtract = false) }
                    Timber.tag(TAG).i("启动完整扫描结束: $outcome")
                }

                state?.indexPolicy != policy.fingerprint || libraryDao.hasUnindexedSongs() -> {
                    Timber.tag(TAG).i("解释规则变化 ${state?.indexPolicy} -> ${policy.fingerprint}，重建艺人与专辑")
                    runExclusive {
                        withContext(NonCancellable) { writer.reindex(policy) }
                        ScanOutcome.Committed(ScanResult(0, 0, 0, 0))
                    }
                }
            }
        }
    }

    private fun hasAudioPermission(): Boolean {
        val permission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    override suspend fun scanMediaStore(forceRescan: Boolean): ScanResult =
        accessGate.withAccess { runExclusive { runFullScan(forceRescan) } }.toResult()

    private suspend fun scanMediaStoreDelta(
        changedMediaStoreIds: Set<Long>,
        deletedMediaStoreIds: Set<Long>,
    ): ScanResult {
        val outcome =
            accessGate.withAccess {
                runExclusive { runDelta(changedMediaStoreIds + deletedMediaStoreIds) }
            }
        if (outcome is ScanOutcome.Failed) {
            Timber.tag(TAG).w("局部同步失败（${outcome.reason}），回退到全量增量扫描")
            mergePendingMediaStoreSync(
                PendingMediaStoreSync(
                    changedIds = emptySet(),
                    deletedIds = emptySet(),
                    requiresFullRescan = true,
                ),
            )
            scheduleDebouncedMediaStoreSync()
        }
        return outcome.toResult()
    }

    override suspend fun scanFolder(folderPath: String): ScanResult {
        Timber.tag(TAG).w("scanFolder 已废弃，请使用 scanExtraFolders()")
        return ScanResult(0, 0, 0, 0)
    }

    override suspend fun scanFolders(folderPaths: List<String>): ScanResult {
        Timber.tag(TAG).w("scanFolders 已废弃，请使用 scanExtraFolders()")
        return ScanResult(0, 0, 0, 0)
    }

    override suspend fun scanExtraFolders(): ScanResult =
        accessGate.withAccess { runExclusive { runExtraFolders() } }.toResult()

    /** 在访问锁内调用：维护扫描状态与进度，异常按失败处理 */
    private suspend fun runExclusive(block: suspend () -> ScanOutcome): ScanOutcome {
        _isScanning.value = true
        isCancelled.set(false)
        return try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "扫描失败")
            ScanOutcome.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            _isScanning.value = false
            _scanProgress.value = null
        }
    }

    private suspend fun isCancelled(): Boolean = isCancelled.get() || !currentCoroutineContext().isActive

    /** 完整扫描：枚举全部 MediaStore 音频，删除本次未出现的歌曲；重建期间成功后执行收尾 */
    private suspend fun runFullScan(forceExtract: Boolean): ScanOutcome {
        if (!hasAudioPermission()) return ScanOutcome.Failed("没有媒体读取权限")
        val rules = loadRules()
        val matcher = ScanRuleMatcher(rules)
        val ignorePrefixes = loadIgnorePrefixes()
        val policy = IndexPolicy(rules.splitArtistNames)
        val state = libraryDao.state()
        val rebuilding = state?.pendingRebuild == true
        val carriedImported = if (rebuilding) libraryDao.carriedImportedIds().toHashSet() else emptySet()
        val existing = songDao.getAllScanInfo().associateBy { it.mediaStoreId }

        val rows =
            queryMediaStore("${MediaStore.Audio.Media.IS_MUSIC} = 1", null)
                ?: return ScanOutcome.Failed("MediaStore 查询失败")
        if (isCancelled()) return ScanOutcome.Cancelled

        val eligible =
            rows.filter { row ->
                existing[row.mediaStoreId]?.isImported == true ||
                    row.mediaStoreId in carriedImported ||
                    isEligible(matcher, row, ignorePrefixes)
            }
        val eligibleIds = eligible.mapTo(HashSet()) { it.mediaStoreId }
        val removed = existing.keys.filter { it !in eligibleIds }
        // 结果为空而旧库有歌单条目时可能是媒体库暂时不可用，推迟收尾，避免把歌单清空
        val finalizeRebuild = rebuilding && (eligible.isNotEmpty() || !libraryDao.hasPlaylistEntries())
        if (rebuilding && !finalizeRebuild) Timber.tag(TAG).w("重建扫描结果为空，保留 pendingRebuild 等待下次")

        Timber.tag(TAG).d("完整扫描：MediaStore ${rows.size} 个音频，收录 ${eligible.size}，移除 ${removed.size}")
        return processAndCommit(
            eligible = eligible,
            existing = existing,
            removed = removed,
            policy = policy,
            policyChanged = state?.indexPolicy != policy.fingerprint,
            forceExtract = forceExtract,
            carriedImported = carriedImported,
            finalizeRebuild = finalizeRebuild,
        )
    }

    /** 局部同步：只处理目标 ID，目标中已不存在或不再符合规则的歌曲被删除 */
    private suspend fun runDelta(targetIds: Set<Long>): ScanOutcome {
        if (targetIds.isEmpty()) return ScanOutcome.Committed(ScanResult(0, 0, 0, 0))
        // 没有权限时查询只返回本应用的文件，目标里的其他歌曲会被误删
        if (!hasAudioPermission()) return ScanOutcome.Failed("没有媒体读取权限")
        val rules = loadRules()
        val matcher = ScanRuleMatcher(rules)
        val ignorePrefixes = loadIgnorePrefixes()
        val policy = IndexPolicy(rules.splitArtistNames)
        val state = libraryDao.state()
        val carriedImported =
            if (state?.pendingRebuild == true) libraryDao.carriedImportedIds().toHashSet() else emptySet()
        val ids = targetIds.toList()
        val existing =
            ids.chunked(QUERY_CHUNK).flatMap { songDao.getScanInfoByMediaStoreIds(it) }.associateBy { it.mediaStoreId }

        val rows = mutableListOf<MediaFileRow>()
        for (chunk in ids.chunked(QUERY_CHUNK)) {
            val placeholders = chunk.joinToString(",") { "?" }
            // 与完整扫描使用同一收录条件，避免非音乐文件被局部同步加入、再被完整扫描删除
            rows +=
                queryMediaStore(
                    "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media._ID} IN ($placeholders)",
                    chunk.map(Long::toString).toTypedArray(),
                ) ?: return ScanOutcome.Failed("MediaStore 查询失败")
            if (isCancelled()) return ScanOutcome.Cancelled
        }

        val eligible =
            rows.filter { row ->
                existing[row.mediaStoreId]?.isImported == true ||
                    row.mediaStoreId in carriedImported ||
                    isEligible(matcher, row, ignorePrefixes)
            }
        val eligibleIds = eligible.mapTo(HashSet()) { it.mediaStoreId }
        val removed = existing.keys.filter { it !in eligibleIds }
        return processAndCommit(
            eligible = eligible,
            existing = existing,
            removed = removed,
            policy = policy,
            policyChanged = state?.indexPolicy != policy.fingerprint,
            forceExtract = false,
            carriedImported = carriedImported,
            finalizeRebuild = false,
        )
    }

    /**
     * 提取新增 / 变更的文件并分批提交。每批一个事务：写入歌曲 → 回填暂存标记 → 按需重建关系。
     * 删除消失的歌曲、最后一次关系重建与重建收尾放在最后一批的事务里，且不响应取消。
     */
    private suspend fun processAndCommit(
        eligible: List<MediaFileRow>,
        existing: Map<Long, SongDao.SongScanInfo>,
        removed: List<Long>,
        policy: IndexPolicy,
        policyChanged: Boolean,
        forceExtract: Boolean,
        carriedImported: Set<Long>,
        finalizeRebuild: Boolean,
    ): ScanOutcome {
        val toExtract = eligible.filter { forceExtract || !SongRowBuilder.isUnchanged(it, existing[it.mediaStoreId]) }
        var processed = eligible.size - toExtract.size
        var added = 0
        var updated = 0
        var lastReindexAt = SystemClock.elapsedRealtime()
        var pending = emptyList<SongEntity>()
        _scanProgress.value = ScanProgress(processed, eligible.size, "")

        val batches = toExtract.chunked(EXTRACT_BATCH)
        for ((index, batch) in batches.withIndex()) {
            if (isCancelled()) return ScanOutcome.Cancelled
            val extracted =
                coroutineScope {
                    batch.map { row -> async(extractDispatcher) { row to extractor.extract(mediaUri(row.mediaStoreId)) } }.awaitAll()
                }
            if (isCancelled()) return ScanOutcome.Cancelled
            val songs =
                extracted.map { (row, tags) ->
                    SongRowBuilder.build(
                        file = row,
                        extracted = tags,
                        existing = existing[row.mediaStoreId],
                        text = text,
                        isImported = row.mediaStoreId in carriedImported,
                    )
                }
            songs.forEach { if (it.songId == 0L) added++ else updated++ }
            processed += batch.size
            _scanProgress.value = ScanProgress(processed, eligible.size, batch.last().fileName)

            if (index == batches.lastIndex) {
                // 最后一批留到收尾事务，与删除、关系重建一起提交
                pending = songs
            } else {
                // 中间批次先入库让曲库逐步出现；关系重建节流，最后一批必定执行
                val now = SystemClock.elapsedRealtime()
                val reindex = now - lastReindexAt >= REINDEX_INTERVAL_MS
                withContext(NonCancellable) { writer.commit(songs, policy = policy, reindex = reindex) }
                if (reindex) lastReindexAt = now
            }
        }
        if (isCancelled()) return ScanOutcome.Cancelled

        // 上次分批提交后中断的歌曲没有关系，文件未变也要补建
        val changed =
            toExtract.isNotEmpty() || removed.isNotEmpty() || policyChanged || libraryDao.hasUnindexedSongs()
        if (changed || finalizeRebuild) {
            withContext(NonCancellable) {
                writer.commit(
                    upserts = pending,
                    removedMediaStoreIds = removed,
                    policy = policy,
                    reindex = true,
                    finalizeRebuild = finalizeRebuild,
                )
            }
        }
        Timber.tag(TAG).i(
            "扫描提交: 收录=${eligible.size}, 新增=$added, 更新=$updated, 删除=${removed.size}, 跳过=${eligible.size - toExtract.size}" +
                if (finalizeRebuild) "，重建完成" else "",
        )
        return ScanOutcome.Committed(
            ScanResult(
                totalScanned = eligible.size,
                newAdded = added,
                updated = updated,
                removed = removed.size,
            ),
        )
    }

    /** 额外目录：SAF 遍历 → 未收录的文件注册进 MediaStore → 按 MediaStore ID 局部同步 */
    private suspend fun runExtraFolders(): ScanOutcome {
        val extraFolders =
            try {
                scanFolderRepository.getExtraFoldersSync()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "加载额外扫描文件夹失败")
                return ScanOutcome.Failed("加载额外扫描文件夹失败")
            }
        if (extraFolders.isEmpty()) return ScanOutcome.Committed(ScanResult(0, 0, 0, 0))

        val ignorePrefixes = loadIgnorePrefixes()
        val matcher = ScanRuleMatcher(loadRules())

        // 第一层防御：预校验 persistedUriPermissions
        val validUris =
            context.contentResolver.persistedUriPermissions
                .filter { it.isReadPermission }
                .map { it.uri }
                .toHashSet()
        val (foldersToScan, lostFolders) = extraFolders.partition { Uri.parse(it.uriString) in validUris }
        lostFolders.forEach {
            Timber.tag(TAG).w("SAF 权限预校验失效: ${it.displayName}")
            try {
                scanFolderRepository.markInaccessible(it.id)
            } catch (_: Exception) {
            }
        }
        if (foldersToScan.isEmpty()) return ScanOutcome.Committed(ScanResult(0, 0, 0, 0))

        // 已在 MediaStore 中的路径不重复注册
        val existingPaths = buildExistingPathsSet()
        var totalScanned = 0
        val registeredMediaIds = mutableSetOf<Long>()
        for (folder in foldersToScan) {
            if (isCancelled()) return ScanOutcome.Cancelled
            try {
                // 第二层防御：每个文件夹独立 try-catch
                val audioFiles = walkDocumentTree(Uri.parse(folder.uriString), ignorePrefixes, matcher)
                for (fileInfo in audioFiles) {
                    if (isCancelled()) return ScanOutcome.Cancelled
                    totalScanned++
                    val filePath = fileInfo.absolutePath ?: continue
                    if (!existingPaths.add(filePath)) continue
                    _scanProgress.value = ScanProgress(totalScanned, audioFiles.size, fileInfo.displayName)
                    registerFileInMediaStore(filePath)?.let(registeredMediaIds::add)
                }
            } catch (e: SecurityException) {
                // 第二层防御：权限在遍历中被撤销
                Timber.tag(TAG).w("SAF 权限在扫描中失效: ${folder.displayName}")
                try {
                    scanFolderRepository.markInaccessible(folder.id)
                } catch (_: Exception) {
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "额外文件夹扫描失败: ${folder.displayName}")
            }
        }

        val outcome = runDelta(registeredMediaIds)
        if (outcome !is ScanOutcome.Committed) return outcome
        Timber.tag(TAG).i("额外文件夹扫描完成: 总计=$totalScanned, 新增=${outcome.result.newAdded}")
        return ScanOutcome.Committed(outcome.result.copy(totalScanned = totalScanned, removed = 0))
    }

    private fun audioCollection(): Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    private fun mediaUri(mediaStoreId: Long): Uri = ContentUris.withAppendedId(audioCollection(), mediaStoreId)

    /** 查询 MediaStore 并完整读取；cursor 为 null 或读取出错返回 null（按失败处理） */
    private fun queryMediaStore(
        selection: String,
        selectionArgs: Array<String>?,
    ): List<MediaFileRow>? {
        val projection =
            buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.DISPLAY_NAME)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(MediaStore.Audio.Media.DATA)
                add(MediaStore.Audio.Media.DATE_MODIFIED)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(MediaStore.Audio.Media.BITRATE)
            }.toTypedArray()
        return try {
            context.contentResolver
                .query(audioCollection(), projection, selection, selectionArgs, "${MediaStore.Audio.Media.DATE_ADDED} DESC")
                ?.use(::readRows)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "MediaStore 查询失败")
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun readRows(cursor: Cursor): List<MediaFileRow> {
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val dateModifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val bitRateColumn =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE) else -1
        val rows = ArrayList<MediaFileRow>(cursor.count.coerceAtLeast(0))
        while (cursor.moveToNext()) {
            val path = cursor.getString(dataColumn).orEmpty()
            rows +=
                MediaFileRow(
                    mediaStoreId = cursor.getLong(idColumn),
                    path = path,
                    fileName = cursor.getString(nameColumn) ?: path.substringAfterLast('/'),
                    size = cursor.getLong(sizeColumn),
                    mimeType = cursor.getString(mimeTypeColumn).orEmpty(),
                    dateModified = cursor.getLong(dateModifiedColumn),
                    durationMs = cursor.getLong(durationColumn),
                    title = cursor.getString(titleColumn),
                    artist = cursor.getString(artistColumn),
                    bitRate = if (bitRateColumn >= 0) cursor.getInt(bitRateColumn) else 0,
                )
        }
        return rows
    }

    private fun isEligible(
        matcher: ScanRuleMatcher,
        row: MediaFileRow,
        ignorePrefixes: List<String>,
    ): Boolean {
        if (!matcher.matchesFormat(row.mimeType, row.path)) return false
        if (!matcher.matchesDuration(row.durationMs)) return false
        if (!matcher.matchesSize(row.size)) return false
        if (row.path.isNotEmpty() && ignorePrefixes.any { row.path.startsWith(it) }) return false
        return true
    }

    private suspend fun loadIgnorePrefixes(): List<String> =
        try {
            scanFolderRepository
                .getIgnoreFoldersSync()
                .mapNotNull { it.pathPrefix }
                .map { if (it.endsWith("/")) it else "$it/" }
        } catch (e: Exception) {
            emptyList()
        }

    /** 从 MediaStore 构建现有文件路径的可变集合 */
    @Suppress("DEPRECATION")
    private fun buildExistingPathsSet(): MutableSet<String> {
        val paths = mutableSetOf<String>()
        context.contentResolver.query(
            audioCollection(),
            arrayOf(MediaStore.Audio.Media.DATA),
            null,
            null,
            null,
        )?.use { cursor ->
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            if (dataCol >= 0) {
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol)
                    if (!path.isNullOrEmpty()) paths.add(path)
                }
            }
        }
        return paths
    }

    /**
     * 递归遍历 SAF 目录树，收集所有音频文件信息
     * @param treeUri  SAF tree URI
     * @param ignorePrefixes 忽略文件夹的绝对路径前缀列表（以 "/" 结尾）
     * @param ruleMatcher 扫描规则匹配器（扩展名 / 最小体积过滤）
     * @param parentDocId  当前递归层级的 document ID，首次调用传 null 使用 tree root
     * @param depth  当前递归深度，超过 MAX_FOLDER_DEPTH 时停止
     */
    private fun walkDocumentTree(
        treeUri: Uri,
        ignorePrefixes: List<String>,
        ruleMatcher: ScanRuleMatcher,
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

            while (cursor.moveToNext() && !isCancelled.get()) {
                val childDocId = cursor.getString(idCol) ?: continue
                val displayName = cursor.getString(nameCol) ?: continue
                val mimeType = cursor.getString(mimeCol) ?: continue
                val size = cursor.getLong(sizeCol)

                // 解析绝对路径（用于忽略文件夹匹配）
                val absolutePath = resolveAbsolutePath(childDocId)

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    // 是目录：本身位于忽略目录之下时跳过整棵子树；
                    // 若忽略目录是它的后代，仍需递归进入，由子层逐项过滤
                    val folderPath = if (absolutePath != null) {
                        if (absolutePath.endsWith("/")) absolutePath else "$absolutePath/"
                    } else null

                    if (folderPath != null && ignorePrefixes.any { folderPath.startsWith(it) }) {
                        continue // 跳过此子目录整棵树
                    }
                    results.addAll(walkDocumentTree(treeUri, ignorePrefixes, ruleMatcher, childDocId, depth + 1))
                } else {
                    // 是文件：按扫描规则过滤扩展名与最小体积
                    if (!ruleMatcher.matchesExtension(displayName)) continue
                    if (!ruleMatcher.matchesSize(size)) continue

                    // 忽略文件夹过滤
                    if (absolutePath != null && ignorePrefixes.any { absolutePath.startsWith(it) }) continue

                    val ext = displayName.substringAfterLast('.', "").lowercase()
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
}
