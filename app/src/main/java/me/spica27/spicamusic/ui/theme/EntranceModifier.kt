package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.updateLayerBlock
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 相邻元素的入场间隔。 */
const val ENTRANCE_STAGGER_MILLIS = 35L

/** 首屏入场动画的最长等待时间。 */
const val ENTRANCE_GATE_MILLIS = 1000L

/** 入场上浮距离。 */
private const val ENTRANCE_TRANSLATION_DP = 28f

/** 入场动画参数。 */
private val ENTER_ANIM_EASING = CubicBezierEasing(0.4f, 0.1f, 0f, 1f)

/** 最短时长。 */
private const val ENTER_ANIM_DURATION_MIN = 250

/** 最长时长。 */
private const val ENTER_ANIM_DURATION_MAX = 550

/** 每向下一个的加的时长 */
private const val ENTER_ANIM_DURATION_ITEM = 45

/** 降级动效下的淡入时长。 */
private const val ENTRANCE_REDUCED_FADE_MILLIS = 120

/** 在图层阶段读取进度的入场动画节点。 */
private class EntranceModifierNode(
    var order: Int,
    var play: Boolean,
    /** `null` 表示跟随 [LocalReducedMotion]。 */
    var reducedMotionOverride: Boolean?,
) : Modifier.Node(),
    LayoutModifierNode,
    CompositionLocalConsumerModifierNode {
    private var progress = Animatable(if (play) 0f else 1f)
    private var hasStarted = !play
    private var animationJob: Job? = null

    /** 动画帧只刷新图层，不触发重组。 */
    private val layerBlock: GraphicsLayerScope.() -> Unit = {
        val currentProgress = progress.value
        alpha = currentProgress
        translationY =
            if (isReducedMotion()) {
                0f
            } else {
                (1f - currentProgress) * ENTRANCE_TRANSLATION_DP.dp.toPx()
            }
    }

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun onAttach() {
        if (play && !hasStarted && progress.value < 1f) {
            startAnimation()
        }
    }

    override fun onDetach() {
        animationJob?.cancel()
        animationJob = null

        // 动画未完成时允许下次 attach 继续播放。
        if (progress.value < 1f) {
            if (play) {
                hasStarted = false
            } else {
                // 未播放状态必须保持完全可见。
                progress = Animatable(1f)
                hasStarted = true
            }
        }
    }

    override fun onReset() {
        animationJob?.cancel()
        animationJob = null
        progress = Animatable(if (play) 0f else 1f)
        hasStarted = !play
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
    ): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0, layerBlock = layerBlock)
        }
    }

    fun update(
        newOrder: Int,
        newPlay: Boolean,
        newReducedMotionOverride: Boolean?,
    ) {
        val oldPlay = play
        val oldReducedMotionOverride = reducedMotionOverride

        order = newOrder
        play = newPlay
        reducedMotionOverride = newReducedMotionOverride

        // false -> true 表示请求重新播放。
        if (!oldPlay && newPlay && progress.value >= 1f) {
            progress = Animatable(0f)
            hasStarted = false
        }

        if (!isAttached) return
        updateLayerBlock(layerBlock)

        if (oldReducedMotionOverride != newReducedMotionOverride) {
            // 动效设置变化时从当前进度继续。
            if (animationJob?.isActive == true) {
                animationJob?.cancel()
                animationJob = null
                startAnimation()
            }
        }

        if (!oldPlay && newPlay && !hasStarted && progress.value < 1f) {
            startAnimation()
        }
    }

    private fun isReducedMotion(): Boolean = reducedMotionOverride ?: currentValueOf(LocalReducedMotion)

    private fun startAnimation() {
        if (!isAttached || !play || hasStarted && animationJob?.isActive == true) return

        hasStarted = true
        // 在 attached 状态下读取 CompositionLocal，避免 detach 竞态。
        val reducedMotion = isReducedMotion()
        animationJob =
            coroutineScope
                .launch {
                    if (reducedMotion) {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec =
                                tween(
                                    durationMillis = ENTRANCE_REDUCED_FADE_MILLIS,
                                    easing = ENTER_ANIM_EASING,
                                ),
                        )
                    } else {
                        progress.animateTo(
                            targetValue = 1f,
                            animationSpec =
                                tween<Float>(
                                    delayMillis = (order.coerceAtLeast(0) * ENTRANCE_STAGGER_MILLIS).toInt(),
                                    durationMillis = (
                                        ENTER_ANIM_DURATION_MIN +
                                            (order * ENTER_ANIM_DURATION_ITEM)
                                                .coerceAtMost(ENTER_ANIM_DURATION_MAX)
                                    ),
                                    easing = ENTER_ANIM_EASING,
                                ),
                        )
                    }
                }.also { job ->
                    job.invokeOnCompletion {
                        if (animationJob === job) {
                            animationJob = null
                        }
                    }
                }
    }
}

/** [EntranceModifierNode] 的参数载体。 */
private data class EntranceNodeElement(
    val order: Int,
    val play: Boolean,
    val reducedMotionOverride: Boolean?,
) : ModifierNodeElement<EntranceModifierNode>() {
    override fun create(): EntranceModifierNode = EntranceModifierNode(order, play, reducedMotionOverride)

    override fun update(node: EntranceModifierNode) {
        node.update(order, play, reducedMotionOverride)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "entrance"
        properties["order"] = order
        properties["play"] = play
        properties["reducedMotion"] = reducedMotionOverride
    }
}

/** 添加一次性的交错入场动画。 */
@Stable
fun Modifier.entrance(
    order: Int,
    play: Boolean = true,
    reducedMotion: Boolean? = null,
): Modifier = then(EntranceNodeElement(order, play, reducedMotion))
