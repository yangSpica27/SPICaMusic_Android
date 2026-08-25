package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutBounce
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.layout
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.findPlayingIndex
import me.spica27.spicamusic.common.entity.voiceAgents
import me.spica27.spicamusic.ui.theme.Shapes
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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

    const val SEEK_OVERLAY_HIDE_DELAY = 450L

    const val SCROLL_VIEWPORT_OFFSET_RATIO = 0.28f

    const val BASE_TEXT_ALPHA = 0.24f
    const val TRANSLATION_TEXT_ALPHA = 0.72f
    const val ACTIVE_TRANSLATION_ALPHA = 0.85f
    const val INACTIVE_TRANSLATION_ALPHA = 0.8f

    const val WORD_GLOW_ALPHA = 0.3f
    const val WORD_GLOW_BLUR_RADIUS = 10f
    const val WORD_TRANSLATION_Y = -1.5f
    const val SLOW_WORD_MIN_DURATION_MS = 1_000L
    const val SLOW_WORD_CHAR_DURATION_THRESHOLD_MS = 200f
    const val SLOW_WORD_ANIMATION_DURATION_RATIO = 0.8f
    const val SLOW_WORD_MAX_SCALE_INCREASE = 0.1f
    const val SLOW_WORD_MAX_DIP = 0.5f
    const val SLOW_WORD_MAX_FLOAT_OFFSET_PX = 4f
    const val ACCOMPANIMENT_VISIBILITY_PADDING_MS = 600L
    const val ACCOMPANIMENT_ANIMATION_DURATION_MS = 480

    val KEEP_ALIVE_ZONE = 100.dp
}

// ==================== 显示模式 ====================

