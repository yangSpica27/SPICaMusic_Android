package me.spica27.navkit.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * 遮罩覆盖层组件，用于 Dialog 和 Popup 场景。
 *
 * 提供半透明黑色背景，随动画进度渐显，点击可触发关闭回调。
 * 该实现修复了黑闪问题：直接在 Color.copy(alpha) 中应用透明度，
 * 而不是使用 graphicsLayer { alpha = ... }，避免了帧序依赖问题。
 *
 * @param progress 动画进度，范围 [0f, 1f]
 * @param maxAlpha 最大不透明度，通常为 0.35f ~ 0.5f
 * @param onDismiss 点击遮罩时的回调，通常用于关闭场景
 * @param modifier 修饰符
 */
@Composable
fun ScrimOverlay(
    progress: Float,
    maxAlpha: Float,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = progress * maxAlpha))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                ) {
                    onDismiss()
                },
    )
}
