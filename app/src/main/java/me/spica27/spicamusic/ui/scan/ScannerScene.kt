package me.spica27.spicamusic.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.ScanState
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.materialSharedAxisZIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisZOut
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 扫描界面
 */
class ScannerScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
        val scanState by viewModel.scanState.collectAsStateWithLifecycle()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { path.popTop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = { Text("扫描音乐") },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
            ) {
                ScannerAmbientBackground()
                ScannerContent(
                    scanState = scanState,
                    onStartScan = { viewModel.startFullScan() },
                    onCancelScan = { viewModel.cancelScan() },
                    onResetState = { viewModel.resetState() },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScannerContent(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val permissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    val permissionState = rememberPermissionState(permissionName)

    AnimatedContent(
        targetState = permissionState.status.isGranted,
        transitionSpec = {
            materialSharedAxisZIn(forward = true) togetherWith
                materialSharedAxisZOut(
                    forward = true,
                )
        },
        label = "scan_permission_anim",
        modifier = modifier,
    ) { granted ->
        if (granted) {
            ScanReadyContent(
                scanState = scanState,
                onStartScan = onStartScan,
                onCancelScan = onCancelScan,
                onResetState = onResetState,
            )
        } else {
            PermissionRequestContent(
                shouldShowRationale = permissionState.status.shouldShowRationale,
                onRequestPermission = { permissionState.launchPermissionRequest() },
            )
        }
    }
}

@Composable
private fun ScannerAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "scanner_ambient")
    val drift by transition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 5200),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scanner_orb_drift",
    )

    Box(Modifier.fillMaxSize()) {
        AmbientOrb(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
            size = 250,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 56.dp, y = (-44).dp)
                    .graphicsLayer {
                        translationY = drift
                        translationX = -drift * 0.42f
                    },
        )
        AmbientOrb(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
            size = 220,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-88).dp, y = 28.dp)
                    .graphicsLayer {
                        translationY = -drift * 0.72f
                        translationX = drift * 0.3f
                    },
        )
        AmbientOrb(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            size = 180,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 64.dp, y = 42.dp)
                    .graphicsLayer {
                        translationY = -drift * 0.35f
                    },
        )
    }
}

@Composable
private fun AmbientOrb(
    color: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .blur(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color,
                            color.copy(alpha = 0f),
                        ),
                    ),
                ),
    )
}

@Composable
private fun PermissionRequestContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    ScannerPageLayout(modifier = modifier) {
        ScannerHeroCard(
            icon = if (shouldShowRationale) Icons.Default.Lock else Icons.Default.LibraryMusic,
            eyebrow = "权限准备",
            title = if (shouldShowRationale) "需要重新开启媒体权限" else "让 SPICa 发现你的音乐",
            subtitle =
                if (shouldShowRationale) {
                    "请前往系统设置开启媒体访问权限，返回后即可扫描本地曲库。"
                } else {
                    "扫描只会读取设备上的音频文件，用来建立本地音乐库和播放列表。"
                },
        ) {
            if (shouldShowRationale) {
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("前往系统设置")
                }
            } else {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("授予媒体权限")
                }
            }
        }
    }
}

@Composable
private fun ScanReadyContent(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stateKey =
        when (scanState) {
            is ScanState.Idle -> 0
            is ScanState.Scanning -> 1
            is ScanState.Success -> 2
            is ScanState.Error -> 3
        }

    AnimatedContent(
        targetState = stateKey,
        transitionSpec = {
            materialSharedAxisZIn(forward = true) togetherWith
                materialSharedAxisZOut(
                    forward = true,
                )
        },
        label = "scan_state_anim",
        modifier = modifier.fillMaxSize(),
    ) { key ->
        when (key) {
            0 -> IdleContent(onStartScan = onStartScan)
            1 ->
                ScanningContent(
                    progress =
                        (scanState as? ScanState.Scanning)?.progress ?: ScanProgress(
                            0,
                            0,
                            "",
                        ),
                    onCancel = onCancelScan,
                )

            2 ->
                SuccessContent(
                    result = (scanState as? ScanState.Success)?.result ?: ScanResult(0, 0, 0, 0),
                    onRescan = {
                        onResetState()
                        onStartScan()
                    },
                    onDone = onResetState,
                )

            else ->
                ErrorContent(
                    message = (scanState as? ScanState.Error)?.message ?: "",
                    onRetry = {
                        onResetState()
                        onStartScan()
                    },
                    onDismiss = onResetState,
                )
        }
    }
}

