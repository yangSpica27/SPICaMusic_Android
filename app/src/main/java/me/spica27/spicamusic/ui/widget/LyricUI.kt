package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.findPlayingIndex
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ==================== 常量 ====================
private object LyricUIConstants {
    const val EMPTY_LYRIC_TEXT = "暂无歌词"
    const val EMPTY_WORD_PLACEHOLDER = " · · · "

    const val LYRIC_ITEM_SPACING = 12
    const val LYRIC_CORNER_RADIUS = 32
    const val LYRIC_HORIZONTAL_PADDING = 24
    const val LYRIC_VERTICAL_PADDING = 16
    const val SEEK_PREVIEW_END_PADDING = 16

    const val ACTIVE_SCALE = 1.12f
    const val INACTIVE_SCALE = 1f
    const val SCALE_ANIMATION_DURATION = 850

    const val EMPHASIS_FACTOR = 0.18f
    const val MIN_EMPHASIS = 0.35f
    const val MAX_BLUR_RADIUS = 6f

    const val ELASTIC_OFFSET_INITIAL = 100f
    const val STAGGER_DELAY_PER_ITEM = 65L
    const val SEEK_OVERLAY_HIDE_DELAY = 450L

    const val SCROLL_ANIMATION_DURATION = 950
    const val SCROLL_VIEWPORT_OFFSET_RATIO = 0.35f

    const val BASE_TEXT_ALPHA = 0.24f
    const val TRANSLATION_TEXT_ALPHA = 0.22f
    const val ACTIVE_TRANSLATION_ALPHA = 0.85f
    const val INACTIVE_TRANSLATION_ALPHA = 0.8f

    const val WORD_GLOW_ALPHA = 0.4f
    const val WORD_GLOW_BLUR_RADIUS = 12f
    const val WORD_TRANSLATION_Y = -1.5f
}

// ==================== 主组件 ====================

/**
 * 歌词显示组件
 * 支持普通歌词和逐字歌词，带弹性滚动动画和拖动定位功能
 */