/**
 * 歌词展示形态
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
    val phoneticTextStyle: TextStyle,
    val accompanimentTextStyle: TextStyle,
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
                    phoneticTextStyle = typography.bodySmall,
                    accompanimentTextStyle = typography.titleMedium,
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
                    phoneticTextStyle = typography.labelSmall,
                    accompanimentTextStyle = typography.bodyMedium,
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
    isSynced: Boolean = true,
    onSeekToTime: (Long) -> Unit = {},
) {
    val lyricLines = remember(lyric) { lyric.sortedBy { it.time } }

    if (lyricLines.isEmpty()) {
        EmptyLyricState(modifier)
        return
    }

    // 无时间戳的纯文本歌词（内嵌/本地常见）：静态可滚动展示，不高亮、不跟随、不可 seek
    if (!isSynced) {
        PlainLyricsList(modifier = modifier, lines = lyricLines, displayMode = displayMode)
        return
    }

    val style = rememberLyricsUIStyle(displayMode)
    val lazyListState = rememberLazyListState()
    var isAutoScrolling by remember { mutableStateOf(false) }
    val isManualScrolling by remember {
        derivedStateOf { lazyListState.isScrollInProgress && !isAutoScrolling }
    }
    var showSeekOverlay by remember { mutableStateOf(false) }
    // 首次显示时列表停在顶部，需要一次无动画的精确定位；之后的行切换才走动画滚动
    var hasSyncedInitialPosition by remember(lyricLines) { mutableStateOf(false) }
    // currentTime 每帧变化，用 rememberUpdatedState 包装后作为稳定实例供 derivedStateOf 读取；
    // derivedStateOf 只在计算结果（行索引）变化时才通知依赖方重组
    val currentTimeState = rememberUpdatedState(currentTime)
    val playingIndex by remember(lyricLines) {
        derivedStateOf { lyricLines.findPlayingIndex(currentTimeState.value) }
    }
    val voiceIds =
        remember(lyricLines) {
            lyricLines
                .flatMap { it.voiceAgents() }
                .map { it.id }
                .distinct()
        }
    val voiceIndexById =
        remember(voiceIds) {
            voiceIds.withIndex().associate { (index, id) -> id to index }
        }
    val activeLineIndices by remember(lyricLines) {
        derivedStateOf {
            val time = currentTimeState.value
            lyricLines
                .indices
                .filterTo(mutableSetOf()) { index -> lyricLines[index].isActiveAt(time) }
        }
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

    LookaheadScope {
        BoxWithConstraints(modifier = modifier) {
            val colorScheme = MaterialTheme.colorScheme
            val density = LocalDensity.current
            // 手动选择线位于视口中央，边缘歌词需要半个视口的滚动留白。
            val selectionPadding = with(density) { constraints.maxHeight.toDp() / 2 }
            val playbackAnchorOffsetPx =
                constraints.maxHeight * LyricUIConstants.SCROLL_VIEWPORT_OFFSET_RATIO
            val keepAliveZone = LyricUIConstants.KEEP_ALIVE_ZONE
            val keepAliveZonePx = with(density) { keepAliveZone.toPx() }

            LaunchedEffect(playingIndex, showSeekOverlay, lyricLines) {
                val target = playingIndex
                if (showSeekOverlay || target == Int.MAX_VALUE || target !in lyricLines.indices) {
                    return@LaunchedEffect
                }
                isAutoScrolling = true
                try {
                    if (hasSyncedInitialPosition) {
                        lazyListState.syncToLyricIndex(
                            targetIndex = target,
                            anchorOffsetPx = (playbackAnchorOffsetPx + keepAliveZonePx).roundToInt(),
                        )
                    } else {
                        // 首次同步：目标行大概率在视口外，动画+估算必然有落点偏差，直接精确跳转
                        lazyListState.snapToLyricIndex(
                            targetIndex = target,
                            anchorOffsetPx = (playbackAnchorOffsetPx + keepAliveZonePx).roundToInt(),
                        )
                    }
                    hasSyncedInitialPosition = true
                } finally {
                    isAutoScrolling = false
                }
            }

            val highlightedIndex = if (showSeekOverlay) previewIndex else playingIndex

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clipToBounds()
                        .layout { measurable, constraints ->
                            val extraHeightPx = (keepAliveZone * 2).roundToPx()
                            val placeable =
                                measurable.measure(
                                    constraints.copy(maxHeight = constraints.maxHeight + extraHeightPx),
                                )
                            layout(constraints.maxWidth, constraints.maxHeight) {
                                placeable.place(0, -keepAliveZone.roundToPx())
                            }
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(style.itemSpacing),
                contentPadding =
                    PaddingValues(
                        vertical = selectionPadding + keepAliveZone,
                        horizontal = LyricUIConstants.SEEK_PREVIEW_END_PADDING.dp,
                    ),
                state = lazyListState,
            ) {
                itemsIndexed(
                    items = lyricLines,
                    key = { _, line -> line.key },
                ) { index, line ->
                    val isActive =
                        index == highlightedIndex ||
                            (!showSeekOverlay && index in activeLineIndices)
                    val distanceFromActive = if (isActive) 0 else calculateDistance(index, highlightedIndex)
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

                    val springStiffness =
                        (120f - (distanceFromActive * 20f)).coerceAtLeast(20f)

                    LyricItemWrapper(
                        modifier =
                            Modifier.springPlacement(
                                lookaheadScope = this@LookaheadScope,
                                itemKey = line.key,
                                isManualScrolling = isManualScrolling,
                                stiffness = springStiffness,
                            ),
                    ) {
                        val lineAgents = line.voiceAgents()
                        val lineVoiceIndex = lineAgents.firstOrNull()?.id?.let(voiceIndexById::get)
                        val lineAccent =
                            if (voiceIds.size > 1) voiceAccent(lineVoiceIndex, colorScheme) else null
                        val alignEnd = lineVoiceIndex?.let(::voiceAlignEnd) == true
                        when (line) {
                            is LyricItem.NormalLyric ->
                                LyricLine(
                                    lyric = line,
                                    isActive = isActive,
                                    alpha = alpha,
                                    scale = scale,
                                    blurRadius = blurRadius,
                                    style = style,
                                    voiceAccent = lineAccent,
                                    alignEnd = alignEnd,
                                )

                            is LyricItem.WordsLyric -> {
                                WordsLyricLine(
                                    lyric = line,
                                    currentTime = currentTime,
                                    alpha = alpha,
                                    scale = scale,
                                    blurRadius = blurRadius,
                                    style = style,
                                    measureCache = wordsMeasureCache,
                                    agents = lineAgents,
                                    voiceAccent = lineAccent,
                                    alignEnd = alignEnd,
                                    voiceIndexById = voiceIndexById,
                                    colorScheme = colorScheme,
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
}

// ==================== 辅助组件 ====================

/**
 * 纯文本歌词列表（无时间戳）
 *
 * 内嵌 / 本地歌词常为无时间轴的整段文本，此处静态居中展示、可自由滚动，
 * 不做行高亮、自动跟随与点按 seek——这些都依赖时间戳。
 */
