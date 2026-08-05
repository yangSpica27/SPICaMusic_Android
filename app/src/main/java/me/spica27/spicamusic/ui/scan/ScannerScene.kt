package me.spica27.spicamusic.ui.scan

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.ScanProgress
import me.spica27.spicamusic.feature.library.domain.ScanResult
import me.spica27.spicamusic.feature.library.domain.ScanRules
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.settings.ScanState
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entranceGraphics
import me.spica27.spicamusic.ui.theme.rememberEntrance
import me.spica27.spicamusic.ui.widget.clickHighlight
import me.spica27.spicamusic.ui.widget.materialSharedAxisZ
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 扫描音乐页：刊头大标题 + 状态面板 + 扫描配置入口 + 常驻底部操作区。
 * 视觉与交互对齐 IgnoredSongsScene 的设计语言（自绘顶栏、跟手收缩、交错入场、clickHighlight）。
 */
class ScannerScene : StackScene() {
    @Composable
    override fun Content() {
        ScannerScreenContent()
    }
}

/** 首屏入场交错间隔 */

/** 大标题完全收进顶栏所需的滚动距离 */
private val MastheadCollapseDistance = 140.dp

/** 最短时长预设（秒），0 = 不限制 */
private val DURATION_PRESETS_SEC = listOf(0, 5, 10, 30, 60)

/** 最小体积预设（KB），0 = 不限制 */
private val SIZE_PRESETS_KB = listOf(0, 100, 500, 1024, 5120)

/** 页面归一化状态 */
private enum class ScanPhase {
    Permission,
    Idle,
    Scanning,
    Success,
    Error,
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun ScannerScreenContent() {
    val path = LocalNavigationPath.current
    val context = LocalContext.current
    val viewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val scanRules by viewModel.scanRules.collectAsStateWithLifecycle()
    val extraFolders by viewModel.extraFolders.collectAsStateWithLifecycle()
    val ignoreFolders by viewModel.ignoreFolders.collectAsStateWithLifecycle()
    val lastScanAt by viewModel.lastScanAt.collectAsStateWithLifecycle()
    val libraryCount by viewModel.libraryCount.collectAsStateWithLifecycle()

    val permissionName =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    val permissionState = rememberPermissionState(permissionName)

    val phase =
        when {
            !permissionState.status.isGranted -> ScanPhase.Permission
            scanState is ScanState.Scanning -> ScanPhase.Scanning
            scanState is ScanState.Success -> ScanPhase.Success
            scanState is ScanState.Error -> ScanPhase.Error
            else -> ScanPhase.Idle
        }

    // 入场编排只在页面首次呈现时播放一次
    var entrancePlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        entrancePlayed = true
    }

    // 记住各状态最后一份有效数据：AnimatedContent 退场帧会用最新数据重组正在退场的旧面板，
    // 若直接从 scanState 强转，会在切换瞬间闪回默认值（0%、0/0/0/0、默认错误文案）
    val statusMemory = remember { ScanStatusMemory() }
    when (val s = scanState) {
        is ScanState.Scanning -> statusMemory.progress = s.progress
        is ScanState.Success -> statusMemory.result = s.result
        is ScanState.Error -> statusMemory.error = s.message
        else -> Unit
    }

