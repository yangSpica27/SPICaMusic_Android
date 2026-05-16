package me.spica27.navkit.scene

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene.Companion.DIALOG_SCALE_MIN
import me.spica27.navkit.scene.DialogScene.Companion.SCRIM_MAX_ALPHA

/**
 *
 * ## 动画模型
 * - [enterProgress]：进场进度，0f = 完全不可见，1f = 完全呈现
 * - 弹簧刚度 600（快速弹出，比 StackScene 300f 更急促）
 * - 进场：从中心缩放（[DIALOG_SCALE_MIN] → 1f）+ alpha 渐显
 * - 退场：反向
 *
 * ## placed 机制
 * 与 [StackScene] 一致：[NavigationStack][me.spica27.navkit.stack.NavigationStack]
 * 在场景首次布局后调用 [notifyPlaced]，解除 [waitAppear] 阻塞，进场动画随即启动。
 *
 * ## 默认 Content 行为
 * - 全屏半透明遮罩（scrim），随进度从 0 渐变到 [SCRIM_MAX_ALPHA]
 * - 点击遮罩区域自动调用 [NavigationPath.pop] 关闭对话框
 * - [DialogContent] 居中显示，带缩放 + alpha 动画
 * - 子类只需实现 [DialogContent]，无需关心动画与遮罩
 *
 * ## 使用示例
 * ```kotlin
 * class ConfirmDeleteScene : DialogScene() {
 *     @Composable
 *     override fun DialogContent() {
 *         Card(Modifier.padding(24.dp)) {
 *             // 对话框卡片内容
 *         }
 *     }
 * }
 *
 * // 打开
 * val path = LocalNavigationPath.current
 * path.push(ConfirmDeleteScene())
 *
 * // 关闭（在对话框内容中）
 * val path = LocalNavigationPath.current
 * val scene = LocalScene.current
 * path.pop(scene)
 * ```
 */
abstract class DialogScene : Scene() {

    /** 进场进度：0f = 完全不可见，1f = 完全呈现 */
    val enterProgress = Animatable(0f)

    private val _placed = MutableStateFlow(false)
    val placed: StateFlow<Boolean> = _placed

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

    /** 进场动画：从 0f 弹簧到 1f */
    override suspend fun onAppear() {
        enterProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                375,
                easing = DIALOG_EASING
            )
        )
    }

    /** 退场动画：从当前进度弹簧到 0f */
    override suspend fun onDisappear() {
        enterProgress.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                350,
                easing = DIALOG_EASING
            )
        )
    }

    /** pop 后重置 placed 状态，供场景实例复用 */
    override suspend fun onPop() {
        _placed.value = false
    }

    // ──────────────────────────────────────────────────────────────────────
    // Composable 内容
    // ──────────────────────────────────────────────────────────────────────

    /**
     * 对话框的卡片内容，由子类实现。
     *
     * 无需关心遮罩、动画或屏幕位置，这些由 [Content] 自动处理。
     * 通常直接在此放置一个 Card 或其他容器。
     */
    @Composable
    abstract fun DialogContent()

    /**
     * 完整内容层：全屏遮罩（scrim）+ 居中的 [DialogContent]（含缩放/透明度动画）。
     *
     * 子类通常**不需要**重写此方法。如需自定义进场效果（如底部弹出 ActionSheet），
     * 可重写此方法并自行处理 [enterProgress]。
     */
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val scene = LocalScene.current

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize()
        ) {
            // 半透明遮罩：随进度渐显，点击关闭对话框
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = enterProgress.value * SCRIM_MAX_ALPHA }
                    .background(Color.Black)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { path.pop(scene) }
            )

            // 对话框卡片：从中心缩放 + alpha 渐显
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        val p = enterProgress.value
                        alpha = p
                        scaleX = DIALOG_SCALE_MIN + (1f - DIALOG_SCALE_MIN) * p
                        scaleY = DIALOG_SCALE_MIN + (1f - DIALOG_SCALE_MIN) * p
                    }
            ) {
                DialogContent()
            }
        }
    }

    companion object {
        /** 遮罩最大不透明度（进度为 1f 时） */
        private const val SCRIM_MAX_ALPHA = 0.5f

        /** 进场起始缩放比（0.92 → 1.0，产生"弹出"感） */
        private const val DIALOG_SCALE_MIN = 0.92f

        /** 进场/退场动画的 easing，前半段稍慢，后半段加速 */
        private val DIALOG_EASING = Easing { fraction ->
            // 自定义 easing，前半段稍慢，后半段加速
            if (fraction < 0.5f) {
                2f * fraction * fraction
            } else {
                -1f + (4f - 2f * fraction) * fraction
            }
        }
    }
}
