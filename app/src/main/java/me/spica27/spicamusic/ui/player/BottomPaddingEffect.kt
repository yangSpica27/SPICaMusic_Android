package me.spica27.spicamusic.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * 重置底部 padding 的副作用
 * 用于没有 NavigationBar 的页面，确保播放器紧贴屏幕底部
 *
 * 使用方式：
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     ResetBottomPadding()
 *     // ... 其他内容
 * }
 * ```
 */
@Composable
fun ResetBottomPadding() {
    val bottomPaddingState = LocalBottomPaddingState.current
    LaunchedEffect(Unit) {
        bottomPaddingState.floatValue = 0f
    }
}

/**
 * 设置底部 padding 的副作用
 * 用于有固定底部栏的页面
 *
 * @param padding 底部 padding 值（像素）
 */
@Composable
fun SetBottomPadding(padding: Float) {
    val bottomPaddingState = LocalBottomPaddingState.current
    LaunchedEffect(padding) {
        bottomPaddingState.floatValue = padding
    }
}
