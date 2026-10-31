@file:OptIn(ExperimentalMaterial3Api::class)

package me.spica27.spicamusic.ui.basic_widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.theme.Spacing
import kotlin.math.roundToInt

@Composable
fun LargeTitleScaffold(
    modifier: Modifier = Modifier,
    title: String,
    scrollBehavior: TopAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(),
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarState = scrollBehavior.state
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)
    Scaffold(
        modifier =
            modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = containerColor,
        topBar = {
            Column(
                modifier =
                    Modifier.drawWithContent {
                        drawContent()
                        val fraction = topBarState.collapsedFraction
                        val alpha = ((fraction - 0.5f) * 2f).coerceIn(0f, 1f)
                        val lineWidth = 0.5.dp.toPx()

                        // 向内移动半个线宽，让整条线位于绘制区域内。
                        val y = size.height - lineWidth / 2f

                        drawLine(
                            dividerColor.copy(dividerColor.alpha * alpha),
                            Offset(0f, y),
                            Offset(size.width, y),
                            strokeWidth = lineWidth,
                        )
                    },
            ) {
                // 顶栏标题部分
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier =
                                Modifier.graphicsLayer {
                                    val fraction = topBarState.collapsedFraction
                                    alpha =
                                        ((fraction - 0.5f) * 2f)
                                            .coerceIn(0f, 1f)
                                },
                        )
                    },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    expandedHeight = 56.dp,
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = containerColor,
                        ),
                )
                // 展开后的大标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clipToBounds()
                            .layout { measurable, constraints ->
                                // 测量文本尺寸
                                val placeable =
                                    measurable.measure(
                                        constraints.copy(minHeight = 0),
                                    )
                                val newLimit = -placeable.height * 1f

                                if (topBarState.heightOffsetLimit != newLimit) {
                                    val wasCollapsed =
                                        topBarState.heightOffsetLimit == topBarState.heightOffset
                                    topBarState.heightOffsetLimit = newLimit
                                    topBarState.heightOffset =
                                        if (wasCollapsed) newLimit else topBarState.heightOffset
                                }
                                // 向父布局报告可见高度
                                val visibleHeight =
                                    (placeable.height + topBarState.heightOffset)
                                        .roundToInt()
                                        .coerceIn(0, placeable.height)

                                layout(placeable.width, visibleHeight) {
                                    placeable.placeRelative(0, 0)
                                }
                            }.graphicsLayer {
                                alpha = 1f - topBarState.collapsedFraction
                            }.padding(horizontal = Spacing.Large),
                )
            }
        },
        content = content,
    )
}

@Composable
fun GroupedPageScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit,
) {
    LargeTitleScaffold(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(paddingValues),
            contentPadding = paddingValues,
        ) {
            content()
        }
    }
}

@Preview(widthDp = 375, heightDp = 800, showBackground = true)
@Composable
private fun LargeTitleScaffoldPreview(modifier: Modifier = Modifier) {
    MaterialTheme {
        GroupedPageScaffold(
            title = "统计信息",
        ) {
            item(key = "overview") {
                Column {
                    GroupHeader(text = "播放概况")

                    GroupContainer(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        containerColor =
                            MaterialTheme.colorScheme.surfaceContainerLowest,
                    ) {
                        ListCell(
                            title = "听歌时长",
                            trailingContent = { Text("12 小时 38 分钟") },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                        )

                        ListCell(
                            title = "播放次数",
                            trailingContent = { Text("256 次") },
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                        )

                        ListCell(
                            title = "听过的歌",
                            trailingContent = { Text("89 首") },
                        )
                    }

                    GroupFooter(text = "当前展示本周的听歌统计")
                }
            }
        }
    }
}
