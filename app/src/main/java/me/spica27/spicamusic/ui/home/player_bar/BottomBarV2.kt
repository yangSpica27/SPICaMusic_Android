package me.spica27.spicamusic.ui.home.player_bar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import me.spica27.spicamusic.ui.theme.LayoutTokens
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp as floatLerp

@Stable
class BottomBarV2State internal constructor(
    internal val fraction: Animatable<Float, AnimationVector1D>,
    private val scope: CoroutineScope,
) {
    init {
        fraction.updateBounds(0f, 1f)
    }

    /** 当前胶囊到全屏的形变进度（0f..1f）。 */
    val progress: Float get() = fraction.value

    /** 全屏播放器重内容是否已经完成过首次组合；首次打开后保留，避免关闭尾帧销毁造成闪现。 */
    val contentPrepared: Boolean = true

    /** 播放器内容是否处于可交互状态。 */
    var contentActive by mutableStateOf(false)
        private set

    /** FFT、TextureView 动态背景和波形分析是否允许运行。 */
    var dynamicEffectsActive by mutableStateOf(false)
        private set

    /** 关闭形变期间把迷你封面冻结到浮层使用的四角星，保证末帧轮廓可无缝交接。 */
    var artworkShapeLocked by mutableStateOf(false)
        private set

    /** 返回键拦截只依赖形变进度，不等待重内容显示。 */
    val isExpanded: Boolean by derivedStateOf { fraction.value > 0.5f }

    /** 点击动画、吸附动画与手势拖拽都视为共享封面的飞行阶段。 */
    val isMorphInFlight: Boolean
        get() = fraction.isRunning || fraction.value in 0.001f..0.999f

    private var transitionJob: Job? = null

    /**
     * 打开分两段：先只让轻量背景胶囊无卡顿地铺满屏幕，再组合并淡入播放器内容。
     * 这样封面、Pager、波形与动态背景不会参与几何形变的每一帧。
     */
    fun expand(isPlaying: Boolean) {
        transitionJob?.cancel()
        transitionJob =
            scope.launch {
                artworkShapeLocked = isPlaying
                contentActive = false
                dynamicEffectsActive = false

                if (isPlaying && fraction.value <= 0.001f) {
                    delay(220)
                }
                if (fraction.value < 0.999f) {
                    fraction.animateTo(1f, expandMorphSpec())
                }

                contentActive = true
                // TextureView/FFT 在几何动画结束的同一帧启动会抢占 UI 线程，
                // 造成容器最后几像素直接跳到终点。控件已预组合并正常显示，
                // 这里只把重型动态效果延后到稳定后的下一小段空闲时间。
                delay(DYNAMIC_EFFECTS_SETTLE_DELAY_MS)
                dynamicEffectsActive = true
            }
    }

    /** 关闭与打开共用同一个几何进度，控件随容器同步反向退出。 */
    fun collapse() {
        transitionJob?.cancel()
        transitionJob =
            scope.launch {
                contentActive = false
                dynamicEffectsActive = false

                if (fraction.value > 0.001f) {
                    fraction.animateTo(0f, collapseMorphSpec())
                }
                artworkShapeLocked = false
            }
    }

    internal fun syncExpandedArtworkShape(isPlaying: Boolean) {
        if (fraction.value >= 0.999f && !fraction.isRunning) {
            artworkShapeLocked = isPlaying
        }
    }

    internal fun onInteractiveDragStart(isPlaying: Boolean) {
        transitionJob?.cancel()
        transitionJob =
            scope.launch {
                // 上滑展开与点击展开必须走同一套封面交接：
                // 手势一开始就把正在循环的多边形平滑收束到圆角十字，
                // 并在整个展开/关闭过程中保持该形状，避免返回胶囊时跳回任意动态帧。
                artworkShapeLocked = isPlaying
                contentActive = false
                dynamicEffectsActive = false
            }
    }

    internal fun onInteractiveSettle(expanded: Boolean) {
        transitionJob?.cancel()
        transitionJob =
            scope.launch {
                if (expanded) {
                    contentActive = true
                    delay(DYNAMIC_EFFECTS_SETTLE_DELAY_MS)
                    dynamicEffectsActive = true
                } else {
                    artworkShapeLocked = false
                    contentActive = false
                    dynamicEffectsActive = false
                }
            }
    }

    internal companion object {
        fun expandMorphSpec() =
            tween<Float>(
                durationMillis = 340,
                easing = FastOutSlowInEasing,
            )

        fun collapseMorphSpec() =
            tween<Float>(
                durationMillis = 300,
                easing = FastOutSlowInEasing,
            )

        fun dragSnapSpec() =
            spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                // Float 动画默认 0.01 的结束阈值映射到整屏约十余像素，
                // 会在尾帧直接吸附到 1。缩小阈值让上滑/下滑完整走到端点。
                visibilityThreshold = 0.0005f,
            )

        private const val DYNAMIC_EFFECTS_SETTLE_DELAY_MS = 120L
    }
}

