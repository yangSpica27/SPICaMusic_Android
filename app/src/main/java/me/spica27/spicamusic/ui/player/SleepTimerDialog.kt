package me.spica27.spicamusic.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.player.api.SleepTimerState
import java.util.Locale
import java.util.concurrent.TimeUnit

private val SleepTimerOptionsMinutes = listOf(15, 30, 45, 60, 90)

@Composable
@Suppress("FunctionName")
fun SleepTimerDialog(
    timer: SleepTimerState?,
    onDismiss: () -> Unit,
    onSetTimer: (durationMs: Long) -> Unit,
    onCancelTimer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_sleep_timer_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (timer != null) {
                    Text(
                        text =
                            stringResource(
                                R.string.settings_sleep_timer_active,
                                formatSleepTimerRemaining(timer.remainingMs),
                            ),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    TextButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_sleep_timer_cancel))
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                SleepTimerOptionsMinutes.forEach { minutes ->
                    TextButton(
                        onClick = {
                            onSetTimer(TimeUnit.MINUTES.toMillis(minutes.toLong()))
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sleep_timer_minutes, minutes),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

fun formatSleepTimerRemaining(remainingMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs.coerceAtLeast(0L))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
