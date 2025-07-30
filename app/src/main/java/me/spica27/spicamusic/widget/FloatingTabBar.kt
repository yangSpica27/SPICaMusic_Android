@file:OptIn(ExperimentalSharedTransitionApi::class)

package me.spica27.spicamusic.widget


import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension


/**
 * 来源:https://github.com/elyesmansour/compose-floating-tab-bar
 */
@Composable
fun FloatingTabBar(
  isInline: Boolean,
  selectedTabKey: Any?,
  modifier: Modifier = Modifier,
  tabBarContentModifier: Modifier = Modifier,
  inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
  expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
  colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
  shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
  sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
  contentKey: Any? = null,
  content: FloatingTabBarScope.() -> Unit
) {
  val scrollConnection = rememberFloatingTabBarScrollConnection(
    initialIsInline = isInline,
    inlineBehavior = FloatingTabBarInlineBehavior.Never
  )

  LaunchedEffect(isInline) {
    if (isInline) scrollConnection.inline() else scrollConnection.expand()
  }

  FloatingTabBar(
    selectedTabKey = selectedTabKey,
    scrollConnection = scrollConnection,
    modifier = modifier,
    tabBarContentModifier = tabBarContentModifier,
    inlineAccessory = inlineAccessory,
    expandedAccessory = expandedAccessory,
    colors = colors,
    shapes = shapes,
    sizes = sizes,
    contentKey = contentKey,
    content = content
  )
}

/**
 * A floating tab bar that transitions between inline and expanded states based on scroll behavior.
 *
 * @param selectedTabKey the key of the currently selected tab
 * @param scrollConnection the scroll connection that handles state transitions
 * @param modifier the modifier to be applied to the tab bar
 * @param colors the colors used by the tab bar components
 * @param shapes the shapes used by the tab bar components
 * @param sizes the sizes and spacing used by the tab bar components
 * @param elevations the elevation values used by the tab bar components
 * @param tabBarContentModifier modifier applied to the tab bar sections containing the grouped tabs and standalone tab.
 * It is applied after the default styling (background, shadow, clip) but before any content padding.
 * @param inlineAccessory the accessory composable that appears in inline state (e.g., compact media player)
 * @param expandedAccessory the accessory composable that appears in expanded state (e.g., full media player)
 * @param contentKey optional key that when changed retriggers the content lambda
 * @param content the content defining the tabs
 */
@Composable
fun FloatingTabBar(
  selectedTabKey: Any?,
  scrollConnection: FloatingTabBarScrollConnection,
  modifier: Modifier = Modifier,
  tabBarContentModifier: Modifier = Modifier,
  inlineAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
  expandedAccessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)? = null,
  colors: FloatingTabBarColors = FloatingTabBarDefaults.colors(),
  shapes: FloatingTabBarShapes = FloatingTabBarDefaults.shapes(),
  sizes: FloatingTabBarSizes = FloatingTabBarDefaults.sizes(),
  elevations: FloatingTabBarElevations = FloatingTabBarDefaults.elevations(),
  contentKey: Any? = null,
  content: FloatingTabBarScope.() -> Unit
) {
  val scope = remember(contentKey) { FloatingTabBarScopeImpl().apply { content() } }

  val isAccessoryShared = inlineAccessory != null && expandedAccessory != null

  SharedTransitionLayout(modifier = modifier) {
    AnimatedContent(
      targetState = scrollConnection.isInline,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      contentAlignment = Alignment.BottomStart
    ) { isInline ->
      if (isInline) {
        InlineBar(
          scope = scope,
          selectedTabKey = selectedTabKey,
          accessory = inlineAccessory,
          isAccessoryShared = isAccessoryShared,
          onInlineTabClick = { scrollConnection.expand() },
          colors = colors,
          shapes = shapes,
          sizes = sizes,
          elevations = elevations,
          tabBarContentModifier = tabBarContentModifier,
          animatedVisibilityScope = this@AnimatedContent
        )
      } else {
        ExpandedBar(
          scope = scope,
          selectedTabKey = selectedTabKey,
          accessory = expandedAccessory,
          isAccessoryShared = isAccessoryShared,
          colors = colors,
          shapes = shapes,
          sizes = sizes,
          elevations = elevations,
          tabBarContentModifier = tabBarContentModifier,
          animatedVisibilityScope = this@AnimatedContent
        )
      }
    }
  }
}

