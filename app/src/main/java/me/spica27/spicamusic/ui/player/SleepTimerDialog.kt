@file:Suppress("FunctionName")

package me.spica27.spicamusic.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.player.api.SleepTimerState
import me.spica27.spicamusic.ui.theme.EaseInOutCubic
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.ListItemFadeInSpec
import me.spica27.spicamusic.ui.theme.ListItemFadeOutSpec
import me.spica27.spicamusic.ui.theme.LocalReducedMotion
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entranceGraphics
import me.spica27.spicamusic.ui.theme.rememberEntrance
import me.spica27.spicamusic.ui.widget.clickHighlight
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val SleepTimerOptionsMinutes = listOf(15, 30, 45, 60, 90)

/** 倒计时环直径 */
private val RingSize = 184.dp

/** 环描边宽度 */
private val RingStroke = 9.dp

/**
 * 环首次显影的扫描时长。
 */
private const val RING_REVEAL_MILLIS = 880

/** 上游倒计时的刷新间隔，环用等长的线性补间跟走，读数与环同一节奏 */
private const val RING_TICK_MILLIS = 1000

/**
 * 点选后到面板收起之间留的确认停顿。
 */
private const val SELECTION_SETTLE_MILLIS = 140L

/**
 * 自定义时长边界与步进（分钟）。
 */
private const val CUSTOM_MIN_MINUTES = 5
private const val CUSTOM_MAX_MINUTES = 240
private const val CUSTOM_STEP_MINUTES = 5

/** 整个可调范围 */
private const val CUSTOM_RANGE_MINUTES = (CUSTOM_MAX_MINUTES - CUSTOM_MIN_MINUTES).toFloat()

/** 自定义起始值 */
private const val CUSTOM_DEFAULT_MINUTES = 20

/** 圆心死区比例 */
private const val DIAL_DEAD_ZONE_RATIO = 0.15f

