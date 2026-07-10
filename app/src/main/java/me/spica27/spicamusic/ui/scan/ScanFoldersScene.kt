package me.spica27.spicamusic.ui.scan

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import org.koin.compose.viewmodel.koinActivityViewModel

class ScanFoldersScene : DialogScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val density = LocalDensity.current
        val slideOffsetPx = with(density) { 80.dp.toPx() }

        BackHandler(true) {
            path.pop(scene)
        }

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize(),
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = enterProgress.value }
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) { path.pop(scene) },
            )

            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .graphicsLayer {
                            val p = enterProgress.value
                            alpha = p
                            translationY = (1f - p) * slideOffsetPx
                        },
            ) {
                DialogContent()
            }
        }
    }

    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val context = LocalContext.current
        val viewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
        val extraFolders by viewModel.extraFolders.collectAsStateWithLifecycle()
        val ignoreFolders by viewModel.ignoreFolders.collectAsStateWithLifecycle()

        var pendingReauthFolderId by rememberSaveable { mutableLongStateOf(-1L) }
        val addExtraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let { viewModel.addExtraFolder(context, it) }
            }
        val addIgnoreLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                uri?.let { viewModel.addIgnoreFolder(context, it) }
            }
        val reauthLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                val folderId = pendingReauthFolderId
                if (uri != null && folderId >= 0) {
                    viewModel.reAuthorizeFolder(context, folderId, uri)
                }
                pendingReauthFolderId = -1L
            }

        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .navigationBarsPadding(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 10.dp)
                            .width(44.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                            .align(Alignment.CenterHorizontally),
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = Spacing.Large, bottom = Spacing.Medium),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
                    ) {
                        Text(
                            text = stringResource(R.string.scan_folders_dialog_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.scan_folders_dialog_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { path.pop(scene) }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp),
                    contentPadding = PaddingValues(bottom = Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    item(key = "extra_header", contentType = "folder_header") {
                        ScanFolderSectionHeader(
                            title = stringResource(R.string.extra_scan_folders),
                            description = stringResource(R.string.add_extra_folder_hint),
                            actionLabel = stringResource(R.string.add_extra_folder),
                            onAddClick = { addExtraLauncher.launch(null) },
                        )
                    }
                    if (extraFolders.isEmpty()) {
                        item(key = "extra_empty", contentType = "folder_empty") {
                            ScanFolderEmptyHint(text = stringResource(R.string.no_extra_scan_folders))
                        }
                    } else {
                        items(
                            items = extraFolders,
                            key = { folder -> "extra_${folder.id}" },
                            contentType = { "scan_folder" },
                        ) { folder ->
                            ScanFolderRow(
                                folder = folder,
                                onRemove = { viewModel.removeFolder(context, folder) },
                                onReAuthorize =
                                    {
                                        pendingReauthFolderId = folder.id
                                        reauthLauncher.launch(null)
                                    }.takeIf { !folder.isAccessible },
                            )
                        }
                    }

                    item(key = "ignore_header", contentType = "folder_header") {
                        ScanFolderSectionHeader(
                            title = stringResource(R.string.ignore_folders),
                            description = stringResource(R.string.add_ignore_folder_hint),
                            actionLabel = stringResource(R.string.add_ignore_folder),
                            onAddClick = { addIgnoreLauncher.launch(null) },
                            modifier = Modifier.padding(top = Spacing.Medium),
                        )
                    }
                    if (ignoreFolders.isEmpty()) {
                        item(key = "ignore_empty", contentType = "folder_empty") {
                            ScanFolderEmptyHint(text = stringResource(R.string.library_no_ignore_folders))
                        }
                    } else {
                        items(
                            items = ignoreFolders,
                            key = { folder -> "ignore_${folder.id}" },
                            contentType = { "scan_folder" },
                        ) { folder ->
                            ScanFolderRow(
                                folder = folder,
                                onRemove = { viewModel.removeFolder(context, folder) },
                                onReAuthorize = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFolderSectionHeader(
    title: String,
    description: String,
    actionLabel: String,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun ScanFolderEmptyHint(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(Spacing.Large),
        )
    }
}

@Composable
private fun ScanFolderRow(
    folder: ScanFolder,
    onRemove: () -> Unit,
    onReAuthorize: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            val badgeColor =
                if (folder.isAccessible) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (folder.isAccessible) Icons.Default.Folder else Icons.Default.FolderOff,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = folder.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (folder.isAccessible) {
                            folder.pathPrefix ?: folder.uriString
                        } else {
                            stringResource(R.string.folder_inaccessible)
                        },
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color =
                        if (folder.isAccessible) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                )
            }

            AnimatedContent(
                targetState = onReAuthorize != null,
                transitionSpec = { materialSharedAxisZ(forward = true) },
                label = "scan_folder_reauthorize",
            ) { needsReauthorize ->
                if (needsReauthorize && onReAuthorize != null) {
                    OutlinedButton(onClick = onReAuthorize) {
                        Text(stringResource(R.string.reauthorize))
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove_folder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
