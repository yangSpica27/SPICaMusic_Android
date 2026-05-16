package me.spica27.spicamusic.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.ScanState
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
        val scanState by viewModel.scanState.collectAsState()

        Scaffold(
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
                )
            },
        ) { paddingValues ->
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

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScannerContent(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Android 13+ 使用 READ_MEDIA_AUDIO，低版本使用 READ_EXTERNAL_STORAGE
    val permissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    val permissionState = rememberPermissionState(permissionName)

    AnimatedContent(
        targetState = permissionState.status.isGranted,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "scan_permission_anim",
        modifier = modifier,
    ) { granted ->
        if (!granted) {
            // 权限未授予
            PermissionRequestContent(
                shouldShowRationale = permissionState.status.shouldShowRationale,
                onRequestPermission = { permissionState.launchPermissionRequest() },
            )
        } else {
            // 已有权限，显示扫描 UI
            ScanReadyContent(
                scanState = scanState,
                onStartScan = onStartScan,
                onCancelScan = onCancelScan,
                onResetState = onResetState,
            )
        }
    }
}

/** 权限未授予时的引导页 */
@Composable
private fun PermissionRequestContent(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (shouldShowRationale) Icons.Default.Lock else Icons.Default.LibraryMusic,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (shouldShowRationale) "媒体访问权限被拒绝" else "需要媒体访问权限",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text =
                if (shouldShowRationale) {
                    "请前往系统设置，手动开启「媒体和文件」访问权限，之后返回应用即可扫描本地音乐。"
                } else {
                    "扫描本地音乐需要读取媒体文件权限，请授予权限以继续。"
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        if (shouldShowRationale) {
            // 被永久拒绝，引导去设置
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        },
                    )
                },
            ) {
                Text("前往系统设置")
            }
        } else {
            Button(onClick = onRequestPermission) {
                Text("授予权限")
            }
        }
    }
}

/** 已有权限时的扫描主界面 */
@Composable
private fun ScanReadyContent(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 用状态类型（Int）作为动画 key，而不是整个 scanState 对象。
    // 这样 Scanning 内部每次进度更新时只做普通重组，不会触发 AnimatedContent 的淡入淡出动画。
    val stateKey =
        when (scanState) {
            is ScanState.Idle -> 0
            is ScanState.Scanning -> 1
            is ScanState.Success -> 2
            is ScanState.Error -> 3
        }

    AnimatedContent(
        targetState = stateKey,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "scan_state_anim",
        modifier = modifier.fillMaxSize(),
    ) { key ->
        when (key) {
            0 -> IdleContent(onStartScan = onStartScan)
            1 ->
                ScanningContent(
                    // scanState 直接从外部读取，每次进度更新直接重组 ScanningContent，
                    // 不经过 AnimatedContent 的 key 判断，不产生抖动
                    progress =
                        (scanState as? ScanState.Scanning)?.progress
                            ?: me.spica27.spicamusic.feature.library.domain
                                .ScanProgress(0, 0, ""),
                    onCancel = onCancelScan,
                )
            2 ->
                SuccessContent(
                    result =
                        (scanState as? ScanState.Success)?.result
                            ?: me.spica27.spicamusic.feature.library.domain
                                .ScanResult(0, 0, 0, 0),
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
private fun IdleContent(
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "扫描本地音乐",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "扫描设备上的音乐文件并加入音乐库",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("开始扫描")
        }
    }
}

@Composable
private fun ScanningContent(
    progress: me.spica27.spicamusic.feature.library.domain.ScanProgress,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(24.dp))
        Text(
            text = "正在扫描音乐...",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))

        // 进度条（total == 0 时显示不确定进度）
        if (progress.total > 0) {
            LinearProgressIndicator(
                progress = { progress.current.toFloat() / progress.total },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${progress.current} / ${progress.total}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // 当前文件名
        if (progress.currentFile.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = progress.currentFile,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel) {
            Text("取消")
        }
    }
}

@Composable
private fun SuccessContent(
    result: me.spica27.spicamusic.feature.library.domain.ScanResult,
    onRescan: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "扫描完成",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(16.dp))

        // 扫描结果统计
        ScanResultRow("共扫描", "${result.totalScanned} 首")
        ScanResultRow("新增", "${result.newAdded} 首")
        ScanResultRow("更新", "${result.updated} 首")
        ScanResultRow("移除", "${result.removed} 首")

        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRescan) {
                Text("重新扫描")
            }
            Button(onClick = onDone) {
                Text("完成")
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
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "扫描失败",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDismiss) { Text("取消") }
            Button(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
private fun ScanResultRow(
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
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
