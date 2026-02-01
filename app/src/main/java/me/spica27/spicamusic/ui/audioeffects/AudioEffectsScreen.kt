package me.spica27.spicamusic.ui.audioeffects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.spicamusic.navigation.LocalNavBackStack
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.math.roundToInt

/**
 * 音效设置页面
 * 包含10段均衡器和混响效果
 */
@Composable
fun AudioEffectsScreen(
    modifier: Modifier = Modifier,
    viewModel: AudioEffectsViewModel = koinActivityViewModel(),
) {
    val backStack = LocalNavBackStack.current

    // 从 ViewModel 获取状态
    val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
    val eqBands by viewModel.eqBands.collectAsStateWithLifecycle()
    val reverbEnabled by viewModel.reverbEnabled.collectAsStateWithLifecycle()
    val reverbLevel by viewModel.reverbLevel.collectAsStateWithLifecycle()
    val roomSize by viewModel.reverbRoomSize.collectAsStateWithLifecycle()

    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        popupHost = { MiuixPopupUtils.MiuixPopupHost() },
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = "音效",
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    // 重置按钮
                    IconButton(onClick = {
                        viewModel.resetToDefaults()
                    }) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "重置",
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .overScrollVertical()
                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 均衡器卡片
            EqualizerCard(
                enabled = eqEnabled,
                onEnableChange = { viewModel.setEqEnabled(it) },
                bands = eqBands,
                onBandChange = { band, gain -> viewModel.setEqBandGain(band, gain) },
            )

            // 混响效果卡片
            ReverbCard(
                enabled = reverbEnabled,
                onEnableChange = { viewModel.setReverbEnabled(it) },
                level = reverbLevel,
                onLevelChange = { viewModel.setReverbLevel(it) },
                roomSize = roomSize,
                onRoomSizeChange = { viewModel.setReverbRoomSize(it) },
            )

            // 预设按钮
            PresetsSection(
                onPresetSelect = { preset ->
                    val presetType =
                        when (preset) {
                            "流行" -> AudioEffectsViewModel.Preset.POP
                            "摇滚" -> AudioEffectsViewModel.Preset.ROCK
                            "古典" -> AudioEffectsViewModel.Preset.CLASSICAL
                            "爵士" -> AudioEffectsViewModel.Preset.JAZZ
                            else -> return@PresetsSection
                        }
                    viewModel.applyPreset(presetType)
                },
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 均衡器卡片
 */
@Composable
private fun EqualizerCard(
    enabled: Boolean,
    onEnableChange: (Boolean) -> Unit,
    bands: List<Float>,
    onBandChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bandFrequencies = listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "10段均衡器",
                    style = MiuixTheme.textStyles.title3,
                    fontWeight = FontWeight.Bold,
                )
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnableChange,
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.height(24.dp))

                    // 频段可视化
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        bands.forEachIndexed { index, gain ->
                            EqualizerBar(
                                frequency = bandFrequencies[index],
                                gain = gain,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 滑块控制
                    bands.forEachIndexed { index, gain ->
                        EqualizerSlider(
                            label = "${bandFrequencies[index]} Hz",
                            value = gain,
                            onValueChange = { onBandChange(index, it) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 均衡器条形图
 */
@Composable
private fun EqualizerBar(
    frequency: String,
    gain: Float,
    modifier: Modifier = Modifier,
) {
    val barHeight = 80.dp
    val normalizedGain = (gain + 12f) / 24f // 归一化到 0-1

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .width(16.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(barHeight * normalizedGain.coerceIn(0f, 1f))
                        .background(
                            Brush.verticalGradient(
                                colors =
                                    listOf(
                                        MiuixTheme.colorScheme.primary.copy(alpha = 0.6f),
                                        MiuixTheme.colorScheme.primary,
                                    ),
                            ),
                        ),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = frequency,
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/**
 * 均衡器滑块
 */
@Composable
private fun EqualizerSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(70.dp),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            modifier = Modifier.weight(1f),
        )
        Spacer(
            modifier = Modifier.width(8.dp),
        )
        Text(
            text = "${value.roundToInt()}dB",
            style = MiuixTheme.textStyles.body2,
            modifier = Modifier.width(50.dp),
            color = if (value >= 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * 混响卡片
 */
@Composable
private fun ReverbCard(
    enabled: Boolean,
    onEnableChange: (Boolean) -> Unit,
    level: Float,
    onLevelChange: (Float) -> Unit,
    roomSize: Float,
    onRoomSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "空间混响",
                        style = MiuixTheme.textStyles.title3,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "模拟音乐厅、教堂等空间效果",
                        style = MiuixTheme.textStyles.body2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnableChange,
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 混响强度
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "混响强度",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.width(70.dp),
                        )
                        Slider(
                            value = level,
                            onValueChange = onLevelChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(level * 100).roundToInt()}%",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.End,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 房间大小
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "房间大小",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.width(70.dp),
                        )
                        Slider(
                            value = roomSize,
                            onValueChange = onRoomSizeChange,
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "${(roomSize * 100).roundToInt()}%",
                            style = MiuixTheme.textStyles.body2,
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 预设选择区域
 */
@Composable
private fun PresetsSection(
    onPresetSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "预设方案",
                style = MiuixTheme.textStyles.title3,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetButton("流行", onPresetSelect, Modifier.weight(1f))
                PresetButton("摇滚", onPresetSelect, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PresetButton("古典", onPresetSelect, Modifier.weight(1f))
                PresetButton("爵士", onPresetSelect, Modifier.weight(1f))
            }
        }
    }
}

/**
 * 预设按钮
 */
@Composable
private fun PresetButton(
    preset: String,
    onPresetSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = { onPresetSelect(preset) },
        modifier = modifier,
        content = {
            Text(
                text = preset,
                style = MiuixTheme.textStyles.body1,
            )
        },
    )
}