@Composable
private fun PlainLyricsList(
    modifier: Modifier = Modifier,
    lines: List<LyricItem>,
    displayMode: LyricsDisplayMode,
) {
    val style = rememberLyricsUIStyle(displayMode)
    val textColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(style.itemSpacing),
        contentPadding =
            PaddingValues(
                horizontal = style.horizontalPadding,
                vertical = style.verticalPadding,
            ),
    ) {
        itemsIndexed(
            items = lines,
            key = { _, line -> line.key },
        ) { _, line ->
            val content =
                when (line) {
                    is LyricItem.NormalLyric -> line.content
                    is LyricItem.WordsLyric -> line.words.joinToString(separator = "") { it.content }
                }
            Text(
                text = content.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
                style = style.mainTextStyle,
                fontWeight = FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

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
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier,
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
    voiceAccent: Color?,
    alignEnd: Boolean,
) {
    // Emphasis is applied once by the row graphics layer below. Keep the
    // per-text alpha here independent so inactive rows are not dimmed twice.
    val inactiveTextColor = MaterialTheme.colorScheme.onSurface
    val activeTextColor = voiceAccent ?: MaterialTheme.colorScheme.onSurface
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start

    Column(
        horizontalAlignment = horizontalAlignment,
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
            textAlign = textAlign,
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
                textAlign = textAlign,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!lyric.phonetic.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lyric.phonetic!!,
                style = style.phoneticTextStyle,
                color = (if (isActive) activeTextColor else inactiveTextColor).copy(alpha = 0.72f),
                textAlign = textAlign,
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
    val duration = (word.endTime - word.startTime).coerceAtLeast(1L)
    return ((time - word.startTime).toFloat() / duration).coerceIn(0f, 1f)
}

private fun slowWordAnimationIntensity(word: LyricItem.WordsLyric.WordWithTiming): Float {
    val characterCount = word.content.length
    val duration = word.endTime - word.startTime
    if (
        characterCount == 0 ||
        duration < LyricUIConstants.SLOW_WORD_MIN_DURATION_MS ||
        duration.toFloat() / characterCount <= LyricUIConstants.SLOW_WORD_CHAR_DURATION_THRESHOLD_MS ||
        word.content.shouldUseSimpleWordAnimation()
    ) {
        return 0f
    }

    return (
        duration -
            LyricUIConstants.SLOW_WORD_CHAR_DURATION_THRESHOLD_MS * characterCount
    ) / 1_000f
}

private fun slowWordScaleAmplitude(word: LyricItem.WordsLyric.WordWithTiming): Float =
    (LyricUIConstants.SLOW_WORD_MAX_SCALE_INCREASE * slowWordAnimationIntensity(word))
        .coerceIn(0f, LyricUIConstants.SLOW_WORD_MAX_SCALE_INCREASE)

private fun slowWordDipAmplitude(word: LyricItem.WordsLyric.WordWithTiming): Float =
    (LyricUIConstants.SLOW_WORD_MAX_DIP * slowWordAnimationIntensity(word))
        .coerceIn(0f, LyricUIConstants.SLOW_WORD_MAX_DIP)

private fun slowWordCharacterAnimationProgress(
    word: LyricItem.WordsLyric.WordWithTiming,
    wordProgress: Float,
    characterIndex: Int,
    characterCount: Int,
): Float {
    if (characterCount == 0) return 1f

    val duration = (word.endTime - word.startTime).coerceAtLeast(1L).toFloat()
    val animationDuration = duration * LyricUIConstants.SLOW_WORD_ANIMATION_DURATION_RATIO
    val characterRatio =
        if (characterCount > 1) characterIndex.toFloat() / (characterCount - 1) else 0.5f
    val startOffset = (duration - animationDuration) * characterRatio
    return ((wordProgress * duration - startOffset) / animationDuration).coerceIn(0f, 1f)
}

private fun slowWordCharacterScale(
    animationProgress: Float,
    amplitude: Float,
): Float {
    if (amplitude <= 0f) return 1f

    // The reference Swell easing is the parabola through (0, 0), (0.5, amplitude), (1, 0).
    val swell = 4f * amplitude * animationProgress * (1f - animationProgress)
    return 1f + swell
}

private fun slowWordCharacterFloatOffset(
    animationProgress: Float,
    dipAmplitude: Float,
): Float {
    if (dipAmplitude <= 0f) return 0f

    // Reference DipAndRise: (0, 0), (0.5, -dip), (1, 1), evaluated in reverse time.
    val reversedProgress = 1f - animationProgress
    val dipAndRise =
        (2f + 4f * dipAmplitude) * reversedProgress * reversedProgress -
            (1f + 4f * dipAmplitude) * reversedProgress
    return LyricUIConstants.SLOW_WORD_MAX_FLOAT_OFFSET_PX * dipAndRise
}

private fun String.shouldUseSimpleWordAnimation(): Boolean {
    val significantCharacters = filterNot { it.isWhitespace() || it.isLyricPunctuation() }
    if (significantCharacters.isEmpty()) return false

    val scripts = significantCharacters.map { Character.UnicodeScript.of(it.code) }
    val isPureCjk =
        scripts.all {
            it == Character.UnicodeScript.HAN ||
                it == Character.UnicodeScript.HIRAGANA ||
                it == Character.UnicodeScript.KATAKANA ||
                it == Character.UnicodeScript.HANGUL
        }
    return isPureCjk ||
        scripts.any {
            it == Character.UnicodeScript.ARABIC || it == Character.UnicodeScript.DEVANAGARI
        }
}

private fun Char.isLyricPunctuation(): Boolean =
    when (Character.getType(this)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        -> true

        else -> false
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
    alpha: Float,
    scale: Float,
    blurRadius: Dp,
    style: LyricsUIStyle,
    measureCache: MutableMap<String, List<MeasuredWord>>,
    agents: List<LyricItem.Agent>,
    voiceAccent: Color?,
    alignEnd: Boolean,
    voiceIndexById: Map<String, Int>,
    colorScheme: androidx.compose.material3.ColorScheme,
) {
    val activeTextColor = voiceAccent ?: MaterialTheme.colorScheme.onSurface
    val baseTextColor = activeTextColor.copy(alpha = LyricUIConstants.BASE_TEXT_ALPHA)
    val translationColor =
        activeTextColor.copy(alpha = LyricUIConstants.TRANSLATION_TEXT_ALPHA)
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    val sortedWords = remember(lyric) { lyric.words.sortedBy { it.startTime } }
    val sentence = remember(sortedWords) { sortedWords.joinToString(separator = "") { it.content } }
    val accompaniment = remember(lyric) { lyric.accompaniment.sortedBy { it.startTime } }
    val wordRanges = remember(sortedWords) { buildWordRanges(sortedWords) }
    val (beforeAccompaniment, afterAccompaniment) =
        remember(accompaniment, lyric.startTime) {
            accompaniment.partition { it.startTime < lyric.startTime }
        }
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier =
            Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .clip(Shapes.ExtraLarge2CornerBasedShape)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                }.padding(
                    horizontal = style.horizontalPadding,
                    vertical = style.verticalPadding,
                ),
    ) {
        beforeAccompaniment.forEachIndexed { index, background ->
            AccompanimentLine(
                background = background,
                currentTime = currentTime,
                placement = AccompanimentPlacement.Before,
                parentKey = lyric.key,
                index = index,
                style = style,
                measureCache = measureCache,
                parentAgents = agents,
                voiceIndexById = voiceIndexById,
                colorScheme = colorScheme,
                fallbackAccent = voiceAccent,
                fallbackAlignEnd = alignEnd,
            )
        }

        ProgressiveWordsText(
            text = sentence.ifBlank { LyricUIConstants.EMPTY_WORD_PLACEHOLDER },
            wordRanges = wordRanges,
            progressProvider = { range -> wordProgress(range.word, currentTime) },
            // 测量样式不含颜色：强调度由外层图层处理，逐字进度只在绘制阶段读取，
            // 避免滚动时测量结果因播放状态变化而失效并引发闪烁
            textStyle = style.wordsTextStyle,
            baseColor = baseTextColor,
            activeColor = activeTextColor,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
            cacheKey = lyric.key,
            measureCache = measureCache,
        )

        if (!lyric.phonetic.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = lyric.phonetic!!,
                style = style.phoneticTextStyle,
                color = activeTextColor.copy(alpha = 0.7f),
                textAlign = textAlign,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val translation = lyric.translation.firstOrNull { it.content.isNotBlank() }?.content
        if (!translation.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = translation,
                style = style.wordsTranslationTextStyle,
                color = translationColor,
                textAlign = textAlign,
                overflow = TextOverflow.Ellipsis,
            )
        }

        afterAccompaniment.forEachIndexed { index, background ->
            AccompanimentLine(
                background = background,
                currentTime = currentTime,
                placement = AccompanimentPlacement.After,
                parentKey = lyric.key,
                index = index,
                style = style,
                measureCache = measureCache,
                parentAgents = agents,
                voiceIndexById = voiceIndexById,
                colorScheme = colorScheme,
                fallbackAccent = voiceAccent,
                fallbackAlignEnd = alignEnd,
            )
        }
    }
}

private enum class AccompanimentPlacement {
    Before,
    After,
}

@Composable
private fun AccompanimentLine(
    background: LyricItem.WordsLyric.AccompanimentLyric,
    currentTime: Long,
    placement: AccompanimentPlacement,
    parentKey: String,
    index: Int,
    style: LyricsUIStyle,
    measureCache: MutableMap<String, List<MeasuredWord>>,
    parentAgents: List<LyricItem.Agent>,
    voiceIndexById: Map<String, Int>,
    colorScheme: androidx.compose.material3.ColorScheme,
    fallbackAccent: Color?,
    fallbackAlignEnd: Boolean,
) {
    val backgroundAgents = background.voiceAgents().ifEmpty { parentAgents }
    val voiceIndex = backgroundAgents.firstOrNull()?.id?.let(voiceIndexById::get)
    val accent = voiceAccent(voiceIndex, colorScheme) ?: fallbackAccent
    val alignEnd = voiceIndex?.let(::voiceAlignEnd) ?: fallbackAlignEnd
    val horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    val textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
    val words = remember(background) { background.words.sortedBy { it.startTime } }
    if (words.isEmpty()) return

    val visible =
        currentTime >= background.startTime - LyricUIConstants.ACCOMPANIMENT_VISIBILITY_PADDING_MS &&
            currentTime <= background.endTime + LyricUIConstants.ACCOMPANIMENT_VISIBILITY_PADDING_MS
    val pivotX = if (alignEnd) 1f else 0f
    val pivotY = if (placement == AccompanimentPlacement.Before) 0f else 1f
    val alignment = if (placement == AccompanimentPlacement.Before) Alignment.Top else Alignment.Bottom

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxWidth(),
        enter =
            fadeIn(tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS)) +
                scaleIn(
                    initialScale = 0.88f,
                    transformOrigin = TransformOrigin(pivotX, pivotY),
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ) +
                slideInVertically(
                    initialOffsetY = { height -> if (placement == AccompanimentPlacement.Before) -height / 3 else height / 3 },
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ) +
                expandVertically(
                    expandFrom = alignment,
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ),
        exit =
            fadeOut(tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS)) +
                scaleOut(
                    targetScale = 0.88f,
                    transformOrigin = TransformOrigin(pivotX, pivotY),
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ) +
                slideOutVertically(
                    targetOffsetY = { height -> if (placement == AccompanimentPlacement.Before) -height / 3 else height / 3 },
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ) +
                shrinkVertically(
                    shrinkTowards = alignment,
                    animationSpec = tween(LyricUIConstants.ACCOMPANIMENT_ANIMATION_DURATION_MS),
                ),
    ) {
        Column(horizontalAlignment = horizontalAlignment, modifier = Modifier.fillMaxWidth()) {
            ProgressiveWordsText(
                text = words.joinToString(separator = "") { it.content },
                wordRanges = remember(words) { buildWordRanges(words) },
                progressProvider = { range -> wordProgress(range.word, currentTime) },
                textStyle = style.accompanimentTextStyle,
                baseColor = (accent ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.42f),
                activeColor = (accent ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.88f),
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
                cacheKey = "$parentKey:accompaniment:$index:${background.startTime}",
                measureCache = measureCache,
            )

            if (!background.phonetic.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = background.phonetic!!,
                    style = style.phoneticTextStyle,
                    color = (accent ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.52f),
                    textAlign = textAlign,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val translation = background.translation.firstOrNull { it.content.isNotBlank() }?.content
            if (!translation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = translation,
                    style = style.wordsTranslationTextStyle,
                    color = (accent ?: MaterialTheme.colorScheme.onSurface).copy(alpha = 0.58f),
                    textAlign = textAlign,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    textStyle: TextStyle,
    baseColor: Color,
    activeColor: Color,
    cacheKey: String,
    measureCache: MutableMap<String, List<MeasuredWord>>,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
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
    val highlightColor = activeColor
    val wordTranslationYPx = with(density) { LyricUIConstants.WORD_TRANSLATION_Y.dp.toPx() }

    val words = measuredWords
    if (words == null || words.size != wordRanges.size) {
        // 测量完成前显示无样式占位文本，避免空白闪烁
        Text(text = text, style = textStyle, color = baseColor, modifier = modifier)
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
    ) {
        words.forEachIndexed { index, measured ->
            val range = wordRanges[index]

            Canvas(
                modifier =
                    Modifier
                        .graphicsLayer {
                            // 进度驱动的弹跳位移，在 graphics layer 中读取以减少 recomposition
                            val progress = progressProvider(range)
                            translationY = progress * wordTranslationYPx
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
                val hasSlowWordAnimation =
                    measured.scaleAmplitude > 0f &&
                        measured.characterLayouts.size == measured.characterBounds.size
                val wordPivot = Offset(measured.box.center.x, measured.box.bottom)
                val scaleOverflowX = measured.box.width * measured.scaleAmplitude / 2f
                val scaleOverflowY = measured.box.height * measured.scaleAmplitude
                val floatOffsetOverflow =
                    if (measured.dipAmplitude > 0f) {
                        LyricUIConstants.SLOW_WORD_MAX_FLOAT_OFFSET_PX
                    } else {
                        0f
                    }
                val dipOverflowY = measured.dipAmplitude * LyricUIConstants.SLOW_WORD_MAX_FLOAT_OFFSET_PX

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
                val highlightBrush =
                    Brush.horizontalGradient(
                        colorStops = colorStops,
                        startX = 0f,
                        endX = size.width,
                    )
                val glow =
                    Shadow(
                        color = highlightColor.copy(alpha = LyricUIConstants.WORD_GLOW_ALPHA * progress),
                        blurRadius =
                            LyricUIConstants.WORD_GLOW_BLUR_RADIUS *
                                EaseOutBounce.transform(progress),
                    )

                // 底层文字（测量样式不含颜色，绘制时显式指定）
                if (hasSlowWordAnimation) {
                    measured.characterLayouts.forEachIndexed { characterIndex, characterLayout ->
                        val characterBox = measured.characterBounds[characterIndex]
                        val animationProgress =
                            slowWordCharacterAnimationProgress(
                                word = range.word,
                                wordProgress = progress,
                                characterIndex = characterIndex,
                                characterCount = measured.characterLayouts.size,
                            )
                        val scale =
                            slowWordCharacterScale(
                                animationProgress = animationProgress,
                                amplitude = measured.scaleAmplitude,
                            )
                        val floatOffset =
                            slowWordCharacterFloatOffset(animationProgress, measured.dipAmplitude)
                        val centeredOffsetX = (characterBox.width - characterLayout.size.width) / 2f
                        val characterPosition =
                            Offset(
                                x = characterBox.left + centeredOffsetX,
                                y = characterBox.top + floatOffset,
                            )
                        withTransform({ scale(scaleX = scale, scaleY = scale, pivot = wordPivot) }) {
                            drawText(
                                textLayoutResult = characterLayout,
                                color = baseColor,
                                topLeft = characterPosition,
                            )
                        }
                    }
                } else {
                    drawText(measured.layout, color = baseColor)
                }

                // 高亮层（渐变 + 发光）
                clipRect(
                    measured.box.left - scaleOverflowX,
                    measured.box.top + wordTranslationYPx - scaleOverflowY - dipOverflowY,
                    size.width + scaleOverflowX,
                    measured.box.bottom + wordTranslationYPx + floatOffsetOverflow,
                ) {
                    if (hasSlowWordAnimation) {
                        measured.characterLayouts.forEachIndexed { characterIndex, characterLayout ->
                            val characterBox = measured.characterBounds[characterIndex]
                            val animationProgress =
                                slowWordCharacterAnimationProgress(
                                    word = range.word,
                                    wordProgress = progress,
                                    characterIndex = characterIndex,
                                    characterCount = measured.characterLayouts.size,
                                )
                            val scale =
                                slowWordCharacterScale(
                                    animationProgress = animationProgress,
                                    amplitude = measured.scaleAmplitude,
                                )
                            val floatOffset =
                                slowWordCharacterFloatOffset(animationProgress, measured.dipAmplitude)
                            val centeredOffsetX = (characterBox.width - characterLayout.size.width) / 2f
                            val characterPosition =
                                Offset(
                                    x = characterBox.left + centeredOffsetX,
                                    y = characterBox.top + floatOffset,
                                )
                            withTransform({ scale(scaleX = scale, scaleY = scale, pivot = wordPivot) }) {
                                drawText(
                                    textLayoutResult = characterLayout,
                                    brush = highlightBrush,
                                    topLeft = characterPosition,
                                    shadow = glow,
                                )
                            }
                        }
                    } else {
                        drawText(
                            measured.layout,
                            brush = highlightBrush,
                            shadow = glow,
                        )
                    }
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
        val scaleAmplitude = slowWordScaleAmplitude(range.word)
        val dipAmplitude = slowWordDipAmplitude(range.word)
        val characterLayouts =
            if (scaleAmplitude > 0f || dipAmplitude > 0f) {
                range.word.content.map { character ->
                    textMeasurer.measure(text = character.toString(), style = textStyle)
                }
            } else {
                emptyList()
            }
        val characterBounds =
            if (scaleAmplitude > 0f || dipAmplitude > 0f) {
                range.word.content.indices
                    .map(layoutResult::getBoundingBox)
            } else {
                emptyList()
            }
        MeasuredWord(
            layout = layoutResult,
            box = boundingBox,
            scaleAmplitude = scaleAmplitude,
            dipAmplitude = dipAmplitude,
            characterLayouts = characterLayouts,
            characterBounds = characterBounds,
        )
    }

/**
 * 预测量的单词数据（不可变，可安全跨帧复用）
 */
private data class MeasuredWord(
    val layout: TextLayoutResult,
    val box: androidx.compose.ui.geometry.Rect,
    val scaleAmplitude: Float,
    val dipAmplitude: Float,
    val characterLayouts: List<TextLayoutResult>,
    val characterBounds: List<androidx.compose.ui.geometry.Rect>,
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
    anchorOffsetPx: Int,
): Float {
    val anchorY = layoutInfo.viewportStartOffset + anchorOffsetPx
    return (itemOffset - anchorY).toFloat()
}

/**
 * 无动画精确定位到目标歌词行
 *
 * 先跳转让目标行完成布局，再按真实偏移对齐到视口锚点，
 * 不依赖平均行高估算，任意距离都能准确落位。用于打开歌词时的首次同步
 */
private suspend fun LazyListState.snapToLyricIndex(
    targetIndex: Int,
    anchorOffsetPx: Int,
) {
    if (anchorOffsetPx < 0 || targetIndex < 0) return
    // Put the target directly at the playback anchor. Passing the offset to
    // scrollToItem avoids the multi-step estimation used by
    // animateScrollToItem, which can visibly travel through the list end for
    // far-away variable-height lyric rows.
    scrollToItem(targetIndex, -anchorOffsetPx)
    val targetItem =
        snapshotFlow {
            layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        }.first { it != null } ?: return
    scrollBy(distanceToAnchor(targetItem.offset, anchorOffsetPx))
}

/**
 * Moves the list to the next lyric without estimating item heights. Visible
 * items are corrected immediately; off-screen targets are snapped directly to
 * the anchor so LazyList cannot expose an intermediate end-of-list position.
 */
private suspend fun LazyListState.syncToLyricIndex(
    targetIndex: Int,
    anchorOffsetPx: Int,
) {
    if (targetIndex < 0 || anchorOffsetPx < 0) return

    val targetItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
    if (targetItem != null) {
        val distance = distanceToAnchor(targetItem.offset, anchorOffsetPx)
        if (abs(distance) >= 0.5f) {
            scrollBy(distance)
        }
    } else {
        scrollToItem(targetIndex, -anchorOffsetPx)
        val settledItem =
            withTimeoutOrNull(500L) {
                snapshotFlow {
                    layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
                }.first { it != null }
            }

        if (settledItem == null) {
            snapToLyricIndex(targetIndex, anchorOffsetPx)
            return
        }

        val residual = distanceToAnchor(settledItem.offset, anchorOffsetPx)
        if (abs(residual) >= 0.5f) {
            scrollBy(residual)
        }
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

private fun LyricItem.isActiveAt(time: Long): Boolean =
    when (this) {
        is LyricItem.NormalLyric -> false
        is LyricItem.WordsLyric -> time >= startTime && time <= endTime
    }

/**
 * 对应歌手的字体颜色
 */
@Composable
private fun voiceAccent(
    index: Int?,
    colorScheme: androidx.compose.material3.ColorScheme,
): Color = MaterialTheme.colorScheme.onSurface

private fun voiceAlignEnd(index: Int): Boolean = index % 2 == 1

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