/** 环跳跃阈值 */
private const val RING_JUMP_THRESHOLD = 0.01f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerDialog(
    timer: SleepTimerState?,
    onDismiss: () -> Unit,
    onSetTimer: (durationMs: Long) -> Unit,
    onCancelTimer: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var dismissing by remember { mutableStateOf(false) }

    // 自定义模式，同一枚环从"还剩多少"改任"设多久"，下半区换成提交按钮
    var editing by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableIntStateOf(CUSTOM_DEFAULT_MINUTES) }

    // 入场瀑布只在首次显影时跑，从自定义返回是回到刚才那一屏，再排一遍队会啰嗦
    var everEdited by remember { mutableStateOf(false) }

    // 先把面板滑下去再通知外部移除，否则 ModalBottomSheet 会被直接从组合里摘掉、没有退场
    fun dismissAnimated(settle: Long = 0L) {
        if (dismissing) return
        dismissing = true
        scope.launch {
            if (settle > 0L) delay(settle)
            sheetState.hide()
            onDismiss()
        }
    }

    // 对不上任何预设的时长就是自定义来的，入口那行要把它显示出来
    val activeCustomMinutes =
        timer
            ?.durationMs
            ?.let { TimeUnit.MILLISECONDS.toMinutes(it).toInt() }
            ?.takeIf { it !in SleepTimerOptionsMinutes }

    // 带着当前值进自定义，正在走的定时器先吸附到最近一档，没有则用默认值
    fun beginEditing() {
        customMinutes =
            timer
                ?.durationMs
                ?.let { snapCustomMinutes(TimeUnit.MILLISECONDS.toMinutes(it).toFloat()) }
                ?: CUSTOM_DEFAULT_MINUTES
        everEdited = true
        editing = true
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = null,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.Large)
                    .padding(bottom = Spacing.Large)
                    .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .padding(top = Spacing.Small, bottom = Spacing.ExtraSmall)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
            )
            SheetHeader(
                editing = editing,
                onBack = { editing = false },
                onDismiss = { dismissAnimated() },
            )
            TimerHero(
                timer = timer,
                editing = editing,
                customMinutes = customMinutes,
                onCustomMinutesChange = { customMinutes = it },
            )
            Spacer(modifier = Modifier.height(Spacing.Large))

            // 下半区整块换人。两种模式高度差不少，交给 SizeTransform 抹平
            AnimatedContent(
                targetState = editing,
                transitionSpec = {
                    fadeIn(ListItemFadeInSpec) togetherWith
                        fadeOut(ListItemFadeOutSpec) using
                        SizeTransform(clip = false) { _, _ ->
                            tween(280, easing = EaseOutEmphasized)
                        }
                },
                label = "sleep_timer_mode",
            ) { isEditing ->
                if (isEditing) {
                    StartTimerButton(
                        enabled = !dismissing,
                        onClick = {
                            onSetTimer(TimeUnit.MINUTES.toMillis(customMinutes.toLong()))
                            dismissAnimated(settle = SELECTION_SETTLE_MILLIS)
                        },
                    )
                } else {
                    Column {
                        PresetRow(
                            activeDurationMs = timer?.durationMs,
                            enabled = !dismissing,
                            playEntrance = !everEdited,
                            onSelect = { durationMs ->
                                onSetTimer(durationMs)
                                dismissAnimated(settle = SELECTION_SETTLE_MILLIS)
                            },
                        )
                        CustomDurationRow(
                            activeMinutes = activeCustomMinutes,
                            enabled = !dismissing,
                            playEntrance = !everEdited,
                            onClick = { beginEditing() },
                        )
                        CancelTimerButton(
                            visible = timer != null && !dismissing,
                            onClick = {
                                onCancelTimer()
                                dismissAnimated()
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 标题行。
 *
 * 自定义模式下左侧滑出返回箭头，标题跟着换，右侧的关闭一直留着，
 * 免得进了自定义就找不到出口。
 */
@Composable
private fun SheetHeader(
    editing: Boolean,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val entrance = rememberEntrance(order = 0)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .entranceGraphics(entrance),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = editing,
            enter = fadeIn(ListItemFadeInSpec) + expandHorizontally(tween(240, easing = EaseOutEmphasized)),
            exit = fadeOut(ListItemFadeOutSpec) + shrinkHorizontally(tween(180, easing = EaseOutEmphasized)),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text =
                if (editing) {
                    stringResource(R.string.settings_sleep_timer_custom)
                } else {
                    stringResource(R.string.settings_sleep_timer_dialog_title)
                },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.cancel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 倒计时环。
 */
@Composable
private fun TimerHero(
    timer: SleepTimerState?,
    editing: Boolean,
    customMinutes: Int,
    onCustomMinutesChange: (Int) -> Unit,
) {
    val entrance = rememberEntrance(order = 1)

    // 编辑时环表示"要设多久"，其余时候表示"还剩多少"，两者共用同一条动画
    val sweepTarget =
        if (editing) {
            customFractionOf(customMinutes)
        } else {
            timer?.let { (it.remainingMs.toFloat() / it.durationMs).coerceIn(0f, 1f) } ?: 0f
        }
    val sweep = rememberRingSweep(sweepTarget)

    val hint =
        when {
            editing -> stringResource(R.string.settings_sleep_timer_custom_hint)
            timer != null -> stringResource(R.string.settings_sleep_timer_preset_subtitle)
            else -> stringResource(R.string.settings_sleep_timer_dialog_subtitle)
        }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .entranceGraphics(entrance)
                .clip(Shapes.ExtraLarge2CornerBasedShape)
                .background(
                    Brush.verticalGradient(
                        0f to MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f),
                        1f to Color.Transparent,
                    ),
                ).padding(top = Spacing.ExtraLarge, bottom = Spacing.Large),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ± 用 SpaceBetween 顶到两端，环占中间的余量，按钮进出时环始终居中
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StepButton(
                    visible = editing,
                    icon = Icons.Rounded.Remove,
                    contentDescription =
                        stringResource(
                            R.string.settings_sleep_timer_step_decrease,
                            CUSTOM_STEP_MINUTES,
                        ),
                    enabled = customMinutes > CUSTOM_MIN_MINUTES,
                    onClick = { onCustomMinutesChange(customMinutes - CUSTOM_STEP_MINUTES) },
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    TimerRing(
                        sweep = sweep,
                        glowActive = editing || timer != null,
                        knob = editing,
                        modifier =
                            if (editing) {
                                Modifier.ringDial(
                                    minutes = customMinutes,
                                    onMinutesChange = onCustomMinutesChange,
                                )
                            } else {
                                Modifier
                            },
                    ) {
                        RingCenter(
                            timer = timer,
                            editing = editing,
                            customMinutes = customMinutes,
                        )
                    }
                }
                StepButton(
                    visible = editing,
                    icon = Icons.Rounded.Add,
                    contentDescription =
                        stringResource(
                            R.string.settings_sleep_timer_step_increase,
                            CUSTOM_STEP_MINUTES,
                        ),
                    enabled = customMinutes < CUSTOM_MAX_MINUTES,
                    onClick = { onCustomMinutesChange(customMinutes + CUSTOM_STEP_MINUTES) },
                )
            }
            Spacer(modifier = Modifier.height(Spacing.Large))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.Large),
            )
        }
    }
}

