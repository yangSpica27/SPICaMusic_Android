package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mocharealm.gaze.capsule.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * 歌词页浮动工具栏
 *
 * 提供歌词偏移量调节和切换歌词功能
 *
 * @param offsetMs 当前歌词偏移量（毫秒）
 * @param onOffsetChange 偏移量变化回调
 * @param onSwitchLyrics 切换歌词回调
 * @param currentLyricIndex 当前歌词源索引（从0开始）
 * @param totalLyricSources 歌词源总数
 * @param modifier Modifier
 */
@Composable
fun FloatingLyricsToolbar(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    onSwitchLyrics: () -> Unit,
    currentLyricIndex: Int,
    totalLyricSources: Int,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 展开的工具栏内容
        AnimatedVisibility(
            visible = isExpanded,
            enter =
                fadeIn(spring(stiffness = Spring.StiffnessMedium)) +
                    expandHorizontally(
                        spring(stiffness = Spring.StiffnessMedium),
                        expandFrom = Alignment.End,
                    ),
            exit =
                fadeOut(spring(stiffness = Spring.StiffnessMedium)) +
                    shrinkHorizontally(
                        spring(stiffness = Spring.StiffnessMedium),
                        shrinkTowards = Alignment.End,
                    ),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 偏移量调节栏
                OffsetAdjustBar(
                    offsetMs = offsetMs,
                    onOffsetChange = onOffsetChange,
                )

                // 切换歌词按钮
                if (totalLyricSources > 1) {
                    SwitchLyricsButton(
                        currentIndex = currentLyricIndex,
                        total = totalLyricSources,
                        onClick = onSwitchLyrics,
                    )
                }
            }
        }

        // 主按钮（展开/收起）
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    ).clickable { isExpanded = !isExpanded },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Rounded.Close else Icons.Rounded.Tune,
                contentDescription = if (isExpanded) "收起" else "歌词工具",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * 偏移量调节栏
 */
@Composable
private fun OffsetAdjustBar(
    offsetMs: Long,
    onOffsetChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(ContinuousRoundedRectangle(22.dp))
                .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // -0.5s 按钮
        ToolbarIconButton(
            onClick = { onOffsetChange(offsetMs - 500) },
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "提前 0.5 秒",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }

        // 偏移量显示
        val offsetText = formatOffset(offsetMs)
        Text(
            text = offsetText,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            modifier =
                Modifier
                    .clip(ContinuousRoundedRectangle(14.dp))
                    .clickable { onOffsetChange(0) } // 点击重置
                    .padding(horizontal = 8.dp, vertical = 4.dp),
        )

        // +0.5s 按钮
        ToolbarIconButton(
            onClick = { onOffsetChange(offsetMs + 500) },
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "延迟 0.5 秒",
                tint = MiuixTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/**
 * 切换歌词按钮
 */
@Composable
private fun SwitchLyricsButton(
    currentIndex: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(ContinuousRoundedRectangle(22.dp))
                .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.15f))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.SwapHoriz,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = "切换歌词 ${currentIndex + 1}/$total",
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
        )
    }
}

/**
 * 工具栏小按钮
 */
@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 格式化偏移量显示
 */
private fun formatOffset(offsetMs: Long): String {
    val seconds = offsetMs / 1000f
    return when {
        offsetMs == 0L -> "0.0s"
        offsetMs > 0 -> String.format(Locale.CHINESE, "+%.1fs", seconds)
        else -> String.format(Locale.CHINESE, "%.1fs", seconds)
    }
}
