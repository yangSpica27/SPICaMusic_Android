package me.spica27.spicamusic.ui.widget

import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

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
        @Composable AnimatedVisibilityScope.() -> Unit,
) {
    var showState by remember { mutableStateOf(false) }

    DisposableEffect(visible) {
        if (showState == visible) {
            return@DisposableEffect onDispose { }
        }
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
