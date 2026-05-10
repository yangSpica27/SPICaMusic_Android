package me.spica27.spicamusic.ui.widget

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 延迟加载性能消耗大的组件，在主线程空闲时才显示
 * @param visible 是否可见
 * @param delayMillis 超时时间(ms)，默认750ms
 */
@Composable
fun ShowOnIdleContent(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Long = 750L,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    label: String = "AnimatedVisibility",
    content:
        @Composable()
        AnimatedVisibilityScope.() -> Unit,
) {
    var showState by remember { mutableStateOf(false) }

    DisposableEffect(visible) {
        val handler = Handler(Looper.getMainLooper())
        val idleHandler =
            MessageQueue.IdleHandler {
                handler.removeCallbacksAndMessages(null)
                showState = visible
                false // 只执行一次
            }

        val queue = Looper.getMainLooper().queue
        queue.addIdleHandler(idleHandler)

        // 超时保护：主线程繁忙时强制执行
        handler.postDelayed({
            queue.removeIdleHandler(idleHandler)
            showState = visible
        }, delayMillis)

        onDispose {
            handler.removeCallbacksAndMessages(null)
            queue.removeIdleHandler(idleHandler)
        }
    }

    AnimatedVisibility(
        visible = showState,
        enter = enter,
        exit = exit,
        modifier = modifier,
        label = label,
        content = content,
    )
}

@Composable
fun AnimateOnEnter(
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> =
        spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
    delayMillis: Int = 0,
    initialValue: Float = 0f,
    targetValue: Float = 1f,
    content: @Composable (animatedValue: Float, Animatable<Float, AnimationVector1D>) -> Unit,
) {
    val animatable = remember { Animatable(initialValue) }
    val visible = rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current

    LaunchedEffect(visible.value) {
        if (visible.value) {
            if (delayMillis > 0) {
                kotlinx.coroutines.delay(delayMillis.toLong())
            }
            animatable.animateTo(targetValue, animationSpec)
        }
    }

    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier =
            modifier
                .onGloballyPositioned { layoutCoordinates ->
                    coroutineScope.launch(Dispatchers.Default) {
                        if (!visible.value) {
                            visible.value = layoutCoordinates.boundsInWindow().height > 40
                        }
                    }
                },
    ) {
        content(animatable.value, animatable)
    }
}
