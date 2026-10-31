package me.spica27.spicamusic.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.DpSize

// 当前内容窗格的尺寸和布局状态
@Immutable
data class PaneInfo(
    val contentSize: DpSize? = null, // 内容区域的尺寸，允许为null表示未测量完成
    val isDetailInTwoPane: Boolean = false, // 是不是处于双窗格中的详情窗口
)

val LocalPaneInfo =
    compositionLocalOf {
        PaneInfo()
    }
