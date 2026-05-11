package me.spica27.navkit.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
/**
 * 支持进退场动画的堆栈式场景。
 *
 * ## 动画模型
 * - [enterProgress]：进场进度，0f = 完全不可见（刚 push），1f = 完全呈现（Appeared）
 * - 进场弹簧刚度 300（偏硬，快速入场）；退场沿用相同弹簧
 * - [aheadEnterProgress]：所有 id 大于当前场景且未 Disappeared 的场景的进场进度之和，
 *   驱动背景压缩效果（下层场景随上层场景入场而向左缩放）
 *
 * ## placed 机制
 * Compose 的首帧布局完成后，[NavigationStack][me.spica27.navkit.stack.NavigationStack]
 * 会调用 [notifyPlaced]，解除 [waitAppear] 的阻塞，此后进场动画才会启动。
 * 这保证 `animateTo(1f)` 时场景已在屏幕上占据正确位置。
 */
abstract class StackScene : Scene() {

    /** 进场进度：0f 表示完全不可见，1f 表示完全呈现 */
    val enterProgress = Animatable(initialValue = 0f)

    /** 场景是否已完成首帧 Compose 布局 */
    private val _placed = MutableStateFlow(false)
    val placed: StateFlow<Boolean> = _placed

    /**
     * 由 NavigationStack 在场景首次通过 onGloballyPositioned 完成布局后调用。
     * 触发后 [waitAppear] 将解除阻塞。
     */
    fun notifyPlaced() {
        _placed.value = true
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scene 生命周期钩子
    // ──────────────────────────────────────────────────────────────────────

    /** push 开始：将进度 snap 到 0f，避免上一次残留值影响动画 */
    override suspend fun onPush() {
        enterProgress.snapTo(0f)
    }

    /** 等待 NavigationStack 通知场景首帧已布局完成 */
    override suspend fun waitAppear() {
        _placed.first { it }
    }

    /** 进场动画：从 0f 弹簧动画到 1f */
    override suspend fun onAppear() {
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    }

    /** 退场动画：从当前进度弹簧动画到 0f */
    override suspend fun onDisappear() {
        enterProgress.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                stiffness = SPRING_STIFFNESS,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        )
    }

    /** pop 后重置 placed 状态，供场景实例复用 */
    override suspend fun onPop() {
        _placed.value = false
    }

    // ──────────────────────────────────────────────────────────────────────
    // aheadEnterProgress — 背景压缩驱动值
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 计算所有排在当前场景之后（id 更大）且尚未 Disappeared 的场景的
     * [enterProgress] 之和。
     *
     * 此值在 `Modifier.graphicsLayer {}` 内读取（Draw 阶段），
     * 避免 Composition 阶段因动画帧触发整棵树重组。
     *
     * @param scenes 导航栈的完整快照列表
     */
    fun aheadEnterProgress(scenes: List<Scene>): Float {
        var sum = 0f
        for (scene in scenes) {
            if (scene is StackScene &&
                scene.id > id &&
                scene.stage.value != SceneStage.Disappeared
            ) {
                sum += scene.enterProgress.value
            }
        }
        return sum
    }

    /**
     * 计算所有排在当前场景之后（id 更大）且尚未 Disappeared 的 [DialogScene] 的
     * [DialogScene.enterProgress] 之和，clamp 到 [0f, 1f]。
     *
     * 此值在 `Modifier.graphicsLayer {}` 内读取（Draw 阶段），
     * 驱动背景场景的变暗（alpha 0.3）与去饱和度（ColorMatrix）效果。
     *
     * @param scenes 导航栈的完整快照列表
     */
    fun dialogForegroundProgress(scenes: List<Scene>): Float {
        var sum = 0f
        for (scene in scenes) {
            if (scene is DialogScene &&
                scene.id > id &&
                scene.stage.value != SceneStage.Disappeared
            ) {
                sum += scene.enterProgress.value
            }
        }
        return sum.coerceIn(0f, 1f)
    }

    companion object {
        /** 进退场弹簧刚度（300 ≈ 较硬，快速响应） */
        private const val SPRING_STIFFNESS = 300f
    }
}
