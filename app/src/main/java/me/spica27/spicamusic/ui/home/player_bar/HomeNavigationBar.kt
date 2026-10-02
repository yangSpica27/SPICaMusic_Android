package me.spica27.spicamusic.ui.home.player_bar

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import kotlin.math.abs

private val NavigationMinHeight = 76.dp
private val IndicatorInset = 8.dp
private val IndicatorShape = RoundedCornerShape(20.dp)

/** 固定底栏，玻璃指示器仅采样导航项。 */
@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun HomeNavigationBar(
    selectedPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
    modifier: Modifier = Modifier,
    glassEnabled: Boolean = LocalLiquidGlassConfig.current.enabled,
) {
    val pages = HomePage.entries
    val scope = rememberCoroutineScope()
    val indicator = remember(scope) { GlassNavigationIndicatorState(selectedPage.ordinal, pages.size, scope) }
    val currentSelection by rememberUpdatedState(selectedPage)
    val onSelection by rememberUpdatedState(onPageSelected)
    val reducedMotion = LocalReducedMotion.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    val colors = MaterialTheme.colorScheme
    val useGlass = glassEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val source = rememberHazeState()
    var bounds by remember { mutableStateOf(IntSize.Zero) }
    val tabWidth = bounds.width.toFloat() / pages.size
    val inset = with(density) { IndicatorInset.toPx() }
    val interacting = indicator.isInteracting && !reducedMotion
    val press =
        animateFloatAsState(
            targetValue = if (interacting) 1f else 0f,
            animationSpec =
                when {
                    reducedMotion -> snap()
                    interacting -> spring(dampingRatio = 0.85f, stiffness = 900f)
                    else -> tween(140)
                },
            label = "navigationPress",
        )

    LaunchedEffect(selectedPage, reducedMotion) { indicator.synchronize(selectedPage.ordinal, reducedMotion) }

    Box(
        modifier
            .fillMaxWidth()
            .clipToBounds()
            .selectableGroup()
            .onSizeChanged { bounds = it }
            .pointerInput(indicator, tabWidth, isLtr, reducedMotion) {
                if (tabWidth <= 0f) return@pointerInput
                val direction = if (isLtr) 1f else -1f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val tracker = VelocityTracker()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    indicator.press()
                    try {
                        val dragStart =
                            awaitHorizontalTouchSlopOrCancellation(down.id) { change, overSlop ->
                                val start = if (isLtr) down.position.x else size.width - down.position.x
                                indicator.beginDrag((start / tabWidth - 0.5f).coerceIn(0f, pages.lastIndex.toFloat()))
                                tracker.addPosition(change.uptimeMillis, change.position)
                                indicator.dragBy(direction * overSlop / tabWidth, 0f)
                                change.consume()
                            }
                        if (dragStart != null) {
                            val completed =
                                horizontalDrag(dragStart.id) { change ->
                                    tracker.addPosition(change.uptimeMillis, change.position)
                                    indicator.dragBy(
                                        direction * change.positionChange().x / tabWidth,
                                        direction * tracker.calculateVelocity().x / tabWidth,
                                    )
                                    change.consume()
                                }
                            val target = if (completed) pages[indicator.nearestIndex()] else currentSelection
                            indicator.animateTo(target.ordinal, reducedMotion)
                            if (completed && target != currentSelection) onSelection(target)
                        }
                    } finally {
                        indicator.release()
                        if (indicator.isDragging) indicator.animateTo(currentSelection.ordinal, reducedMotion)
                    }
                }
            },
    ) {
        // 导航项与指示器分层，避免采样自身。
        Row(
            Modifier
                .fillMaxWidth()
                .then(if (useGlass) Modifier.hazeSource(source) else Modifier)
                .background(colors.surface),
        ) {
            pages.forEach { page ->
                val selected = page == selectedPage
                val color = if (selected) colors.primary else colors.onSurfaceVariant
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = NavigationMinHeight)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                indicator.animateTo(page.ordinal, reducedMotion)
                                if (page != currentSelection) onSelection(page)
                            },
                        ).padding(horizontal = 8.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    Icon(if (selected) page.selectedIcon else page.icon, null, Modifier.size(24.dp), tint = color)
                    Text(
                        stringResource(page.titleRes),
                        color = color,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (bounds.width > 0 && bounds.height > 0) {
            val lensWidth = (tabWidth - 2f * inset).coerceAtLeast(1f)
            val lensHeight = (bounds.height - 2f * inset).coerceAtLeast(1f)
            Box(
                Modifier
                    .size(with(density) { lensWidth.toDp() }, with(density) { lensHeight.toDp() })
                    .graphicsLayer {
                        val stretch = (abs(indicator.velocity) / 6f).coerceIn(0f, 1f) * press.value
                        scaleX = 1f + 0.035f * press.value + 0.025f * stretch
                        scaleY = 1f + 0.045f * press.value - 0.02f * stretch
                        val x =
                            if (isLtr) {
                                indicator.position * tabWidth + inset
                            } else {
                                bounds.width - (indicator.position + 1f) * tabWidth +
                                    inset
                            }
                        translationX = x.coerceIn(inset / 2f, (bounds.width - lensWidth - inset / 2f).coerceAtLeast(inset / 2f))
                        translationY = inset
                    }.clip(IndicatorShape)
                    .then(
                        Modifier.background(colors.primary.copy(alpha = 0.09f)),
                    ).clearAndSetSemantics {},
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun HomeNavigationBarPreview() {
    var page by remember { mutableStateOf(HomePage.Music) }
    MaterialTheme { HomeNavigationBar(page, { page = it }) }
}
