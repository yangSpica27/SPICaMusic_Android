package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.findPlayingIndex
import me.spica27.spicamusic.ui.theme.Shapes
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// ==================== 常量 ====================
private object LyricUIConstants {
    const val EMPTY_LYRIC_TEXT = "暂无歌词"
    const val EMPTY_WORD_PLACEHOLDER = " · · · "

    const val LYRIC_CORNER_RADIUS = 32
    const val SEEK_PREVIEW_END_PADDING = 16

    const val INACTIVE_SCALE = 1f
    const val SCALE_ANIMATION_DURATION = 850

    const val EMPHASIS_FACTOR = 0.18f
    const val MIN_EMPHASIS = 0.35f
    const val MAX_BLUR_RADIUS = 6f

    const val ELASTIC_OFFSET_INITIAL = 100f
    const val STAGGER_DELAY_PER_ITEM = 35L
    const val SEEK_OVERLAY_HIDE_DELAY = 450L

    const val SCROLL_ANIMATION_DURATION = 550
    const val SCROLL_CORRECTION_DURATION = 250
    const val SCROLL_VIEWPORT_OFFSET_RATIO = 0.35f

    const val BASE_TEXT_ALPHA = 0.24f
    const val TRANSLATION_TEXT_ALPHA = 0.72f
    const val ACTIVE_TRANSLATION_ALPHA = 0.85f
    const val INACTIVE_TRANSLATION_ALPHA = 0.8f

    const val WORD_GLOW_ALPHA = 0.3f
    const val WORD_GLOW_BLUR_RADIUS = 10f
    const val WORD_TRANSLATION_Y = -1.5f
    const val WORD_HIGHLIGHT_FADE_DURATION = 220
}

// ==================== 显示模式 ====================

/**
 * 歌词展示形态
 *
 * [Fullscreen] 用于全屏页面；[Compact] 用于卡片等小尺寸容器，
 * 采用更小的字号与间距，避免文字溢出视口
 */
enum class LyricsDisplayMode {
    Fullscreen,
    Compact,
}

/**
 * 随展示形态变化的排版参数
 */
@Immutable
private data class LyricsUIStyle(
    val mainTextStyle: TextStyle,
    val translationTextStyle: TextStyle,
    val wordsTextStyle: TextStyle,
    val wordsTranslationTextStyle: TextStyle,
    val activeScale: Float,
    val itemSpacing: Dp,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
)

@Composable
private fun rememberLyricsUIStyle(displayMode: LyricsDisplayMode): LyricsUIStyle {
    val typography = MaterialTheme.typography
    return remember(typography, displayMode) {
        when (displayMode) {
            LyricsDisplayMode.Fullscreen ->
                LyricsUIStyle(
                    mainTextStyle = typography.headlineMedium,
                    translationTextStyle = typography.titleMedium,
                    wordsTextStyle = typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    wordsTranslationTextStyle = typography.bodySmall,
                    activeScale = 1.12f,
                    itemSpacing = 12.dp,
                    horizontalPadding = 24.dp,
                    verticalPadding = 16.dp,
                )

            LyricsDisplayMode.Compact ->
                LyricsUIStyle(
                    mainTextStyle = typography.titleLarge,
                    translationTextStyle = typography.bodyMedium,
                    wordsTextStyle = typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    wordsTranslationTextStyle = typography.bodySmall,
                    activeScale = 1.06f,
                    itemSpacing = 6.dp,
                    horizontalPadding = 16.dp,
                    verticalPadding = 8.dp,
                )
        }
    }
}

// ==================== 主组件 ====================

/**
 * 歌词显示组件
 * 支持普通歌词和逐字歌词，带弹性滚动动画和拖动定位功能
 */
