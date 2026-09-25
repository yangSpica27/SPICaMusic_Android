package me.spica27.spicamusic.ui.navigation

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.get
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.spica27.spicamusic.ui.theme.DialogDecelerateEasing
import kotlin.math.roundToInt

/** 为 [DialogRoute] 提供平台对话框和自定义转场，并跨窗口采样背景。 */
class MotionDialogSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        val dialogProperties = entry.metadata[DialogSceneStrategy.Companion.DialogKey] ?: return null

        return MotionDialogScene(
            key = entry.contentKey,
            entry = entry,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            dialogProperties = dialogProperties,
            onBack = onBack,
        )
    }
}

private class MotionDialogScene<T : Any>(
    override val key: Any,
    private val entry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val dialogProperties: DialogProperties,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    private val contentProgress = Animatable(0f, visibilityThreshold = 0.0001f)
    private val dimProgress = Animatable(0f, visibilityThreshold = 0.0001f)
    private val removeMutex = Mutex()

    override val content: @Composable () -> Unit = {
        val lifecycleOwner = rememberLifecycleOwner()
        val dismissing = remember { mutableStateOf(false) }
        val dismiss =
            remember(onBack) {
                {
                    if (!dismissing.value) {
                        dismissing.value = true
                        onBack()
                    }
                }
            }

        val anchor = entry.metadata[PopupAnchorMetadataKey] as? PopupAnchor

        Dialog(
            onDismissRequest = dismiss,
            properties = dialogProperties,
        ) {
            disablePlatformDialogDefaultEffects()
            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                MotionDialogRouteLayout(
                    contentProgress = contentProgress,
                    dimProgress = dimProgress,
                    dialogProperties = dialogProperties,
                    anchor = anchor,
                    onDismissRequest = dismiss,
                    content = { entry.Content() },
                )
            }
        }
    }

    override suspend fun onRemove() {
        // NavDisplay can ask an overlay to leave more than once before its first removal finishes.
        // Serializing the work keeps every caller suspended until the one visible exit completes.
        removeMutex.withLock {
            coroutineScope {
                launch {
                    dimProgress.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = 250, easing = DialogDecelerateEasing),
                    )
                }
                contentProgress.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 260, easing = DialogDecelerateEasing),
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MotionDialogScene<*>) return false

        return key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries &&
            dialogProperties == other.dialogProperties
    }

    override fun hashCode(): Int =
        (
            ((key.hashCode() * 31 + entry.hashCode()) * 31 + previousEntries.hashCode()) * 31 +
                overlaidEntries.hashCode()
        ) * 31 + dialogProperties.hashCode()
}

@Composable
private fun MotionDialogRouteLayout(
    contentProgress: Animatable<Float, *>,
    dimProgress: Animatable<Float, *>,
    dialogProperties: DialogProperties,
    anchor: PopupAnchor?,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val windowInfo = LocalWindowInfo.current
    val isLargeScreen =
        windowInfo.containerDpSize.width >= 840.dp && windowInfo.containerDpSize.height >= 480.dp
    val scrimColor =
        MaterialTheme.colorScheme.scrim.copy(
            alpha = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) 0.6f else 0.3f,
        )

    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                dimProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300, easing = DialogDecelerateEasing),
                )
            }
            contentProgress.animateTo(
                targetValue = 1f,
                animationSpec =
                    if (isLargeScreen || anchor != null) {
                        spring(
                            dampingRatio = 0.9f,
                            stiffness = 438.6f,
                            visibilityThreshold = 0.0001f,
                        )
                    } else {
                        spring(
                            dampingRatio = 0.88f,
                            stiffness = 450f,
                            visibilityThreshold = 0.0001f,
                        )
                    },
            )
        }
    }

    val dismissModifier =
        if (dialogProperties.dismissOnClickOutside) {
            Modifier.pointerInput(onDismissRequest) {
                detectTapGestures(onTap = { onDismissRequest() })
            }
        } else {
            Modifier
        }

    Box(
        modifier = Modifier.fillMaxSize().then(dismissModifier),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(scrimColor.copy(alpha = scrimColor.alpha * dimProgress.value))
                    },
        )

        if (anchor != null) {
            AnchoredPopupContent(
                anchor = anchor,
                contentProgress = contentProgress,
                content = content,
            )
        } else {
            DockedDialogContent(
                isLargeScreen = isLargeScreen,
                contentProgress = contentProgress,
                windowHeight = windowInfo.containerSize.height,
                content = content,
            )
        }
    }
}