@Composable
fun rememberBottomBarV2State(initialProgress: Float = 0f): BottomBarV2State {
    val scope = rememberCoroutineScope()
    return remember {
        BottomBarV2State(
            fraction = Animatable(initialProgress),
            scope = scope,
        )
    }
}

/** 自定义 Layout 在测量期写入、子节点在布局期读取的共享尺寸（px）。 */
private class SheetMetrics {
    var widthPx: Int = 0
    var fullHeightPx: Int = 1
    var miniHeightPx: Int = 0
    var navHeightPx: Int = 0

    /** 收起态卡片顶部 Y，也是长胶囊到全屏的总移动距离。 */
    val collapsedCardTopPx: Int
        get() = (fullHeightPx - navHeightPx - miniHeightPx).coerceAtLeast(0)
}

/**
 * 长胶囊播放器到全屏播放器的容器形变。
 *
 * 几何动画期间只绘制轻量背景面；重播放器内容在形变结束后淡入，关闭时先淡出，
 * 因而不会再把封面解码、Pager、FFT 或 TextureView 的启动/停止混进位移动画。
 */
@Composable
fun BottomBarV2(
    modifier: Modifier = Modifier,
    state: BottomBarV2State = rememberBottomBarV2State(),
    isPlaying: Boolean = false,
    horizontalInset: Dp = LayoutTokens.PlayerCollapsedHorizontalInset,
    collapsedCornerRadius: Dp = LayoutTokens.PlayerCollapsedCornerRadius,
    collapsedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.80f),
    expandedContainerColor: Color = MaterialTheme.colorScheme.surface,
    onMorphStart: () -> Unit = {},
    onCollapsed: () -> Unit = {},
    navigationBar: @Composable () -> Unit,
    playBar: @Composable () -> Unit,
    fullScreenPlayer: @Composable (
        morphProgressProvider: () -> Float,
        morphInFlightProvider: () -> Boolean,
        onCollapse: () -> Unit,
        dragToCollapseModifier: Modifier,
    ) -> Unit,
    transitionOverlay: @Composable (
        morphProgressProvider: () -> Float,
        inFlightProvider: () -> Boolean,
    ) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val fractionProvider = remember(state) { { state.fraction.value } }
    val inFlightProvider = remember(state) { { state.isMorphInFlight } }
    val currentOnCollapsed by rememberUpdatedState(onCollapsed)
    val currentIsPlaying by rememberUpdatedState(isPlaying)
    val metrics = remember { SheetMetrics() }

    val handler =
        remember(state) {
            VerticalDragGestureHandler(
                scope = scope,
                fraction = state.fraction,
                snapSpec = BottomBarV2State.dragSnapSpec(),
                onDragStarted = {
                    onMorphStart()
                    state.onInteractiveDragStart(currentIsPlaying)
                },
                onSettled = state::onInteractiveSettle,
            )
        }

    LaunchedEffect(state) {
        snapshotFlow { state.progress <= 0.001f && !state.isMorphInFlight }
            .distinctUntilChanged()
            .filter { it }
            .collect { currentOnCollapsed() }
    }

    val dragDistanceThresholdPx = with(density) { 5.dp.toPx() }
    val velocityThresholdPx = with(density) { 150.dp.toPx() }
    val insetPx = with(density) { horizontalInset.roundToPx() }
    val collapsedCornerPx = with(density) { collapsedCornerRadius.toPx() }

    val expandedCollapseConnection =
        remember(state, handler, dragDistanceThresholdPx, velocityThresholdPx) {
            object : NestedScrollConnection {
                private var collapseDragActive = false

                override fun onPreScroll(
                    available: Offset,
                    source: NestedScrollSource,
                ): Offset {
                    if (source != NestedScrollSource.UserInput) return Offset.Zero

                    val shouldStartCollapse =
                        !collapseDragActive &&
                            state.fraction.value >= 0.999f &&
                            available.y > 0f
                    if (shouldStartCollapse) {
                        collapseDragActive = true
                        handler.onDragStart()
                    }
                    if (!collapseDragActive) return Offset.Zero

                    handler.onDrag(available.y)
                    return Offset(x = 0f, y = available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (!collapseDragActive) return Velocity.Zero

                    collapseDragActive = false
                    handler.onDragEnd(
                        velocity = available.y,
                        distanceThresholdPx = dragDistanceThresholdPx,
                        velocityThresholdPx = velocityThresholdPx,
                    )
                    return Velocity(x = 0f, y = available.y)
                }
            }
        }

    val fullOnTop by remember { derivedStateOf { state.fraction.value >= 0.45f } }

    BackHandler(enabled = state.isExpanded) { state.collapse() }

    fun Modifier.verticalPlayerDrag(): Modifier =
        pointerInput(state) {
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
        }

    fun Modifier.expandedPlayerCollapseDrag(): Modifier = nestedScroll(expandedCollapseConnection)

    Box(modifier = modifier.fillMaxSize()) {
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                // 槽位 0：导航区始终保留组合，只做绘制层淡出，避免关闭尾端重新组合造成跳帧。
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            val f = fractionProvider()
                            val visibility = (1f - f / 0.42f).coerceIn(0f, 1f)
                            alpha = visibility
                            translationY = size.height * (1f - visibility)
                        },
                ) {
                    navigationBar()
                }

                // 槽位 1：从迷你播放胶囊连续生长成全屏的轻量容器。
                Box(
                    modifier =
                        Modifier
                            .zIndex(if (fullOnTop) 1f else 0f)
                            .graphicsLayer {
                                val f = fractionProvider()
                                val corner = floatLerp(collapsedCornerPx, 0f, f)
                                shape = RoundedCornerShape(corner)
                                clip = true
                            }.drawBehind {
                                val f = fractionProvider()
                                val containerVisibility = (f / 0.28f).coerceIn(0f, 1f)
                                val containerColor =
                                    lerp(
                                        collapsedContainerColor,
                                        expandedContainerColor,
                                        f,
                                    )
                                // 收起态沿用半透明玻璃胶囊，铺满屏幕时逐渐变为不透明页面底色。
                                // 颜色与透明度也参与形变，避免迷你条淡出后突然闪成纯白/纯黑。
                                drawRect(
                                    color =
                                        containerColor.copy(
                                            alpha = containerColor.alpha * containerVisibility,
                                        ),
                                )
                            }.layout { measurable, _ ->
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
                                    // 全屏内容始终按整屏测量，只改变外部裁剪窗口，不逐帧重排内部控件。
                                    placeable.placeRelative(-padPx, 0)
                                }
                            },
                ) {
                    if (state.contentPrepared) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // 动态背景从胶囊形变的前半段就平滑出现。
                                        val f = fractionProvider()
                                        alpha = ((f - 0.04f) / 0.46f).coerceIn(0f, 1f)
                                    },
                        ) {
                            fullScreenPlayer(
                                fractionProvider,
                                inFlightProvider,
                                { state.collapse() },
                                Modifier.expandedPlayerCollapseDrag(),
                            )
                        }
                    }
                }

                // 槽位 2：迷你播放条始终保留组合，并在形变前半段淡出。
                Box(
                    modifier =
                        Modifier
                            .zIndex(if (fullOnTop) 0f else 1f)
                            .graphicsLayer {
                                val f = fractionProvider()
                                alpha = (1f - f / 0.42f).coerceIn(0f, 1f)
                            }.verticalPlayerDrag(),
                ) {
                    playBar()
                }
            },
            measurePolicy = { measurables, constraints ->
                val width = constraints.maxWidth
                val height = constraints.maxHeight
                val miniWidth = (width - insetPx * 2).coerceAtLeast(0)

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

                val playerPlaceable =
                    measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

                val collapsedCardTop = metrics.collapsedCardTopPx
                handler.dragDistancePx = collapsedCardTop.toFloat().coerceAtLeast(1f)

                layout(width, height) {
                    val f = state.fraction.value
                    val cardTop = (collapsedCardTop * (1f - f)).roundToInt()
                    val padPx = (insetPx * (1f - f)).roundToInt().coerceAtLeast(0)

                    navPlaceable.placeRelative(insetPx, height - navPlaceable.height)
                    playerPlaceable.placeRelative(padPx, cardTop)
                    playPlaceable.placeRelative(padPx, cardTop)
                }
            },
        )
        transitionOverlay(fractionProvider, inFlightProvider)
    }
}
