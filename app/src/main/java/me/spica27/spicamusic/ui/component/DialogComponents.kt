package me.spica27.spicamusic.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.theme.Shapes

/**
 * 标准对话框容器，统一所有对话框的外观样式。
 *
 * 提供固定的圆角、阴影和色调提升效果，消除重复代码。
 * 用于 TextInputDialogScene、ConfirmationDialogScene 等场景。
 *
 * @param modifier 修饰符
 * @param content 对话框内容
 */
@Composable
fun DialogContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.ExtraLarge1CornerBasedShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        content()
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
