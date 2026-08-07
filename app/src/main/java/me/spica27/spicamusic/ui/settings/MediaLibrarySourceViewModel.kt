package me.spica27.spicamusic.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.compose.runtime.Stable
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.FolderType
import me.spica27.spicamusic.feature.library.domain.MusicScanUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.feature.library.domain.ScanFolderUseCases
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult
import me.spica27.spicamusic.feature.library.domain.ScanRules
import me.spica27.spicamusic.feature.library.domain.ScanRulesUseCases
import me.spica27.spicamusic.feature.library.domain.SongUseCases
import me.spica27.spicamusic.feature.settings.domain.SettingsUseCases
import timber.log.Timber

/**
 * 媒体库扫描状态
 */
sealed class ScanState {
    data object Idle : ScanState()

    data class Scanning(
        val progress: ScanProgress,
    ) : ScanState()

    data class Success(
        val result: ScanResult,
    ) : ScanState()

    data class Error(
        val message: String,
    ) : ScanState()
}

/**
 * 媒体库来源 ViewModel
 */
@Stable
class MediaLibrarySourceViewModel(
    private val app: Application,
    private val scanService: MusicScanUseCases,
    private val folderRepository: ScanFolderUseCases,
    private val scanRulesUseCases: ScanRulesUseCases,
    private val settingsUseCases: SettingsUseCases,
    songRepository: SongUseCases,
) : AndroidViewModel(app) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    /** 当前由本 VM 发起的扫描任务；取消扫描时必须真正取消协程，否则串行的第二阶段会继续执行 */
    private var scanJob: Job? = null

    val extraFolders: StateFlow<List<ScanFolder>> =
        folderRepository
            .getExtraFoldersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ignoreFolders: StateFlow<List<ScanFolder>> =
        folderRepository
            .getIgnoreFoldersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 当前扫描规则（时长/体积/格式），供扫描页摘要与规则配置页共用 */
    val scanRules: StateFlow<ScanRules> =
        scanRulesUseCases
            .getRulesFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScanRules.DEFAULT)

    /** 上次扫描完成时间（epoch millis），null 表示还没扫描过 */
    val lastScanAt: StateFlow<Long?> =
        settingsUseCases
            .getString(SettingsUseCases.Keys.SCAN_LAST_COMPLETED_AT, "")
            .map { it.toLongOrNull() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 曲库当前歌曲数 */
    val libraryCount: StateFlow<Int> =
        songRepository
            .getSongsCountFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // 监听扫描进度：非空 → 扫描中；回落 null 且没有本 VM 发起的任务在跑
        // （即 MediaStore 观察者在服务侧自行触发的同步结束）→ 复位，避免页面永久卡在「扫描中」
        viewModelScope.launch {
            scanService.getScanProgress().collect { progress ->
                if (progress != null) {
                    _scanState.value = ScanState.Scanning(progress)
                } else if (scanJob?.isActive != true && _scanState.value is ScanState.Scanning) {
                    _scanState.value = ScanState.Idle
                }
            }
        }
    }

    /**
     * 开始扫描 MediaStore
     */
    fun startMediaStoreScan() {
        startScan { scanService.scanMediaStore() }
    }

    /**
     * 全量扫描：MediaStore + 额外文件夹（串行）
     */
    fun startFullScan() {
        startScan {
            val r1 = scanService.scanMediaStore()
            val r2 = scanService.scanExtraFolders()
            ScanResult(
                totalScanned = r1.totalScanned + r2.totalScanned,
                newAdded = r1.newAdded + r2.newAdded,
                updated = r1.updated + r2.updated,
                removed = r1.removed + r2.removed,
            )
        }
    }

    // ── 扫描规则 ─────────────────────────────────────────────────────────

    fun setMinDurationSec(seconds: Int) {
        viewModelScope.launch {
            scanRulesUseCases.setMinDurationSec(seconds)
        }
    }

    fun setMinFileSizeKb(kb: Int) {
        viewModelScope.launch {
            scanRulesUseCases.setMinFileSizeKb(kb)
        }
    }

    /** 切换格式启用状态；至少保留一种格式（取消最后一种时忽略操作）。
     * 以持久化的最新规则为基线，避免 stateIn 初始 DEFAULT 值覆盖已存的格式集合。 */
    fun toggleFormat(key: String) {
        viewModelScope.launch {
            val current = scanRulesUseCases.getRulesFlow().first().enabledFormatKeys
            val next =
                if (key in current) {
                    if (current.size <= 1) return@launch
                    current - key
                } else {
                    current + key
                }
            scanRulesUseCases.setEnabledFormats(next)
        }
    }

    private suspend fun markScanCompleted() {
        try {
            settingsUseCases.setString(
                SettingsUseCases.Keys.SCAN_LAST_COMPLETED_AT,
                System.currentTimeMillis().toString(),
            )
        } catch (e: Exception) {
            timber.log.Timber.w(e, "Failed to record last scan time")
        }
    }

    /**
     * 添加额外扫描文件夹（后台线程安全）
     * 自动处理 SAF 权限申请 + DisplayName 解析，添加成功后自动扫描该目录
     */
    fun addExtraFolder(
        context: Context,
        uri: Uri,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 申请持久 URI 权限
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )

                // 解析显示名称（IO 操作）
                val displayName =
                    DocumentFile.fromTreeUri(context, uri)?.name
                        ?: uri.lastPathSegment
                        ?: "Unknown"

                // 添加到数据库
                folderRepository.addFolder(
                    uriString = uri.toString(),
                    displayName = displayName,
                    folderType = FolderType.EXTRA,
                    pathPrefix = resolvePathPrefix(uri),
                )

                // 自动扫描新目录，将其中音频注册进 MediaStore 并入库
                startScan { scanService.scanExtraFolders() }
            } catch (e: Exception) {
                // 权限申请失败或 IO 错误，静默处理
                timber.log.Timber.w(e, "Failed to add extra folder")
            }
        }
    }

    /**
     * 添加忽略文件夹（后台线程安全）
     * 不需要 SAF 权限，仅存储路径做过滤；添加后自动重扫以移除已入库的忽略歌曲
     */
    fun addIgnoreFolder(
        context: Context,
        uri: Uri,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 解析显示名称（IO 操作）
                val displayName =
                    DocumentFile.fromTreeUri(context, uri)?.name
                        ?: uri.lastPathSegment
                        ?: "Unknown"

                // 添加到数据库
                folderRepository.addFolder(
                    uriString = uri.toString(),
                    displayName = displayName,
                    folderType = FolderType.IGNORE,
                    pathPrefix = resolvePathPrefix(uri),
                )

                // 重扫 MediaStore：全量扫描会把忽略目录下已入库的歌曲移除
                startScan { scanService.scanMediaStore() }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to add ignore folder")
            }
        }
    }

    /** 重新授权失效的 EXTRA 文件夹（用新 URI 替换旧记录），并自动重扫 */
    fun reAuthorizeFolder(
        context: Context,
        id: Long,
        newUri: Uri,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    newUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                folderRepository.reAuthorize(id, newUri.toString(), resolvePathPrefix(newUri))
                startScan { scanService.scanExtraFolders() }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to re-authorize folder")
            }
        }
    }

    /**
     * 删除目录：
     * - EXTRA：释放 SAF 持久权限（系统持久权限有上限），已入库歌曲保留（它们已注册进 MediaStore）
     * - IGNORE：删除后重扫 MediaStore，让该目录下的歌曲重新入库
     */
    fun removeFolder(
        context: Context,
        folder: ScanFolder,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                folderRepository.removeFolder(folder.id)
                when (folder.folderType) {
                    FolderType.EXTRA -> {
                        try {
                            context.contentResolver.releasePersistableUriPermission(
                                Uri.parse(folder.uriString),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        } catch (e: SecurityException) {
                            // 权限可能已被系统回收
                        }
                    }

                    FolderType.IGNORE -> {
                        startScan { scanService.scanMediaStore() }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to remove folder")
            }
        }
    }

    /**
     * 启动一次可取消的扫描并同步 scanState。
     * - Success/Error 只在仍处于 Scanning 时写入：用户已取消（Idle）时不覆盖、不误记完成时间
     * - CancellationException 原样抛出，保证协程取消语义
     */
    private fun startScan(block: suspend () -> ScanResult) {
        scanJob?.cancel()
        scanJob =
            viewModelScope.launch {
                runScan(block)
            }
    }

    /** 执行扫描并同步 scanState（供目录增删后的自动重扫复用） */
    private suspend fun runScan(block: suspend () -> ScanResult) {
        try {
            _scanState.value = ScanState.Scanning(ScanProgress(0, 0, app.getString(R.string.preparing_scan)))
            val result = block()
            if (_scanState.value is ScanState.Scanning) {
                markScanCompleted()
                _scanState.value = ScanState.Success(result)
            }
        } catch (e: Exception) {
            if (_scanState.value is ScanState.Scanning) {
                _scanState.value = ScanState.Error(e.message ?: app.getString(R.string.scan_failed))
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        scanService.cancelScan()
        _scanState.value = ScanState.Idle
    }

    fun resetState() {
        _scanState.value = ScanState.Idle
    }

    /**
     * 从 SAF tree URI 解析绝对路径前缀
     * 主存储：primary:relative/path → /storage/emulated/0/relative/path
     * 外置存储：XXXX-XXXX:relative/path → /storage/XXXX-XXXX/relative/path
     */
    private fun resolvePathPrefix(treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val parts = docId.split(":", limit = 2)
            if (parts.size != 2) return null
            val (volume, relative) = parts
            if (volume.equals("primary", ignoreCase = true)) {
                "${Environment.getExternalStorageDirectory()}/$relative"
            } else {
                "/storage/$volume/$relative"
            }
        } catch (e: Exception) {
            null
        }
    }
}
