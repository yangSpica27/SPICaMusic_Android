package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** 页面选中值由调用方持有；这里仅管理指示器的连续位置与短暂交互。 */
@Stable
internal class GlassNavigationIndicatorState(
    initialIndex: Int,
    private val tabCount: Int,
    private val scope: CoroutineScope,
) {
    var position by mutableFloatStateOf(initialIndex.toFloat())
        private set
    var velocity by mutableFloatStateOf(0f)
        private set
    var isPressed by mutableStateOf(false)
        private set
    var isDragging by mutableStateOf(false)
        private set
    var isSettling by mutableStateOf(false)
        private set

    val isInteracting: Boolean
        get() = isPressed || isDragging

    private val motion = Animatable(initialIndex.toFloat())
    private var motionJob: Job? = null
    private var targetIndex = initialIndex
    private var dragPosition = initialIndex.toFloat()

    fun press() {
        isPressed = true
    }

    fun release() {
        isPressed = false
    }

    fun beginDrag(startPosition: Float = position) {
        motionJob?.cancel()
        isSettling = false
        isDragging = true
        dragPosition = startPosition
        position = startPosition
        velocity = 0f
    }

    fun dragBy(
        delta: Float,
        dragVelocity: Float,
    ) {
        dragPosition += delta
        val lastIndex = (tabCount - 1).toFloat()
        val edge = dragPosition.coerceIn(0f, lastIndex)
        val excess = dragPosition - edge
        // 手指可以越过首尾，阻力逐渐增大；仅最终页面索引受标签范围限制。
        position = edge + excess / (1f + abs(excess) / 0.3f)
        velocity = dragVelocity.coerceIn(-6f, 6f)
    }

    fun nearestIndex(): Int = position.roundToInt().coerceIn(0, tabCount - 1)

    fun synchronize(
        index: Int,
        reducedMotion: Boolean,
    ) {
        if (!isDragging) animateTo(index, reducedMotion)
    }

    fun animateTo(
        index: Int,
        reducedMotion: Boolean,
    ) {
        val target = index.coerceIn(0, tabCount - 1)
        if (!reducedMotion && !isDragging && targetIndex == target && isSettling) return
        if (!isDragging && !isSettling && abs(position - target) < 0.001f) return

        motionJob?.cancel()
        targetIndex = target
        isDragging = false
        if (reducedMotion) {
            position = target.toFloat()
            velocity = 0f
            isSettling = false
            return
        }

        isSettling = true
        val initialVelocity = velocity
        motionJob =
            scope.launch {
                motion.snapTo(position)
                motion.animateTo(
                    targetValue = target.toFloat(),
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f, visibilityThreshold = 0.002f),
                    initialVelocity = initialVelocity,
                ) {
                    position = value
                    this@GlassNavigationIndicatorState.velocity = velocity
                }
                position = target.toFloat()
                velocity = 0f
                isSettling = false
            }
    }
}