@Composable
fun LyricsUI(
    modifier: Modifier = Modifier,
    lyric: List<LyricItem>,
    currentTime: Long,
    onSeekToTime: (Long) -> Unit = {},
) {
    val lyricLines = remember(lyric) { lyric.sortedBy { it.time } }

    if (lyricLines.isEmpty()) {
        EmptyLyricState(modifier)
        return
    }

    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    val playingIndex by remember(currentTime, lyricLines) {
        derivedStateOf { lyricLines.findPlayingIndex(currentTime) }
    }

    val previewIndex by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset) + (layoutInfo.viewportSize.height / 2)
            val visible = layoutInfo.visibleItemsInfo
            if (visible.isEmpty()) {
                playingIndex
            } else {
                visible
                    .minByOrNull { item ->
                        val itemCenter = item.offset + item.size / 2
                        return@minByOrNull kotlin.math.abs(itemCenter - viewportCenter)
                    }?.index ?: playingIndex
            }
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }.collectLatest { inProgress ->
            if (inProgress && !isAutoScrolling) {
                showSeekOverlay = true
            } else if (!inProgress) {
                if (!isAutoScrolling) {
                    delay(LyricUIConstants.SEEK_OVERLAY_HIDE_DELAY)
                }
                showSeekOverlay = false
                isAutoScrolling = false
            }
        }
    }

    BoxWithConstraints(modifier = modifier) {
        LaunchedEffect(playingIndex, showSeekOverlay, lyricLines) {
            val target = playingIndex
            if (showSeekOverlay || target == Int.MAX_VALUE || target !in lyricLines.indices) {
                return@LaunchedEffect
            }
            isAutoScrolling = true
            try {
                lazyListState.slowScrollToIndex(
                    targetIndex = target,
                    viewportHeightPx = constraints.maxHeight,
                )
            } finally {
                isAutoScrolling = false
            }
        }

        val highlightedIndex = if (showSeekOverlay) previewIndex else playingIndex

        val density = LocalDensity.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LyricUIConstants.LYRIC_ITEM_SPACING.dp),
            contentPadding =
                PaddingValues(
                    vertical = with(density) { constraints.maxHeight.toDp() / 2 },
                    horizontal = LyricUIConstants.SEEK_PREVIEW_END_PADDING.dp,
                ),
            state = lazyListState,
        ) {
            itemsIndexed(
                items = lyricLines,
                key = { _, line -> line.key },
            ) { index, line ->
                val distanceFromActive = calculateDistance(index, highlightedIndex)
                val emphasis = calculateEmphasis(distanceFromActive)
                val scale by animateFloatAsState(
                    targetValue = if (distanceFromActive == 0) LyricUIConstants.ACTIVE_SCALE else LyricUIConstants.INACTIVE_SCALE,
                    label = "lyricScale",
                    animationSpec = tween(LyricUIConstants.SCALE_ANIMATION_DURATION),
                )
                val alpha by animateFloatAsState(
                    targetValue = emphasis,
                    label = "lyricAlpha",
                )
                val blurRadius = ((1f - emphasis) * LyricUIConstants.MAX_BLUR_RADIUS).dp

                val elasticOffset = remember { Animatable(0f) }
                var lastPlayingIndex by remember { mutableStateOf(playingIndex) }

                LaunchedEffect(playingIndex) {
                    if (playingIndex != Int.MAX_VALUE && playingIndex != lastPlayingIndex) {
                        if (index > playingIndex) {
                            elasticOffset.snapTo(LyricUIConstants.ELASTIC_OFFSET_INITIAL)
                            val staggerDelay = (index - playingIndex) * LyricUIConstants.STAGGER_DELAY_PER_ITEM
                            delay(staggerDelay)
                            elasticOffset.animateTo(
                                targetValue = 0f,
                                animationSpec =
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow,
                                    ),
                            )
                        } else {
                            elasticOffset.snapTo(0f)
                        }
                        lastPlayingIndex = playingIndex
                    }
                }

                LyricItemWrapper(
                    elasticOffset = elasticOffset.value,
                ) {
                    when (line) {
                        is LyricItem.NormalLyric ->
                            LyricLine(
                                lyric = line,
                                isActive = index == highlightedIndex,
                                alpha = alpha,
                                scale = scale,
                                blurRadius = blurRadius,
                            )

                        is LyricItem.WordsLyric ->
                            WordsLyricLine(
                                lyric = line,
                                currentTime = currentTime,
                                isActive = index == highlightedIndex,
                                alpha = alpha,
                                scale = scale,
                                blurRadius = blurRadius,
                            )
                    }
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.TopCenter),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter),
        )

        if (showSeekOverlay) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
        }

        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = showSeekOverlay && previewIndex in lyricLines.indices,
            enter = materialSharedAxisXIn(true),
            exit = materialSharedAxisXOut(true),
        ) {
            val previewLine = lyricLines.getOrNull(previewIndex)
            if (previewLine != null) {
                SeekPreview(
                    timeText = formatLyricTime(previewLine.time),
                    onSeek = { onSeekToTime(previewLine.time) },
                )
            }
        }
    }
}

// ==================== 辅助组件 ====================

/**
 * 空歌词状态显示
 */
@Composable
private fun EmptyLyricState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = LyricUIConstants.EMPTY_LYRIC_TEXT,
            style = MiuixTheme.textStyles.body1,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

/**
 * 歌词项弹性偏移包装器
 */
@Composable
private fun LyricItemWrapper(
    elasticOffset: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.graphicsLayer {
                translationY = elasticOffset
            },
    ) {
        content()
    }
}

// ==================== 普通歌词 ====================

/**
 * 普通歌词行显示
 */
