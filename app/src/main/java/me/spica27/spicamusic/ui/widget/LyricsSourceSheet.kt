package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.LyricSource
import me.spica27.spicamusic.common.entity.LyricSourceType

/**
 * 歌词来源选择面板（分区列表）
 *
 * 按来源类型分区展示：内嵌歌词（0/1 条）、本地文件（导入入口 + 已导入项）、在线（N 条候选，懒加载）。
 * 点击任一候选即选中并关闭；本地区点击"从文件选择"触发 SAF picker。
 *
 * @param embedded 内嵌歌词候选，null 表示无
 * @param local 已导入的本地歌词，null 表示尚未导入
 * @param online 在线候选列表
 * @param onlineLoading 在线候选是否正在加载
 * @param currentSourceType 当前正在使用的来源类型（用于"使用中"标记）
 * @param currentRawText 当前正在使用的来源原文（用于精确匹配在线候选中的"使用中"项）
 * @param onSelect 选中某来源回调
 * @param onImportLocalFile 请求导入本地文件回调（由上层拉起 SAF picker，回传 uri 字符串）
 * @param onDismiss 关闭面板回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSourceSheet(
    embedded: LyricSource.Embedded?,
    local: LyricSource.LocalFile?,
    online: List<LyricSource.Online>,
    onlineLoading: Boolean,
    currentSourceType: LyricSourceType,
    currentRawText: String?,
    currentTime: Long,
    onSelect: (LyricSource) -> Unit,
    onImportLocalFile: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // SAF 文档选择器：限文本类，回传 uri 字符串给上层快照入库
    val picker =
        androidx.activity.compose.rememberLauncherForActivityResult(
            contract =
                androidx.activity.result.contract.ActivityResultContracts
                    .OpenDocument(),
        ) { uri ->
            if (uri != null) {
                onImportLocalFile(uri.toString())
                onDismiss()
            }
        }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp, max = 560.dp),
        ) {
            // 标题栏
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.choose_lyrics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(bottom = 24.dp),
            ) {
                // ── 内嵌区 ──
                item { SectionHeader(stringResource(R.string.lyrics_source_embedded)) }
                if (embedded != null) {
                    item {
                        SourceRow(
                            icon = Icons.Rounded.MusicNote,
                            title = stringResource(R.string.lyrics_source_embedded),
                            subtitle = stringResource(R.string.lyrics_source_embedded_subtitle),
                            selected = currentSourceType == LyricSourceType.EMBEDDED,
                            onClick = {
                                onSelect(embedded)
                                onDismiss()
                            },
                        )
                    }
                } else {
                    item { EmptyHint(stringResource(R.string.lyrics_source_none_embedded)) }
                }

                // ── 本地文件区 ──
                item { SectionHeader(stringResource(R.string.lyrics_source_local)) }
                if (local != null) {
                    item {
                        SourceRow(
                            icon = Icons.Rounded.LibraryMusic,
                            title = local.fileName,
                            subtitle = stringResource(R.string.lyrics_source_local),
                            selected = currentSourceType == LyricSourceType.LOCAL_FILE,
                            onClick = {
                                onSelect(local)
                                onDismiss()
                            },
                        )
                    }
                }
                item {
                    SourceRow(
                        icon = Icons.Rounded.Add,
                        title = stringResource(R.string.lyrics_source_local_pick),
                        subtitle = null,
                        selected = false,
                        onClick = {
                            // OpenDocument 需 MIME 数组；.lrc 无标准 MIME，用宽松集合覆盖
                            picker.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
                        },
                    )
                }

                // ── 在线区 ──
                item {
                    SectionHeader(
                        if (online.isEmpty()) {
                            stringResource(R.string.lyrics_source_online)
                        } else {
                            stringResource(R.string.lyrics_source_online_count, online.size)
                        },
                    )
                }
                when {
                    onlineLoading ->
                        item {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                    online.isEmpty() ->
                        item { EmptyHint(stringResource(R.string.lyrics_source_online_empty)) }

                    else ->
                        itemsIndexed(
                            items = online,
                            // 服务端 id 可能重复或为 0，叠加下标保证 LazyColumn key 唯一，避免重复 key 崩溃
                            key = { index, item -> "${item.stableKey}#$index" },
                        ) { _, source ->
                            SourceRow(
                                icon = Icons.Rounded.Language,
                                title = source.title,
                                subtitle = source.subtitle,
                                selected =
                                    currentSourceType == LyricSourceType.ONLINE &&
                                        currentRawText == source.rawLyrics,
                                onClick = {
                                    onSelect(source)
                                    onDismiss()
                                },
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.lyrics_source_current),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
