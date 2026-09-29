@file:Suppress("ktlint:standard:function-naming")

package me.spica27.spicamusic.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.transfer.domain.TransferState
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.ElasticDragDefaults
import me.spica27.spicamusic.ui.widget.elasticDrag
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun LibraryTransferSettings(modifier: Modifier = Modifier) {
    val viewModel: LibraryTransferViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            uri?.let(viewModel::export)
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::inspect)
        }
    LibraryTransferContent(
        state = state,
        onExport = { export.launch("SPICaMusic-${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}.spica") },
        onImport = { import.launch(arrayOf("*/*")) },
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onDiscard = viewModel::discard,
        onConfirmImport = viewModel::confirmImport,
        modifier = modifier,
    )
}

@Composable
internal fun LibraryTransferContent(
    state: TransferState,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onConfirmImport: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = state is TransferState.Idle || state is TransferState.Success || (state as? TransferState.Failure)?.canRetry == false
    SettingsSectionCard(
        title = stringResource(R.string.library_transfer_title),
        subtitle = stringResource(R.string.library_transfer_subtitle),
        modifier = modifier,
    ) {
        NavigationRow(
            title = stringResource(R.string.library_transfer_export),
            summary = stringResource(R.string.library_transfer_export_description),
            icon = Icons.Default.FileUpload,
            enabled = ready,
            onClick = onExport,
        )
        SettingsItemDivider()
        NavigationRow(
            title = stringResource(R.string.library_transfer_import),
            summary = stringResource(R.string.library_transfer_import_description),
            icon = Icons.Default.FileDownload,
            enabled = ready,
            onClick = onImport,
        )
        TransferStatus(state, onPause, onResume, onDiscard)
    }
    val preview = state as? TransferState.Preview
    if (preview != null) {
        var replaceLyrics by rememberSaveable(preview.summary) { mutableStateOf(false) }
        val context = LocalContext.current
        AlertDialog(
            modifier = Modifier.elasticDrag(ElasticDragDefaults.Dialog),
            onDismissRequest = onDiscard,
            title = { Text(stringResource(R.string.library_transfer_preview_title)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    Text(stringResource(R.string.library_transfer_preview_counts, preview.summary.songs, preview.summary.playlists))
                    Text(
                        stringResource(
                            R.string.library_transfer_preview_merge,
                            preview.summary.newSongs,
                            preview.summary.reusedSongs,
                            preview.summary.newPlaylists,
                        ),
                    )
                    Text(
                        stringResource(
                            R.string.library_transfer_preview_space,
                            Formatter.formatFileSize(context, preview.summary.bytesToCopy),
                        ),
                    )
                    Text(stringResource(R.string.library_transfer_merge_policy))
                    if (preview.summary.lyricConflicts > 0) {
                        Text(stringResource(R.string.library_transfer_lyric_conflicts, preview.summary.lyricConflicts))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = replaceLyrics, onCheckedChange = { replaceLyrics = it })
                            Text(stringResource(R.string.library_transfer_replace_lyrics))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmImport(replaceLyrics) },
                ) { Text(stringResource(R.string.library_transfer_start_import)) }
            },
            dismissButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun TransferStatus(
    state: TransferState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (state is TransferState.Idle || state is TransferState.Preview) return
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        val message =
            when (state) {
                is TransferState.Running -> state.message
                is TransferState.Success -> state.message
                is TransferState.Interrupted -> state.message
                is TransferState.Failure -> state.message
            }
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (state is TransferState.Failure) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state is TransferState.Running) {
            if (state.total > 0) {
                LinearProgressIndicator(progress = {
                    (state.completed.toDouble() / state.total).toFloat().coerceIn(0f, 1f)
                }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onPause) { Text(stringResource(R.string.library_transfer_pause)) }
        }
        if (state is TransferState.Interrupted || state is TransferState.Failure && state.canRetry) {
            Row {
                TextButton(onClick = onResume) { Text(stringResource(R.string.library_transfer_resume)) }
                TextButton(onClick = onDiscard) { Text(stringResource(R.string.library_transfer_discard)) }
            }
        }
    }
}
