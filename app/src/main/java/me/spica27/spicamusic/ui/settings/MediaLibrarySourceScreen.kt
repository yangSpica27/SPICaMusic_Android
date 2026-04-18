package me.spica27.spicamusic.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.ScanFolder
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.navigation.Screen
import me.spica27.spicamusic.utils.navSharedBounds
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.SinkFeedback
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 媒体库来源页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaLibrarySourceScreen(modifier: Modifier = Modifier) {
    val backStack = LocalNavBackStack.current
    val viewModel = koinViewModel<MediaLibrarySourceViewModel>()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val extraFolders by viewModel.extraFolders.collectAsStateWithLifecycle()
    val ignoreFolders by viewModel.ignoreFolders.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 权限状态
    var hasPermission by remember {
        mutableStateOf(checkAudioPermission(context))
    }

    // 权限申请启动器
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                viewModel.startFullScan()
            }
        }

    // 额外扫描文件夹选择器
    val extraFolderPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let { viewModel.addExtraFolder(context, it) }
        }

    // 忽略文件夹选择器
    val ignoreFolderPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let { viewModel.addIgnoreFolder(context, it) }
        }

    // 重新授权选择器（点击失效文件夹时使用）
    var pendingReAuthFolderId by remember { mutableStateOf<Long?>(null) }
    val reAuthPicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            uri?.let {
                pendingReAuthFolderId?.let { folderId ->
                    context.contentResolver.takePersistableUriPermission(
                        it,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    viewModel.reAuthorizeFolder(folderId, it)
                }
            }
            pendingReAuthFolderId = null
        }

    Scaffold(
        modifier =
            modifier
                .navSharedBounds(
                    Screen.MediaLibrarySource,
                ).fillMaxSize(),
        topBar = {
            TopAppBar(
                title = stringResource(R.string.media_library_source_title),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(
                    modifier =
                        Modifier
                            .height(4.dp),
                )
            }

            // MediaStore 扫描卡片
            item {
                MediaStoreScanCard(
                    hasPermission = hasPermission,
                    onScanClick = {
                        if (hasPermission) {
                            viewModel.startFullScan()
                        } else {
                            permissionLauncher.launch(getAudioPermission())
                        }
                    },
                )
            }

            // 扫描状态显示（带动画）
            item {
                AnimatedContent(
                    targetState = scanState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                animationSpec = tween(300),
                                initialOffsetY = { it / 4 },
                            ) togetherWith fadeOut(animationSpec = tween(300)) +
                            slideOutVertically(
                                animationSpec = tween(300),
                                targetOffsetY = { -it / 4 },
                            )
                    },
                    label = "ScanStateAnimation",
                ) { state ->
                    when (state) {
                        is ScanState.Idle -> {
                            InfoCard(
                                title = stringResource(R.string.scan_instructions_title),
                                message = stringResource(R.string.scan_instructions_message),
                            )
                        }

                        is ScanState.Scanning -> {
                            ScanningCard(progress = state.progress)
                        }

                        is ScanState.Success -> {
                            ScanResultCard(
                                result = state.result,
                                onDismiss = { viewModel.resetState() },
                            )
                        }

                        is ScanState.Error -> {
                            ErrorCard(
                                message = state.message,
                                onRetry = { viewModel.startFullScan() },
                                onDismiss = { viewModel.resetState() },
                            )
                        }
                    }
                }
            }

//            // 额外扫描文件夹区域
//            item {
//                FolderSectionHeader(
//                    title = stringResource(R.string.extra_folders_title),
//                    description = stringResource(R.string.extra_folders_desc),
//                    onAddClick = { extraFolderPicker.launch(null) },
//                )
//            }
//
//            if (extraFolders.isEmpty()) {
//                item(key = "empty_extra") {
//                    EmptyFoldersCard(message = stringResource(R.string.no_extra_folders))
//                }
//            } else {
//                items(extraFolders, key = { it.id }) { folder ->
//                    FolderItemCard(
//                        folder = folder,
//                        isIgnoreType = false,
//                        onReAuthClick = {
//                            pendingReAuthFolderId = folder.id
//                            reAuthPicker.launch(null)
//                        },
//                        onDeleteClick = { viewModel.removeFolder(folder.id) },
//                    )
//                }
//            }
//
//            // 忽略文件夹区域
//            item {
//                Spacer(modifier = Modifier.height(8.dp))
//                FolderSectionHeader(
//                    title = stringResource(R.string.ignore_folders_title),
//                    description = stringResource(R.string.ignore_folders_desc),
//                    onAddClick = { ignoreFolderPicker.launch(null) },
//                )
//            }
//
//            if (ignoreFolders.isEmpty()) {
//                item(key = "empty_ignore") {
//                    EmptyFoldersCard(message = stringResource(R.string.no_ignore_folders))
//                }
//            } else {
//                items(ignoreFolders, key = { it.id }) { folder ->
//                    FolderItemCard(
//                        folder = folder,
//                        isIgnoreType = true,
//                        onReAuthClick = {},
//                        onDeleteClick = { viewModel.removeFolder(folder.id) },
//                    )
//                }
//            }

            item { Spacer(modifier = Modifier.height(160.dp)) }
        }
    }
}

