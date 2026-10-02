package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 统一管理展开动画与拖拽，拖拽直接更新进度。 */
@Stable
internal class PlayerSheetState(
    private val scope: CoroutineScope,
    initiallyExpanded: Boolean = false,
) {
    var progress by mutableFloatStateOf(if (initiallyExpanded) 1f else 0f)
        private set
    var targetExpanded by mutableStateOf(initiallyExpanded)
        private set
    var isDragging by mutableStateOf(false)
        private set
    val isVisible by derivedStateOf { progress > 0f || targetExpanded || isDragging }

    private var animation: Job? = null
    private var dragDistance = 0f

    fun animateTo(
        expanded: Boolean,
        reducedMotion: Boolean = false,
        velocity: Float = 0f,
    ) {
        animation?.cancel()
        targetExpanded = expanded
        isDragging = false
        val target = if (expanded) 1f else 0f
        if (reducedMotion || progress == target) {
            progress = target
            return
        }
        animation =
            scope.launch {
                Animatable(progress).animateTo(
                    targetValue = target,
                    animationSpec = spring(dampingRatio = 1f, stiffness = 450f, visibilityThreshold = 0.0005f),
                    initialVelocity = velocity,
                ) {
                    progress = value.coerceIn(0f, 1f)
                }
                progress = target
            }
    }

    fun beginDrag() {
        animation?.cancel()
        dragDistance = 0f
        isDragging = true
    }

    fun dragBy(
        delta: Float,
        travel: Float,
    ) {
        dragDistance += delta
        progress = (progress - delta / travel.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    fun settle(
        velocity: Float,
        travel: Float,
        distanceThreshold: Float,
        velocityThreshold: Float,
        reducedMotion: Boolean,
    ) {
        val expand =
            when {
                abs(velocity) >= velocityThreshold -> velocity < 0f
                abs(dragDistance) >= distanceThreshold -> dragDistance < 0f
                else -> progress >= 0.5f
            }
        animateTo(expand, reducedMotion, (-velocity / travel.coerceAtLeast(1f)).coerceIn(-8f, 8f))
    }

    fun reset() {
        animation?.cancel()
        targetExpanded = false
        isDragging = false
        progress = 0f
    }
}

@Composable
internal fun rememberPlayerSheetState(): PlayerSheetState {
    val scope = rememberCoroutineScope()
    return rememberSaveable(
        saver =
            Saver(
                save = { if (it.isDragging) it.progress >= 0.5f else it.targetExpanded },
                restore = { PlayerSheetState(scope, it) },
            ),
    ) { PlayerSheetState(scope) }
}
