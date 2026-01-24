package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.captionBar
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirst
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun MainTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    largeTitle: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: ScrollBehavior? = null,
    defaultWindowInsetsPadding: Boolean = true,
    horizontalPadding: Dp = 26.dp,
) {
    val largeTitleHeight = remember { mutableStateOf(0) }

    val expandedHeightPx by rememberUpdatedState(
        remember(largeTitleHeight.value) {
            largeTitleHeight.value.toFloat().coerceAtLeast(0f)
        },
    )

    SideEffect {
        // Sets the app bar's height offset to collapse the entire bar's height when content is
        // scrolled.
        if (scrollBehavior?.state?.heightOffsetLimit != -expandedHeightPx) {
            scrollBehavior?.state?.heightOffsetLimit = -expandedHeightPx
        }
    }

    val actionsRow =
        @Composable {
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }

    TopAppBarLayout(
        modifier = modifier,
        title = title,
        largeTitle = largeTitle,
        navigationIcon = navigationIcon,
        actions = actionsRow,
        scrolledOffset = { scrollBehavior?.state?.heightOffset ?: 0f },
        expandedHeightPx = expandedHeightPx,
        horizontalPadding = horizontalPadding,
        largeTitleHeight = largeTitleHeight,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
    )
}

@Composable
private fun TopAppBarLayout(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = {},
    largeTitle: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit,
    actions: @Composable () -> Unit,
    scrolledOffset: ScrolledOffset,
    expandedHeightPx: Float,
    horizontalPadding: Dp,
    largeTitleHeight: MutableState<Int>,
    defaultWindowInsetsPadding: Boolean,
) {
    // Subtract the scrolledOffset from the maxHeight. The scrolledOffset is expected to be
    // equal or smaller than zero.
    val heightOffset by remember(scrolledOffset) {
        derivedStateOf {
            val offset = scrolledOffset.offset()
            if (offset.isNaN()) 0 else offset.roundToInt()
        }
    }

    // Small Title Animation
    val extOffset by remember(scrolledOffset) {
        derivedStateOf {
            abs(scrolledOffset.offset()) / expandedHeightPx * 2
        }
    }

    // Large Title Alpha Animation
    val largeTitleAlpha by remember(scrolledOffset, expandedHeightPx) {
        derivedStateOf {
            1f - (abs(scrolledOffset.offset()) / expandedHeightPx * 2).coerceIn(0f, 1f)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (1 - extOffset.coerceIn(0f, 1f) == 0f) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
    )
    val translationY by animateFloatAsState(
        targetValue = if (extOffset > 1f) 0f else 12f,
        animationSpec = tween(durationMillis = 250),
    )

    val statusBarsInsets = WindowInsets.statusBars
    val captionBarInsets = WindowInsets.captionBar
    val displayCutoutInsets = WindowInsets.displayCutout
    val navigationBarsInsets = WindowInsets.navigationBars

    Layout(
        {
            Box(
                Modifier
                    .layoutId("navigationIcon"),
            ) {
                navigationIcon()
            }
            Box(
                Modifier
                    .layoutId("title")
                    .padding(horizontal = horizontalPadding)
                    .graphicsLayer(
                        alpha = alpha,
                        translationY = translationY,
                    ),
            ) {
                title()
            }
            Box(
                Modifier
                    .layoutId("actionIcons"),
            ) {
                actions()
            }
            Box(
                Modifier
                    .layoutId("largeTitle")
                    .padding(horizontal = horizontalPadding)
                    .alpha(largeTitleAlpha)
                    .onSizeChanged {
                        largeTitleHeight.value = it.height
                    },
            ) {
                largeTitle()
            }
        },
        modifier =
            modifier
                .windowInsetsPadding(statusBarsInsets.only(WindowInsetsSides.Top))
                .windowInsetsPadding(captionBarInsets.only(WindowInsetsSides.Top))
                .then(
                    if (defaultWindowInsetsPadding) {
                        Modifier
                            .windowInsetsPadding(displayCutoutInsets.only(WindowInsetsSides.Horizontal))
                            .windowInsetsPadding(navigationBarsInsets.only(WindowInsetsSides.Horizontal))
                    } else {
                        Modifier
                    },
                ).clipToBounds()
                .pointerInput(Unit) { detectVerticalDragGestures { _, _ -> } },
    ) { measurables, constraints ->
        val navigationIconPlaceable =
            measurables
                .fastFirst { it.layoutId == "navigationIcon" }
                .measure(constraints.copy(minWidth = 0, minHeight = 0))

        val actionIconsPlaceable =
            measurables
                .fastFirst { it.layoutId == "actionIcons" }
                .measure(constraints.copy(minWidth = 0, minHeight = 0))

        val maxTitleWidth = constraints.maxWidth - navigationIconPlaceable.width - actionIconsPlaceable.width

        val titlePlaceable =
            measurables
                .fastFirst { it.layoutId == "title" }
                .measure(constraints.copy(minWidth = 0, maxWidth = (maxTitleWidth * 0.9).roundToInt(), minHeight = 0))

        val largeTitlePlaceable =
            measurables
                .fastFirst { it.layoutId == "largeTitle" }
                .measure(
                    constraints.copy(
                        minWidth = 0,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    ),
                )

        val collapsedHeight = 56.dp.roundToPx()
        val expandedHeight =
            kotlin.comparisons.maxOf(
                collapsedHeight,
                largeTitlePlaceable.height,
            )

        val layoutHeight =
            lerp(
                start = collapsedHeight,
                stop = expandedHeight,
                fraction =
                    if (expandedHeightPx > 0f) {
                        val offset = scrolledOffset.offset()
                        if (offset.isNaN()) 1f else (1f - (abs(offset) / expandedHeightPx).coerceIn(0f, 1f))
                    } else {
                        1f
                    },
            ).toFloat().roundToInt()

        layout(constraints.maxWidth, layoutHeight) {
            val verticalCenter = collapsedHeight / 2

            // Navigation icon
            navigationIconPlaceable.placeRelative(
                x = 0,
                y = verticalCenter - navigationIconPlaceable.height / 2,
            )

            // Title
            var baseX = (constraints.maxWidth - titlePlaceable.width) / 2
            if (baseX < navigationIconPlaceable.width) {
                baseX += (navigationIconPlaceable.width - baseX)
            } else if (baseX + titlePlaceable.width > constraints.maxWidth - actionIconsPlaceable.width) {
                baseX += ((constraints.maxWidth - actionIconsPlaceable.width) - (baseX + titlePlaceable.width))
            }
            titlePlaceable.placeRelative(
                x = baseX,
                y = verticalCenter - titlePlaceable.height / 2,
            )

            // Action icons
            actionIconsPlaceable.placeRelative(
                x = constraints.maxWidth - actionIconsPlaceable.width,
                y = verticalCenter - actionIconsPlaceable.height / 2,
            )

            // Large title
            largeTitlePlaceable.placeRelative(
                x = 0,
                y = 0,
            )
        }
    }
}

private fun interface ScrolledOffset {
    fun offset(): Float
}
