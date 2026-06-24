package me.spica27.spicamusic.ui.home.player_bar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.theme.LayoutTokens
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp as floatLerp

@Stable
class BottomBarV2State internal constructor(
    internal val fraction: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
) {
    /** 当前展开进度（0f..1f），可在布局/绘制阶段读取。 */
    val progress: Float get() = fraction.value

    /** 是否处于（接近）展开态。用于返回键拦截等需要重组的判断。 */
    val isExpanded: Boolean by derivedStateOf { fraction.value > 0.5f }

    fun expand() {
        scope.launch { fraction.animateTo(1f, snapSpec()) }
    }

    fun collapse() {
        scope.launch { fraction.animateTo(0f, snapSpec()) }
    }

    internal companion object {
        fun snapSpec() = tween<Float>(durationMillis = 400, easing = EaseInOutCubic)
    }
}

@Composable
fun rememberBottomBarV2State(initialProgress: Float = 0f): BottomBarV2State {
    val scope = rememberCoroutineScope()
    return remember { BottomBarV2State(Animatable(initialProgress), scope) }
}

/** 自定义 Layout 在测量期写入、子节点在布局期读取的共享尺寸（px）。 */
private class SheetMetrics {
    var widthPx: Int = 0
    var fullHeightPx: Int = 1
    var miniHeightPx: Int = 0
    var navHeightPx: Int = 0

    /** 收起态卡片顶部的 Y（= 屏幕高 − 导航高 − 迷你条高），也是收起→展开的总行程。 */
    val collapsedCardTopPx: Int
        get() = (fullHeightPx - navHeightPx - miniHeightPx).coerceAtLeast(0)
}

