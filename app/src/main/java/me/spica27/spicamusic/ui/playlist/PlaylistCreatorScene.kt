package me.spica27.spicamusic.ui.playlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.roundToInt

/** 歌单名称最大长度 */
private const val MAX_NAME_LENGTH = 40

/** 推荐歌单名 */
private val NAME_SUGGESTIONS = listOf("我喜欢的", "通勤路上", "运动节拍", "深夜电台", "学习专注")

/**
 * 创建歌单界面
 *
 * - 顶部导航栏：返回按钮
 * - 中央区域：带入场动画的封面预览 + 实时名称预览 + 圆角输入框 + 推荐名称 Chips
 * - 底部：跟随键盘抬升的全宽"创建歌单"按钮
 * - 名称为空时点确认会触发封面抖动 + 错误提示；输入合法后自动清除错误状态
 */
class PlaylistCreatorScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: PlaylistViewModel = koinActivityViewModel()

        // 歌单名称本地状态
        var name by remember { mutableStateOf("") }
        // 是否显示"名称不能为空"错误
        var showError by remember { mutableStateOf(false) }
        // 输入框焦点请求器，进场后自动弹出键盘
        val focusRequester = remember { FocusRequester() }

        // 封面入场缩放动画
        val coverScale = remember { Animatable(0.6f) }
        // 错误时的水平抖动偏移
        val shakeOffset = remember { Animatable(0f) }
        // 错误抖动触发计数
        var shakeTrigger by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            coverScale.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = 0.55f,
                        stiffness = 380f,
                    ),
            )
        }

        LaunchedEffect(shakeTrigger) {
            if (shakeTrigger == 0) return@LaunchedEffect
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = 360
                        (-16f) at 60 using FastOutSlowInEasing
                        16f at 140 using FastOutSlowInEasing
                        (-10f) at 220 using FastOutSlowInEasing
                        6f at 300 using FastOutSlowInEasing
                        0f at 360
                    },
            )
        }

        // 确认逻辑：校验 → 创建 → 返回
        fun confirm() {
            if (name.isBlank()) {
                showError = true
                shakeTrigger++
                return
            }
            viewModel.createPlaylist(name.trim())
            path.popTop()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { path.popTop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = { Text("创建歌单") },
                )
            },
            bottomBar = {
                // 底部主操作按钮：跟随键盘抬升
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = { confirm() },
                        enabled = name.isNotBlank(),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = "创建歌单",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 封面预览：入场弹性缩放 + 错误抖动 + 渐变描边阴影
                Box(
                    modifier =
                        Modifier
                            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                            .graphicsLayer {
                                scaleX = coverScale.value
                                scaleY = coverScale.value
                            }.size(150.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = MaterialTheme.shapes.extraLarge,
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            ).clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceContainerHigh,
                                        MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ),
                            ).border(
                                width = 1.dp,
                                brush =
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        ),
                                    ),
                                shape = MaterialTheme.shapes.extraLarge,
                            ),
                ) {
                    PlaylistCoverView(
                        albumIds = emptyList(),
                        modifier = Modifier.fillMaxSize(),
                        iconSize = 48.dp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 实时名称预览
                AnimatedContent(
                    targetState = name.ifBlank { "我的歌单" },
                    transitionSpec = {
                        (slideInVertically { it / 3 } + fadeIn(tween(180)))
                            .togetherWith(slideOutVertically { -it / 3 } + fadeOut(tween(120)))
                    },
                    label = "playlist_name_preview",
                ) { previewName ->
                    Text(
                        text = previewName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (name.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // 歌单名称输入框
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        if (newValue.length <= MAX_NAME_LENGTH) {
                            name = newValue
                        }
                        // 开始输入后立即清除错误提示
                        if (showError && newValue.isNotBlank()) showError = false
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    label = { Text("歌单名称") },
                    placeholder = { Text("给歌单起个名字吧") },
                    isError = showError,
                    supportingText = {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = if (showError) "请输入歌单名称" else "",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            // 字数统计
                            Text(
                                text = "${name.length}/$MAX_NAME_LENGTH",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    trailingIcon =
                        if (name.isNotEmpty()) {
                            {
                                IconButton(onClick = { name = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清空",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 推荐名称 Chips
                Text(
                    text = "试试这些名字",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    NAME_SUGGESTIONS.forEach { suggestion ->
                        SuggestionChip(
                            onClick = {
                                name = suggestion
                                showError = false
                            },
                            label = { Text(suggestion) },
                        )
                    }
                }
            }
        }
    }
}
