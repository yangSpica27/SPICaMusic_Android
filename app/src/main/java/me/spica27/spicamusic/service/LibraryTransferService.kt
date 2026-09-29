package me.spica27.spicamusic.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.spica27.spicamusic.MainActivity
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.transfer.data.LibraryTransferCoordinator
import me.spica27.spicamusic.feature.transfer.data.TransferAction
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import org.koin.android.ext.android.inject

/** 页面离开前台后继续执行曲库传输。 */
class LibraryTransferService : Service() {
    private val coordinator: LibraryTransferCoordinator by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var operation: Job? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.library_transfer_title), NotificationManager.IMPORTANCE_LOW),
        )
        startForeground(NOTIFICATION, notification(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        scope.launch {
            coordinator.state.collect { state ->
                if (state is TransferState.Running) manager.notify(NOTIFICATION, notification(state))
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        if (operation?.isActive == true) return START_NOT_STICKY
        val action = intent?.getStringExtra(EXTRA_ACTION)?.let { runCatching { TransferAction.valueOf(it) }.getOrNull() }
        if (action == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        operation =
            scope.launch {
                try {
                    coordinator.execute(action, intent.getStringExtra(EXTRA_URI), intent.getBooleanExtra(EXTRA_REPLACE_LYRICS, false))
                } finally {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        return START_NOT_STICKY
    }

    override fun onTimeout(
        startId: Int,
        fgsType: Int,
    ) {
        coordinator.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun notification(state: TransferState.Running?): android.app.Notification {
        val open =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val total = state?.total ?: 0L
        val done = state?.completed ?: 0L
        return NotificationCompat
            .Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(getString(R.string.library_transfer_title))
            .setContentText(state?.message ?: getString(R.string.library_transfer_preparing))
            .setContentIntent(open)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(1000, if (total > 0) (done.toDouble() / total * 1000).toInt().coerceIn(0, 1000) else 0, total <= 0)
            .build()
    }

    companion object {
        const val EXTRA_ACTION = "transfer_action"
        const val EXTRA_URI = "transfer_uri"
        const val EXTRA_REPLACE_LYRICS = "replace_lyrics"
        private const val CHANNEL = "library_transfer"
        private const val NOTIFICATION = 7103
    }
}