    val listState = rememberLazyListState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val mastheadGone by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    top = statusBarTop + 56.dp,
                    bottom = 220.dp,
                ),
            overscrollEffect = rememberIOSOverScrollEffect(Orientation.Vertical),
        ) {
            item(key = "scanner_masthead") {
                val entrance = rememberEntrance(order = 0, play = !entrancePlayed)
                ScannerMasthead(
                    phase = phase,
                    errorMessage = statusMemory.error,
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.Large)
                            .graphicsLayer {
                                // 跟手收缩：大标题缩小、上移、淡出，直接耦合滚动偏移
                                val t = mastheadCollapse(listState)
                                val enter = entrance.alpha
                                transformOrigin = TransformOrigin(0f, 0f)
                                alpha = (1f - t) * enter
                                translationY = -t * 16.dp.toPx() + entrance.translateFraction * 28.dp.toPx()
                                scaleX = 1f - 0.18f * t
                                scaleY = 1f - 0.18f * t
                            },
                )
            }

            item(key = "scanner_status") {
                val entrance = rememberEntrance(order = 1, play = !entrancePlayed)
                ScannerStatusPanel(
                    phase = phase,
                    progress = statusMemory.progress,
                    result = statusMemory.result,
                    errorMessage = statusMemory.error,
                    libraryCount = libraryCount,
                    lastScanAt = lastScanAt,
                    shouldShowRationale = permissionState.status.shouldShowRationale,
                    onRequestPermission = { permissionState.launchPermissionRequest() },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            },
                        )
                    },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.ExtraLarge)
                            .entranceGraphics(entrance),
                )
            }

            item(key = "scanner_setup") {
                val entrance = rememberEntrance(order = 2, play = !entrancePlayed)
                ScannerSetupSection(
                    rules = scanRules,
                    extraFolderCount = extraFolders.size,
                    ignoreFolderCount = ignoreFolders.size,
                    enabled = phase != ScanPhase.Scanning,
                    onOpenRules = { path.push(ScanRulesScene()) },
                    onOpenFolders = { path.push(ScanFoldersScene()) },
                    modifier =
                        Modifier
                            .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                            .padding(top = Spacing.ExtraLarge)
                            .entranceGraphics(entrance),
                )
            }
        }

        ScannerTopBar(
            title = stringResource(R.string.scan_music),
            listState = listState,
            solid = mastheadGone,
            onBack = { path.popTop() },
            modifier = Modifier.align(Alignment.TopStart),
        )

        AnimatedVisibility(
            visible = entrancePlayed && phase != ScanPhase.Permission,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(tween(durationMillis = 200)),
            exit = slideOutVertically { it } + fadeOut(tween(durationMillis = 160)),
        ) {
            ScannerActionBar(
                phase = phase,
                onStartScan = { viewModel.startFullScan() },
                onCancelScan = { viewModel.cancelScan() },
                onResetState = { viewModel.resetState() },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 入场编排 / 刊头收缩（与 IgnoredSongsScene 同一套手感）
// ──────────────────────────────────────────────────────────────────────────

/** 大标题收缩进度：0f=完全展开 1f=完全收进顶栏 */
private fun Density.mastheadCollapse(listState: LazyListState): Float =
    if (listState.firstVisibleItemIndex > 0) {
        1f
    } else {
        (listState.firstVisibleItemScrollOffset / MastheadCollapseDistance.toPx()).coerceIn(0f, 1f)
    }

/** 固定顶栏：背景与标题透明度跟随刊头收缩进度，收起后出现分隔线 */
@Composable
private fun ScannerTopBar(
    title: String,
    listState: LazyListState,
    solid: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val backgroundColor = MaterialTheme.colorScheme.background
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(statusBarTop + 56.dp)
                .drawBehind {
                    drawRect(color = backgroundColor.copy(alpha = mastheadCollapse(listState)))
                },
    ) {
        if (solid) {
            HorizontalDivider(
                modifier = Modifier.align(Alignment.BottomStart),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.14f),
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = statusBarTop)
                    .padding(horizontal = Spacing.Small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .weight(1f)
                        .graphicsLayer { alpha = mastheadCollapse(listState) },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 刊头
// ──────────────────────────────────────────────────────────────────────────

/** 刊头：大标题 + 跟随页面状态切换的副标题 */
@Composable
private fun ScannerMasthead(
    phase: ScanPhase,
    errorMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.scan_music),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        AnimatedContent(
            targetState = phase,
            transitionSpec = {
                (fadeIn(tween(durationMillis = 240)) + slideInVertically(tween(durationMillis = 240)) { it / 6 })
                    .togetherWith(fadeOut(tween(durationMillis = 160)))
            },
            label = "scanner_masthead_subtitle",
        ) { p ->
            Text(
                text =
                    when (p) {
                        ScanPhase.Permission -> stringResource(R.string.scanner_hero_subtitle)
                        ScanPhase.Idle -> stringResource(R.string.scanner_idle_subtitle)
                        ScanPhase.Scanning -> stringResource(R.string.scanner_running_title)
                        ScanPhase.Success -> stringResource(R.string.scanner_complete_title)
                        ScanPhase.Error -> errorMessage.ifBlank { stringResource(R.string.scanner_error_title) }
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 状态面板
// ──────────────────────────────────────────────────────────────────────────

/** 各状态最后一份有效数据（普通字段：退场中的旧面板读到的是定格值，不跟随新状态跳变） */
private class ScanStatusMemory {
    var progress: ScanProgress = ScanProgress(0, 0, "")
    var result: ScanResult = ScanResult(0, 0, 0, 0)
    var error: String = ""
}

@Composable
private fun ScannerStatusPanel(
    phase: ScanPhase,
    progress: ScanProgress,
    result: ScanResult,
    errorMessage: String,
    libraryCount: Int,
    lastScanAt: Long?,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            (fadeIn(tween(durationMillis = 240)) + slideInVertically(tween(durationMillis = 240)) { it / 8 })
                .togetherWith(fadeOut(tween(durationMillis = 160)))
        },
        label = "scanner_status_panel",
        modifier = modifier.fillMaxWidth(),
    ) { p ->
        when (p) {
            ScanPhase.Permission ->
                PermissionCard(
                    shouldShowRationale = shouldShowRationale,
                    onRequestPermission = onRequestPermission,
                    onOpenSettings = onOpenSettings,
                )

            ScanPhase.Idle ->
                LibrarySnapshotCard(
                    libraryCount = libraryCount,
                    lastScanAt = lastScanAt,
                )

            ScanPhase.Scanning ->
                ScanProgressCard(
                    progress = progress,
                )

            ScanPhase.Success ->
                ScanResultCard(
                    result = result,
                )

            ScanPhase.Error ->
                ScanErrorCard(
                    message = errorMessage,
                )
        }
    }
}

/** 状态卡通用容器 */
@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f),
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(color)
                .padding(Spacing.Large),
    ) {
        content()
    }
}

/** 46dp 圆角图标底座（与设置页 SettingsRow 同规格） */
@Composable
private fun StatusIconBadge(
    icon: ImageVector,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    Box(
        modifier =
            Modifier
                .size(46.dp)
                .clip(Shapes.LargeCornerBasedShape)
                .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 待机态：曲库概况（歌曲数 + 上次扫描时间），新用户显示引导文案 */
@Composable
private fun LibrarySnapshotCard(
    libraryCount: Int,
    lastScanAt: Long?,
) {
    StatusCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            StatusIconBadge(icon = Icons.Default.LibraryMusic)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.scanner_library_count, libraryCount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text =
                        if (lastScanAt == null) {
                            stringResource(R.string.scanner_never_scanned)
                        } else {
                            stringResource(
                                R.string.scanner_last_scan_at,
                                DateUtils.getRelativeTimeSpanString(lastScanAt).toString(),
                            )
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 权限态：解释用途 + 授权/去设置 */
@Composable
private fun PermissionCard(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    StatusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                StatusIconBadge(icon = Icons.Default.Lock)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.scanner_permission_title),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text =
                            if (shouldShowRationale) {
                                stringResource(R.string.scanner_permission_denied_msg)
                            } else {
                                stringResource(R.string.scanner_hero_subtitle)
                            },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (shouldShowRationale) {
                ScannerActionPill(
                    text = stringResource(R.string.scanner_go_to_settings),
                    icon = null,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                ScannerActionPill(
                    text = stringResource(R.string.scanner_grant_permission),
                    icon = null,
                    container = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 扫描态：百分比 + 进度条 + 当前文件 */
@Composable
private fun ScanProgressCard(progress: ScanProgress) {
    val determinate = progress.total > 0
    val fraction =
        if (determinate) {
            (progress.current.toFloat() / progress.total).coerceIn(0f, 1f)
        } else {
            0f
        }
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 360),
        label = "scan_progress_fraction",
    )

    StatusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = if (determinate) "${(fraction * 100).toInt()}%" else "…",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text =
                        if (determinate) {
                            stringResource(
                                R.string.scanner_running_progress_format,
                                progress.current,
                                progress.total,
                            )
                        } else {
                            stringResource(R.string.scanner_preparing)
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }

            ScanProgressBar(
                determinate = determinate,
                progressProvider = { animatedFraction },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
            ) {
                Text(
                    text = stringResource(R.string.scanner_current_file_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = progress.currentFile.ifBlank { stringResource(R.string.scanner_waiting_file) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 6dp 圆角进度条：确定态画填充，准备态画流动光带；动画值全部在 Draw 阶段读取 */
@Composable
private fun ScanProgressBar(
    determinate: Boolean,
    progressProvider: () -> Float,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val fillColor = MaterialTheme.colorScheme.primary
    val barModifier =
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
    if (determinate) {
        Canvas(barModifier) {
            drawRect(color = trackColor)
            val width = size.width * progressProvider().coerceIn(0f, 1f)
            if (width > 0f) {
                drawRect(color = fillColor, size = size.copy(width = width))
            }
        }
    } else {
        // 只有准备态才启动无限动画，确定态不空转
        val infinite = rememberInfiniteTransition(label = "scan_progress_bar")
        val sweep by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1200, easing = LinearEasing),
                ),
            label = "scan_progress_sweep",
        )
        Canvas(barModifier) {
            drawRect(color = trackColor)
            // 30% 宽的光带从左到右循环流动
            val bandWidth = size.width * 0.3f
            val x = (size.width + bandWidth) * sweep - bandWidth
            drawRect(
                color = fillColor.copy(alpha = 0.85f),
                topLeft = Offset(x, 0f),
                size = size.copy(width = bandWidth),
            )
        }
    }
}

/** 完成态：结果统计四宫格 */
@Composable
private fun ScanResultCard(result: ScanResult) {
    StatusCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            ) {
                StatusIconBadge(
                    icon = Icons.Rounded.Check,
                    tint = MaterialTheme.colorScheme.primary,
                    container = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                )
                Text(
                    text = stringResource(R.string.scanner_complete_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                ScanResultTile(
                    label = stringResource(R.string.total_scanned),
                    value = result.totalScanned,
                    modifier = Modifier.weight(1f),
                )
                ScanResultTile(
                    label = stringResource(R.string.new_added),
                    value = result.newAdded,
                    modifier = Modifier.weight(1f),
                )
                ScanResultTile(
                    label = stringResource(R.string.updated),
                    value = result.updated,
                    modifier = Modifier.weight(1f),
                )
                ScanResultTile(
                    label = stringResource(R.string.removed),
                    value = result.removed,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScanResultTile(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(Shapes.LargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.72f))
                .padding(vertical = Spacing.Medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 出错态 */
@Composable
private fun ScanErrorCard(message: String) {
    StatusCard(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            StatusIconBadge(
                icon = Icons.Default.PriorityHigh,
                tint = MaterialTheme.colorScheme.error,
                container = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.scanner_error_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = message.ifBlank { stringResource(R.string.scanner_error_default) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 扫描配置区
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerSetupSection(
    rules: ScanRules,
    extraFolderCount: Int,
    ignoreFolderCount: Int,
    enabled: Boolean,
    onOpenRules: () -> Unit,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(durationMillis = 200),
        label = "scanner_setup_alpha",
    )
    Column(
        modifier = modifier.graphicsLayer { alpha = contentAlpha },
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Text(
            text = stringResource(R.string.scanner_setup_group),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(Shapes.ExtraLargeCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f)),
        ) {
            ScannerSetupRow(
                icon = Icons.Default.Tune,
                title = stringResource(R.string.scanner_rules_row_title),
                subtitle = rulesSummary(rules),
                enabled = enabled,
                onClick = onOpenRules,
            )
            HorizontalDivider(
                modifier = Modifier.padding(start = 78.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
            ScannerSetupRow(
                icon = Icons.Default.Folder,
                title = stringResource(R.string.scan_folders_dialog_title),
                subtitle =
                    stringResource(
                        R.string.scanner_folders_row_subtitle,
                        extraFolderCount,
                        ignoreFolderCount,
                    ),
                enabled = enabled,
                onClick = onOpenFolders,
            )
        }
    }
}

/** 规则摘要：时长 · 体积 · 格式 */
@Composable
private fun rulesSummary(rules: ScanRules): String {
    val durationPart =
        if (rules.minDurationSec <= 0) {
            stringResource(R.string.scanner_rules_summary_duration_any)
        } else {
            stringResource(R.string.scanner_rules_summary_duration, rules.minDurationSec)
        }
    val sizePart =
        if (rules.minFileSizeKb <= 0) {
            stringResource(R.string.scanner_rules_summary_size_any)
        } else {
            stringResource(R.string.scanner_rules_summary_size, formatSizeKb(rules.minFileSizeKb))
        }
    val formatsPart =
        if (rules.allFormatsEnabled) {
            stringResource(R.string.scanner_rules_summary_formats_all)
        } else {
            stringResource(R.string.scanner_rules_summary_formats, rules.enabledFormatKeys.size)
        }
    return "$durationPart · $sizePart · $formatsPart"
}

@Composable
private fun formatSizeKb(kb: Int): String =
    if (kb >= 1024) {
        stringResource(R.string.scanner_size_mb_format, kb / 1024)
    } else {
        stringResource(R.string.scanner_size_kb_format, kb)
    }

@Composable
private fun ScannerSetupRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickHighlight(enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
    ) {
        StatusIconBadge(icon = icon)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AnimatedContent(
                targetState = subtitle,
                transitionSpec = { materialSharedAxisZ(forward = true) },
                label = "scanner_setup_row_subtitle",
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 底部操作区
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannerActionBar(
    phase: ScanPhase,
    onStartScan: () -> Unit,
    onCancelScan: () -> Unit,
    onResetState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 6.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding, vertical = Spacing.Medium),
        ) {
            AnimatedContent(
                targetState = phase,
                transitionSpec = { materialSharedAxisZ(forward = true) },
                label = "scanner_action_bar",
            ) { p ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                ) {
                    when (p) {
                        ScanPhase.Permission -> Unit

                        ScanPhase.Idle -> {
                            ScannerActionPill(
                                text = stringResource(R.string.scanner_start),
                                icon = Icons.Default.GraphicEq,
                                container = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = onStartScan,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        ScanPhase.Scanning -> {
                            ScannerActionPill(
                                text = stringResource(R.string.stop_scanner),
                                icon = null,
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.error,
                                onClick = onCancelScan,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        ScanPhase.Success -> {
                            ScannerActionPill(
                                text = stringResource(R.string.scanner_rescan),
                                icon = null,
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = {
                                    onResetState()
                                    onStartScan()
                                },
                                modifier = Modifier.weight(1f),
                            )
                            ScannerActionPill(
                                text = stringResource(R.string.scanner_done),
                                icon = null,
                                container = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = onResetState,
                                modifier = Modifier.weight(1f),
                            )
                        }

                        ScanPhase.Error -> {
                            ScannerActionPill(
                                text = stringResource(R.string.scanner_later),
                                icon = null,
                                container = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                onClick = onResetState,
                                modifier = Modifier.weight(1f),
                            )
                            ScannerActionPill(
                                text = stringResource(R.string.scanner_retry),
                                icon = null,
                                container = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                onClick = {
                                    onResetState()
                                    onStartScan()
                                },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 52dp 高主操作胶囊（clip + background + clickHighlight，与 IgnoredSongsScene 底栏同款） */
@Composable
private fun ScannerActionPill(
    text: String,
    icon: ImageVector?,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val contentAlpha = if (enabled) 1f else 0.42f
    Row(
        modifier =
            modifier
                .height(52.dp)
                .clip(Shapes.MediumCornerBasedShape)
                .background(if (enabled) container else container.copy(alpha = 0.45f))
                .clickHighlight(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(Spacing.Small))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor.copy(alpha = contentAlpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ──────────────────────────────────────────────────────────────────────────
// 供 ScanRulesScene 复用的预设常量
// ──────────────────────────────────────────────────────────────────────────

internal object ScanRulePresets {
    val durationSecOptions: List<Int> = DURATION_PRESETS_SEC
    val sizeKbOptions: List<Int> = SIZE_PRESETS_KB
}