@Composable
private fun BoxScope.DockedDialogContent(
    isLargeScreen: Boolean,
    contentProgress: Animatable<Float, *>,
    windowHeight: Int,
    content: @Composable () -> Unit,
) {
    val alignment = if (isLargeScreen) Alignment.Center else Alignment.BottomCenter
    val contentPadding =
        if (isLargeScreen) {
            PaddingValues(0.dp)
        } else {
            PaddingValues(horizontal = 12.dp, vertical = 12.dp)
        }

    Box(
        modifier =
            Modifier
                .align(alignment)
                .navigationBarsPadding()
                .imePadding()
                .padding(contentPadding)
                .graphicsLayer {
                    val progress = contentProgress.value
                    if (isLargeScreen) {
                        val scale = 0.8f + 0.2f * progress
                        scaleX = scale
                        scaleY = scale
                        alpha = progress
                        translationY = 0f
                    } else {
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                        translationY = (1f - progress) * windowHeight
                    }
                }.pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
    ) {
        content()
    }
}

@Composable
private fun BoxScope.AnchoredPopupContent(
    anchor: PopupAnchor,
    contentProgress: Animatable<Float, *>,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val windowInfo = LocalWindowInfo.current
    val windowW = windowInfo.containerSize.width
    val windowH = windowInfo.containerSize.height
    val margin = with(density) { 8.dp.roundToPx() }

    var menuSize by remember { mutableStateOf(IntSize.Zero) }

    // 优先显示在锚点下方，空间不足时移至上方；水平方向右对齐并限制在窗口内。
    val (offset, origin) =
        remember(anchor, menuSize, windowW, windowH, margin) {
            val menuW = menuSize.width
            val menuH = menuSize.height

            val below = (anchor.y + anchor.height).roundToInt() + margin / 2
            val above = (anchor.y).roundToInt() - menuH - margin / 2
            val fitsBelow = below + menuH + margin <= windowH
            val y = if (fitsBelow || above < margin) below else above

            val preferredX = (anchor.x + anchor.width - menuW).roundToInt()
            val minX = margin
            val maxX = maxOf(minX, windowW - menuW - margin)
            val x = preferredX.coerceIn(minX, maxX)

            val originX =
                if (menuW <= 0) 0.5f else ((anchor.x + anchor.width / 2f) - x) / menuW
            val originY = if (fitsBelow || above < margin) 0f else 1f
            IntOffset(x, y) to TransformOrigin(originX.coerceIn(0f, 1f), originY)
        }

    Box(
        modifier =
            Modifier
                .align(Alignment.TopStart)
                .offset { offset }
                .wrapContentSize()
                .onSizeChanged { menuSize = it }
                .graphicsLayer {
                    val progress = contentProgress.value
                    val scale = 0.05f + 0.95f * progress
                    scaleX = scale
                    scaleY = scale
                    alpha = progress
                    transformOrigin = origin
                }.pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
    ) {
        content()
    }
}

@Composable
private fun disablePlatformDialogDefaultEffects() {
    val parent = LocalView.current.parent

    DisposableEffect(parent) {
        val window = (parent as? DialogWindowProvider)?.window
        window?.setWindowAnimations(0)
        window?.setDimAmount(0f)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        onDispose {}
    }
}
