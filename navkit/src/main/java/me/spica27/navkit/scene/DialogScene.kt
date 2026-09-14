package me.spica27.navkit.scene

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.dialog
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import me.spica27.navkit.component.ScrimOverlay
import me.spica27.navkit.motion.EaseOutStrong
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.path.LocalScene
import me.spica27.navkit.scene.DialogScene.Companion.DIALOG_SCALE_MIN
import me.spica27.navkit.scene.DialogScene.Companion.SCRIM_MAX_ALPHA
import me.spica27.navkit.scene.SceneStage

/**
 * 对话框场景基类，继承自 [OverlayScene]。
 *
 * ## 动画模型
 * - [enterProgress]：进场进度，0f = 完全不可见，1f = 完全呈现
 * - 进场：miuix 风格欠阻尼弹簧，从中心缩放（[DIALOG_SCALE_MIN] → 1f）+ alpha 渐显
 * - 退场：200ms 强 ease-out——关闭是系统响应，短时长、起步即动
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
abstract class DialogScene : OverlayScene() {

    /** 进场动画 spec：miuix 风格欠阻尼弹簧 */
    override val enterAnimationSpec: AnimationSpec<Float>
        get() =
            spring(
                dampingRatio = DIALOG_ENTER_DAMPING_RATIO,
                stiffness = DIALOG_ENTER_STIFFNESS,
                visibilityThreshold = PROGRESS_VISIBILITY_THRESHOLD,
            )

    /** 退场动画 spec：200ms 强 ease-out */
    override val exitAnimationSpec: AnimationSpec<Float>
        get() = tween(200, easing = EaseOutStrong)

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
     * 将默认的弹簧缩放应用到自定义 [Content] 的内容层。
     */
    protected fun GraphicsLayerScope.applyDefaultShowTransform(
        progress: Float,
        origin: TransformOrigin = TransformOrigin.Center,
    ) {
        val scale = DIALOG_SCALE_MIN + (1f - DIALOG_SCALE_MIN) * progress
        alpha = progress.coerceIn(0f, 1f)
        scaleX = scale
        scaleY = scale
        transformOrigin = origin
    }

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

        // 只有栈顶且尚未退场的弹窗消费返回事件，避免旧弹窗截获其上方场景的返回。
        BackHandler(
            enabled = path.isForeground(scene) &&
                    scene.stage.value != SceneStage.Disappearing
        ) {
            path.pop(scene)
        }

        Box(
            Modifier
                .zIndex(3f)
                .fillMaxSize(),
        ) {
            // 半透明遮罩：随进度渐显，点击关闭对话框
            ScrimOverlay(
                progress = enterProgress.value,
                maxAlpha = SCRIM_MAX_ALPHA,
                onDismiss = { path.pop(scene) },
            )

            // 对话框卡片：从中心缩放 + alpha 渐显
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = DIALOG_HORIZONTAL_MARGIN,
                        vertical = DIALOG_VERTICAL_MARGIN,
                    )
                    .widthIn(max = DIALOG_MAX_WIDTH)
                    .fillMaxWidth()
                    .semantics { dialog() }
                    .graphicsLayer {
                        applyDefaultShowTransform(enterProgress.value)
                    }
            ) {
                DialogContent()
            }
        }
    }

    companion object {
        /** 遮罩最大不透明度（进度为 1f 时） */
        private const val SCRIM_MAX_ALPHA = 0.5f

        /** 进场起始缩放比。 */
        private const val DIALOG_SCALE_MIN = 0.8f

        /** 默认对话框弹簧参数：灵动但不过度回弹。 */
        private const val DIALOG_ENTER_DAMPING_RATIO = 0.9f
        private const val DIALOG_ENTER_STIFFNESS = 438.6f
        private const val PROGRESS_VISIBILITY_THRESHOLD = 0.0001f

        /** Dialog 内容与窗口边缘的最小留白，以及大屏上的最大宽度。 */
        private val DIALOG_HORIZONTAL_MARGIN = 24.dp
        private val DIALOG_VERTICAL_MARGIN = 32.dp
        private val DIALOG_MAX_WIDTH = 560.dp
    }
}
