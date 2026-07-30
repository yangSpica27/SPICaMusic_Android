package me.spica27.spicamusic.ui.player

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath
import androidx.graphics.shapes.transformed
import me.spica27.spicamusic.ui.widget.AudioCover
import kotlin.math.abs
import kotlin.math.roundToInt
import android.graphics.Matrix as AndroidMatrix
import androidx.compose.ui.util.lerp as floatLerp

/**
 * 迷你播放器封面与全屏播放器封面之间的几何过渡状态。
 *
 * 源位置记录在底栏宿主坐标中，目标位置记录为全屏播放器根节点内的局部坐标。
 * 过渡开始前会冻结源位置；否则迷你播放条随容器移动时源坐标也会变化，飞行封面
 * 会追着移动的起点计算路径，造成重叠、跳动和“顿一下”的错觉。
 */
@Stable
class PlayerArtworkMorphState internal constructor() {
    private var hostCoordinates: LayoutCoordinates? = null
    private var playerRootCoordinates: LayoutCoordinates? = null
    private var lastSourceCoordinates: LayoutCoordinates? = null
    private var lastTargetCoordinates: LayoutCoordinates? = null
    private var sourceCaptureEnabled = true

    var sourceBounds by mutableStateOf(Rect.Zero)
        private set

    var targetBounds by mutableStateOf(Rect.Zero)
        private set

    internal fun updateHostCoordinates(coordinates: LayoutCoordinates) {
        hostCoordinates = coordinates
        recalculateSourceBounds()
    }

    internal fun updatePlayerRootCoordinates(coordinates: LayoutCoordinates) {
        playerRootCoordinates = coordinates
        recalculateTargetBounds()
    }

    internal fun updateSourceCoordinates(coordinates: LayoutCoordinates) {
        lastSourceCoordinates = coordinates
        recalculateSourceBounds()
    }

    internal fun updateTargetCoordinates(coordinates: LayoutCoordinates) {
        lastTargetCoordinates = coordinates
        recalculateTargetBounds()
    }

    /** 在点击展开或开始上拉前冻结迷你封面的真实收起位置。 */
    fun freezeSourceBounds() {
        sourceCaptureEnabled = false
    }

    /** 完全收起后恢复源坐标采集，以适配底栏模式、旋转和窗口尺寸变化。 */
    fun resumeSourceCapture() {
        sourceCaptureEnabled = true
        recalculateSourceBounds()
    }

    private fun recalculateSourceBounds() {
        if (!sourceCaptureEnabled) return
        val host = hostCoordinates?.takeIf { it.isAttached } ?: return
        val source = lastSourceCoordinates?.takeIf { it.isAttached } ?: return
        val topLeft = host.localPositionOf(source, Offset.Zero)
        updateSourceBounds(Rect(offset = topLeft, size = source.size.toSize()))
    }

    private fun recalculateTargetBounds() {
        val playerRoot = playerRootCoordinates?.takeIf { it.isAttached } ?: return
        val target = lastTargetCoordinates?.takeIf { it.isAttached } ?: return
        val topLeft = playerRoot.localPositionOf(target, Offset.Zero)
        updateTargetBounds(Rect(offset = topLeft, size = target.size.toSize()))
    }

    private fun updateSourceBounds(value: Rect) {
        if (!sourceBounds.approximatelyEquals(value)) sourceBounds = value
    }

    private fun updateTargetBounds(value: Rect) {
        if (!targetBounds.approximatelyEquals(value)) targetBounds = value
    }

    val hasUsableBounds: Boolean
        get() = sourceBounds.isUsable() && targetBounds.isUsable()
}

@Composable
fun rememberPlayerArtworkMorphState(): PlayerArtworkMorphState = remember { PlayerArtworkMorphState() }

/** 记录整个底部播放器宿主的坐标，供迷你封面计算源位置。 */
fun Modifier.playerArtworkMorphHost(state: PlayerArtworkMorphState): Modifier = onGloballyPositioned(state::updateHostCoordinates)

/** 记录迷你播放器封面的源位置。 */
fun Modifier.playerArtworkMorphSource(state: PlayerArtworkMorphState): Modifier = onGloballyPositioned(state::updateSourceCoordinates)

/** 记录全屏播放器根节点，目标封面会相对于此根节点保存坐标。 */
fun Modifier.playerArtworkMorphRoot(state: PlayerArtworkMorphState): Modifier = onGloballyPositioned(state::updatePlayerRootCoordinates)

/** 记录全屏播放器封面的目标位置。 */
fun Modifier.playerArtworkMorphTarget(state: PlayerArtworkMorphState): Modifier = onGloballyPositioned(state::updateTargetCoordinates)

/**
 * M3 风格的共享封面浮层。
 *
 * 过渡期间中段只绘制这一份封面，并在源/目标矩形间连续插值。
 * 源端保留一个很短的形状交接区；目标端采用原子接管，不让目标封面和浮层重叠。
 * 浮层不参与触摸命中。
 */
