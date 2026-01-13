package me.spica27.spicamusic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.ScanProgress
import me.spica27.spicamusic.storage.api.ScanResult

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
    private val scanService: IMusicScanService,
) : ViewModel() {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

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
                _scanState.value =
                    ScanState.Scanning(
                        ScanProgress(0, 0, "准备扫描..."),
                    )
                val result = scanService.scanMediaStore()
                _scanState.value = ScanState.Success(result)
            } catch (e: Exception) {
                _scanState.value = ScanState.Error(e.message ?: "扫描失败")
            }
        }
    }

    /**
     * 取消扫描
     */
    fun cancelScan() {
        scanService.cancelScan()
        _scanState.value = ScanState.Idle
    }

    /**
     * 重置状态
     */
    fun resetState() {
        _scanState.value = ScanState.Idle
    }
}
