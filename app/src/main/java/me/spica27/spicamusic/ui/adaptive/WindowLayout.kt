package me.spica27.spicamusic.ui.adaptive

import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import me.spica27.spicamusic.ui.adaptive.WidthClass.Compact
import me.spica27.spicamusic.ui.adaptive.WidthClass.Expanded
import me.spica27.spicamusic.ui.adaptive.WidthClass.ExtraLarge
import me.spica27.spicamusic.ui.adaptive.WidthClass.Large
import me.spica27.spicamusic.ui.adaptive.WidthClass.Medium

/** 窗口宽度档位，按从窄到宽排列；依据当前窗口尺寸划分，与设备是否为平板无关。 */
enum class WidthClass { Compact, Medium, Expanded, Large, ExtraLarge }

/**
 * 当前应用窗口的尺寸、折叠姿态，以及由此计算的布局策略。
 *
 * 由应用外壳统一提供。页面自身的可用空间仍需按内容容器测量，尤其是在分栏布局中。
 *
 * @param width 窗口逻辑宽度，单位已是 dp，可以直接与 dp 断点比较。
 * @param height 窗口逻辑高度，单位为 dp。
 * @param posture 折叠姿态与铰链信息，用于判断半折叠布局及避让铰链。
 */
@Immutable
class WindowLayout(
    val width: Dp,
    val height: Dp,
    val posture: Posture,
) {
    /** 五档宽度的下界依次为 0 / 600 / 840 / 1200 / 1600dp，从最大断点开始匹配。 */
    val widthClass =
        if (width >= WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND.dp) {
            WidthClass.ExtraLarge
        } else if (width >= WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND.dp) {
            WidthClass.Large
        } else if (width >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp) {
            WidthClass.Expanded
        } else if (width >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp) {
            WidthClass.Medium
        } else {
            WidthClass.Compact
        }

    /** 高度不足 480dp 的短窗口，例如横屏手机、上下分屏，需要优先保留内容的垂直空间。 */
    val isShort = height < WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND.dp

    /** 窄且不短的窗口用底部导航，Large 及以上用常驻侧栏，其余使用收起的宽导航轨。 */
    val navigation: NavigationSuiteType =
        if (widthClass == WidthClass.Compact && !isShort) {
            NavigationSuiteType.NavigationBar // 底部导航栏
        } else if (width >= WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND.dp) {
            NavigationSuiteType.NavigationDrawer // 常驻侧边栏
        } else {
            NavigationSuiteType.WideNavigationRailCollapsed // 收起的宽导航轨
        }

    /** 外壳是否采用播放 Dock；其余窗口使用迷你播放条，实际显隐由外壳结合播放状态决定。 */
    val dock = (width >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp) || isShort

    /** 列表与详情可采用双窗格的窗口条件；短窗口仍保留单窗格。 */
    val twoPane = (width >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp) && !isShort

    /** 播放页可采用左右双栏；短且横向的窗口即使不足 Expanded 宽度也可启用。 */
    val twoColumnPlayer =
        (width >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp) || (isShort && width > height)

    /** 半折叠姿态下采用上下布局，具体分界需结合姿态中的铰链位置确定。 */
    val tableTopPlayer = posture.isTabletop

    /** 选择类面板可改为居中对话框的窗口条件；确认和文本输入对话框始终居中。 */
    val centeredDialog = width >= WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND.dp

    /** 上下文菜单可优先从触发位置锚定弹出，调用方仍需确认存在可用锚点。 */
    val anchoredMenu = width >= WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND.dp

    /** 窗口级的页面边距建议值；分栏页面仍需结合自身容器宽度决定最终布局。 */
    val pageMargin =
        when (widthClass) {
            Compact -> 16.dp
            Medium -> 24.dp
            Expanded -> 24.dp
            Large -> 32.dp
            ExtraLarge -> 32.dp
        }
}

/** 应用外壳提供的窗口布局环境；独立 Preview 中也需要显式提供。 */
val LocalWindowLayout =
    compositionLocalOf<WindowLayout> {
        error("没有定义，No WindowLayout!!!")
    }

/** 读取当前窗口的 dp 尺寸和折叠姿态，供应用外壳创建布局环境。 */
@Composable
fun rememberWindowLayout(): WindowLayout {
    val info = currentWindowAdaptiveInfoV2()
    // containerDpSize 已完成像素到 dp 的转换，无需再用 LocalDensity 转换一次。
    val size = LocalWindowInfo.current.containerDpSize
    return remember(
        size,
        info.windowPosture,
    ) {
        WindowLayout(
            size.width,
            size.height,
            info.windowPosture,
        )
    }
}
