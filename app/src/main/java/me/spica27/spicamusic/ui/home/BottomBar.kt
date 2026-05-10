package me.spica27.spicamusic.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.style.ExperimentalFoundationStyleApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.widget.CustomRotatingMorphShape
import me.spica27.spicamusic.ui.widget.highLightClickable
import me.spica27.spicamusic.ui.widget.materialSharedAxisYIn
import me.spica27.spicamusic.ui.widget.materialSharedAxisYOut
import org.koin.compose.viewmodel.koinActivityViewModel

@OptIn(ExperimentalFoundationStyleApi::class)
@Composable
fun BottomMediaBar() {
    val homeViewModel: HomeViewModel = koinActivityViewModel()

    val navigationPath = LocalNavigationPath.current

    val showCreateMenu = homeViewModel.showCreateMenu.collectAsStateWithLifecycle().value

    BackHandler(showCreateMenu) {
        homeViewModel.toggleCreateMenu()
    }

    val shapeA =
        remember {
            RoundedPolygon.star(
                4,
                rounding = CornerRounding(0.2f),
            )
        }
    val shapeB =
        remember {
            RoundedPolygon(
                6,
                rounding = CornerRounding(0.2f),
            )
        }
    val morph =
        remember {
            Morph(shapeA, shapeB)
        }
    val infiniteTransition = rememberInfiniteTransition("infinite outline movement")
    val animatedProgress =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    tween(2000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "animatedMorphProgress",
        )
    val animatedRotation =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec =
                infiniteRepeatable(
                    tween(3000, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "animatedMorphProgress",
        )

    val addBtnRotate =
        animateFloatAsState(
            targetValue = if (showCreateMenu) 45f else 0f,
            label = "",
            animationSpec =
                spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy,
                ),
        ).value

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Column(
            modifier =
                Modifier
                    .animateContentSize(
                        animationSpec =
                            spring(
                                stiffness = Spring.StiffnessMediumLow,
                                dampingRatio = Spring.DampingRatioLowBouncy,
                            ),
                    ),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                        ).background(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.large,
                        ).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 封面占位
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(
                                CustomRotatingMorphShape(
                                    morph,
                                    animatedProgress.value,
                                    animatedRotation.value,
                                ),
                            ).background(
                                color = MaterialTheme.colorScheme.primary,
                            ),
                )
                // 歌曲信息占位
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "歌曲名称",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "歌手名称",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // 播放控制占位
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(shape = CircleShape)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                            ).highLightClickable {},
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            AnimatedVisibility(
                navigationPath.scenes.lastOrNull() is HomeScene,
                enter = materialSharedAxisYIn(forward = true),
                exit = materialSharedAxisYOut(forward = false),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 16.dp,
                            ).padding(vertical = 8.dp),
                ) {
                    HomePageSwitcher(
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier =
                            Modifier
                                .rotate(
                                    addBtnRotate,
                                ).size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.tertiary,
                                    shape = CircleShape,
                                ).highLightClickable {
                                    homeViewModel.toggleCreateMenu()
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
        } // end inner Column
    }
}

@Composable
private fun HomePageSwitcher(modifier: Modifier = Modifier) {
    val homeViewModel: HomeViewModel = koinActivityViewModel()
    val tabs = remember { HomePage.entries.toTypedArray() }
    val selectIndex = homeViewModel.currentPage.collectAsStateWithLifecycle().value
    val tabPositions = remember { mutableStateMapOf<HomePage, Dp>() }
    val tabWidths = remember { mutableStateMapOf<HomePage, Dp>() }
    val tabHeight = remember { mutableStateMapOf<HomePage, Dp>() }
    val density = LocalDensity.current

    val indicatorOffset by animateDpAsState(
        targetValue = tabPositions.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
    )
    val indicatorWidth by animateDpAsState(
        targetValue = tabWidths.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
    )
    val indicatorHeight by animateDpAsState(
        targetValue = tabHeight.getOrElse(selectIndex) { 0.dp },
        label = "",
        animationSpec =
            spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioLowBouncy,
            ),
    )
    val indicatorColor = MaterialTheme.colorScheme.primaryContainer

    Row(
        modifier =
            modifier
                .height(48.dp)
                .padding(end = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = CircleShape,
                ).drawWithCache {
                    onDrawBehind {
                        if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) {
                            drawRoundRect(
                                color = indicatorColor,
                                topLeft =
                                    androidx.compose.ui.geometry.Offset(
                                        indicatorOffset.toPx(),
                                        0f,
                                    ),
                                size =
                                    androidx.compose.ui.geometry.Size(
                                        indicatorWidth.toPx(),
                                        indicatorHeight.toPx(),
                                    ),
                                cornerRadius =
                                    androidx.compose.ui.geometry.CornerRadius(
                                        100f,
                                        100f,
                                    ),
                            )
                        }
                    }
                }.animateContentSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (page in tabs) {
            HomePageSwitchItem(
                modifier =
                    Modifier
                        .onGloballyPositioned {
                            tabPositions[page] = with(density) { it.positionInParent().x.toDp() }
                            tabWidths[page] = with(density) { it.size.width.toDp() }
                            tabHeight[page] = with(density) { it.size.height.toDp() }
                        }.weight(1f),
                icon = {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Discover",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                },
                title = page.title,
                bandHomePage = page,
            )
        }
    }
}

@Composable
private fun HomePageSwitchItem(
    modifier: Modifier,
    icon: @Composable () -> Unit,
    title: String,
    bandHomePage: HomePage,
) {
    val navigationPath = LocalNavigationPath.current

    val homeViewModel: HomeViewModel = koinActivityViewModel()

    val currentHomePage = homeViewModel.currentPage.collectAsStateWithLifecycle().value

    val isSelected =
        remember(currentHomePage) {
            currentHomePage == bandHomePage
        }

    Row(
        modifier =
            modifier
                .highLightClickable {
                    if (!isSelected) {
                        homeViewModel.navigateToPage(bandHomePage)
                    }
                }.height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedVisibility(
            isSelected,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut(),
        ) {
            Row {
                icon()
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}