@Composable
fun LyricsUI(
    modifier: Modifier = Modifier,
    lyric: ImmutableList<LyricItem>,
    currentTime: Long,
    displayMode: LyricsDisplayMode = LyricsDisplayMode.Fullscreen,
    onSeekToTime: (Long) -> Unit = {},
) {
    val lyricLines = remember(lyric) { lyric.sortedBy { it.time } }

    if (lyricLines.isEmpty()) {
        EmptyLyricState(modifier)
        return
    }

    val style = rememberLyricsUIStyle(displayMode)
    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    // 首次显示时列表停在顶部，需要一次无动画的精确定位；之后的行切换才走动画滚动
    var hasSyncedInitialPosition by remember(lyricLines) { mutableStateOf(false) }
    var leavingPlayingIndex by remember(lyricLines) { mutableIntStateOf(Int.MAX_VALUE) }
    var lastSettledPlayingIndex by remember(lyricLines) { mutableIntStateOf(Int.MAX_VALUE) }
    // currentTime 每帧变化，用 rememberUpdatedState 包装后作为稳定实例供 derivedStateOf 读取；
    // derivedStateOf 只在计算结果（行索引）变化时才通知依赖方重组
    val currentTimeState = rememberUpdatedState(currentTime)
    val playingIndex by remember(lyricLines) {
        derivedStateOf { lyricLines.findPlayingIndex(currentTimeState.value) }
    }

    // 逐字行测量缓存：按行 key 复用。滚动时 item 被回收重建不再重新测量，
    // 避免「占位文本 -> 逐字渲染」的反复切换闪烁
    val wordsTextStyle = style.wordsTextStyle
    val wordsMeasureCache = remember(lyricLines, wordsTextStyle) { HashMap<String, List<MeasuredWord>>() }
    val prewarmTextMeasurer = rememberTextMeasurer(cacheSize = 0)

    // 后台预热全部逐字行的测量结果，行首次进入视口时即可同步命中缓存
    LaunchedEffect(lyricLines, wordsTextStyle) {
        val pending =
            lyricLines
                .filterIsInstance<LyricItem.WordsLyric>()
                .filter { it.key !in wordsMeasureCache }
        if (pending.isEmpty()) return@LaunchedEffect
        val results =
            withContext(Dispatchers.Default) {
                pending.associate { line ->
                    val ranges = buildWordRanges(line.words.sortedBy { it.startTime })
                    line.key to measureWordRanges(prewarmTextMeasurer, ranges, wordsTextStyle)
                }
            }
        results.forEach { (key, value) -> wordsMeasureCache.putIfAbsent(key, value) }
    }

    val previewIndex by remember(lyricLines) {
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
                // 用户已手动定位，后续行切换直接走动画滚动，不再无动画跳转
                hasSyncedInitialPosition = true
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

    LaunchedEffect(playingIndex, lyricLines) {
        if (playingIndex == lastSettledPlayingIndex) return@LaunchedEffect

        val outgoingIndex = lastSettledPlayingIndex
        lastSettledPlayingIndex = playingIndex

        if (outgoingIndex !in lyricLines.indices) {
            leavingPlayingIndex = Int.MAX_VALUE
            return@LaunchedEffect
        }

        leavingPlayingIndex = outgoingIndex
        delay(LyricUIConstants.WORD_HIGHLIGHT_FADE_DURATION.toLong())

        if (leavingPlayingIndex == outgoingIndex) {
            leavingPlayingIndex = Int.MAX_VALUE
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
                if (hasSyncedInitialPosition) {
                    lazyListState.slowScrollToIndex(
                        targetIndex = target,
                        viewportHeightPx = constraints.maxHeight,
                    )
                } else {
                    // 首次同步：目标行大概率在视口外，动画+估算必然有落点偏差，直接精确跳转
                    lazyListState.snapToLyricIndex(
                        targetIndex = target,
                        viewportHeightPx = constraints.maxHeight,
                    )
                }
                hasSyncedInitialPosition = true
            } finally {
                isAutoScrolling = false
            }
        }

        val highlightedIndex = if (showSeekOverlay) previewIndex else playingIndex

        val density = LocalDensity.current

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(style.itemSpacing),
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
                    targetValue = if (distanceFromActive == 0) style.activeScale else LyricUIConstants.INACTIVE_SCALE,
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
                    elasticOffset = elasticOffset,
                ) {
                    when (line) {
                        is LyricItem.NormalLyric ->
                            LyricLine(
                                lyric = line,
                                isActive = index == highlightedIndex,
                                alpha = alpha,
                                scale = scale,
                                blurRadius = blurRadius,
                                style = style,
                            )

                        is LyricItem.WordsLyric -> {
                            val renderPhase =
                                when (index) {
                                    playingIndex -> LyricRenderPhase.Active
                                    leavingPlayingIndex -> LyricRenderPhase.Leaving
                                    else -> LyricRenderPhase.Inactive
                                }
                            WordsLyricLine(
                                lyric = line,
                                currentTime = currentTime,
                                renderPhase = renderPhase,
                                alpha = alpha,
                                scale = scale,
                                blurRadius = blurRadius,
                                style = style,
                                measureCache = wordsMeasureCache,
                            )
                        }
                    }
                }
            }
            item {
                Spacer(
                    modifier = Modifier.height(200.dp),
                )
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
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
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
            text = stringResource(R.string.no_lyrics),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}