@Composable
private fun ScannerPageLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    PaddingValues(
                        start = LayoutTokens.MusicHeaderHorizontalPadding,
                        end = LayoutTokens.MusicHeaderHorizontalPadding,
                        top = Spacing.Large,
                        bottom = Spacing.Huge,
                    ),
                ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun ScannerHeroCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.ExtraLarge2CornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f),
        tonalElevation = 8.dp,
        shadowElevation = 2.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.46f),
                            ),
                        ),
                    ).padding(Spacing.ExtraLarge),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.Large),
                horizontalAlignment = Alignment.Start,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(66.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    Text(
                        text = eyebrow,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun IdleContent(
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScannerPageLayout(modifier = modifier) {
        ScannerHeroCard(
            icon = Icons.Default.Scanner,
            eyebrow = "本地曲库",
            title = "重新整理你的音乐宇宙",
            subtitle = "一次扫描 MediaStore 与额外文件夹，自动同步新增、更新和已移除的歌曲。",
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                ScanFeatureChip(
                    icon = Icons.Default.LibraryMusic,
                    label = "音频文件",
                    modifier = Modifier.weight(1f),
                )
                ScanFeatureChip(
                    icon = Icons.Default.Folder,
                    label = "额外目录",
                    modifier = Modifier.weight(1f),
                )
            }
            ElevatedButton(
                onClick = onStartScan,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.Small))
                Text("开始智能扫描")
            }
        }
    }
}

@Composable
private fun ScanFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.62f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.ExtraSmall))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ScanningContent(
    progress: ScanProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rawProgress = if (progress.total > 0) progress.current.toFloat() / progress.total else 0f
    val progressFraction = rawProgress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 360),
        label = "scan_progress",
    )

    ScannerPageLayout(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = Shapes.ExtraLarge2CornerBasedShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            tonalElevation = 8.dp,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.ExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                Box(
                    modifier = Modifier.size(132.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (progress.total > 0) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 9.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Text(
                            text = "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 9.dp,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        )
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp),
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    Text(
                        text = "正在捕捉音乐信号",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (progress.total > 0) "${progress.current} / ${progress.total} 首" else "正在准备扫描队列",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                CurrentFileCard(currentFile = progress.currentFile)
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("停止扫描")
                }
            }
        }
    }
}

@Composable
private fun CurrentFileCard(currentFile: String) {
    val targetColor =
        if (currentFile.isBlank()) {
            MaterialTheme.colorScheme.surfaceContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
    val cardColor by animateColorAsState(targetColor, label = "scan_file_card_color")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.LargeCornerBasedShape,
        color = cardColor.copy(alpha = 0.78f),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.Large),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            Icon(
                imageVector = Icons.Default.LibraryMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(
                    text = "当前文件",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentFile.ifBlank { "等待下一个音频文件..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    result: ScanResult,
    onRescan: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScannerPageLayout(modifier = modifier) {
        ScannerHeroCard(
            icon = Icons.Default.CheckCircle,
            eyebrow = "扫描完成",
            title = "曲库已经焕然一新",
            subtitle = "已完成本地媒体与额外目录同步，你可以立刻回到资料库继续播放。",
        ) {
            ScanResultGrid(result = result)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                OutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("重新扫描")
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("完成")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScannerPageLayout(modifier = modifier) {
        ScannerHeroCard(
            icon = Icons.Default.Error,
            eyebrow = "扫描中断",
            title = "没有完成本次整理",
            subtitle = message.ifBlank { "扫描过程中发生未知错误，请稍后重试。" },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("稍后再说")
                }
                Button(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun ScanResultGrid(
    result: ScanResult,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            ScanResultTile(
                label = "共扫描",
                value = result.totalScanned.toString(),
                modifier = Modifier.weight(1f),
            )
            ScanResultTile(
                label = "新增",
                value = result.newAdded.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
            ScanResultTile(
                label = "更新",
                value = result.updated.toString(),
                modifier = Modifier.weight(1f),
            )
            ScanResultTile(
                label = "移除",
                value = result.removed.toString(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScanResultTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "$label / 首",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