/**
 * 把环变成可拖的拨盘。
 */
@Composable
private fun Modifier.ringDial(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val currentMinutes = rememberUpdatedState(minutes)
    val currentCallback = rememberUpdatedState(onMinutesChange)

    return pointerInput(Unit) {
        var lastAngle = 0f
        var raw = 0f

        detectDragGestures(
            onDragStart = { offset ->
                val ringCenter = Offset(size.width / 2f, size.height / 2f)
                lastAngle = angleAt(offset, ringCenter)
                raw = currentMinutes.value.toFloat()
            },
        ) { change, _ ->
            // 吃掉事件，否则纵向分量会被面板的下拉关闭和内容滚动抢走
            change.consume()

            val ringCenter = Offset(size.width / 2f, size.height / 2f)
            val angle = angleAt(change.position, ringCenter)
            val radius = (change.position - ringCenter).getDistance()

            if (radius >= minOf(size.width, size.height) * DIAL_DEAD_ZONE_RATIO) {
                var delta = angle - lastAngle
                // 跨 ±180° 的接缝要折回来，否则一步会被当成绕了大半圈
                if (delta > 180f) delta -= 360f
                if (delta < -180f) delta += 360f

                raw =
                    (raw + delta / 360f * CUSTOM_RANGE_MINUTES)
                        .coerceIn(CUSTOM_MIN_MINUTES.toFloat(), CUSTOM_MAX_MINUTES.toFloat())

                val snapped = snapCustomMinutes(raw)
                if (snapped != currentMinutes.value) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    currentCallback.value(snapped)
                }
            }
            // 死区里也要更新基准，否则手指移回来会突跳一大截
            lastAngle = angle
        }
    }
}

/**
 * 环两侧的步进按钮。
 *
 * 拨盘给手感，它给精度，也是这个控件唯一的键盘与读屏入口。
 */
