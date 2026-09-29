package me.spica27.spicamusic.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import me.spica27.spicamusic.feature.transfer.data.LibraryTransferCoordinator
import me.spica27.spicamusic.feature.transfer.data.TransferAction
import me.spica27.spicamusic.service.LibraryTransferService

class LibraryTransferViewModel(
    private val application: Application,
    private val coordinator: LibraryTransferCoordinator,
) : ViewModel() {
    val state = coordinator.state

    fun export(uri: Uri) {
        persistPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        start(TransferAction.EXPORT, uri)
    }

    fun inspect(uri: Uri) {
        persistPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        start(TransferAction.INSPECT, uri)
    }

    fun confirmImport(replaceLyrics: Boolean) = start(TransferAction.IMPORT, replaceLyrics = replaceLyrics)

    fun resume() = start(TransferAction.RESUME)

    fun discard() = start(TransferAction.DISCARD)

    fun pause() = coordinator.cancel()

    private fun start(
        action: TransferAction,
        uri: Uri? = null,
        replaceLyrics: Boolean = false,
    ) {
        try {
            ContextCompat.startForegroundService(
                application,
                Intent(application, LibraryTransferService::class.java)
                    .putExtra(LibraryTransferService.EXTRA_ACTION, action.name)
                    .putExtra(LibraryTransferService.EXTRA_URI, uri?.toString())
                    .putExtra(LibraryTransferService.EXTRA_REPLACE_LYRICS, replaceLyrics),
            )
        } catch (e: Exception) {
            coordinator.reportStartFailure(e.message ?: "无法启动曲库传输")
        }
    }

    private fun persistPermission(
        uri: Uri,
        flags: Int,
    ) {
        // 文件提供方若不支持持久授权，导入时会先将文件复制到暂存区。
        runCatching { application.contentResolver.takePersistableUriPermission(uri, flags) }
    }
}
