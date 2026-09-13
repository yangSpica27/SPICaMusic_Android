package me.spica27.navkit.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

/**
 * 覆盖层场景基类，为 DialogScene 和 PopupMenuScene 提供统一的动画生命周期管理。
 * ## 动画模型
 * - [enterProgress]: 0f = 完全不可见，1f = 完全呈现
 * - 进场：子类定义的动画规格（通常是弹簧动画）
 * - 退场：子类定义的动画规格（通常是补间动画）
 *
 * ## 子类实现
 * 子类需要：
 * 1. 覆写 [enterAnimationSpec] 和 [exitAnimationSpec]
 * 2. 实现 [Content] 提供具体的 UI 内容
 * 3. 在 Content 中读取 [enterProgress] 驱动动画效果
 *
 * @see DialogScene
 * @see me.spica27.navkit.popup.PopupMenuScene
 */
abstract class OverlayScene : Scene() {

    /** 进场进度：0f = 完全不可见，1f = 完全呈现 */
    val enterProgress = Animatable(0f)

    /** 进场动画规格，由子类覆写以定义具体的动画效果 */
    protected abstract val enterAnimationSpec: AnimationSpec<Float>

    /** 退场动画规格，由子类覆写以定义具体的动画效果 */
    protected abstract val exitAnimationSpec: AnimationSpec<Float>

    private val _placed = MutableStateFlow(false)

    /** 场景是否已完成首次布局 */
    val placed: StateFlow<Boolean> = _placed

    private val _enterAnimEnd = MutableStateFlow(false)

    /** 进场动画是否已完成（enterProgress 达到 1f） */
    val enterAnimEnd: StateFlow<Boolean> = _enterAnimEnd

    /**
     * 由 [me.spica27.navkit.stack.NavigationStack] 在场景首次通过
     * onGloballyPositioned 完成布局后调用，触发 [waitAppear] 解除阻塞。
     */
    fun notifyPlaced() {
        _placed.value = true
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scene 生命周期钩子
    // ──────────────────────────────────────────────────────────────────────

    /** push 开始：将进度 snap 到 0f，避免残留值影响动画 */
    override suspend fun onPush() {
        enterProgress.snapTo(0f)
    }

    /** 等待 NavigationStack 通知场景首帧已布局完成 */
    override suspend fun waitAppear() {
        _placed.first { it }
    }

    /** 进场动画：从 0f 动画到 1f */
    override suspend fun onAppear() {
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = enterAnimationSpec,
        )
        _enterAnimEnd.value = true
    }

    /** 退场动画：从当前进度动画到 0f（Animatable 可中断重定向） */
    override suspend fun onDisappear() {
        enterProgress.animateTo(
            targetValue = 0f,
            animationSpec = exitAnimationSpec,
        )
    }

    /** pop 后重置 placed 状态，供场景实例复用 */
    override suspend fun onPop() {
        _placed.value = false
        _enterAnimEnd.value = false
    }
}
