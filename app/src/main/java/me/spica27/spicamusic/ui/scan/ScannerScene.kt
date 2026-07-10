package me.spica27.spicamusic.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.ScanState
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.clickHighlight
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 扫描界面：以中央"雷达脉冲"圆盘为视觉主体，
 * 待机时缓慢呼吸，扫描时旋转扫光 + 进度环，完成 / 出错时切换状态色。
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
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = { Text(stringResource(R.string.scan_music)) },
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
//                ScannerAuroraBackground()
                ScannerBody(
                    scanState = scanState,
                    onStartScan = { viewModel.startFullScan() },
                    onCancelScan = { viewModel.cancelScan() },
                    onResetState = { viewModel.resetState() },
                    onOpenScanFolders = { path.push(ScanFoldersScene()) },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 页面状态归一
// ──────────────────────────────────────────────────────────────────────────

/** 圆盘 / 文案 / 操作区共用的归一化页面状态 */
private enum class DialPhase {
    Permission,
    Idle,
    Scanning,
    Success,
    Error,
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScannerBody(
    scanState: ScanState,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    onOpenScanFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    val permissionState = rememberPermissionState(permissionName)
    val granted = permissionState.status.isGranted

    val phase =
        when {
            !granted -> DialPhase.Permission
            scanState is ScanState.Scanning -> DialPhase.Scanning
            scanState is ScanState.Success -> DialPhase.Success
            scanState is ScanState.Error -> DialPhase.Error
            else -> DialPhase.Idle
        }

    val progress = (scanState as? ScanState.Scanning)?.progress ?: ScanProgress(0, 0, "")
    val progressFraction =
        if (progress.total > 0) {
            (progress.current.toFloat() / progress.total).coerceIn(0f, 1f)
        } else {
            0f
        }
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 360),
        label = "scan_progress",
    )

    Column(
        modifier =
            modifier.padding(
                horizontal = LayoutTokens.MusicHeaderHorizontalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        ScannerDial(
            phase = phase,
            progress = animatedProgress,
            hasDeterminateProgress = progress.total > 0,
        )

        Spacer(Modifier.height(Spacing.ExtraLarge))

        ScannerStatusText(
            phase = phase,
            progress = progress,
            errorMessage = (scanState as? ScanState.Error)?.message.orEmpty(),
        )

        Spacer(Modifier.weight(1f))

        ScannerActionPanel(
            phase = phase,
            progress = progress,
            result = (scanState as? ScanState.Success)?.result ?: ScanResult(0, 0, 0, 0),
            shouldShowRationale = permissionState.status.shouldShowRationale,
            onRequestPermission = { permissionState.launchPermissionRequest() },
            onOpenSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    },
                )
            },
            onStartScan = onStartScan,
            onCancelScan = onCancelScan,
            onResetState = onResetState,
            onOpenScanFolders = onOpenScanFolders,
        )

        Spacer(Modifier.height(Spacing.Huge))
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 背景氛围
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerAuroraBackground() {
    val transition = rememberInfiniteTransition(label = "scanner_aurora")
    val drift by transition.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 6400),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "scanner_aurora_drift",
    )

    Box(Modifier.fillMaxSize()) {
        AuroraOrb(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            size = 280,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 72.dp, y = (-56).dp)
                    .graphicsLayer {
                        translationY = drift
                        translationX = -drift * 0.4f
                    },
        )
        AuroraOrb(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
            size = 240,
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-96).dp)
                    .graphicsLayer {
                        translationY = -drift * 0.7f
                        translationX = drift * 0.3f
                    },
        )
        AuroraOrb(
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            size = 200,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 64.dp, y = 48.dp)
                    .graphicsLayer { translationY = -drift * 0.4f },
        )
    }
}

@Composable
private fun AuroraOrb(
    color: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(size.dp)
                .blur(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(color, color.copy(alpha = 0f))),
                ),
    )
}

