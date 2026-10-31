package me.spica27.spicamusic.ui.basic_widget

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.clickHighlight

@Composable
fun ListCell(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier =
            modifier
                .clickHighlight(enabled = onClick != null, onClick = {
                    onClick?.invoke()
                })
                .heightIn(min = if (subtitle == null) 52.dp else 60.dp)
                .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        leadingContent?.invoke()
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        trailingContent?.invoke()
    }
}

/**
 * 分组容器
 */
@Composable
fun GroupContainer(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        contentColor = contentColorFor(containerColor),
        content = {
            Column {
                content()
            }
        },
    )
}

@Preview(name = "听歌统计 · 浅色", widthDp = 393, showBackground = true)
@Preview(
    name = "听歌统计 · 深色",
    widthDp = 393,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GroupContainerPreview() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            GroupContainer(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(Spacing.Large),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ) {
                ListCell(
                    title = "听歌时长",
                    trailingContent = {
                        Text(
                            text = "12 小时 38 分钟",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = Spacing.Large),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ListCell(
                    title = "播放次数",
                    trailingContent = {
                        Text(
                            text = "256 次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = Spacing.Large),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ListCell(
                    title = "听过的歌",
                    trailingContent = {
                        Text(
                            text = "89 首",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

@Composable
fun GroupHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier.padding(
                start = 32.dp,
                end = 32.dp,
                top = 20.dp,
                bottom = 8.dp,
            ),
    )
}

@Composable
fun GroupFooter(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            modifier
                .padding(horizontal = 32.dp)
                .padding(top = 8.dp),
    )
}
