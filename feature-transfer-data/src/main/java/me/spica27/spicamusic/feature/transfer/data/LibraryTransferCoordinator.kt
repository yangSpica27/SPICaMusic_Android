package me.spica27.spicamusic.feature.transfer.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import me.spica27.spicamusic.feature.transfer.domain.PackageJson
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import me.spica27.spicamusic.storage.api.ILyricSourceReader
import me.spica27.spicamusic.storage.api.LibraryAccessGate
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import timber.log.Timber
import java.io.File
import java.util.UUID

enum class TransferAction { EXPORT, INSPECT, IMPORT, RESUME, DISCARD }

@Serializable
private data class TransferJob(
    val taskId: String,
    val uri: String,
    val phase: String,
    val preferImportedLyrics: Boolean = false,
)

/** 传输状态属于应用，前台服务负责维持任务运行。 */
class LibraryTransferCoordinator(
    private val context: Context,
    private val database: AppDatabase,
    private val accessGate: LibraryAccessGate,
    private val lyricReader: ILyricSourceReader,
) {
    private val directory = File(context.filesDir, "library-transfer")
    private val journal = AtomicFile(File(directory, "job.json"))
    private val operationMutex = Mutex()
    private var activeJob: Job? = null
    private val _state =
        MutableStateFlow<TransferState>(
            if (journal.baseFile.exists()) TransferState.Interrupted("有未完成的曲库传输，可以继续或取消。") else TransferState.Idle,
        )
    val state = _state.asStateFlow()
    private var lastProgressAt = 0L

    fun cancel() {
        activeJob?.cancel()
    }

    fun reportStartFailure(message: String) {
        _state.value = TransferState.Failure(message, journal.baseFile.exists())
    }

    suspend fun execute(
        action: TransferAction,
        uri: String? = null,
        preferImportedLyrics: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        if (!operationMutex.tryLock()) return@withContext
        activeJob = currentCoroutineContext()[Job]
        val previousPreview = (_state.value as? TransferState.Preview)?.summary
        var record: TransferJob? = null
        try {
            check(directory.exists() || directory.mkdirs()) { "无法创建传输工作目录" }
            record =
                when (action) {
                    TransferAction.EXPORT, TransferAction.INSPECT -> {
                        require(!journal.baseFile.exists()) { "请先继续或取消已有的传输任务" }
                        TransferJob(UUID.randomUUID().toString(), requireNotNull(uri), action.name).also(::save)
                    }
                    else -> load()
                }
            if (action == TransferAction.DISCARD) {
                if (record != null) discard(record)
                _state.value = TransferState.Idle
                return@withContext
            }
            var job = requireNotNull(record) { "没有可继续的传输任务" }
            if (action == TransferAction.IMPORT) {
                require(job.phase == "PREVIEW" && previousPreview != null) { "请先预览完整包" }
                job = job.copy(phase = "IMPORT", preferImportedLyrics = preferImportedLyrics)
                save(job)
                record = job
            }
            _state.value = TransferState.Running("准备曲库传输")
            accessGate.withAccess {
                val receipt = database.transferDao().receipt(job.taskId)
                if (receipt != null) {
                    finish(job, "已导入 ${receipt.songCount} 首歌曲、${receipt.playlistCount} 个歌单")
                    return@withAccess
                }
                if (job.phase == "EXPORT") {
                    val result = LibraryPackageExporter(context, database, lyricReader).export(Uri.parse(job.uri), ::progress)
                    finish(job, "已导出 ${result.first} 首歌曲、${result.second} 个歌单及音频、收藏和歌词")
                } else {
                    val importer = LibraryPackageImporter(context, database, lyricReader)
                    // 先清理上次中断且尚未提交的音频。
                    importer.cleanup(job.taskId)
                    val input = stage(job)
                    val plan = importer.prepare(input, ::progress)
                    // 恢复导入时重新预览，曲库变化后重新确认。
                    if (action != TransferAction.IMPORT || previousPreview != plan.preview) {
                        save(job.copy(phase = "PREVIEW"))
                        _state.value = TransferState.Preview(plan.preview)
                    } else {
                        importer.commit(job.taskId, input, plan, job.preferImportedLyrics, ::progress)
                        finish(job, "已导入 ${plan.preview.songs} 首歌曲、${plan.preview.playlists} 个歌单（复用 ${plan.preview.reusedSongs} 首已有歌曲）")
                    }
                }
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                if (!recoverCommitted(record)) {
                    if (record?.phase == "EXPORT") truncateExport(record)
                    _state.value = TransferState.Interrupted("传输已暂停。可以继续；尚未提交的导入不会改变曲库。")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Library transfer failed")
            if (!recoverCommitted(record)) {
                if (record?.phase == "EXPORT") truncateExport(record)
                _state.value = TransferState.Failure(e.message ?: "曲库传输失败", journal.baseFile.exists())
            }
        } finally {
            activeJob = null
            operationMutex.unlock()
        }
    }

    private suspend fun stage(job: TransferJob): File {
        val file = File(directory, "${job.taskId}.spica")
        if (file.isFile) return file
        val uri = Uri.parse(job.uri)
        val size =
            if (uri.scheme ==
                "file"
            ) {
                File(requireNotNull(uri.path)).length()
            } else {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
                    if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L
                } ?: 0L
            }
        val available = directory.usableSpace - 32L * 1024 * 1024
        require(available > 0 && (size <= 0 || size < available)) { "空间不足，无法暂存完整包" }
        val partial = File(directory, "${job.taskId}.partial")
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取完整包，请重新选择文件")
        input.use {
            partial.outputStream().use { output ->
                PackageArchive.copyAndHash(it, output, available) { bytes ->
                    progress(TransferState.Running("读取完整包", bytes, size))
                }
            }
        }
        check(partial.renameTo(file)) { "无法保存完整包" }
        return file
    }

    private suspend fun discard(job: TransferJob) {
        accessGate.withAccess {
            if (job.phase != "EXPORT" && database.transferDao().receipt(job.taskId) == null) {
                LibraryPackageImporter(context, database, lyricReader).cleanup(job.taskId)
            }
            if (job.phase == "EXPORT") truncateExport(job)
            clearFiles(job)
        }
    }

    private fun finish(
        job: TransferJob,
        message: String,
    ) {
        clearFiles(job)
        _state.value = TransferState.Success(message)
    }

    private suspend fun recoverCommitted(job: TransferJob?): Boolean {
        if (job == null || job.phase == "EXPORT") return false
        val receipt = runCatching { database.transferDao().receipt(job.taskId) }.getOrNull() ?: return false
        finish(job, "已导入 ${receipt.songCount} 首歌曲、${receipt.playlistCount} 个歌单")
        return true
    }

    private fun clearFiles(job: TransferJob) {
        File(directory, "${job.taskId}.spica").delete()
        File(directory, "${job.taskId}.partial").delete()
        journal.delete()
    }

    private fun truncateExport(job: TransferJob) {
        runCatching { context.contentResolver.openOutputStream(Uri.parse(job.uri), "wt")?.close() }
    }

    private fun load(): TransferJob? =
        if (journal.baseFile.exists()) {
            journal.openRead().use { PackageJson.decodeFromString<TransferJob>(it.readBounded(16 * 1024).toString(Charsets.UTF_8)) }
        } else {
            null
        }

    private fun save(job: TransferJob) {
        val output = journal.startWrite()
        try {
            output.write(PackageJson.encodeToString(job).toByteArray(Charsets.UTF_8))
            journal.finishWrite(output)
        } catch (e: Exception) {
            journal.failWrite(output)
            throw e
        }
    }

    private fun progress(value: TransferState.Running) {
        val now = android.os.SystemClock.elapsedRealtime()
        val previous = _state.value as? TransferState.Running
        if (previous?.message != value.message || now - lastProgressAt > 120 || value.completed == value.total) {
            lastProgressAt = now
            _state.value = value
        }
    }
}
