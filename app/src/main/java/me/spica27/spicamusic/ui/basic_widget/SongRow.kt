package me.spica27.spicamusic.ui.basic_widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.artwork.MusicArtwork
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.combinedClickHighlight

/** 当前曲目暂停时仍保留高亮，只有律动动画停止。 */
enum class SongRowPlayback {
    None,
    CurrentPaused,
    CurrentPlaying,
}

/** 已完成本地化、关键词高亮和场景副标题组合的展示数据。 */
@Immutable
data class SongRowContent(
    val title: AnnotatedString,
    val subtitle: AnnotatedString,
    val artwork: MusicArtwork? = null,
    val durationText: String? = null,
    /** 非 null 时显示独立专辑列，由页面根据自身容器宽度决定是否提供。 */
    val albumText: String? = null,
)

@Immutable
sealed interface SongRowSelection {
    data object None : SongRowSelection

    data class MultiSelect(
        val selected: Boolean,
    ) : SongRowSelection

    data class Picker(
        val selected: Boolean,
    ) : SongRowSelection
}

@Immutable
data class SongRowStyle(
    val shape: Shape,
    val artworkShape: Shape,
    val titleTextStyle: TextStyle,
    val subtitleTextStyle: TextStyle,
    val durationTextStyle: TextStyle,
    val minHeight: Dp = 56.dp,
    val artworkSize: Dp = 44.dp,
    val horizontalPadding: Dp = 16.dp,
    val verticalPadding: Dp = 4.dp,
    val contentSpacing: Dp = 12.dp,
)

@Immutable
data class SongRowColors(
    val containerColor: Color,
    val titleColor: Color,
    val subtitleColor: Color,
    val currentTitleColor: Color,
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val disabledContainerColor: Color,
    val disabledContentColor: Color,
)

object SongRowDefaults {
    @Composable
    fun style(): SongRowStyle =
        SongRowStyle(
            shape = RectangleShape,
            artworkShape = MaterialTheme.shapes.small,
            titleTextStyle = MaterialTheme.typography.bodyLarge,
            subtitleTextStyle = MaterialTheme.typography.bodySmall,
            durationTextStyle = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
        )

    @Composable
    fun colors(): SongRowColors {
        val scheme = MaterialTheme.colorScheme
        return SongRowColors(
            containerColor = Color.Transparent,
            titleColor = scheme.onSurface,
            subtitleColor = scheme.onSurfaceVariant,
            currentTitleColor = scheme.primary,
            selectedContainerColor = scheme.primaryContainer,
            selectedContentColor = scheme.onPrimaryContainer,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
        )
    }
}

/** 仅用于让标准插槽组件继承所在行的样式和禁用状态。 */
@Immutable
internal data class SongRowEnvironment(
    val style: SongRowStyle,
    val colors: SongRowColors,
    val enabled: Boolean,
)

internal val LocalSongRowEnvironment = compositionLocalOf<SongRowEnvironment?> { null }

/**
 * 受控的歌曲行。播放、选中集合、菜单导航和拖动手势由调用方管理。
 */