@Composable
private fun StepButton(
    visible: Boolean,
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(ListItemFadeInSpec) + expandHorizontally(tween(240, easing = EaseOutEmphasized)),
        exit = fadeOut(ListItemFadeOutSpec) + shrinkHorizontally(tween(180, easing = EaseOutEmphasized)),
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickHighlight(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint =
                    MaterialTheme.colorScheme.onSurface
                        .copy(alpha = if (enabled) 1f else 0.38f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun TimerRing(
    sweep: State<Float>,
    glowActive: Boolean,
    knob: Boolean,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit,
) {
    val glow = rememberRingGlow(active = glowActive)

    val ringColor = MaterialTheme.colorScheme.primary
    val ringAccent = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val headCoreColor = MaterialTheme.colorScheme.onPrimary

    Box(
        modifier = modifier.size(RingSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2f
            val diameter = size.minDimension - stroke
            val center = Offset(size.minDimension / 2f, size.minDimension / 2f)

            // 环后的呼吸光晕。画在最底层，向外化开，撑出景深
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0f to ringColor.copy(alpha = glow.value),
                        0.62f to ringColor.copy(alpha = glow.value * 0.45f),
                        1f to Color.Transparent,
                    ),
                radius = size.minDimension / 2f,
                center = center,
            )
            drawArc(
                color = trackColor.copy(alpha = 0.9f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(diameter, diameter),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )

            val fraction = sweep.value.coerceIn(0f, 1f)

            // 双色扫描渐变：环身沿周长换色，比纯色更有质感
            if (fraction > 0f) {
                drawArc(
                    brush =
                        Brush.sweepGradient(
                            0f to ringColor,
                            0.5f to ringAccent,
                            1f to ringColor,
                            center = center,
                        ),
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(diameter, diameter),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }

            // 旋钮态即使停在最小值也要留住这个点，否则拨盘看不出来能拖
            if (fraction <= 0f && !knob) return@Canvas

            // 弧头亮点：把"走到哪儿了"标到一个精确的点上，环因此有了朝向
            val headRad = Math.toRadians((-90f + 360f * fraction).toDouble())
            val headOffset =
                Offset(
                    x = center.x + (diameter / 2f) * cos(headRad).toFloat(),
                    y = center.y + (diameter / 2f) * sin(headRad).toFloat(),
                )
            // 旋钮比进度点大一圈，看着就该拿手指去拨
            val haloRadius = if (knob) stroke * 2.1f else stroke * 1.5f
            val coreRadius = if (knob) stroke * 0.62f else stroke * 0.3f
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0f to ringColor.copy(alpha = 0.55f),
                        1f to Color.Transparent,
                        center = headOffset,
                        radius = haloRadius,
                    ),
                radius = haloRadius,
                center = headOffset,
            )
            drawCircle(
                color = headCoreColor,
                radius = coreRadius,
                center = headOffset,
            )
            if (knob) {
                // 描一圈主色边，跟内芯拉开层次
                drawCircle(
                    color = ringColor,
                    radius = coreRadius,
                    center = headOffset,
                    style = Stroke(width = stroke * 0.22f),
                )
            }
        }

        centerContent()
    }
}

/**
 * 环心读数。
 *
 * 三种态各只放一件东西，读数、设定值、空闲图标，不叠标签。
 */
@Composable
private fun RingCenter(
    timer: SleepTimerState?,
    editing: Boolean,
    customMinutes: Int,
) {
    when {
        editing -> CustomValueReadout(minutes = customMinutes)

        timer != null ->
            Text(
                text = formatSleepTimerRemaining(timer.remainingMs),
                // 等宽数字：秒位每秒都在变，不定宽会让整串读数左右抖
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontFeatureSettings = "tnum",
                    ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )

        else ->
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                modifier = Modifier.size(44.dp),
            )
    }
}

/**
 * 自定义值读数。
 *
 */