/**
 * A [NestedScrollConnection] that handles scroll events to transition between inline and expanded states.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded).
 * @param scrollThresholdPx The minimum scroll distance in pixels required to trigger a state change.
 * @param inlineBehavior Defines when the tab bar should transition to inline state.
 */
class FloatingTabBarScrollConnection(
  initialIsInline: Boolean = false,
  private val scrollThresholdPx: Float,
  private val inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
) : NestedScrollConnection {
  var isInline by mutableStateOf(initialIsInline)
    private set

  private var accumulatedScroll = 0f

  fun expand() {
    isInline = false
    accumulatedScroll = 0f
  }

  fun inline() {
    isInline = true
    accumulatedScroll = 0f
  }

  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    // If behavior is Never, don't change state
    if (inlineBehavior == FloatingTabBarInlineBehavior.Never) {
      return Offset.Zero
    }

    val scrollDelta = available.y

    // Reset accumulated scroll if changing direction
    if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
      accumulatedScroll = 0f
    }

    // Accumulate scroll
    accumulatedScroll += scrollDelta

    when (inlineBehavior) {
      FloatingTabBarInlineBehavior.OnScrollDown -> {
        // Check if we've scrolled enough to trigger state change
        if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
          // Scrolling down enough - transition to inline mode
          isInline = true
          accumulatedScroll = 0f // Reset after state change
        } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
          // Scrolling up enough - transition to expanded mode
          isInline = false
          accumulatedScroll = 0f // Reset after state change
        }
      }

      FloatingTabBarInlineBehavior.OnScrollUp -> {
        // Check if we've scrolled enough to trigger state change
        if (accumulatedScroll >= scrollThresholdPx && !isInline) {
          // Scrolling up enough - transition to inline mode
          isInline = true
          accumulatedScroll = 0f // Reset after state change
        } else if (accumulatedScroll <= -scrollThresholdPx && isInline) {
          // Scrolling down enough - transition to expanded mode
          isInline = false
          accumulatedScroll = 0f // Reset after state change
        }
      }

      FloatingTabBarInlineBehavior.Never -> {
        // Already handled above, but included for completeness
      }
    }

    return Offset.Zero // Don't consume the scroll, let it pass through
  }
}

/**
 * Creates and remembers a [FloatingTabBarScrollConnection] instance.
 *
 * @param initialIsInline Initial state of the tab bar (inline or expanded). Default is false.
 * @param scrollThreshold The minimum scroll distance required to trigger a state change. Default is 50.dp.
 * @param inlineBehavior Defines when the tab bar should transition to inline state. Default is [FloatingTabBarInlineBehavior.OnScrollDown].
 * @return A remembered [FloatingTabBarScrollConnection] instance.
 */
@Composable
fun rememberFloatingTabBarScrollConnection(
  initialIsInline: Boolean = false,
  scrollThreshold: Dp = 50.dp,
  inlineBehavior: FloatingTabBarInlineBehavior = FloatingTabBarInlineBehavior.OnScrollDown
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
  val scrollThresholdPx = scrollThreshold.toPx()
  remember(scrollThresholdPx, inlineBehavior, initialIsInline) {
    FloatingTabBarScrollConnection(initialIsInline, scrollThresholdPx, inlineBehavior)
  }
}

/**
 * Defines when the floating tab bar should transition to inline state.
 */
enum class FloatingTabBarInlineBehavior {
  /** Never transition to inline - it stays in expanded state */
  Never,

  /** Transition to inline when scrolling down */
  OnScrollDown,

  /** Transition to inline when scrolling up */
  OnScrollUp
}

interface FloatingTabBarScope {
  /**
   * Adds a regular tab to the floating tab bar.
   *
   * @param key Unique identifier for the tab
   * @param title Composable content for the tab title
   * @param icon Composable content for the tab icon
   * @param onClick Callback invoked when the tab is clicked
   * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
   */
  fun tab(
    key: Any,
    title: @Composable () -> Unit = {},
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    indication: (@Composable () -> Indication)? = { LocalIndication.current }
  )