@Composable
fun SongRow(
    content: SongRowContent,
    modifier: Modifier = Modifier,
    playback: SongRowPlayback = SongRowPlayback.None,
    selection: SongRowSelection = SongRowSelection.None,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    subtitleLeadingContent: (@Composable () -> Unit)? = null,
    style: SongRowStyle = SongRowDefaults.style().copy(shape = MaterialTheme.shapes.medium),
    colors: SongRowColors =
        SongRowDefaults.colors().copy(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
) {
    val selected =
        when (selection) {
            SongRowSelection.None -> false
            is SongRowSelection.MultiSelect -> selection.selected
            is SongRowSelection.Picker -> selection.selected
        }
    val multiSelect = selection is SongRowSelection.MultiSelect
    val picker = selection is SongRowSelection.Picker
    val reducedMotion = LocalReducedMotion.current
    val animationSpec =
        if (reducedMotion) {
            snap<Color>()
        } else {
            tween(durationMillis = SONG_ROW_STATE_ANIMATION_MILLIS, easing = EaseOutEmphasized)
        }
    val background =
        animateColorAsState(
            targetValue =
                when {
                    !enabled -> colors.disabledContainerColor
                    selected -> colors.selectedContainerColor
                    else -> colors.containerColor
                },
            animationSpec = animationSpec,
            label = "songRowBackground",
        )
    val titleColor =
        animateColorAsState(
            targetValue =
                when {
                    !enabled -> colors.disabledContentColor
                    selected -> colors.selectedContentColor
                    playback != SongRowPlayback.None -> colors.currentTitleColor
                    else -> colors.titleColor
                },
            animationSpec = animationSpec,
            label = "songRowTitle",
        )
    val supportingColor =
        when {
            !enabled -> colors.disabledContentColor
            selected -> colors.selectedContentColor
            else -> colors.subtitleColor
        }
    val subtitleColor =
        animateColorAsState(
            targetValue = supportingColor,
            animationSpec = animationSpec,
            label = "songRowSubtitle",
        )
    // 选择器和多选中的长按不再次进入选择模式。
    val longClick = onLongClick.takeIf { selection == SongRowSelection.None }
    val clickModifier =
        if (onClick != null || longClick != null) {
            Modifier.combinedClickHighlight(
                enabled = enabled,
                onClick = { onClick?.invoke() },
                onLongClick = longClick,
            )
        } else {
            Modifier
        }
    val showTrailing =
        playback != SongRowPlayback.None ||
            !content.durationText.isNullOrBlank() ||
            picker ||
            (selection == SongRowSelection.None && trailingContent != null)

    CompositionLocalProvider(
        LocalSongRowEnvironment provides SongRowEnvironment(style, colors, enabled),
        LocalContentColor provides supportingColor,
        LocalTextStyle provides style.subtitleTextStyle,
    ) {
        Row(
            modifier =
                modifier
                    .fillMaxWidth()
                    .clip(style.shape)
                    .drawBehind { drawRect(background.value) }
                    .then(clickModifier)
                    .heightIn(min = style.minHeight)
                    .padding(horizontal = style.horizontalPadding, vertical = style.verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(style.contentSpacing),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(style.contentSpacing),
            ) {
                AnimatedVisibility(
                    visible = multiSelect,
                    enter =
                        expandHorizontally(
                            animationSpec =
                                if (reducedMotion) {
                                    snap()
                                } else {
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                },
                            expandFrom = Alignment.Start,
                        ) + fadeIn(animationSpec = if (reducedMotion) snap() else ListItemFadeInSpec),
                    exit =
                        shrinkHorizontally(
                            animationSpec =
                                if (reducedMotion) {
                                    snap()
                                } else {
                                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
                                },
                            shrinkTowards = Alignment.Start,
                        ) + fadeOut(animationSpec = if (reducedMotion) snap() else ListItemFadeOutSpec),
                ) {
                    SongRowCheckbox(
                        selected = selected,
                        enabled = enabled && multiSelect,
                        onClick = onClick,
                        color = supportingColor,
                        colors = colors,
                    )
                }
                if (leadingContent != null) {
                    leadingContent()
                } else {
                    AudioCover(
                        artwork = content.artwork,
                        modifier =
                            Modifier
                                .size(style.artworkSize)
                                .clip(style.artworkShape)
                                .graphicsLayer { alpha = if (enabled) 1f else 0.5f },
                    )
                }
            }

            // 先为两端控件分配空间，文字与可选专辑列只使用剩余区域。
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(style.contentSpacing),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SongRowText(content.title, style.titleTextStyle, titleColor)
                    if (content.subtitle.isNotEmpty() || subtitleLeadingContent != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(songRowMetadataSpacing),
                        ) {
                            subtitleLeadingContent?.invoke()
                            if (content.subtitle.isNotEmpty()) {
                                SongRowText(
                                    content.subtitle,
                                    style.subtitleTextStyle,
                                    subtitleColor,
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                content.albumText?.takeIf { it.isNotBlank() }?.let { album ->
                    SongRowText(
                        AnnotatedString(album),
                        style.subtitleTextStyle,
                        subtitleColor,
                        Modifier.weight(SONG_ROW_ALBUM_WEIGHT, fill = false).widthIn(max = songRowAlbumMaxWidth),
                    )
                }
            }

            if (showTrailing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(songRowMetadataSpacing),
                ) {
                    if (playback != SongRowPlayback.None) {
                        SongRowPlaybackIndicator(playback, titleColor)
                    }
                    content.durationText?.takeIf { it.isNotBlank() }?.let { duration ->
                        SongRowText(
                            AnnotatedString(duration),
                            style.durationTextStyle,
                            subtitleColor,
                            Modifier.widthIn(min = songRowDurationMinWidth),
                        )
                    }
                    if (picker) {
                        SongRowCheckbox(selected, enabled, onClick, supportingColor, colors)
                    } else if (selection == SongRowSelection.None) {
                        trailingContent?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRowText(
    text: AnnotatedString,
    style: TextStyle,
    color: State<Color>,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        color = { color.value },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SongRowCheckbox(
    selected: Boolean,
    enabled: Boolean,
    onClick: (() -> Unit)?,
    color: Color,
    colors: SongRowColors,
) {
    Checkbox(
        checked = selected,
        onCheckedChange = onClick?.let { click -> { _: Boolean -> click() } },
        enabled = enabled,
        colors =
            CheckboxDefaults.colors(
                uncheckedColor = color,
                disabledCheckedColor = colors.disabledContentColor,
                disabledUncheckedColor = colors.disabledContentColor,
                disabledIndeterminateColor = colors.disabledContentColor,
            ),
    )
}

@Composable
private fun SongRowPlaybackIndicator(
    playback: SongRowPlayback,
    color: State<Color>,
) {
    val animate = playback == SongRowPlayback.CurrentPlaying && !LocalReducedMotion.current
    val heights =
        if (animate) {
            val transition = rememberInfiniteTransition(label = "songRowPlaying")
            List(3) { index ->
                transition.animateFloat(
                    initialValue = songRowStaticBarHeights[index],
                    targetValue = songRowAnimatedBarHeights[index],
                    animationSpec =
                        infiniteRepeatable(
                            tween(
                                durationMillis = SONG_ROW_BAR_PERIOD_MILLIS + index * SONG_ROW_BAR_PERIOD_OFFSET_MILLIS,
                                easing = EaseOutEmphasized,
                            ),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "songRowBar" + index,
                )
            }
        } else {
            null
        }
    Canvas(Modifier.size(width = 20.dp, height = 18.dp)) {
        val barWidth = 3.dp.toPx()
        val gap = 3.dp.toPx()
        val start = (size.width - 3 * barWidth - 2 * gap) / 2f
        repeat(3) { index ->
            val height = size.height * (heights?.get(index)?.value ?: songRowStaticBarHeights[index])
            drawRoundRect(
                color = color.value,
                topLeft = Offset(start + index * (barWidth + gap), size.height - height),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

private const val SONG_ROW_STATE_ANIMATION_MILLIS = 200
private const val SONG_ROW_BAR_PERIOD_MILLIS = 420
private const val SONG_ROW_BAR_PERIOD_OFFSET_MILLIS = 130
private const val SONG_ROW_ALBUM_WEIGHT = 0.6f
private val songRowMetadataSpacing = 8.dp
private val songRowDurationMinWidth = 36.dp
private val songRowAlbumMaxWidth = 192.dp
private val songRowStaticBarHeights = floatArrayOf(0.4f, 0.75f, 0.55f)
private val songRowAnimatedBarHeights = floatArrayOf(0.95f, 0.3f, 0.9f)