@Composable
private fun LyricLine(
    lyric: LyricItem.NormalLyric,
    isActive: Boolean,
    alpha: Float,
    scale: Float,
    blurRadius: Dp,
) {
    val inactiveTextColor = MiuixTheme.colorScheme.onSurface.copy(alpha = alpha)
    val activeTextColor = MiuixTheme.colorScheme.onSurface

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LyricUIConstants.LYRIC_CORNER_RADIUS.dp))
                .hazeEffect { this.blurRadius = blurRadius }
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }.padding(
                    horizontal = LyricUIConstants.LYRIC_HORIZONTAL_PADDING.dp,
                    vertical = LyricUIConstants.LYRIC_VERTICAL_PADDING.dp,
                ),
    ) {
        Text(
            text = lyric.content.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
            style = MiuixTheme.textStyles.title2,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) activeTextColor else inactiveTextColor,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
        )

        if (!lyric.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = lyric.translation!!,
                style = MiuixTheme.textStyles.body2,
                color =
                    if (isActive) {
                        activeTextColor.copy(alpha = LyricUIConstants.ACTIVE_TRANSLATION_ALPHA)
                    } else {
                        inactiveTextColor.copy(alpha = LyricUIConstants.INACTIVE_TRANSLATION_ALPHA)
                    },
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ==================== 逐字歌词 ====================

/**
 * 计算单词播放进度
 */
private fun wordProgress(
    word: LyricItem.WordsLyric.WordWithTiming,
    time: Long,
): Float {
    if (time == Long.MIN_VALUE) return 0f
    val duration = (word.endTime - word.startTime).coerceAtLeast(1L)
    return when {
        time <= word.startTime -> 0f
        time >= word.endTime -> 1f
        else -> ((time - word.startTime).toFloat() / duration).coerceIn(0f, 1f)
    }
}

/**
 * 单词字符范围
 */
private data class WordCharRange(
    val start: Int,
    val end: Int,
    val word: LyricItem.WordsLyric.WordWithTiming,
)

/**
 * 构建单词字符范围列表
 */
private fun buildWordRanges(words: List<LyricItem.WordsLyric.WordWithTiming>): List<WordCharRange> {
    if (words.isEmpty()) return emptyList()
    var cursor = 0
    return words.map { word ->
        val start = cursor
        val end = cursor + word.content.length
        cursor = end
        WordCharRange(start, end, word)
    }
}

/**
 * 获取单词在文本布局中的边界框
 */
private fun wordBoundingBox(
    layout: TextLayoutResult,
    start: Int,
    end: Int,
): androidx.compose.ui.geometry.Rect {
    val safeStart = start.coerceIn(0, layout.layoutInput.text.length)
    val safeEnd = end.coerceIn(safeStart, layout.layoutInput.text.length)
    if (safeEnd <= safeStart) return androidx.compose.ui.geometry.Rect.Zero

    var rect = layout.getBoundingBox(safeStart)
    for (index in (safeStart + 1) until safeEnd) {
        val box = layout.getBoundingBox(index)
        rect =
            androidx.compose.ui.geometry.Rect(
                left = min(rect.left, box.left),
                top = min(rect.top, box.top),
                right = max(rect.right, box.right),
                bottom = max(rect.bottom, box.bottom),
            )
    }
    return rect
}

/**
 * 逐字歌词行显示（支持单词级别进度控制）
 */
@Composable
private fun WordsLyricLine(
    lyric: LyricItem.WordsLyric,
    currentTime: Long,
    isActive: Boolean,
    alpha: Float,
    scale: Float,
    blurRadius: Dp,
) {
    val activeTextColor = MiuixTheme.colorScheme.onSurface
    val baseTextColor = activeTextColor.copy(alpha = LyricUIConstants.BASE_TEXT_ALPHA * alpha)
    val translationColor =
        activeTextColor.copy(alpha = LyricUIConstants.TRANSLATION_TEXT_ALPHA * alpha)
    val sortedWords = remember(lyric) { lyric.words.sortedBy { it.startTime } }
    val sentence = remember(sortedWords) { sortedWords.joinToString(separator = "") { it.content } }
    val wordRanges = remember(sortedWords) { buildWordRanges(sortedWords) }
    val effectiveTime = if (isActive) currentTime else Long.MIN_VALUE

    val lineProgressTarget =
        remember(effectiveTime, lyric) {
            if (effectiveTime == Long.MIN_VALUE) {
                0f
            } else {
                val duration = (lyric.endTime - lyric.startTime).coerceAtLeast(1L)
                ((effectiveTime - lyric.startTime).toFloat() / duration).coerceIn(0f, 1f)
            }
        }
    val lineProgress by animateFloatAsState(
        targetValue = lineProgressTarget,
        label = "wordsLineProgress",
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LyricUIConstants.LYRIC_CORNER_RADIUS.dp))
                .hazeEffect { this.blurRadius = blurRadius }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.padding(
                    horizontal = LyricUIConstants.LYRIC_HORIZONTAL_PADDING.dp,
                    vertical = LyricUIConstants.LYRIC_VERTICAL_PADDING.dp,
                ),
    ) {
        ProgressiveWordsText(
            text = sentence.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
            wordRanges = wordRanges,
            progressProvider = { range -> wordProgress(range.word, effectiveTime) },
            baseStyle =
                MiuixTheme.textStyles.title2.copy(
                    color = baseTextColor,
                    fontWeight = FontWeight.Medium,
                ),
            activeStyle =
                MiuixTheme.textStyles.title2.copy(
                    color = activeTextColor,
                    fontWeight = FontWeight.Medium,
                ),
            modifier = Modifier.fillMaxWidth(),
        )

        val translation = lyric.translation.firstOrNull { it.content.isNotBlank() }?.content
        if (!translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = translation,
                style = MiuixTheme.textStyles.body2,
                color = translationColor,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 渐进式单词文本显示（带发光效果和进度控制）
 */
@Composable
private fun ProgressiveWordsText(
    text: String,
    wordRanges: List<WordCharRange>,
    progressProvider: (WordCharRange) -> Float,
    baseStyle: TextStyle,
    activeStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier = modifier) {
        BasicText(
            text = text,
            style = baseStyle,
            modifier = Modifier.fillMaxWidth(),
            overflow = TextOverflow.Ellipsis,
        )

        Crossfade(text.isNotBlank() && wordRanges.isNotEmpty()) {
            BasicText(
                text = text,
                style =
                    activeStyle.copy(
                        shadow =
                            Shadow(
                                color = activeStyle.color.copy(alpha = LyricUIConstants.WORD_GLOW_ALPHA),
                                offset = Offset(0f, 0f),
                                blurRadius = LyricUIConstants.WORD_GLOW_BLUR_RADIUS,
                            ),
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = LyricUIConstants.WORD_TRANSLATION_Y
                        }.drawWithCache {
                            val layout = textLayoutResult
                            onDrawWithContent {
                                if (layout == null) return@onDrawWithContent
                                wordRanges.forEach { range ->
                                    val progress = progressProvider(range)
                                    if (progress <= 0f) return@forEach
                                    val bounds = wordBoundingBox(layout, range.start, range.end)
                                    if (bounds.width <= 0f) return@forEach
                                    val clipRight = bounds.left + bounds.width * progress
                                    clipRect(
                                        left = bounds.left,
                                        top = bounds.top,
                                        right = clipRight,
                                        bottom = bounds.bottom,
                                    ) {
                                        this@onDrawWithContent.drawContent()
                                    }
                                }
                            }
                        },
                onTextLayout = { textLayoutResult = it },
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ==================== UI 辅助组件 ====================

/**
 * 拖动定位预览（显示时间和播放按钮）
 */
@Composable
private fun SeekPreview(
    timeText: String,
    onSeek: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(end = LyricUIConstants.SEEK_PREVIEW_END_PADDING.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = timeText,
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.onSurface,
            )
        }

        IconButton(
            onClick = onSeek,
            modifier =
                Modifier
                    .width(88.dp)
                    .height(44.dp),
            backgroundColor = MiuixTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "定位到此行",
                tint = MiuixTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ==================== 工具函数 ====================

/**
 * LazyListState 缓慢滚动扩展函数
 * 实现平滑的歌词切换动画
 */
private suspend fun LazyListState.slowScrollToIndex(
    targetIndex: Int,
    viewportHeightPx: Int,
    durationMillis: Int = LyricUIConstants.SCROLL_ANIMATION_DURATION,
) {
    if (viewportHeightPx <= 0 || targetIndex < 0) return
    val layoutInfoSnapshot = layoutInfo
    val averageItemSize =
        layoutInfoSnapshot.visibleItemsInfo
            .takeIf { it.isNotEmpty() }
            ?.map { it.size }
            ?.average()
            ?.toFloat()
            ?.takeIf { it > 0f } ?: (viewportHeightPx / 6f)

    val currentOffsetPx = firstVisibleItemIndex * averageItemSize + firstVisibleItemScrollOffset
    val targetOffsetPx = targetIndex * averageItemSize
    val desiredOffsetPx =
        targetOffsetPx + viewportHeightPx * LyricUIConstants.SCROLL_VIEWPORT_OFFSET_RATIO
    val distance = desiredOffsetPx - currentOffsetPx

    if (abs(distance) < 0.5f) return
    animateScrollBy(
        value = distance,
        animationSpec = tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing),
    )
}

/**
 * 格式化歌词时间为 mm:ss 格式
 */
private fun formatLyricTime(time: Long): String {
    val totalSeconds = (time / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

// ==================== 计算辅助函数 ====================

/**
 * 计算歌词项与激活项的距离
 */
private fun calculateDistance(
    index: Int,
    highlightedIndex: Int,
): Int =
    if (highlightedIndex == Int.MAX_VALUE) {
        Int.MAX_VALUE
    } else {
        abs(index - highlightedIndex)
    }

/**
 * 根据距离计算强调程度（用于透明度和模糊）
 */
private fun calculateEmphasis(distance: Int): Float =
    (1f - distance * LyricUIConstants.EMPHASIS_FACTOR).coerceIn(
        LyricUIConstants.MIN_EMPHASIS,
        1f,
    )
