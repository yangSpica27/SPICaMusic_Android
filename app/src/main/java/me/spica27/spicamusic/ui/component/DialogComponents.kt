package me.spica27.spicamusic.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import me.spica27.spicamusic.ui.glass.LiquidGlassVariant
import me.spica27.spicamusic.ui.glass.LocalDialogHazeState
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.glass.liquidGlass
import me.spica27.spicamusic.ui.theme.Shapes

/** 统一的对话框容器；关闭玻璃效果时使用不透明背景。 */
@Composable
fun DialogContainer(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.ExtraLarge1CornerBasedShape,
    enableGlass: Boolean = true,
    content: @Composable () -> Unit,
) {
    val glassEnabled = LocalLiquidGlassConfig.current.enabled
    val hazeState = LocalDialogHazeState.current

    if (enableGlass && glassEnabled && hazeState != null) {
        GlassSurface(
            hazeState = hazeState,
            variant = LiquidGlassVariant.Dialog,
            shape = shape,
            modifier = modifier.fillMaxWidth(),
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}

/** 带宽度约束的弹出菜单容器。 */
@Composable
fun PopupMenuContainer(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.ExtraLarge1CornerBasedShape,
    minWidth: Dp = 220.dp,
    maxWidth: Dp = 300.dp,
    content: @Composable () -> Unit,
) {
    val glassEnabled = LocalLiquidGlassConfig.current.enabled
    val hazeState = LocalDialogHazeState.current
    val sizeModifier = Modifier.widthIn(min = minWidth, max = maxWidth)

    if (glassEnabled && hazeState != null) {
        GlassSurface(
            hazeState = hazeState,
            variant = LiquidGlassVariant.PopupMenu,
            shape = shape,
            modifier = modifier.then(sizeModifier),
            content = content,
        )
    } else {
        Surface(
            modifier = modifier.then(sizeModifier),
            shape = shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
        ) {
            content()
        }
    }
}

@Composable
private fun GlassSurface(
    hazeState: HazeState,
    variant: LiquidGlassVariant,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(
                        hazeState = hazeState,
                        variant = variant,
                        shape = shape,
                        fallbackColor = MaterialTheme.colorScheme.surface,
                    ),
        ) {
            content()
        }
    }
}

/** 带图标、可选副标题和尾部内容的对话框菜单项。 */
@Composable
fun DialogMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    showDivider: Boolean = false,
    destructive: Boolean = false,
) {
    val iconContainerColor =
        if (destructive) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }
    val iconTint =
        if (destructive) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.primary
        }
    val titleColor =
        if (destructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onClick),
            shape = RoundedCornerShape(18.dp),
            color = androidx.compose.ui.graphics.Color.Transparent,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = iconContainerColor,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = titleColor,
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
                if (trailing != null) {
                    trailing()
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

/**
 * 对话框图标容器，显示圆形背景的图标。
 *
 * 用于确认对话框、错误提示等场景，提供一致的视觉样式。
 * 支持常规和警告（destructive）两种样式。
 *
 * @param icon 要显示的图标
 * @param destructive 是否为警告样式（使用错误色）
 * @param modifier 修饰符
 */
@Composable
fun IconContainer(
    icon: ImageVector,
    destructive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(52.dp),
        shape = CircleShape,
        color =
            if (destructive) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (destructive) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                modifier = Modifier.size(26.dp),
            )
        }
    }
}
