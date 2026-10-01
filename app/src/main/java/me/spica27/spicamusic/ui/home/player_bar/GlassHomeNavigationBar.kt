package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.spica27.spicamusic.ui.glass.LiquidGlassVariant
import me.spica27.spicamusic.ui.glass.LocalLiquidGlassConfig
import me.spica27.spicamusic.ui.glass.liquidGlass
import me.spica27.spicamusic.ui.home.HomePage
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import kotlin.math.abs

private val NavigationHeight = 56.dp
private val IndicatorInset = 6.dp
private val PressedIndicatorHeight = 72.dp

@Composable
@OptIn(ExperimentalHazeApi::class)
internal fun GlassHomeNavigationBar(
    selectedPage: HomePage,
    onPageSelected: (HomePage) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    val pages = HomePage.entries
    val scope = rememberCoroutineScope()
    val indicator = remember(scope) { GlassNavigationIndicatorState(selectedPage.ordinal, pages.size, scope) }
    val selectedPageState = rememberUpdatedState(selectedPage)
    val onPageSelectedState = rememberUpdatedState(onPageSelected)
    val reducedMotion = LocalReducedMotion.current
    val glassEnabled = LocalLiquidGlassConfig.current.enabled && hazeState != null
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val density = LocalDensity.current
    var navigationWidth by remember { mutableFloatStateOf(0f) }
    val tabWidth = navigationWidth / pages.size
    val insetPx = with(density) { IndicatorInset.toPx() }
    val pressedScaleY = PressedIndicatorHeight / (NavigationHeight - IndicatorInset * 2)
    val isInteracting = !reducedMotion && indicator.isInteracting
    val pressProgress =
        animateFloatAsState(
            targetValue = if (isInteracting) 1f else 0f,
            animationSpec =
                when {
                    reducedMotion -> snap()
                    isInteracting -> spring(dampingRatio = 0.85f, stiffness = 900f)
                    // 松手立即收缩，不等待位置吸附，也不保留弹簧末段的长尾。
                    else -> tween(durationMillis = 140, easing = EaseOutEmphasized)
                },
            label = "navigationGlassPress",
        )
    val lensVisible by remember(glassEnabled, pressProgress) {
        derivedStateOf { glassEnabled && pressProgress.value > 0.001f }
    }
    // 仅捕获导航底座和标签；指示器是兄弟节点，避免把自身再次采样进透镜。
    val indicatorSource = rememberHazeState()
    val surfaceModifier =
        if (hazeState != null) {
            Modifier.liquidGlass(hazeState, LiquidGlassVariant.Navigation, CircleShape)
        } else {
            Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
        }
    val colors = MaterialTheme.colorScheme
    val indicatorColor = if (glassEnabled) colors.onSurface.copy(alpha = 0.10f) else colors.primaryContainer
    val darkSurface = colors.surface.luminance() < 0.5f
    val lensStyle =
        remember(darkSurface) {
            GlassStyle.clear.then {
                shape(CircleShape)
                optics(
                    refractionStrength = 0.82f,
                    refractionHeightFraction = 0.4f,
                    refractionDisplacement = 10.dp,
                    blurRadius = 0.dp,
                    refractionProfile = RefractionProfile.Edge(10.dp),
                )
                backgroundColor(Color.Transparent)
                tint(Color.White.copy(alpha = if (darkSurface) 0.08f else 0.04f))
                specularIntensity(0.7f)
                edgeShadow(Color.Black.copy(alpha = 0.14f))
                lightPosition(Alignment.TopStart)
            }
        }
    val backdropCopyStyle =
        remember {
            GlassStyle {
                shape(RoundedCornerShape(0.dp))
                optics(refractionStrength = 0f, refractionDisplacement = 0.dp, blurRadius = 0.dp)
                backgroundColor(Color.Transparent)
                tint(Color.Transparent)
                specularIntensity(0f)
                ambientResponse(0f)
                edgeShadow(Color.Transparent)
                chromaticAberrationStrength(0f)
                contrast(0f)
                whitePoint(0f)
                chromaMultiplier(1f)
            }
        }
    val indicatorModifier =
        Modifier
            .graphicsLayer {
                val press = pressProgress.value.coerceIn(0f, 1f)
                val stretch = (abs(indicator.velocity) / 6f).coerceIn(0f, 1f) * press
                translationX =
                    if (isLtr) {
                        indicator.position * tabWidth + insetPx
                    } else {
                        navigationWidth - (indicator.position + 1f) * tabWidth + insetPx
                    }
                translationY = insetPx
                // 玻璃浮层超过底座的上下边缘；仅底座裁剪为胶囊，不裁剪指示器。
                clip = false
                scaleX = 1f + (if (glassEnabled) 0.28f else 0.10f) * press + 0.08f * stretch
                scaleY = 1f + (if (glassEnabled) pressedScaleY - 1f else 0.07f) * press - 0.04f * stretch
            }.width(with(density) { (tabWidth - 2f * insetPx).coerceAtLeast(0f).toDp() })
            .height(NavigationHeight - IndicatorInset * 2)

    LaunchedEffect(selectedPage, reducedMotion) {
        indicator.synchronize(selectedPage.ordinal, reducedMotion)
    }

    Box(
        modifier =
            modifier
                .height(NavigationHeight)
                .padding(end = 12.dp)
                .onSizeChanged { navigationWidth = it.width.toFloat() }
                .selectableGroup()
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
                                    indicator.beginDrag()
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
                                val target = if (completed) pages[indicator.nearestIndex()] else selectedPageState.value
                                indicator.animateTo(target.ordinal, reducedMotion)
                                if (completed && target != selectedPageState.value) onPageSelectedState.value(target)
                            }
                        } finally {
                            indicator.release()
                            if (indicator.isDragging) indicator.animateTo(selectedPageState.value.ordinal, reducedMotion)
                        }
                    }
                },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .then(if (lensVisible) Modifier.hazeSource(indicatorSource, zIndex = 0f) else Modifier)
                .then(surfaceModifier),
        )
        Box(
            indicatorModifier
                .align(AbsoluteAlignment.TopLeft)
                .graphicsLayer { alpha = if (glassEnabled) 1f - pressProgress.value.coerceIn(0f, 1f) else 1f }
                .background(indicatorColor, CircleShape),
        )
        Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically) {
            for (page in pages) {
                val selected = page == selectedPage
                NavigationTab(
                    page = page,
                    selected = selected,
                    modifier =
                        Modifier.selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = null,
                            indication = null,
                            onClick = {
                                indicator.animateTo(page.ordinal, reducedMotion)
                                if (page != selectedPageState.value) onPageSelectedState.value(page)
                            },
                        ),
                    reducedMotion = reducedMotion,
                )
            }
        }
        if (lensVisible) {
            // 把底座周围的页面也加入独立采样，浮出胶囊的部分仍有背景可折射。
            val horizontalOutset = with(density) { (tabWidth * 0.55f).toDp() } + 12.dp
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0f }
                    .requiredSize(
                        width = with(density) { navigationWidth.toDp() } + horizontalOutset * 2,
                        height = NavigationHeight + 48.dp,
                    ).hazeSource(indicatorSource, zIndex = -1f)
                    .hazeGlass(
                        input = HazeInput.Sources(checkNotNull(hazeState)),
                        style = backdropCopyStyle,
                        performanceMode = HazePerformanceMode.Quality,
                        expandLayerBounds = false,
                    ),
            )
            // 放大的强调色标签只供透镜读取，既不直接显示，也不重复暴露无障碍节点。
            Row(
                Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0f }
                    .clearAndSetSemantics {}
                    .hazeSource(indicatorSource, zIndex = 1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (page in pages) {
                    NavigationTab(
                        page = page,
                        selected = page == selectedPage,
                        modifier =
                            Modifier.graphicsLayer {
                                val scale = 1f + 0.14f * pressProgress.value.coerceIn(0f, 1f)
                                scaleX = scale
                                scaleY = scale
                            },
                        lensSource = true,
                        reducedMotion = reducedMotion,
                    )
                }
            }
            Box(
                indicatorModifier
                    .align(AbsoluteAlignment.TopLeft)
                    .zIndex(1f)
                    .graphicsLayer { alpha = pressProgress.value.coerceIn(0f, 1f) }
                    .hazeGlass(
                        input = HazeInput.Sources(indicatorSource),
                        style = lensStyle,
                        performanceMode = HazePerformanceMode.Quality,
                        expandLayerBounds = true,
                    ),
            )
        }
    }
}

@Composable
private fun RowScope.NavigationTab(
    page: HomePage,
    selected: Boolean,
    modifier: Modifier = Modifier,
    lensSource: Boolean = false,
    reducedMotion: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val color = if (selected || lensSource) colors.primary else colors.onSurfaceVariant
    Row(
        modifier =
            Modifier
                .weight(1f)
                .then(modifier)
                .fillMaxSize()
                .padding(horizontal = IndicatorInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = if (reducedMotion) fadeIn(snap()) else expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = if (reducedMotion) fadeOut(snap()) else shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(page.icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(2.dp))
            }
        }
        Text(
            text = stringResource(page.titleRes),
            modifier = Modifier.weight(1f, fill = false),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GlassHomeNavigationBarPreview() {
    var selectedPage by remember { mutableStateOf(HomePage.Music) }
    MaterialTheme {
        GlassHomeNavigationBar(
            selectedPage = selectedPage,
            onPageSelected = { selectedPage = it },
            modifier = Modifier.width(320.dp),
        )
    }
}