@Composable
fun BottomBarV2(
    modifier: Modifier = Modifier,
    state: BottomBarV2State = rememberBottomBarV2State(),
    horizontalInset: Dp = LayoutTokens.PlayerCollapsedHorizontalInset,
    collapsedCornerRadius: Dp = LayoutTokens.PlayerCollapsedCornerRadius,
    navigationBar: @Composable () -> Unit,
    playBar: @Composable () -> Unit,
    fullScreenPlayer: @Composable (progressProvider: () -> Float, onCollapse: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val fractionProvider = remember(state) { { state.fraction.value } }
    val metrics = remember { SheetMetrics() }

    val handler =
        remember(state) {
            VerticalDragGestureHandler(
                scope = scope,
                fraction = state.fraction,
                snapSpec = BottomBarV2State.snapSpec(),
            )
        }

    val dragDistanceThresholdPx = with(density) { 5.dp.toPx() }
    val velocityThresholdPx = with(density) { 150.dp.toPx() }
    val insetPx = with(density) { horizontalInset.roundToPx() }
    val collapsedCornerPx = with(density) { collapsedCornerRadius.toPx() }

    // 展开时（接近全屏）才合成全屏播放器，收起时省去开销
    val showFullPlayer by remember { derivedStateOf { state.fraction.value > 0.001f } }
    // 完全展开后移除迷你内容，避免无谓测量与手势拦截
    val showMiniContent by remember { derivedStateOf { state.fraction.value < 0.999f } }
    // z 序：展开过半后全屏播放器置顶（同时接管触摸），否则迷你条置顶
    val fullOnTop by remember { derivedStateOf { state.fraction.value >= 0.5f } }

    // 展开态拦截返回键，收起播放器
    BackHandler(enabled = state.isExpanded) { state.collapse() }

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // 槽位 0：导航 / Tab 区（独立元素，展开时整体下滑淡出）
            Box(
                modifier =
                    Modifier.graphicsLayer {
                        val f = fractionProvider()
                        alpha = (1f - f * 2f).coerceIn(0f, 1f)
                        translationY = size.height * f
                    },
            ) {
                if (showMiniContent) navigationBar()
            }

            // 槽位 1：会生长的全屏播放器卡片（按全尺寸测量 + 圆角裁剪窗口）
            Box(
                modifier =
                    Modifier
                        .zIndex(if (fullOnTop) 1f else 0f)
                        .graphicsLayer {
                            val f = fractionProvider()
//                            alpha = ((f - 0.25f) / 0.75f).coerceIn(0f, 1f)
                            val corner = floatLerp(collapsedCornerPx, 0f, f)
                            shape = RoundedCornerShape(corner)
                            clip = true
                        }
                        // 内容按整屏 (W×H) 测量，仅把卡片窗口裁到当前大小；内容贴卡片顶端、水平居中
                        .layout { measurable, _ ->
                            val f = fractionProvider()
                            val full = metrics.fullHeightPx.coerceAtLeast(1)
                            val width = metrics.widthPx.coerceAtLeast(1)
                            val collapsedTop = metrics.collapsedCardTopPx
                            val miniH = metrics.miniHeightPx
                            val cardTop = collapsedTop * (1f - f)
                            val cardBottom =
                                floatLerp((collapsedTop + miniH).toFloat(), full.toFloat(), f)
                            val cardHeight = (cardBottom - cardTop).roundToInt().coerceIn(0, full)
                            val padPx = (insetPx * (1f - f)).roundToInt().coerceAtLeast(0)
                            val cardWidth = (width - padPx * 2).coerceIn(0, width)

                            val placeable =
                                measurable.measure(
                                    androidx.compose.ui.unit.Constraints
                                        .fixed(width, full),
                                )
                            layout(cardWidth, cardHeight) {
                                // 整屏内容左移 padPx，配合卡片整体右移 padPx → 内容左缘对齐屏幕
                                placeable.placeRelative(-padPx, 0)
                            }
                        },
            ) {
                if (showFullPlayer) {
                    fullScreenPlayer(fractionProvider) { state.collapse() }
                }
            }

            // 槽位 2：迷你播放条（承载拖拽手势，展开时淡出；位于卡片顶端覆盖全屏播放器之上）
            Box(
                modifier =
                    Modifier
                        .zIndex(if (fullOnTop) 0f else 1f)
                        .graphicsLayer {
                            val f = fractionProvider()
                            alpha = (1f - f * 2f).coerceIn(0f, 1f)
                        }.pointerInput(state) {
                            val velocityTracker = VelocityTracker()
                            detectVerticalDragGestures(
                                onDragStart = {
                                    velocityTracker.resetTracking()
                                    handler.onDragStart()
                                },
                                onVerticalDrag = { change, dragAmount ->
                                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                                    handler.onDrag(dragAmount)
                                    change.consume()
                                },
                                onDragEnd = {
                                    handler.onDragEnd(
                                        velocity = velocityTracker.calculateVelocity().y,
                                        distanceThresholdPx = dragDistanceThresholdPx,
                                        velocityThresholdPx = velocityThresholdPx,
                                    )
                                },
                                onDragCancel = {
                                    handler.onDragEnd(
                                        velocity = 0f,
                                        distanceThresholdPx = dragDistanceThresholdPx,
                                        velocityThresholdPx = velocityThresholdPx,
                                    )
                                },
                            )
                        },
            ) {
                if (showMiniContent) playBar()
            }
        },
        measurePolicy = { measurables, constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            val miniWidth = (width - insetPx * 2).coerceAtLeast(0)

            // 先测量导航与迷你条，拿到各自高度，再据此确定卡片几何。
            // 两者宽度都收缩到 miniWidth（左右各留 inset），与收起态胶囊对齐。
            val navPlaceable =
                measurables[0].measure(
                    constraints.copy(minWidth = miniWidth, maxWidth = miniWidth, minHeight = 0),
                )
            val playPlaceable =
                measurables[2].measure(
                    constraints.copy(minWidth = miniWidth, maxWidth = miniWidth, minHeight = 0),
                )

            metrics.widthPx = width
            metrics.fullHeightPx = height
            metrics.miniHeightPx = playPlaceable.height
            metrics.navHeightPx = navPlaceable.height

            // 全屏播放器卡片：其 .layout 已据 metrics + 进度算出窗口大小
            val playerPlaceable =
                measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

            val collapsedCardTop = metrics.collapsedCardTopPx
            // 告知拖拽手势：收起→展开的总行程（= 卡片顶移动距离）
            handler.dragDistancePx = collapsedCardTop.toFloat().coerceAtLeast(1f)

            layout(width, height) {
                val f = state.fraction.value
                val cardTop = (collapsedCardTop * (1f - f)).roundToInt()
                val padPx = (insetPx * (1f - f)).roundToInt().coerceAtLeast(0)

                // 先画导航（在下层，卡片生长时盖住它），再画卡片，最后迷你条覆盖在卡片顶端
                navPlaceable.placeRelative(insetPx, height - navPlaceable.height)
                playerPlaceable.placeRelative(padPx, cardTop)
                playPlaceable.placeRelative(padPx, cardTop)
            }
        },
    )
}
