package me.spica27.spicamusic.ui.scan

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.feature.library.domain.ScanFormats
import me.spica27.spicamusic.ui.settings.MediaLibrarySourceViewModel
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.clickHighlight
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 扫描规则配置对话框（与 ScanFoldersScene 同一套呈现），
 * 配置最短时长 / 最小文件体积 / 收录格式，改动即时持久化，下次扫描生效。
 */
class ScanRulesScene : DialogScene() {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun DialogContent() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current
        val viewModel: MediaLibrarySourceViewModel = koinActivityViewModel()
        val rules by viewModel.scanRules.collectAsStateWithLifecycle()

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Large),
            ) {
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
                            text = stringResource(R.string.scanner_rules_row_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.scanner_rules_sheet_subtitle),
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

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 560.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = Spacing.Large),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large),
                ) {
                    ScanRuleSection(
                        title = stringResource(R.string.scanner_rule_min_duration_title),
                        description = stringResource(R.string.scanner_rule_min_duration_desc),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                        ) {
                            ScanRulePresets.durationSecOptions.forEach { seconds ->
                                ScanRuleChip(
                                    label =
                                        if (seconds <= 0) {
                                            stringResource(R.string.scanner_rule_any)
                                        } else {
                                            stringResource(R.string.scanner_rule_seconds_format, seconds)
                                        },
                                    selected = rules.minDurationSec == seconds,
                                    onClick = { viewModel.setMinDurationSec(seconds) },
                                )
                            }
                        }
                    }

                    ScanRuleSection(
                        title = stringResource(R.string.scanner_rule_min_size_title),
                        description = stringResource(R.string.scanner_rule_min_size_desc),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                        ) {
                            ScanRulePresets.sizeKbOptions.forEach { kb ->
                                ScanRuleChip(
                                    label =
                                        when {
                                            kb <= 0 -> stringResource(R.string.scanner_rule_any)
                                            kb >= 1024 -> stringResource(R.string.scanner_size_mb_format, kb / 1024)
                                            else -> stringResource(R.string.scanner_size_kb_format, kb)
                                        },
                                    selected = rules.minFileSizeKb == kb,
                                    onClick = { viewModel.setMinFileSizeKb(kb) },
                                )
                            }
                        }
                    }

                    ScanRuleSection(
                        title = stringResource(R.string.scanner_rule_formats_title),
                        description = stringResource(R.string.scanner_rule_formats_desc),
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
                        ) {
                            ScanFormats.all.forEach { format ->
                                ScanRuleChip(
                                    label = format.label,
                                    selected = format.key in rules.enabledFormatKeys,
                                    onClick = { viewModel.toggleFormat(format.key) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanRuleSection(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
        content()
    }
}

/** 单选/多选胶囊：选中态填充主色，未选中态容器色 */
@Composable
private fun ScanRuleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        label = "scan_rule_chip_container",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "scan_rule_chip_content",
    )
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(containerColor)
                .clickHighlight(onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}