  /**
   * Adds a standalone tab to the floating tab bar.
   *
   * Note: Calling this method more than once will override the previous standalone tab value.
   *
   * @param key Unique identifier for the standalone tab
   * @param icon Composable content for the tab icon
   * @param onClick Callback invoked when the tab is clicked
   * @param indication Optional indication provider for touch feedback, defaults to LocalIndication.current
   */
  fun standaloneTab(
    key: Any,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    indication: (@Composable () -> Indication)? = { LocalIndication.current }
  )
}

@Composable
private fun SharedTransitionScope.InlineBar(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  onInlineTabClick: () -> Unit,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  elevations: FloatingTabBarElevations,
  tabBarContentModifier: Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope
) {
  val inlineTab = scope.getInlineTab(selectedTabKey)
  val standaloneTab = scope.standaloneTab
  val hasInlineTab = inlineTab != null
  val hasStandaloneTab = standaloneTab != null

  ConstraintLayout(Modifier.fillMaxWidth()) {
    val (tabGroupRef, accessoryRef, standaloneTabRef) = createRefs()

    if (hasInlineTab) {
      InlineTab(
        inlineTab = inlineTab,
        onInlineTabClick = onInlineTabClick,
        shapes = shapes,
        sizes = sizes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier.constrainAs(tabGroupRef) {
          start.linkTo(parent.start)
          centerVerticallyTo(parent)
        }
      )
    }

    if (accessory != null) {
      InlineAccessory(
        accessory = accessory,
        isAccessoryShared = isAccessoryShared,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.constrainAs(accessoryRef) {
          width = Dimension.fillToConstraints
          height = Dimension.fillToConstraints
          centerVerticallyTo(parent)
          when {
            hasInlineTab && hasStandaloneTab -> {
              start.linkTo(tabGroupRef.end, sizes.componentSpacing)
              end.linkTo(standaloneTabRef.start, sizes.componentSpacing)
            }

            hasInlineTab -> {
              start.linkTo(tabGroupRef.end, sizes.componentSpacing)
              end.linkTo(parent.end)
            }

            hasStandaloneTab -> {
              start.linkTo(parent.start)
              end.linkTo(standaloneTabRef.start, sizes.componentSpacing)
            }

            else -> {
              start.linkTo(parent.start)
              end.linkTo(parent.end)
            }
          }
        }
      )
    }

    if (hasStandaloneTab) {
      InlineStandaloneTab(
        standaloneTab = standaloneTab,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier.constrainAs(standaloneTabRef) {
          width = Dimension.ratio("1:1")
          end.linkTo(parent.end)
          if (hasInlineTab) {
            height = Dimension.fillToConstraints
            centerVerticallyTo(tabGroupRef)
          }
        }
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.InlineTab(
  inlineTab: FloatingTabBarTab,
  onInlineTabClick: () -> Unit,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  Box(
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState("tabGroup"),
        animatedVisibilityScope = animatedVisibilityScope,
        zIndexInOverlay = 1f,
        renderInOverlayDuringTransition = true
      )
      .background(
        color = colors.backgroundColor,
        shape = shapes.tabBarShape
      )
      .innerShadow(
        shape = shapes.tabBarShape,
        Shadow(
          radius = 6.dp,
          color = MaterialTheme.colorScheme.onSurface,
          alpha = .11f
        )
      )
      .clip(shapes.tabBarShape)
      .then(tabBarContentModifier)
      .clickable(
        onClick = {
          onInlineTabClick()
          inlineTab.onClick()
        },
        indication = inlineTab.indication?.invoke(),
        interactionSource = remember { MutableInteractionSource() }
      )
      .padding(sizes.tabInlineContentPadding)
  ) {
    Tab(
      icon = {
        Box(
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState("tab#${inlineTab.key}-icon"),
            animatedVisibilityScope = animatedVisibilityScope,
            zIndexInOverlay = 1f,
            renderInOverlayDuringTransition = true
          )
        ) {
          inlineTab.icon()
        }
      },
      title = { inlineTab.title() },
      isInline = true
    )
  }
}

@Composable
private fun SharedTransitionScope.InlineStandaloneTab(
  standaloneTab: FloatingTabBarTab,
  shapes: FloatingTabBarShapes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  Tab(
    icon = standaloneTab.icon,
    title = standaloneTab.title,
    isInline = true,
    isStandalone = true,
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState("standaloneTab"),
        animatedVisibilityScope = animatedVisibilityScope,
        zIndexInOverlay = 1f
      )
      .background(
        color = colors.backgroundColor,
        shape = shapes.standaloneTabShape
      )
      .innerShadow(
        shape = shapes.tabBarShape,
        Shadow(
          radius = 6.dp,
          color = MaterialTheme.colorScheme.onSurface,
          alpha = .11f
        )
      )
      .clip(shapes.standaloneTabShape)
      .then(tabBarContentModifier)
      .clickable(
        onClick = standaloneTab.onClick,
        indication = standaloneTab.indication?.invoke(),
        interactionSource = remember { MutableInteractionSource() }
      )
  )
}

@Composable
private fun SharedTransitionScope.InlineAccessory(
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier
) {
  accessory?.let { accessory ->
    Box(
      modifier = modifier
        .then(
          if (isAccessoryShared) {
            Modifier.sharedBounds(
              sharedContentState = rememberSharedContentState("accessory"),
              animatedVisibilityScope = animatedVisibilityScope,
              renderInOverlayDuringTransition = true,
              zIndexInOverlay = 1f
            )
          } else {
            Modifier.animateEnterExitAccessory(
              sharedTransitionScope = this,
              animatedVisibilityScope = animatedVisibilityScope
            )
          }
        )
    ) {
      accessory(
        Modifier
          .fillMaxSize()
//          .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
//          .innerShadow(
//            shape = shapes.tabBarShape,
//            Shadow(
//               radius = 6.dp,
//              color = MaterialTheme.colorScheme.onSurface,
//              alpha = .11f
//            )
//          )
          .clip(shapes.accessoryShape),
        animatedVisibilityScope
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedBar(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  accessory: (@Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit)?,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  elevations: FloatingTabBarElevations,
  tabBarContentModifier: Modifier,
  animatedVisibilityScope: AnimatedVisibilityScope
) {
  val standaloneTab = scope.standaloneTab
  val hasStandaloneTab = standaloneTab != null
  val hasTabGroup = scope.tabs.isNotEmpty()

  ConstraintLayout(Modifier.fillMaxWidth()) {
    val (accessoryRef, tabGroupRef, standaloneTabRef) = createRefs()

    if (accessory != null) {
      ExpandedAccessory(
        accessory = accessory,
        isAccessoryShared = isAccessoryShared,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        modifier = Modifier.constrainAs(accessoryRef) {
          start.linkTo(parent.start)
          end.linkTo(parent.end)
          top.linkTo(parent.top)
        }
      )
    }

    if (hasTabGroup) {
      ExpandedTabs(
        scope = scope,
        selectedTabKey = selectedTabKey,
        shapes = shapes,
        sizes = sizes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier
          .constrainAs(tabGroupRef) {
            width = Dimension.fillToConstraints
            start.linkTo(parent.start)
            if (hasStandaloneTab) {
              end.linkTo(standaloneTabRef.start, margin = sizes.componentSpacing)
            } else {
              end.linkTo(parent.end)
            }
            if (accessory != null) {
              top.linkTo(accessoryRef.bottom, margin = sizes.componentSpacing)
            }
            horizontalBias = 0f
          }
          .wrapContentWidth(align = Alignment.Start)
      )
    }

    if (hasStandaloneTab) {
      ExpandedStandaloneTab(
        standaloneTab = standaloneTab,
        shapes = shapes,
        colors = colors,
        elevations = elevations,
        animatedVisibilityScope = animatedVisibilityScope,
        tabBarContentModifier = tabBarContentModifier,
        modifier = Modifier.constrainAs(standaloneTabRef) {
          width = Dimension.ratio("1:1")
          end.linkTo(parent.end)
          if (hasTabGroup) {
            height = Dimension.fillToConstraints
            centerVerticallyTo(tabGroupRef)
          } else if (accessory != null) {
            top.linkTo(accessoryRef.bottom, margin = sizes.componentSpacing)
          }
        }
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedAccessory(
  accessory: @Composable SharedTransitionScope.(Modifier, AnimatedVisibilityScope) -> Unit,
  isAccessoryShared: Boolean,
  colors: FloatingTabBarColors,
  shapes: FloatingTabBarShapes,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier
) {
  Box(
    modifier = modifier
      .then(
        if (isAccessoryShared) {
          Modifier.sharedBounds(
            sharedContentState = rememberSharedContentState("accessory"),
            animatedVisibilityScope = animatedVisibilityScope,
            renderInOverlayDuringTransition = true,
            zIndexInOverlay = 1f
          )
        } else {
          Modifier.animateEnterExitAccessory(
            sharedTransitionScope = this,
            animatedVisibilityScope = animatedVisibilityScope
          )
        }
      )
  ) {
    accessory(
      Modifier
//        .background(color = colors.accessoryBackgroundColor, shapes.accessoryShape)
//        .innerShadow(
//          shape = shapes.tabBarShape,
//          Shadow(
//             radius = 6.dp,
//            color = MaterialTheme.colorScheme.onSurface,
//            alpha = .11f
//          )
//        )
        .clip(shapes.accessoryShape),
      animatedVisibilityScope
    )
  }
}

@Composable
private fun SharedTransitionScope.ExpandedTabs(
  scope: FloatingTabBarScopeImpl,
  selectedTabKey: Any?,
  shapes: FloatingTabBarShapes,
  sizes: FloatingTabBarSizes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  val inlineTab = scope.getInlineTab(selectedTabKey)

  Row(
    horizontalArrangement = Arrangement.spacedBy(sizes.tabSpacing),
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState("tabGroup"),
        animatedVisibilityScope = animatedVisibilityScope,
        zIndexInOverlay = 1f
      )
      .background(
        color = colors.backgroundColor,
        shape = shapes.tabBarShape
      )
      .innerShadow(
        shape = shapes.tabBarShape,
        Shadow(
          radius = 6.dp,
          color = MaterialTheme.colorScheme.onSurface,
          alpha = .11f
        )
      )
      .clip(shapes.tabBarShape)
      .then(tabBarContentModifier)
      .padding(sizes.tabBarContentPadding)
      .wrapContentWidth(align = Alignment.Start, unbounded = true)
      .animateContentSize()
  ) {
    scope.tabs.forEach { tab ->
      Tab(
        icon = {
          Box(
            modifier = if (tab.key == inlineTab?.key) {
              Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState("tab#${tab.key}-icon"),
                animatedVisibilityScope = animatedVisibilityScope,
                zIndexInOverlay = 1f
              )
            } else {
              Modifier.animateEnterExitTab(
                sharedTransitionScope = this@ExpandedTabs,
                animatedVisibilityScope = animatedVisibilityScope
              )
            }
          ) {
            tab.icon()
          }
        },
        title = {
          Box(
            Modifier.animateEnterExitTab(
              sharedTransitionScope = this@ExpandedTabs,
              animatedVisibilityScope = animatedVisibilityScope
            )
          ) {
            tab.title()
          }
        },
        isInline = false,
        modifier = Modifier
          .skipToLookaheadSize()
          .clip(shapes.tabShape)
          .clickable(
            onClick = tab.onClick,
            indication = tab.indication?.invoke(),
            interactionSource = remember { MutableInteractionSource() }
          )
          .padding(sizes.tabExpandedContentPadding)
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedStandaloneTab(
  standaloneTab: FloatingTabBarTab,
  shapes: FloatingTabBarShapes,
  colors: FloatingTabBarColors,
  elevations: FloatingTabBarElevations,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier,
  tabBarContentModifier: Modifier
) {
  Tab(
    icon = standaloneTab.icon,
    title = standaloneTab.title,
    isInline = false,
    isStandalone = true,
    modifier = modifier
      .sharedBounds(
        sharedContentState = rememberSharedContentState("standaloneTab"),
        animatedVisibilityScope = animatedVisibilityScope,
        zIndexInOverlay = 1f
      )
      .background(
        color = colors.backgroundColor,
        shape = shapes.standaloneTabShape
      )
      .innerShadow(
        shape = shapes.tabBarShape,
        Shadow(
          radius = 6.dp,
          color = MaterialTheme.colorScheme.onSurface,
          alpha = .11f
        )
      )
      .clip(shapes.standaloneTabShape)
      .then(tabBarContentModifier)
      .clickable(
        onClick = standaloneTab.onClick,
        indication = standaloneTab.indication?.invoke(),
        interactionSource = remember { MutableInteractionSource() }
      )
  )
}

@Composable
private fun Tab(
  icon: @Composable () -> Unit,
  title: @Composable () -> Unit,
  isInline: Boolean,
  modifier: Modifier = Modifier,
  isStandalone: Boolean = false
) {
  Column(
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    icon()
//    if (!isStandalone && !isInline) {
//      title()
//    }
  }
}

/**
 * A custom modifier that provides smooth enter/exit animations without clipping shadows or other content.
 * This is an alternative to animateEnterExit that uses renderInSharedTransitionScopeOverlay to prevent clipping.
 */
@Composable
private fun Modifier.animateEnterExitAccessory(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
  with(animatedVisibilityScope) {
    val animatedAlpha by transition.animateFloat { targetState ->
      when (targetState) {
        EnterExitState.Visible -> 1f
        else -> 0f
      }
    }

    this@animateEnterExitAccessory
      .renderInSharedTransitionScopeOverlay()
      .graphicsLayer(
        compositingStrategy = CompositingStrategy.ModulateAlpha,
        alpha = animatedAlpha
      )
  }
}

/**
 * A custom modifier that provides smooth enter/exit animations with fade and blur effects.
 */
@Composable
private fun Modifier.animateEnterExitTab(
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope
): Modifier = with(sharedTransitionScope) {
  with(animatedVisibilityScope) {
    val enterStartFraction = 0.5f
    val enterEndFraction = 0.8f
    val durationMs = 150

    val animatedAlpha by transition.animateFloat(
      transitionSpec = {
        keyframes {
          durationMillis = durationMs
          if (targetState == EnterExitState.Visible) {
            0f atFraction enterStartFraction using FastOutSlowInEasing
            1f atFraction enterEndFraction
          }
        }
      }
    ) { targetState ->
      when (targetState) {
        EnterExitState.Visible -> 1f
        else -> 0f
      }
    }

    val blurRadius = with(LocalDensity.current) { 50.dp.toPx() }
    val animatedBlur by transition.animateFloat(
      transitionSpec = {
        keyframes {
          durationMillis = durationMs
          if (targetState == EnterExitState.Visible) {
            blurRadius atFraction enterStartFraction using FastOutSlowInEasing
            0f atFraction enterEndFraction
          }
        }
      }
    ) { targetState ->
      when (targetState) {
        EnterExitState.Visible -> 0f
        else -> blurRadius
      }
    }

    graphicsLayer {
      alpha = animatedAlpha
      renderEffect = BlurEffect(
        radiusX = animatedBlur,
        radiusY = animatedBlur
      )
    }
  }
}

private class FloatingTabBarScopeImpl : FloatingTabBarScope {
  val tabs = mutableStateListOf<FloatingTabBarTab>()
  var standaloneTab: FloatingTabBarTab? by mutableStateOf(null)
    private set
  private var inlineTab: FloatingTabBarTab? = null

  fun getInlineTab(selectedTabKey: Any?): FloatingTabBarTab? {
    return if (selectedTabKey != standaloneTab?.key) {
      val selectedTab = tabs.find { it.key == selectedTabKey }
      if (selectedTab != null) {
        inlineTab = selectedTab
        selectedTab
      } else {
        inlineTab ?: tabs.firstOrNull()
      }
    } else {
      inlineTab ?: tabs.firstOrNull()
    }
  }

  override fun tab(
    key: Any,
    title: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    indication: (@Composable () -> Indication)?
  ) {
    tabs.add(
      FloatingTabBarTab(
        key = key,
        title = title,
        icon = icon,
        onClick = onClick,
        indication = indication
      )
    )
  }

  override fun standaloneTab(
    key: Any,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    indication: (@Composable () -> Indication)?
  ) {
    standaloneTab = FloatingTabBarTab(
      key = key,
      title = {},
      icon = icon,
      onClick = onClick,
      indication = indication
    )
  }
}

private data class FloatingTabBarTab(
  val key: Any,
  val title: @Composable () -> Unit,
  val icon: @Composable () -> Unit,
  val onClick: () -> Unit,
  val indication: (@Composable () -> Indication)?
)

/**
 * Represents the colors used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarColors(
  val backgroundColor: Color,
  val accessoryBackgroundColor: Color,
)

/**
 * Represents the shapes used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarShapes(
  val tabBarShape: Shape,
  val tabShape: Shape,
  val standaloneTabShape: Shape,
  val accessoryShape: Shape,
)

/**
 * Represents the elevations used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarElevations(
  val inlineElevation: Dp,
  val expandedElevation: Dp,
)

/**
 * Represents the sizes and spacing used in [FloatingTabBar].
 */
@Immutable
data class FloatingTabBarSizes(
  val tabBarContentPadding: PaddingValues,
  val tabInlineContentPadding: PaddingValues,
  val tabExpandedContentPadding: PaddingValues,
  val componentSpacing: Dp,
  val tabSpacing: Dp,
)

/**
 * Contains the default values used by [FloatingTabBar].
 */
object FloatingTabBarDefaults {
  /**
   * Creates a [FloatingTabBarColors] that represents the default colors used in a [FloatingTabBar].
   *
   * @param backgroundColor the color used for the tab bar background
   * @param accessoryBackgroundColor the color used for the accessory background
   */
  @Composable
  fun colors(
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    accessoryBackgroundColor: Color = Color.Transparent,
  ): FloatingTabBarColors = FloatingTabBarColors(
    backgroundColor = backgroundColor,
    accessoryBackgroundColor = accessoryBackgroundColor,
  )

  /**
   * Creates a [FloatingTabBarShapes] that represents the default shapes used in a [FloatingTabBar].
   *
   * @param tabBarShape the shape used to clip the tab bar
   * @param tabShape the shape used to clip individual tabs. Can be useful for example to control the click ripple effect shape
   * @param standaloneTabShape the shape used to clip the standalone tab
   * @param accessoryShape the shape used to clip the accessory container
   */
  @Composable
  fun shapes(
    tabBarShape: Shape = RoundedCornerShape(100),
    tabShape: Shape = RoundedCornerShape(100),
    standaloneTabShape: Shape = CircleShape,
    accessoryShape: Shape = RoundedCornerShape(100),
  ): FloatingTabBarShapes = FloatingTabBarShapes(
    tabBarShape = tabBarShape,
    tabShape = tabShape,
    standaloneTabShape = standaloneTabShape,
    accessoryShape = accessoryShape,
  )

  /**
   * Creates a [FloatingTabBarSizes] that represents the default sizes used in a [FloatingTabBar].
   *
   * @param tabBarContentPadding the padding applied to the tab bar content. This also applies to the standalone tab content.
   * @param tabInlineContentPadding the padding applied to tabs in inline state
   * @param tabExpandedContentPadding the padding applied to tabs in expanded state
   * @param componentSpacing the spacing between components
   * @param tabSpacing the spacing between tabs in expanded state
   */
  @Composable
  fun sizes(
    tabBarContentPadding: PaddingValues = PaddingValues(vertical = 4.dp, horizontal = 4.dp),
    tabInlineContentPadding: PaddingValues = PaddingValues(10.dp),
    tabExpandedContentPadding: PaddingValues = PaddingValues(vertical = 6.dp, horizontal = 20.dp),
    componentSpacing: Dp = 8.dp,
    tabSpacing: Dp = 0.dp,
  ): FloatingTabBarSizes = FloatingTabBarSizes(
    tabBarContentPadding = tabBarContentPadding,
    tabInlineContentPadding = tabInlineContentPadding,
    tabExpandedContentPadding = tabExpandedContentPadding,
    componentSpacing = componentSpacing,
    tabSpacing = tabSpacing,
  )

  /**
   * Creates a [FloatingTabBarElevations] that represents the default elevations used in a [FloatingTabBar].
   *
   * @param inlineElevation the elevation used for tabs in inline state
   * @param expandedElevation the elevation used for tabs in expanded state
   */
  @Composable
  fun elevations(
    inlineElevation: Dp = 0.dp,
    expandedElevation: Dp = 0.dp,
  ): FloatingTabBarElevations = FloatingTabBarElevations(
    inlineElevation = inlineElevation,
    expandedElevation = expandedElevation,
  )
}