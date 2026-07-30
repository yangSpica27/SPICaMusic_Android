package me.spica27.navkit.popup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize

/**
 * 弹出菜单锚点的状态容器，连接"前一个页面里的锚点组件"与 [PopupMenuScene]。
 *
 * ## 工作原理
 * 1. 锚点组件附加 [Modifier.popupMenuAnchor]，静止时持续把自身窗口矩形写入 [anchorRect]
 * 2. push [PopupMenuScene] 时场景把自己写入 [owner]，矩形随即**冻结**
 *    （菜单开合动画期间锚点坐标不再刷新，参照
 *    [GeometryTransition][me.spica27.navkit.geometry.GeometryTransition] 的源矩形冻结策略）
 * 3. 场景的 morph 容器完成首帧布局后置起 [sourceHidden]，锚点组件隐藏（alpha = 0），
 *    由场景内钉在同一位置的"幽灵"复制品接管显示——先上屏再隐藏，交接零闪烁
 * 4. 场景 pop 完成后 [owner] 置空、[sourceHidden] 复位，锚点组件恢复显示、矩形恢复刷新
 *
 * 状态实例应创建在比锚点组件更稳定的作用域（如页面级 remember），
 * 避免锚点在 Lazy 容器中被回收时状态一并丢失。
 */
@Stable
class PopupMenuAnchorState {

    /** 锚点组件在窗口坐标系中的未裁剪矩形；菜单打开期间冻结 */
    val anchorRect: MutableState<Rect> = mutableStateOf(Rect.Zero)

    /** 当前占有此锚点的菜单场景；非 null 表示菜单已打开（矩形冻结） */
    val owner: MutableState<PopupMenuScene?> = mutableStateOf(null)

    /** 场景的幽灵复制品已上屏、源组件应当隐藏；由 [PopupMenuScene] 维护 */
    val sourceHidden: MutableState<Boolean> = mutableStateOf(false)

    /** 菜单是否已打开 */
    val isOpen: Boolean get() = owner.value != null
}

/** 创建并记住一个 [PopupMenuAnchorState]。 */
@Composable
fun rememberPopupMenuAnchorState(): PopupMenuAnchorState = remember { PopupMenuAnchorState() }

/**
 * 将此可组合元素标记为 [PopupMenuScene] 的锚点。
 *
 * - 布局完成时把窗口矩形写入 [PopupMenuAnchorState.anchorRect]（菜单打开期间冻结）
 * - 场景的幽灵复制品上屏后隐藏自身（alpha = 0），显示权交给钉在同一位置的幽灵，
 *   使"组件过渡成菜单 / 菜单收回成组件"在视觉上无缝衔接
 *
 * 应放在尺寸链的最外层（clip/background/padding 之前），确保记录的矩形
 * 覆盖组件的完整视觉边界。
 */
fun Modifier.popupMenuAnchor(state: PopupMenuAnchorState): Modifier =
    this
        .onGloballyPositioned { coords ->
            if (!state.isOpen) {
                // 未裁剪矩形：与 GeometryModifiers 一致，避免 Lazy 视口裁剪污染起点
                state.anchorRect.value = Rect(coords.positionInWindow(), coords.size.toSize())
            }
        }
        .graphicsLayer { alpha = if (state.sourceHidden.value) 0f else 1f }
