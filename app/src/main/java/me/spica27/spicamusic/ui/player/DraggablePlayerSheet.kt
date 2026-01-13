package me.spica27.spicamusic.ui.player

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.launch

/**
 * 可拖拽的播放器面板
 * 支持三种状态：收起、迷你播放条、全屏
 *
 * @param viewModel 播放器 ViewModel
 * @param bottomBarHeight 底部导航栏高度（像素）
 * @param content 主内容区域
 */
@Composable
fun DraggablePlayerSheet(
    viewModel: PlayerViewModel,
    bottomPadding: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val progress = remember { Animatable(0f) }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            progress.animateTo(1f)
        } else {
            progress.animateTo(0f)
        }
    }

    // 返回键处理
    BackHandler(enabled = isExpanded) {
        scope.launch {
            isExpanded = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        content()

        SubcomposeLayout(
            modifier = Modifier.fillMaxSize(),
        ) { constraints: Constraints ->

            val looseConstraints =
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                )

            val miniPlayerPlaceables =
                subcompose("MiniPlayer") {
                    BottomPlayerBar(
                        onExpand = {
                            isExpanded = !isExpanded
                        },
                    )
                }.map { it.measure(looseConstraints) }

            val fullPlayerPlaceables =
                subcompose("FullPlayer") {
                    ExpandedPlayerScreen(
                        onCollapse = {
                            isExpanded = !isExpanded
                        },
                    )
                }.map {
                    it.measure(
                        looseConstraints.copy(
                            maxHeight =
                                miniPlayerPlaceables.first().height +
                                    ((constraints.maxHeight - miniPlayerPlaceables.first().height) * progress.value).toInt(),
                        ),
                    )
                }

            layout(
                width = constraints.maxWidth,
                height = constraints.maxHeight,
            ) {
                miniPlayerPlaceables.forEach {
                    // 播放条应该紧贴 NavigationBar 上方
                    // Y坐标 = 屏幕高度 - NavigationBar高度 - 播放条实际高度
                    val yPosition = constraints.maxHeight - bottomPadding.toInt() - it.height
                    it.place(0, yPosition)
                }

                val yPosition2 =
                    (constraints.maxHeight - miniPlayerPlaceables.first().height - bottomPadding.toInt()) -
                        ((constraints.maxHeight - miniPlayerPlaceables.first().height - bottomPadding.toInt()) * progress.value).toInt()

                Log.d("DraggablePlayerSheet", "yPosition2: $yPosition2")

                fullPlayerPlaceables.forEach {
                    it.place(0, yPosition2)
                }
            }
        }
    }
}
