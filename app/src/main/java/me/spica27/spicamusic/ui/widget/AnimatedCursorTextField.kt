package me.spica27.spicamusic.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 两次光标移动间隔小于此值即视为“高频连续变化”（长按删除/连打）。
private const val RAPID_CURSOR_MOVE_MILLIS = 90L

// 高频连续变化时的 spring 刚度：远高于 StiffnessMedium(1500f)，让光标快速滑动追赶而非瞬移。
private const val CURSOR_RAPID_STIFFNESS = 8000f

/**
 * 带自定义光标动画的单行输入框。
 * @param value 当前文本（受控）。
 * @param onValueChange 文本变化回调。
 * @param cursorColor 光标颜色，可传随状态变化的动态色（如错误态变红）。
 * @param onImeAction IME 动作键（Search/Done 等）触发回调；为 null 时用默认行为。
 * @param inputTransformation 输入变换，如 `InputTransformation.maxLength(40)` 限制长度。
 * @param focusRequester 需要主动请求焦点时传入。
 * @param placeholder 空文本时显示的占位内容（样式由调用方决定）。
 */
@Composable
fun AnimatedCursorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textStyle: TextStyle = TextStyle.Default,
    cursorColor: Color = Color.Unspecified,
    cursorWidth: Dp = 2.dp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onImeAction: (() -> Unit)? = null,
    inputTransformation: InputTransformation? = null,
    focusRequester: FocusRequester? = null,
    placeholder: (@Composable () -> Unit)? = null,
) {
    val textFieldState = rememberTextFieldState(value)
    // 外部 value 与内部 TextFieldState 双向同步：
    // 内部编辑 → 上报；外部变更（清空 / 填充建议）→ 回写并把光标置尾。
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collect { currentOnValueChange(it) }
    }
    LaunchedEffect(value) {
        if (value != textFieldState.text.toString()) {
            textFieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }

    val scrollState = rememberScrollState()

    // onTextLayout 回调把每次布局结果读进 state；getCursorRect 由此计算光标位置。
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var focused by remember { mutableStateOf(false) }

    // 呼吸动画
    val infinite = rememberInfiniteTransition(label = "cursor-breath")
    val breathAlpha by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursor-breath-alpha",
    )

    // 位移动画
    val animatedTopLeft = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var cursorHeight by remember { mutableFloatStateOf(0f) }
    var cursorVisible by remember { mutableStateOf(false) }

    val selection = textFieldState.selection
    // 记录上次光标更新时刻：长按删除的时候刚度加速追赶，
    // 始终保持位移动画、不降级为瞬移。
    var lastCursorMoveAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(textLayout, selection, focused) {
        val layout = textLayout
        if (layout != null && focused && selection.collapsed) {
            val rect = layout.getCursorRect(selection.start)
            cursorHeight = rect.height
            if (!cursorVisible) {
                // 首次出现：直接落位，不从 (0,0) 滑入。
                animatedTopLeft.snapTo(rect.topLeft)
                cursorVisible = true
            } else {
                val now = withFrameMillis { it }
                val rapid = now - lastCursorMoveAt < RAPID_CURSOR_MOVE_MILLIS
                lastCursorMoveAt = now
                // 高频时用高刚度 spring 快速追赶；单次移动用中等刚度平滑滑动。
                // spring retarget 会保留当前速度，连续变化时平滑接续、不从零重启。
                animatedTopLeft.animateTo(
                    targetValue = rect.topLeft,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = if (rapid) CURSOR_RAPID_STIFFNESS else Spring.StiffnessMedium,
                        ),
                )
            }
        } else {
            cursorVisible = false
        }
    }

    BasicTextField(
        state = textFieldState,
        modifier =
            modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .drawWithContent {
                    drawContent()
                    if (cursorVisible && cursorHeight > 0f) {
                        val topLeft = animatedTopLeft.value
                        // getCursorRect 返回完整文本坐标，单行滚动后需减去水平滚动量才对齐。
                        val x = topLeft.x - scrollState.value
                        val strokeWidth = cursorWidth.toPx()
                        val centerX = x + strokeWidth / 2f
                        drawLine(
                            color = cursorColor.copy(alpha = cursorColor.alpha * breathAlpha),
                            start = Offset(centerX, topLeft.y),
                            end = Offset(centerX, topLeft.y + cursorHeight),
                            strokeWidth = strokeWidth,
                            cap = StrokeCap.Round,
                        )
                    }
                },
        enabled = enabled,
        textStyle = textStyle,
        lineLimits = TextFieldLineLimits.SingleLine,
        scrollState = scrollState,
        keyboardOptions = keyboardOptions,
        onKeyboardAction =
            onImeAction?.let { action ->
                KeyboardActionHandler { action() }
            },
        inputTransformation = inputTransformation,
        cursorBrush = SolidColor(Color.Transparent), // 关键：藏掉原生闪烁光标
        onTextLayout = { getResult -> textLayout = getResult() },
        decorator =
            if (placeholder != null) {
                TextFieldDecorator { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) placeholder()
                        innerTextField()
                    }
                }
            } else {
                null
            },
    )
}
