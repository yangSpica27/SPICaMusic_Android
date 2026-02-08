package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.player.impl.utils.getCoverUri
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 通用歌曲选择器 BottomSheet（分页版本）
 *
 * 独立封装的歌曲多选组件，支持：
 * - 关键字搜索筛选（歌名 / 艺术家），过滤在数据库层面完成
 * - 全选 / 全不选 快捷操作
 * - 确认回调返回选中歌曲 ID 列表
 *
 * @param songsPagingFlow 分页歌曲数据流（由 ViewModel 提供，已排除歌单中已有歌曲）
 * @param songCount 当前筛选结果歌曲总数
 * @param onKeywordChange 搜索关键词变更回调（通知 ViewModel 更新分页查询）
 * @param onSelectAll 全选回调，返回所有符合条件的歌曲 ID
 * @param onDismiss 关闭回调
 * @param onConfirm 确认选择回调，返回选中的歌曲 songId 列表
 * @param title 标题文本
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongPickerSheet(
    songsPagingFlow: Flow<PagingData<Song>>,
    songCount: Int,
    onKeywordChange: (String) -> Unit,
    onSelectAll: suspend () -> List<Long>,
    onDismiss: () -> Unit,
    onConfirm: (List<Long>) -> Unit,
    title: String = "添加歌曲",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    // 搜索关键字（本地 UI 状态）
    var keyword by remember { mutableStateOf("") }

    // 分页数据
    val pagingItems: LazyPagingItems<Song> = songsPagingFlow.collectAsLazyPagingItems()

    // 选中的歌曲 songId 集合
    val selectedIds = remember { mutableStateListOf<Long>() }

    // 当前筛选结果是否全部被选中
    val isAllSelected = songCount > 0 && selectedIds.size >= songCount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MiuixTheme.colorScheme.surface,
        dragHandle = null,
        modifier = Modifier.imePadding(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            // ── 顶部标题栏 ──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = MiuixTheme.colorScheme.onSurface,
                    )
                }
            }

            // ── 搜索输入框 ──
            TextField(
                value = keyword,
                onValueChange = {
                    keyword = it
                    onKeywordChange(it)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                label = "搜索歌名或艺术家",
                useLabelAsPlaceholder = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                        modifier =
                            Modifier
                                .padding(
                                    vertical = 10.dp,
                                    horizontal = 14.dp,
                                ).size(20.dp),
                    )
                },
                cornerRadius = 4.dp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── 快捷操作栏 ──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 已选数量统计
                Text(
                    text =
                        if (selectedIds.isEmpty()) {
                            "共 $songCount 首可选"
                        } else {
                            "已选 ${selectedIds.size} 首"
                        },
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.7f),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 全选 / 取消全选
                    TextButton(
                        text = if (isAllSelected) "取消全选" else "全选",
                        cornerRadius = 4.dp,
                        insideMargin =
                            PaddingValues(
                                horizontal = 12.dp,
                                vertical = 4.dp,
                            ),
                        onClick = {
                            if (isAllSelected) {
                                selectedIds.clear()
                            } else {
                                coroutineScope.launch {
                                    val allIds = onSelectAll()
                                    allIds.forEach { id ->
                                        if (id !in selectedIds) {
                                            selectedIds.add(id)
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── 歌曲列表 ──
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    count = pagingItems.itemCount,
                    key = pagingItems.itemKey { it.songId ?: it.mediaStoreId },
                ) { index ->
                    val song = pagingItems[index] ?: return@items
                    val isSelected = song.songId in selectedIds
                    PickerSongItem(
                        song = song,
                        isSelected = isSelected,
                        onClick = {
                            song.songId?.let { id ->
                                if (isSelected) {
                                    selectedIds.remove(id)
                                } else {
                                    selectedIds.add(id)
                                }
                            }
                        },
                    )
                }

                // 底部留白
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── 底部确认栏 ──
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    insideMargin =
                        PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    text = "取消",
                    onClick = onDismiss,
                    cornerRadius = 4.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    insideMargin =
                        PaddingValues(
                            horizontal = 12.dp,
                            vertical = 4.dp,
                        ),
                    onClick = {
                        onConfirm(selectedIds.toList())
                    },
                    enabled = selectedIds.isNotEmpty(),
                    cornerRadius = 4.dp,
                ) {
                    Text(
                        text =
                            if (selectedIds.isEmpty()) {
                                "确认添加"
                            } else {
                                "确认添加 (${selectedIds.size})"
                            },
                    )
                }
            }
        }
    }
}

/**
 * 歌曲选择列表项
 */
@Composable
private fun PickerSongItem(
    song: Song,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        targetValue =
            if (isSelected) {
                MiuixTheme.colorScheme.tertiaryContainer
            } else {
                MiuixTheme.colorScheme.surface
            },
        label = "picker_item_bg",
    )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(ContinuousRoundedRectangle(12.dp))
                .clickable(onClick = onClick),
        colors =
            CardDefaults.defaultColors(
                color = bgColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 封面
            Box(
                modifier =
                    Modifier
                        .size(48.dp)
                        .clip(ContinuousRoundedRectangle(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                AudioCover(
                    uri = song.getCoverUri(),
                    modifier = Modifier.fillMaxSize(),
                    placeHolder = {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(ContinuousRoundedRectangle(8.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                modifier =
                                    Modifier
                                        .size(20.dp)
                                        .align(Alignment.Center),
                            )
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 歌曲信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.displayName,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = song.artist,
                    style = MiuixTheme.textStyles.body2,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 选中状态
            Icon(
                imageVector =
                    if (isSelected) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.RadioButtonUnchecked
                    },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint =
                    if (isSelected) {
                        MiuixTheme.colorScheme.primary
                    } else {
                        MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f)
                    },
            )
        }
    }
}