@Composable
private fun MediaStoreScanCard(
    hasPermission: Boolean,
    onScanClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                contentColor = MiuixTheme.colorScheme.primaryContainer,
                color = MiuixTheme.colorScheme.primaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.full_scan),
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    if (hasPermission) {
                        stringResource(R.string.full_scan_desc)
                    } else {
                        stringResource(R.string.need_audio_permission_to_scan)
                    },
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScanClick,
                modifier =
                    Modifier
                        .pressable(interactionSource = null, indication = SinkFeedback())
                        .align(Alignment.End),
            ) {
                Text(
                    if (hasPermission) {
                        stringResource(R.string.start_scanner)
                    } else {
                        stringResource(R.string.grant_permission_and_scan)
                    },
                )
            }
        }
    }
}

@Composable
private fun FolderSectionHeader(
    title: String,
    description: String,
    onAddClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.title3,
                color = MiuixTheme.colorScheme.onSurface,
            )
            IconButton(
                onClick = onAddClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = description,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun FolderItemCard(
    folder: ScanFolder,
    isIgnoreType: Boolean,
    onReAuthClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                ),
        colors =
            CardDefaults.defaultColors(
                contentColor =
                    if (isIgnoreType) {
                        MiuixTheme.colorScheme.errorContainer
                    } else {
                        MiuixTheme.colorScheme.secondaryContainer
                    },
                color =
                    if (isIgnoreType) {
                        MiuixTheme.colorScheme.errorContainer
                    } else {
                        MiuixTheme.colorScheme.secondaryContainer
                    },
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    if (isIgnoreType) {
                        Icons.Default.FolderOff
                    } else {
                        Icons.Default.FolderOpen
                    },
                contentDescription = null,
                tint =
                    if (isIgnoreType) {
                        MiuixTheme.colorScheme.onErrorContainer
                    } else {
                        MiuixTheme.colorScheme.onSecondaryContainer
                    },
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.displayName,
                    style = MiuixTheme.textStyles.body1,
                    color =
                        if (isIgnoreType) {
                            MiuixTheme.colorScheme.onErrorContainer
                        } else {
                            MiuixTheme.colorScheme.onSecondaryContainer
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // 警告信息（带动画）
                AnimatedVisibility(
                    visible = !folder.isAccessible && !isIgnoreType,
                    enter =
                        fadeIn(animationSpec = tween(200)) +
                            slideInVertically(
                                animationSpec = tween(200),
                                initialOffsetY = { -it / 2 },
                            ),
                    exit =
                        fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -it / 2 },
                            ),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.folder_inaccessible),
                                style = MiuixTheme.textStyles.body2,
                                color = MiuixTheme.colorScheme.error,
                            )
                        }
                    }
                }

                if (folder.pathPrefix != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = folder.pathPrefix.toString(),
                        style = MiuixTheme.textStyles.body2,
                        color =
                            if (isIgnoreType) {
                                MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                            } else {
                                MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // 重新授权按钮（带动画）
            AnimatedVisibility(
                visible = !folder.isAccessible && !isIgnoreType,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(200)) + scaleOut(animationSpec = tween(200)),
            ) {
                TextButton(
                    text = stringResource(R.string.reauthorize_folder),
                    onClick = onReAuthClick,
                    modifier =
                        Modifier.pressable(
                            interactionSource = null,
                            indication = SinkFeedback(),
                        ),
                )
            }
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.remove_folder),
                    tint =
                        if (isIgnoreType) {
                            MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        } else {
                            MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        },
                )
            }
        }
    }
}

@Composable
private fun EmptyFoldersCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                contentColor = MiuixTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ScanningCard(progress: ScanProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                contentColor = MiuixTheme.colorScheme.secondaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.scanning),
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onSecondaryContainer,
                )
            }

            if (progress.total > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = progress.current.toFloat() / progress.total,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${progress.current} / ${progress.total}",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = "${(progress.current * 100 / progress.total)}%",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = progress.currentFile,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ScanResultCard(
    result: ScanResult,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                contentColor = MiuixTheme.colorScheme.tertiaryContainer,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.scan_completed),
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ResultRow(
                stringResource(R.string.total_scanned),
                stringResource(R.string.songs_count_format, result.totalScanned),
            )
            ResultRow(
                stringResource(R.string.new_added),
                stringResource(R.string.songs_count_format, result.newAdded),
            )
            ResultRow(
                stringResource(R.string.updated),
                stringResource(R.string.songs_count_format, result.updated),
            )
            ResultRow(
                stringResource(R.string.removed),
                stringResource(R.string.songs_count_format, result.removed),
            )

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = stringResource(R.string.btn_sure),
                onClick = onDismiss,
                modifier =
                    Modifier
                        .pressable(interactionSource = null, indication = SinkFeedback())
                        .align(Alignment.End),
            )
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(contentColor = MiuixTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.scan_failed),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onErrorContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    text = stringResource(R.string.cancel),
                    onClick = onDismiss,
                )
                Button(onClick = onRetry) {
                    Text(stringResource(R.string.retry))
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    message: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                contentColor = MiuixTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = title,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.8f),
                textAlign = TextAlign.Start,
            )
        }
    }
}

/**
 * 获取需要的音频权限
 * Android 13+ 使用 READ_MEDIA_AUDIO
 * Android 13- 使用 READ_EXTERNAL_STORAGE
 */
private fun getAudioPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

/**
 * 检查是否有音频权限
 */
private fun checkAudioPermission(context: Context): Boolean {
    val permission = getAudioPermission()
    return ContextCompat.checkSelfPermission(
        context,
        permission,
    ) == PackageManager.PERMISSION_GRANTED
}