// ──────────────────────────────────────────────────────────────────────────
// 中央雷达圆盘
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerDial(
    phase: DialPhase,
    progress: Float,
    hasDeterminateProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    val isScanning = phase == DialPhase.Scanning
    val accentColor =
        when (phase) {
            DialPhase.Error -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.primary
        }
    val animatedAccent by animateColorAsState(accentColor, label = "dial_accent")
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    val infinite = rememberInfiniteTransition(label = "dial_anim")

    // 扫描时的旋转扫光角度（线性匀速）
    val sweepAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2600, easing = LinearEasing),
            ),
        label = "dial_sweep",
    )

    // 雷达涟漪相位（0..1 循环，三道涟漪相位错开）
    val ripplePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = EaseOutCubic),
            ),
        label = "dial_ripple",
    )

    // 待机呼吸缩放
    val breath by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 2400),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dial_breath",
    )

    val rippleStrength by animateFloatAsState(
        targetValue = if (isScanning) 1f else 0.45f,
        animationSpec = tween(600),
        label = "dial_ripple_strength",
    )

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 涟漪 + 扫光 + 进度环（Draw 阶段读取动画值，避免整树重组）
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val discRadius = size.minDimension * 0.255f
            val maxRadius = size.minDimension * 0.5f

            // 三道相位错开的雷达涟漪
            repeat(3) { i ->
                val t = (ripplePhase + i / 3f) % 1f
                val radius = discRadius + (maxRadius - discRadius) * t
                val alpha = (1f - t) * 0.30f * rippleStrength
                if (alpha > 0.01f) {
                    drawCircle(
                        color = animatedAccent.copy(alpha = alpha),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            val ringRadius = size.minDimension * 0.36f
            val ringStroke = 10.dp.toPx()

            // 进度环轨道
            drawCircle(
                color = trackColor,
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringStroke),
            )

            if (isScanning) {
                // 旋转扫光（雷达扫描臂）
                rotate(degrees = sweepAngle, pivot = center) {
                    drawCircle(
                        brush =
                            Brush.sweepGradient(
                                colors =
                                    listOf(
                                        animatedAccent.copy(alpha = 0f),
                                        animatedAccent.copy(alpha = 0f),
                                        animatedAccent.copy(alpha = 0.55f),
                                    ),
                                center = center,
                            ),
                        radius = ringRadius,
                        center = center,
                        style = Stroke(width = ringStroke),
                    )
                }
            }

            // 确定性进度弧
            val progressSweep =
                when {
                    isScanning && hasDeterminateProgress -> 360f * progress
                    phase == DialPhase.Success -> 360f
                    else -> 0f
                }
            if (progressSweep > 0f) {
                drawArc(
                    color = animatedAccent,
                    startAngle = -90f,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                )
            }
        }

        // 中央圆盘
        Box(
            modifier =
                Modifier
                    .size(132.dp)
                    .graphicsLayer {
                        val scale = if (isScanning) 1f else breath
                        scaleX = scale
                        scaleY = scale
                    }.clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                animatedAccent.copy(alpha = 0.92f),
                                animatedAccent,
                            ),
                        ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = phase to (isScanning && hasDeterminateProgress),
                transitionSpec = {
                    (
                        fadeIn(tween(220)) +
                            scaleIn(
                                spring(stiffness = Spring.StiffnessMedium),
                                initialScale = 0.7f,
                            )
                    ).togetherWith(fadeOut(tween(160)))
                },
                label = "dial_center",
            ) { (p, determinate) ->
                if (determinate) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector =
                            when (p) {
                                DialPhase.Permission -> Icons.Default.Lock
                                DialPhase.Idle -> Icons.Default.MusicNote
                                DialPhase.Scanning -> Icons.Default.GraphicEq
                                DialPhase.Success -> Icons.Default.Check
                                DialPhase.Error -> Icons.Default.PriorityHigh
                            },
                        contentDescription = null,
                        tint =
                            if (p == DialPhase.Error) {
                                MaterialTheme.colorScheme.onError
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            },
                        modifier = Modifier.size(52.dp),
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 状态文案
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerStatusText(
    phase: DialPhase,
    progress: ScanProgress,
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 6 })
                .togetherWith(fadeOut(tween(160)))
        },
        label = "scan_status_text",
        modifier = modifier,
    ) { p ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            val (title, subtitle) =
                when (p) {
                    DialPhase.Permission ->
                        stringResource(R.string.scanner_hero_title) to
                            stringResource(R.string.scanner_hero_subtitle)

                    DialPhase.Idle ->
                        stringResource(R.string.scanner_idle_title) to
                            stringResource(R.string.scanner_idle_subtitle)

                    DialPhase.Scanning ->
                        stringResource(R.string.scanner_running_title) to
                            if (progress.total > 0) {
                                stringResource(
                                    R.string.scanner_running_progress_format,
                                    progress.current,
                                    progress.total,
                                )
                            } else {
                                stringResource(R.string.scanner_preparing)
                            }

                    DialPhase.Success ->
                        stringResource(R.string.scanner_complete_title) to
                            stringResource(R.string.scanner_complete_subtitle)

                    DialPhase.Error ->
                        stringResource(R.string.scanner_error_title) to
                            errorMessage.ifBlank { stringResource(R.string.scanner_error_default) }
                }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 底部操作区
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerActionPanel(
    phase: DialPhase,
    progress: ScanProgress,
    result: ScanResult,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    onOpenScanFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(260)) + slideInVertically(tween(260)) { it / 5 })
                .togetherWith(fadeOut(tween(160)))
        },
        label = "scan_action_panel",
        modifier = modifier.fillMaxWidth(),
    ) { p ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            when (p) {
                DialPhase.Permission -> {
                    if (shouldShowRationale) {
                        Text(
                            text = stringResource(R.string.scanner_permission_denied_msg),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Button(
                            onClick = onOpenSettings,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                        ) {
                            Text(stringResource(R.string.scanner_go_to_settings))
                        }
                    } else {
                        Button(
                            onClick = onRequestPermission,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                        ) {
                            Text(stringResource(R.string.scanner_grant_permission))
                        }
                    }
                }

                DialPhase.Idle -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        ScanFeatureChip(
                            icon = Icons.Default.LibraryMusic,
                            label = stringResource(R.string.scanner_audio_files_chip),
                            modifier = Modifier.weight(1f),
                            onClick = {
                                Toast
                                    .makeText(
                                        App.getInstance(),
                                        "用于调节音频的其他扫描条件，比如体积、时长、格式，还没写",
                                        Toast.LENGTH_LONG,
                                    ).show()
                            },
                        )
                        ScanFeatureChip(
                            icon = Icons.Default.Folder,
                            label = stringResource(R.string.scanner_extra_folders_chip),
                            onClick = onOpenScanFolders,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Button(
                        onClick = onStartScan,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(Spacing.Small))
                        Text(stringResource(R.string.scanner_start))
                    }
                }

                DialPhase.Scanning -> {
                    CurrentFileCard(currentFile = progress.currentFile)
                    OutlinedButton(
                        onClick = onCancelScan,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        colors =
                            ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                    ) {
                        Text(stringResource(R.string.stop_scanner))
                    }
                }

                DialPhase.Success -> {
                    ScanResultRow(result = result)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        OutlinedButton(
                            onClick = {
                                onResetState()
                                onStartScan()
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(52.dp),
                        ) {
                            Text(stringResource(R.string.scanner_rescan))
                        }
                        Button(
                            onClick = onResetState,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(52.dp),
                        ) {
                            Text(stringResource(R.string.scanner_done))
                        }
                    }
                }

                DialPhase.Error -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        OutlinedButton(
                            onClick = onResetState,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(52.dp),
                        ) {
                            Text(stringResource(R.string.scanner_later))
                        }
                        Button(
                            onClick = {
                                onResetState()
                                onStartScan()
                            },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(52.dp),
                        ) {
                            Text("重试")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanFeatureChip(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier =
            if (onClick == null) {
                modifier
            } else {
                modifier
                    .clip(Shapes.LargeCornerBasedShape)
                    .clickHighlight(onClick = onClick)
            },
        shape = Shapes.LargeCornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
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
                    text = stringResource(R.string.scanner_current_file_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = currentFile.ifBlank { stringResource(R.string.scanner_waiting_file) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ScanResultRow(
    result: ScanResult,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        ScanResultTile(
            stringResource(R.string.total_scanned),
            result.totalScanned.toString(),
            Modifier.weight(1f),
        )
        ScanResultTile(
            stringResource(R.string.new_added),
            result.newAdded.toString(),
            Modifier.weight(1f),
        )
        ScanResultTile(
            stringResource(R.string.updated),
            result.updated.toString(),
            Modifier.weight(1f),
        )
        ScanResultTile(
            stringResource(R.string.removed),
            result.removed.toString(),
            Modifier.weight(1f),
        )
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f),
    ) {
        Column(
            modifier = Modifier.padding(vertical = Spacing.Medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
