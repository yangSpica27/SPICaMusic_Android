package me.spica27.spicamusic.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 避让距离
 * 它负责描述迷你播放条、Dock 等外壳组件覆盖内容时，页面需要避让多少空间
 */
@Immutable
data class ShellInsets(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
)

val LocalShellInsets =
    compositionLocalOf {
        ShellInsets()
    }