/**
 * 歌词项弹性偏移包装器
 *
 * [elasticOffset] 在 graphicsLayer 块内读取：弹簧动画期间只更新图层，不触发重组
 */
@Composable
private fun LyricItemWrapper(
    elasticOffset: Animatable<Float, *>,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            Modifier.graphicsLayer {
                translationY = elasticOffset.value
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
    style: LyricsUIStyle,
) {
    val inactiveTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    val activeTextColor = MaterialTheme.colorScheme.onSurface

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLarge2CornerBasedShape)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }.blur(blurRadius)
                .padding(
                    horizontal = style.horizontalPadding,
                    vertical = style.verticalPadding,
                ),
    ) {
        Text(
            text = lyric.content.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
            style = style.mainTextStyle,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isActive) activeTextColor else inactiveTextColor,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
        )

        if (!lyric.translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = lyric.translation!!,
                style = style.translationTextStyle,
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

private enum class LyricRenderPhase {
    Active,
    Leaving,
    Inactive,
}

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
    renderPhase: LyricRenderPhase,
    alpha: Float,
    scale: Float,
    blurRadius: Dp,
    style: LyricsUIStyle,
    measureCache: MutableMap<String, List<MeasuredWord>>,
) {
    val activeTextColor = MaterialTheme.colorScheme.onSurface
    val baseTextColor = activeTextColor.copy(alpha = LyricUIConstants.BASE_TEXT_ALPHA * alpha)
    val translationColor =
        activeTextColor.copy(alpha = LyricUIConstants.TRANSLATION_TEXT_ALPHA * alpha)
    val sortedWords = remember(lyric) { lyric.words.sortedBy { it.startTime } }
    val sentence = remember(sortedWords) { sortedWords.joinToString(separator = "") { it.content } }
    val wordRanges = remember(sortedWords) { buildWordRanges(sortedWords) }
    val latestCurrentTime by rememberUpdatedState(currentTime)
    var retainedDisplayTime by remember(lyric.key) { mutableLongStateOf(Long.MIN_VALUE) }
    var lastRenderPhase by remember(lyric.key) { mutableStateOf(renderPhase) }

    LaunchedEffect(renderPhase, lyric.key) {
        when (renderPhase) {
            LyricRenderPhase.Active -> {
                retainedDisplayTime = Long.MIN_VALUE
            }

            LyricRenderPhase.Leaving -> {
                if (lastRenderPhase != LyricRenderPhase.Leaving) {
                    retainedDisplayTime = latestCurrentTime
                }
            }

            LyricRenderPhase.Inactive -> {
                if (lastRenderPhase != LyricRenderPhase.Inactive) {
                    retainedDisplayTime = Long.MIN_VALUE
                }
            }
        }
        lastRenderPhase = renderPhase
    }

    val displayTime =
        when (renderPhase) {
            LyricRenderPhase.Active -> currentTime
            LyricRenderPhase.Leaving ->
                retainedDisplayTime.takeIf { it != Long.MIN_VALUE } ?: latestCurrentTime
            LyricRenderPhase.Inactive -> Long.MIN_VALUE
        }
    val highlightStrength by animateFloatAsState(
        targetValue =
            when (renderPhase) {
                LyricRenderPhase.Active -> 1f
                LyricRenderPhase.Leaving -> 0f
                LyricRenderPhase.Inactive -> 0f
            },
        animationSpec = tween(LyricUIConstants.WORD_HIGHLIGHT_FADE_DURATION),
        label = "wordsHighlightStrength",
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .clip(Shapes.ExtraLarge2CornerBasedShape)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.padding(
                    horizontal = style.horizontalPadding,
                    vertical = style.verticalPadding,
                ),
    ) {
        ProgressiveWordsText(
            text = sentence.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
            wordRanges = wordRanges,
            progressProvider = { range -> wordProgress(range.word, displayTime) },
            // 测量样式不含颜色：颜色随强调度逐帧动画，
            // 若参与测量 key 会导致滚动时测量结果每帧失效引发闪烁
            textStyle = style.wordsTextStyle,
            baseColor = baseTextColor,
            activeColor = activeTextColor,
            modifier = Modifier.fillMaxWidth(),
            highlightStrength = highlightStrength,
            cacheKey = lyric.key,
            measureCache = measureCache,
        )

        val translation = lyric.translation.firstOrNull { it.content.isNotBlank() }?.content
        if (!translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = translation,
                style = style.wordsTranslationTextStyle,
                color = translationColor,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 渐进式单词文本显示（带发光效果和进度控制）
 *
 * 渲染管线设计：
 * - 文本测量与颜色解耦：textStyle 不携带颜色，强调度动画不会使测量失效
 * - 测量结果优先从 [measureCache] 同步命中（由 LyricsUI 后台预热），
 *   miss 时才后台测量并回填缓存，避免滚动复用时闪烁占位文本
 * - 每词进度（progressProvider）在绘制阶段内联计算，不触发 recomposition
 * - Canvas 绘制层处理渐变高亮 + 发光 + 弹跳位移，全部在 draw phase 完成
 */
@Composable
private fun ProgressiveWordsText(
    text: String,
    wordRanges: List<WordCharRange>,
    progressProvider: (WordCharRange) -> Float,
    textStyle: TextStyle,
    baseColor: Color,
    activeColor: Color,
    cacheKey: String,
    measureCache: MutableMap<String, List<MeasuredWord>>,
    modifier: Modifier = Modifier,
    highlightStrength: Float = 1f,
) {
    if (wordRanges.isEmpty()) {
        Text(
            text = text,
            style = textStyle,
            color = baseColor,
            modifier = modifier,
        )
        return
    }

    // cacheSize=0：禁用内部缓存，确保相同字符各自持有独立的 TextLayoutResult 实例，
    // 避免 drawText(shadow=...) 修改共享 MultiParagraph 内部 TextPaint 导致渲染污染
    val textMeasurer = rememberTextMeasurer(cacheSize = 0)

    // 优先同步命中缓存；miss 时后台测量后回填（仅发生在预热尚未覆盖时）
    var measuredWords by remember(text, wordRanges, textStyle) {
        mutableStateOf(measureCache[cacheKey])
    }

    LaunchedEffect(text, wordRanges, textStyle) {
        if (measuredWords != null) return@LaunchedEffect
        val result =
            withContext(Dispatchers.Default) {
                measureWordRanges(textMeasurer, wordRanges, textStyle)
            }
        measureCache.putIfAbsent(cacheKey, result)
        measuredWords = result
    }

    val density = LocalDensity.current
    val highlightColor = activeColor.copy(alpha = activeColor.alpha * highlightStrength)
    val wordTranslationYPx = with(density) { LyricUIConstants.WORD_TRANSLATION_Y.dp.toPx() }

    val words = measuredWords
    if (words == null || words.size != wordRanges.size) {
        // 测量完成前显示无样式占位文本，避免空白闪烁
        Text(text = text, style = textStyle, color = baseColor, modifier = modifier)
        return
    }

    FlowRow(
        modifier = modifier,
    ) {
        words.forEachIndexed { index, measured ->
            val range = wordRanges[index]

            Canvas(
                modifier =
                    Modifier
                        .graphicsLayer {
                            // 进度驱动的弹跳位移，在 graphics layer 中读取以减少 recomposition
                            val progress = progressProvider(range)
                            translationY = progress * wordTranslationYPx * highlightStrength
                        }.size(
                            with(density) {
                                Size(
                                    width = measured.box.width,
                                    height = measured.box.height - wordTranslationYPx,
                                ).toDpSize()
                            },
                        ),
            ) {
                // ── 绘制阶段：每帧仅执行 draw 操作，无测量 ──
                val progress = progressProvider(range)

                val fadeCenter = measured.box.left + measured.box.width * progress
                val fadeWidth = measured.box.width * 0.25f
                val fadeStart =
                    ((fadeCenter - fadeWidth / 2 - measured.box.left) / measured.box.width)
                        .coerceIn(0f, 1f)
                val fadeEnd =
                    ((fadeCenter + fadeWidth / 2 - measured.box.left) / measured.box.width)
                        .coerceIn(0f, 1f)

                val colorStops =
                    arrayOf(
                        0.0f to highlightColor,
                        fadeStart to highlightColor,
                        fadeEnd to baseColor,
                        1.0f to baseColor,
                    )

                // 底层文字（测量样式不含颜色，绘制时显式指定）
                drawText(measured.layout, color = baseColor)

                // 高亮层（渐变 + 发光）
                clipRect(
                    measured.box.left,
                    measured.box.top + wordTranslationYPx,
                    size.width,
                    measured.box.bottom + wordTranslationYPx,
                ) {
                    drawText(
                        measured.layout,
                        brush =
                            Brush.horizontalGradient(
                                colorStops = colorStops,
                                startX = 0f,
                                endX = size.width,
                            ),
                        shadow =
                            Shadow(
                                color = highlightColor.copy(alpha = LyricUIConstants.WORD_GLOW_ALPHA * progress),
                                blurRadius =
                                    LyricUIConstants.WORD_GLOW_BLUR_RADIUS *
                                        EaseOutBounce.transform(progress),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * 逐词测量（线程安全，可在后台线程调用）
 */
private fun measureWordRanges(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    wordRanges: List<WordCharRange>,
    textStyle: TextStyle,
): List<MeasuredWord> =
    wordRanges.map { range ->
        val layoutResult =
            textMeasurer.measure(
                text = range.word.content,
                style = textStyle,
            )
        val boundingBox = wordBoundingBox(layoutResult, 0, range.word.content.length)
        MeasuredWord(layoutResult, boundingBox)
    }

/**
 * 预测量的单词数据（不可变，可安全跨帧复用）
 */
private data class MeasuredWord(
    val layout: TextLayoutResult,
    val box: androidx.compose.ui.geometry.Rect,
)

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
                    .clip(Shapes.ExtraLarge1CornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        IconButton(
            onClick = onSeek,
            modifier =
                Modifier
                    .width(88.dp)
                    .height(44.dp),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.seek_to_lyric_line),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

// ==================== 工具函数 ====================

/**
 * 计算把目标行顶对齐到视口锚点(35% 高度处)所需的滚动距离
 */
private fun LazyListState.distanceToAnchor(
    itemOffset: Int,
    viewportHeightPx: Int,
): Float {
    val anchorY =
        layoutInfo.viewportStartOffset +
            viewportHeightPx * LyricUIConstants.SCROLL_VIEWPORT_OFFSET_RATIO
    return itemOffset - anchorY
}

/**
 * 无动画精确定位到目标歌词行
 *
 * 先跳转让目标行完成布局，再按真实偏移对齐到视口锚点，
 * 不依赖平均行高估算，任意距离都能准确落位。用于打开歌词时的首次同步
 */
private suspend fun LazyListState.snapToLyricIndex(
    targetIndex: Int,
    viewportHeightPx: Int,
) {
    if (viewportHeightPx <= 0 || targetIndex < 0) return
    scrollToItem(targetIndex)
    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex } ?: return
    scrollBy(distanceToAnchor(targetItem.offset, viewportHeightPx))
}

/**
 * LazyListState 缓慢滚动扩展函数
 * 实现平滑的歌词切换动画
 *
 * 目标行可见时使用真实布局偏移，行高差异不会造成落点漂移；
 * 不可见时按平均行高估算滚动，结束后再按真实布局二次校正落点
 */
private suspend fun LazyListState.slowScrollToIndex(
    targetIndex: Int,
    viewportHeightPx: Int,
    durationMillis: Int = LyricUIConstants.SCROLL_ANIMATION_DURATION,
) {
    if (viewportHeightPx <= 0 || targetIndex < 0) return
    val layoutInfoSnapshot = layoutInfo
    val targetItem = layoutInfoSnapshot.visibleItemsInfo.firstOrNull { it.index == targetIndex }
    val firstVisibleItem = layoutInfoSnapshot.visibleItemsInfo.firstOrNull() ?: return

    val distance =
        if (targetItem != null) {
            // 目标行已可见：用真实偏移把行顶对齐到视口锚点
            distanceToAnchor(targetItem.offset, viewportHeightPx)
        } else {
            // 目标行不可见：以首个可见行的真实偏移为基准，按平均行高外推
            val averageItemSize =
                layoutInfoSnapshot.visibleItemsInfo
                    .map { it.size }
                    .average()
                    .toFloat()
                    .takeIf { it > 0f } ?: (viewportHeightPx / 6f)
            val estimatedTargetOffset =
                firstVisibleItem.offset + (targetIndex - firstVisibleItem.index) * averageItemSize
            distanceToAnchor(estimatedTargetOffset.toInt(), viewportHeightPx)
        }

    if (abs(distance) >= 0.5f) {
        animateScrollBy(
            value = distance,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearOutSlowInEasing),
        )
    }

    if (targetItem != null) return

    // 估算滚动的落点校正：此时目标行应已可见，按真实偏移修正残差
    val settledItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
    if (settledItem == null) {
        snapToLyricIndex(targetIndex, viewportHeightPx)
        return
    }
    val residual = distanceToAnchor(settledItem.offset, viewportHeightPx)
    if (abs(residual) >= 0.5f) {
        animateScrollBy(
            value = residual,
            animationSpec =
                tween(
                    durationMillis = LyricUIConstants.SCROLL_CORRECTION_DURATION,
                    easing = LinearOutSlowInEasing,
                ),
        )
    }
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
