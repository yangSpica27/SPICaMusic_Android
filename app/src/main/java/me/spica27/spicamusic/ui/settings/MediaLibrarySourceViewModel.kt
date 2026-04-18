package me.spica27.spicamusic.ui.settings

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.FolderType
import me.spica27.spicamusic.feature.library.domain.MusicScanUseCases
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.feature.library.domain.ScanFolderUseCases
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult

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
class MediaLibrarySourceViewModel(
    private val app: Application,
    private val scanService: MusicScanUseCases,
    private val folderRepository: ScanFolderUseCases,
) : AndroidViewModel(app) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    val extraFolders: StateFlow<List<ScanFolder>> =
        folderRepository
            .getExtraFoldersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ignoreFolders: StateFlow<List<ScanFolder>> =
        folderRepository
            .getIgnoreFoldersFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 监听扫描进度
        viewModelScope.launch {
            scanService.getScanProgress().collect { progress ->
                if (progress != null) {
                    _scanState.value = ScanState.Scanning(progress)
                }
            }
        }
    }

    /**
     * 开始扫描 MediaStore
     */
    fun startMediaStoreScan() {
        viewModelScope.launch {
            try {
                _scanState.value = ScanState.Scanning(ScanProgress(0, 0, app.getString(R.string.preparing_scan)))
                val result = scanService.scanMediaStore()
                _scanState.value = ScanState.Success(result)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: app.getString(R.string.scan_failed))
            }
        }
    }

    /**
     * 全量扫描：MediaStore + 额外文件夹（串行）
     */
    fun startFullScan() {
        viewModelScope.launch {
            try {
                _scanState.value = ScanState.Scanning(ScanProgress(0, 0, app.getString(R.string.preparing_scan)))
                val r1 = scanService.scanMediaStore()
                val r2 = scanService.scanExtraFolders()
                _scanState.value =
                    ScanState.Success(
                        ScanResult(
                            totalScanned = r1.totalScanned + r2.totalScanned,
                            newAdded = r1.newAdded + r2.newAdded,
                            updated = r1.updated + r2.updated,
                            removed = r1.removed + r2.removed,
                        ),
                    )
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: app.getString(R.string.scan_failed))
            }
        }
    }

    /**
     * 添加额外扫描文件夹（后台线程安全）
     * 自动处理 SAF 权限申请 + DisplayName 解析
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
            } catch (e: Exception) {
                // 权限申请失败或 IO 错误，静默处理
                timber.log.Timber.w(e, "Failed to add extra folder")
            }
        }
    }

    /**
     * 添加忽略文件夹（后台线程安全）
     * 不需要 SAF 权限，仅存储路径做过滤
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
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to add ignore folder")
            }
        }
    }

    /** 重新授权失效的 EXTRA 文件夹（用新 URI 替换旧记录） */
    fun reAuthorizeFolder(
        id: Long,
        newUri: Uri,
    ) {
        viewModelScope.launch {
            folderRepository.reAuthorize(id, newUri.toString())
        }
    }

    fun removeFolder(id: Long) {
        viewModelScope.launch {
            folderRepository.removeFolder(id)
        }
    }

    fun cancelScan() {
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