@Composable
private fun CustomValueReadout(minutes: Int) {
    val hours = minutes / 60
    val rest = minutes % 60
    val value =
        if (hours > 0) {
            String.format(LocalLocale.current.platformLocale, "%d:%02d", hours, rest)
        } else {
            minutes.toString()
        }
    val unit =
        if (hours > 0) {
            stringResource(R.string.settings_sleep_timer_unit_hours)
        } else {
            stringResource(R.string.settings_sleep_timer_unit_minutes)
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style =
                MaterialTheme.typography.headlineLarge.copy(
                    fontFeatureSettings = "tnum",
                ),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * 环的扫过比例。
 */
@Composable
private fun rememberRingSweep(target: Float): State<Float> {
    val reducedMotion = LocalReducedMotion.current
    val sweep = remember { Animatable(0f) }

    LaunchedEffect(target, reducedMotion) {
        val delta = abs(target - sweep.value)
        when {
            reducedMotion -> sweep.snapTo(target)
            // 只有还没显影过的那一次走 ease-out，其余都是跟走
            sweep.value == 0f && target > 0f ->
                sweep.animateTo(target, tween(RING_REVEAL_MILLIS, easing = EaseOutEmphasized))

            // 换档或切模式，跳变用临界阻尼弹簧收住，有落点感又不会冲过目标值
            delta > RING_JUMP_THRESHOLD ->
                sweep.animateTo(
                    target,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )

            else -> sweep.animateTo(target, tween(RING_TICK_MILLIS, easing = LinearEasing))
        }
    }
    return sweep.asState()
}

/** 环后光晕的呼吸强度。用 Reverse 往回走，避免到顶后突然跳回起点 */
@Composable
private fun rememberRingGlow(active: Boolean): State<Float> {
    // 未设定时压暗：光晕是"正在走时"的信号，空闲时不该抢注意力
    val ceiling = if (active) 1f else 0.45f

    // LocalReducedMotion 在进程内不变，这里的条件分支不会真的重排 slot
    if (LocalReducedMotion.current) {
        return remember(ceiling) { mutableStateOf(0.16f * ceiling) }
    }

    val transition = rememberInfiniteTransition(label = "sleep_timer_glow")
    val breathing =
        transition.animateFloat(
            initialValue = 0.10f,
            targetValue = 0.24f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(2200, easing = EaseInOutCubic),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "sleep_timer_glow_alpha",
        )
    return remember(ceiling, breathing) {
        derivedStateOf { breathing.value * ceiling }
    }
}

/**
 * 时长选择行：五枚等宽胶囊排成一行。
 *
 */
@Composable
private fun PresetRow(
    activeDurationMs: Long?,
    enabled: Boolean,
    playEntrance: Boolean,
    onSelect: (durationMs: Long) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        SleepTimerOptionsMinutes.forEachIndexed { index, minutes ->
            val durationMs = TimeUnit.MINUTES.toMillis(minutes.toLong())
            SleepTimerPreset(
                minutes = minutes,
                selected = activeDurationMs == durationMs,
                enabled = enabled,
                // 接在 Hero（order 1）之后逐个落位，形成一道短瀑布
                entranceOrder = 2 + index,
                playEntrance = playEntrance,
                onClick = { onSelect(durationMs) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 自定义时长入口。
 */
@Composable
private fun CustomDurationRow(
    activeMinutes: Int?,
    enabled: Boolean,
    playEntrance: Boolean,
    onClick: () -> Unit,
) {
    val entrance = rememberEntrance(order = 7, play = playEntrance)
    val active = activeMinutes != null
    val contentColor =
        if (active) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    Row(
        modifier =
            Modifier
                .padding(top = Spacing.Small)
                .fillMaxWidth()
                .entranceGraphics(entrance)
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(
                    if (active) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ).then(
                    if (active) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = Shapes.ExtraLargeCornerBasedShape,
                        )
                    },
                ).clickHighlight(enabled = enabled, onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Tune,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.Medium))
        Text(
            text = stringResource(R.string.settings_sleep_timer_custom),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            modifier = Modifier.weight(1f),
        )
        if (activeMinutes != null) {
            // 当前生效的自定义值直接摆出来，不用点进去才知道设了多久
            Text(
                text = formatCustomDuration(activeMinutes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
            Spacer(modifier = Modifier.width(Spacing.ExtraSmall))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 自定义模式的提交按钮。
 */
@Composable
private fun StartTimerButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(Shapes.LargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickHighlight(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bedtime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.Small))
        Text(
            text = stringResource(R.string.settings_sleep_timer_start),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/**
 * 把分钟数说成人话。
 */
@Composable
private fun formatCustomDuration(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 ->
            stringResource(R.string.settings_sleep_timer_hours_minutes, hours, rest)

        hours > 0 -> stringResource(R.string.settings_sleep_timer_hours, hours)
        else -> stringResource(R.string.settings_sleep_timer_minutes, minutes)
    }
}

@Composable
private fun SleepTimerPreset(
    minutes: Int,
    selected: Boolean,
    enabled: Boolean,
    entranceOrder: Int,
    playEntrance: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entrance = rememberEntrance(order = entranceOrder, play = playEntrance)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val presetLabel = stringResource(R.string.settings_sleep_timer_minutes, minutes)

    val containerColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sleep_timer_preset_color",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sleep_timer_preset_content",
    )
    // 按下缩一点、选中胀一点：两种反馈用同一根弹簧，手感统一
    val scale by animateFloatAsState(
        targetValue =
            when {
                pressed -> 0.94f
                selected -> 1.04f
                else -> 1f
            },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "sleep_timer_preset_scale",
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.32f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sleep_timer_preset_glow",
    )
    val glowColor = MaterialTheme.colorScheme.primary

    Column(
        modifier =
            modifier
                .entranceGraphics(entrance)
                // 用 graphicsLayer 而不是 Modifier.scale：后者在组合期读值，
                // 弹簧跑动的每一帧都会重组这枚胶囊
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.drawBehind {
                    // 选中态外扩的一圈柔光，让当前项从这一行里"浮"起来
                    if (glowAlpha <= 0f) return@drawBehind
                    drawCircle(
                        brush =
                            Brush.radialGradient(
                                0f to glowColor.copy(alpha = glowAlpha),
                                1f to Color.Transparent,
                            ),
                        radius = size.maxDimension * 0.72f,
                    )
                }.clip(Shapes.ExtraLargeCornerBasedShape)
                .background(containerColor)
                .then(
                    if (selected) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            shape = Shapes.ExtraLargeCornerBasedShape,
                        )
                    },
                ).clickHighlight(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    onClick = onClick,
                ).padding(vertical = Spacing.Medium)
                // 数字和单位分两行读起来是"15 分钟"，但 TalkBack 该听到完整的一句
                .clearAndSetSemantics {
                    contentDescription = presetLabel
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = minutes.toString(),
            style =
                MaterialTheme.typography.titleMedium.copy(
                    fontFeatureSettings = "tnum",
                ),
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
        )
        Text(
            text = stringResource(R.string.settings_sleep_timer_unit_minutes),
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

/** 定时进行中才出现的取消入口。放在最后、用弱化的填充色，不跟时长选择抢视线 */
@Composable
private fun CancelTimerButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        // 高度与透明度用同一组时长/曲线；两者的 spec 泛型不同（IntSize / Float），只能分开写
        enter = fadeIn(ListItemFadeInSpec) + expandVertically(tween(220, easing = EaseOutEmphasized)),
        exit = fadeOut(ListItemFadeOutSpec) + shrinkVertically(tween(160, easing = EaseOutEmphasized)),
    ) {
        Row(
            modifier =
                Modifier
                    .padding(top = Spacing.Medium)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(Shapes.LargeCornerBasedShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickHighlight(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.Small))
            Text(
                text = stringResource(R.string.settings_sleep_timer_cancel),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun formatSleepTimerRemaining(remainingMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMs.coerceAtLeast(0L))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

/**
 * 把任意分钟值吸附到最近一档。
 */
private fun snapCustomMinutes(minutes: Float): Int {
    val clamped = minutes.coerceIn(CUSTOM_MIN_MINUTES.toFloat(), CUSTOM_MAX_MINUTES.toFloat())
    val steps = ((clamped - CUSTOM_MIN_MINUTES) / CUSTOM_STEP_MINUTES).roundToInt()
    return (CUSTOM_MIN_MINUTES + steps * CUSTOM_STEP_MINUTES).coerceIn(CUSTOM_MIN_MINUTES, CUSTOM_MAX_MINUTES)
}

/**
 * 分钟值在环上占的比例。
 */
private fun customFractionOf(minutes: Int): Float = ((minutes - CUSTOM_MIN_MINUTES) / CUSTOM_RANGE_MINUTES).coerceIn(0f, 1f)

/**
 * 触点相对圆心的极角，单位度。
 */
private fun angleAt(
    position: Offset,
    center: Offset,
): Float =
    Math
        .toDegrees(
            atan2((position.y - center.y).toDouble(), (position.x - center.x).toDouble()),
        ).toFloat()
