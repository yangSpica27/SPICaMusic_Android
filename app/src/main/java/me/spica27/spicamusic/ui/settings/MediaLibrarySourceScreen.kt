package me.spica27.spicamusic.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import me.spica27.spicamusic.navigation.LocalNavBackStack
import me.spica27.spicamusic.storage.api.ScanResult
import me.spica27.spicamusic.ui.player.ResetBottomPadding
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
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
    val scanState by viewModel.scanState.collectAsState()
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
                // 权限授予后自动开始扫描
                viewModel.startMediaStoreScan()
            }
        }

    ResetBottomPadding()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "媒体库来源",
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 扫描来源卡片
            ScanSourceCard(
                hasPermission = hasPermission,
                onScanMediaStore = {
                    if (hasPermission) {
                        viewModel.startMediaStoreScan()
                    } else {
                        // 请求权限
                        permissionLauncher.launch(getAudioPermission())
                    }
                },
            )

            // 扫描状态显示
            when (val state = scanState) {
                is ScanState.Idle -> {
                    InfoCard(
                        title = "扫描说明",
                        message =
                            "点击上方按钮开始扫描设备中的音乐文件\n\n" +
                                "• 支持格式: MP3, FLAC, WAV, M4A, OGG, OPUS\n" +
                                "• 自动过滤小于 10 秒的音频\n" +
                                "• 扫描完成后会自动更新媒体库",
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
                        onRetry = { viewModel.startMediaStoreScan() },
                        onDismiss = { viewModel.resetState() },
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.defaultColors().copy(
                        contentColor = MiuixTheme.colorScheme.tertiaryContainer,
                        color = MiuixTheme.colorScheme.tertiaryContainer,
                    ),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "自定义文件夹扫描功能",
                        style = MiuixTheme.textStyles.title4,
                        color = MiuixTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "即将推出，敬请期待",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanSourceCard(
    hasPermission: Boolean,
    onScanMediaStore: () -> Unit,
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
            Text(
                text = "扫描媒体库",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text =
                    if (hasPermission) {
                        "扫描设备中的所有音乐文件"
                    } else {
                        "需要音频文件访问权限才能扫描设备中的音乐文件"
                    },
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onScanMediaStore,
                modifier =
                    Modifier
                        .pressable(interactionSource = null, indication = SinkFeedback())
                        .align(Alignment.End),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (hasPermission) "开始扫描" else "授予权限并扫描")
            }
        }
    }
}

@Composable
private fun ScanningCard(progress: me.spica27.spicamusic.storage.api.ScanProgress) {
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
                    text = "正在扫描...",
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
                    text = "扫描完成",
                    style = MiuixTheme.textStyles.title4,
                    color = MiuixTheme.colorScheme.onTertiaryContainer,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            ResultRow("总计扫描", "${result.totalScanned} 首")
            ResultRow("新增歌曲", "${result.newAdded} 首")
            ResultRow("更新歌曲", "${result.updated} 首")
            ResultRow("删除歌曲", "${result.removed} 首")

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                text = "确定",
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
                text = "扫描失败",
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
                    text = "取消",
                    onClick = onDismiss,
                )
                Button(onClick = onRetry) {
                    Text("重试")
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