@Composable
fun PlayerArtworkMorphOverlay(
    state: PlayerArtworkMorphState,
    artworkUri: Uri?,
    artworkPainter: Painter?,
    sourceShape: Shape,
    isPlaying: Boolean,
    progressProvider: () -> Float,
    inFlightProvider: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val source = state.sourceBounds
    val target = state.targetBounds
    if (!state.hasUsableBounds) return

    val targetWidth = target.width.coerceAtLeast(1f)
    val targetHeight = target.height.coerceAtLeast(1f)
    val density = LocalDensity.current
    val currentSourceShape by rememberUpdatedState(sourceShape)
    val transitionShape =
        remember(isPlaying, progressProvider) {
            UnifiedArtworkShape(
                isPlaying = isPlaying,
                progressProvider = progressProvider,
                sourceShapeProvider = { currentSourceShape },
            )
        }

    val artwork: @Composable () -> Unit = {
        if (artworkPainter != null) {
            Image(
                painter = artworkPainter,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            AudioCover(
                uri = artworkUri,
                modifier = Modifier.fillMaxSize(),
                placeHolder = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .offset {
                        IntOffset(
                            x = target.left.roundToInt(),
                            y = target.top.roundToInt(),
                        )
                    }.size(
                        width = with(density) { targetWidth.toDp() },
                        height = with(density) { targetHeight.toDp() },
                    ).zIndex(30f)
                    .graphicsLayer {
                        val progress = progressProvider().coerceIn(0f, 1f)
                        // 几何位置直接使用原始进度，手势拖动时封面与手指严格同步；
                        // 只有形状交接使用平滑曲线。
                        val currentLeft = floatLerp(source.left, target.left, progress)
                        val currentTop = floatLerp(source.top, target.top, progress)
                        val currentWidth = floatLerp(source.width, target.width, progress)
                        val currentHeight = floatLerp(source.height, target.height, progress)

                        transformOrigin = TransformOrigin(0f, 0f)
                        translationX = currentLeft - target.left
                        translationY = currentTop - target.top
                        scaleX = (currentWidth / targetWidth).coerceAtLeast(0.01f)
                        scaleY = (currentHeight / targetHeight).coerceAtLeast(0.01f)
                        compositingStrategy = CompositingStrategy.Offscreen
                        alpha =
                            artworkOverlayAlpha(
                                inFlight = inFlightProvider(),
                            )
                    },
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                            shape = transitionShape
                            clip = true
                        }.background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                artwork()
            }
        }
    }
}

/**
 * 迷你封面的可见度。
 *
 * 源端只在过渡开始的一小段与共享浮层交叉接管，用来保留播放时的动态多边形封面；
 * 中段不会再与浮层重复绘制。
 */
fun sourceArtworkAlpha(
    progress: Float,
    inFlight: Boolean,
    hasUsableBounds: Boolean,
): Float {
    if (!hasUsableBounds) return 1f
    if (!inFlight) return if (progress <= STABLE_COLLAPSED_EPSILON) 1f else 0f
    return 0f
}

/**
 * 全屏目标封面的可见度。
 *
 * 共享浮层飞行期间始终为 0；只有形变完全结束后才一次性交给目标封面。
 * 这样不会出现浮层封面和播放器原封面叠在一起的“双封面”帧。
 */
fun targetArtworkAlpha(
    progress: Float,
    inFlight: Boolean,
    hasUsableBounds: Boolean,
): Float {
    if (!hasUsableBounds) return if (progress >= STABLE_EXPANDED_THRESHOLD) 1f else 0f
    return if (!inFlight && progress >= STABLE_EXPANDED_THRESHOLD) 1f else 0f
}

private fun artworkOverlayAlpha(inFlight: Boolean): Float = if (inFlight) 1f else 0f

private class UnifiedArtworkShape(
    isPlaying: Boolean,
    private val progressProvider: () -> Float,
    private val sourceShapeProvider: () -> Shape,
) : Shape {
    private val matrix = Matrix()
    private val morph =
        Morph(
            start =
                if (isPlaying) {
                    RoundedPolygon
                        .star(
                            numVerticesPerRadius = 3,
                            rounding = CornerRounding(radius = 0.20f, smoothing = 0.30f),
                        ).transformed(
                            AndroidMatrix().apply {
                                setRotate(DIAGONAL_TRIANGLE_DEGREES)
                            },
                        )
                } else {
                    RoundedPolygon.circle(numVertices = 16)
                },
            end =
                RoundedPolygon.rectangle(
                    width = 2f,
                    height = 2f,
                    rounding = CornerRounding(radius = 0.12f, smoothing = 0.15f),
                ),
        )

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val rawProgress = progressProvider().coerceIn(0f, 1f)
        if (rawProgress <= SOURCE_SHAPE_HANDOFF_PROGRESS) {
            // 手势刚开始及关闭末段沿用真实迷你封面形状。
            // 这样动态图形收束为固定斜三角的动画不会因为浮层接管而被跳过。
            return sourceShapeProvider().createOutline(size, layoutDirection, density)
        }

        matrix.reset()
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)

        val morphProgress =
            (
                (rawProgress - SOURCE_SHAPE_HANDOFF_PROGRESS) /
                    (1f - SOURCE_SHAPE_HANDOFF_PROGRESS)
            ).coerceIn(0f, 1f)
        val path =
            morph
                .toPath(progress = smoothStep(morphProgress))
                .asComposePath()
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

private fun smoothStep(value: Float): Float {
    val t = value.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun Rect.isUsable(): Boolean = this != Rect.Zero && left.isFinite() && top.isFinite() && width > 1f && height > 1f

private fun Rect.approximatelyEquals(other: Rect): Boolean =
    abs(left - other.left) < 0.5f &&
        abs(top - other.top) < 0.5f &&
        abs(right - other.right) < 0.5f &&
        abs(bottom - other.bottom) < 0.5f

private const val STABLE_COLLAPSED_EPSILON = 0.001f
private const val STABLE_EXPANDED_THRESHOLD = 0.999f
private const val SOURCE_SHAPE_HANDOFF_PROGRESS = 0.22f
private const val DIAGONAL_TRIANGLE_DEGREES = 15f
