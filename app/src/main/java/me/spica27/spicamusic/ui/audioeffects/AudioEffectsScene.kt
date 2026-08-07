package me.spica27.spicamusic.ui.audioeffects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.about.AboutScaffold
import me.spica27.spicamusic.ui.about.AboutSectionCard
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.clickHighlight
import org.koin.compose.viewmodel.koinViewModel

/**
 * 音效设置页
 *
 * 均衡器（开关 + 预设 + 10 段增益）、混响（开关 + 强度/房间大小）、
 * 响度归一化（开关）。视觉语言复用 [AboutScaffold] / [AboutSectionCard]，与设置页保持一致。
 */
class AudioEffectsScene : StackScene() {
    @Composable
    override fun Content() {
        val viewModel: AudioEffectsViewModel = koinViewModel()

        val eqEnabled by viewModel.eqEnabled.collectAsStateWithLifecycle()
        val eqBands by viewModel.eqBands.collectAsStateWithLifecycle()
        val reverbEnabled by viewModel.reverbEnabled.collectAsStateWithLifecycle()
        val reverbLevel by viewModel.reverbLevel.collectAsStateWithLifecycle()
        val reverbRoomSize by viewModel.reverbRoomSize.collectAsStateWithLifecycle()
        val loudnessEnabled by viewModel.loudnessNormalizationEnabled.collectAsStateWithLifecycle()

        AboutScaffold(title = stringResource(R.string.settings_sound_effects)) {
            item {
                AboutSectionCard(
                    title = stringResource(R.string.audio_effects_section_equalizer),
                    subtitle = stringResource(R.string.audio_effects_subtitle),
                ) {
                    EffectSwitchRow(
                        title = stringResource(R.string.audio_effects_eq_enable),
                        subtitle = stringResource(R.string.audio_effects_eq_enable_desc),
                        icon = Icons.Default.GraphicEq,
                        checked = eqEnabled,
                        onCheckedChange = viewModel::setEqEnabled,
                    )
                    AnimatedVisibility(
                        visible = eqEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            EqualizerPresets(onPreset = viewModel::applyPreset)
                            EqualizerBands(
                                bands = eqBands,
                                onBandChange = viewModel::setEqBandGain,
                            )
                        }
                    }
                }
            }

            item {
                AboutSectionCard(title = stringResource(R.string.audio_effects_section_reverb)) {
                    EffectSwitchRow(
                        title = stringResource(R.string.audio_effects_reverb_enable),
                        subtitle = stringResource(R.string.reverb_spatial_desc),
                        icon = Icons.Default.SurroundSound,
                        checked = reverbEnabled,
                        onCheckedChange = viewModel::setReverbEnabled,
                    )
                    AnimatedVisibility(
                        visible = reverbEnabled,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column {
                            EffectSliderRow(
                                label = stringResource(R.string.reverb_intensity),
                                value = reverbLevel,
                                valueText = percentText(reverbLevel),
                                onValueChange = viewModel::setReverbLevel,
                            )
                            EffectSliderRow(
                                label = stringResource(R.string.room_size),
                                value = reverbRoomSize,
                                valueText = percentText(reverbRoomSize),
                                onValueChange = viewModel::setReverbRoomSize,
                            )
                        }
                    }
                }
            }

            item {
                AboutSectionCard(title = stringResource(R.string.audio_effects_section_loudness)) {
                    EffectSwitchRow(
                        title = stringResource(R.string.audio_effects_loudness_title),
                        subtitle = stringResource(R.string.audio_effects_loudness_desc),
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        checked = loudnessEnabled,
                        onCheckedChange = viewModel::setLoudnessNormalizationEnabled,
                    )
                }
            }

            item {
                ResetRow(onReset = viewModel::resetToDefaults)
            }
        }
    }
}

/** 音效开关行：左图标 + 标题副标题 + 右 Switch，整行可点。 */
@Composable
private fun EffectSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickHighlight(onClick = { onCheckedChange(!checked) })
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .clip(Shapes.LargeCornerBasedShape)
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =
                    if (checked) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
        )
    }
}

/** 参数滑杆行（0~1）：左侧标签，右侧百分比，下方 Slider。 */
@Composable
private fun EffectSliderRow(
    label: String,
    value: Float,
    valueText: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Large, vertical = Spacing.Small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
        )
    }
}

/** 预设方案：横向 FilterChip 快速套用。 */
@Composable
private fun EqualizerPresets(onPreset: (AudioEffectsViewModel.Preset) -> Unit) {
    val presets =
        listOf(
            AudioEffectsViewModel.Preset.POP to stringResource(R.string.preset_pop),
            AudioEffectsViewModel.Preset.ROCK to stringResource(R.string.preset_rock),
            AudioEffectsViewModel.Preset.CLASSICAL to stringResource(R.string.preset_classical),
            AudioEffectsViewModel.Preset.JAZZ to stringResource(R.string.preset_jazz),
        )
    Column(
        modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        Text(
            text = stringResource(R.string.presets_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            presets.forEach { (preset, label) ->
                FilterChip(
                    selected = false,
                    onClick = { onPreset(preset) },
                    label = { Text(label) },
                )
            }
        }
    }
}

/**
 * 目标响度选择：横向 FilterChip。
 *
 * 用离散 chip 而非滑杆，因为目标响度是几个有明确来历的约定值，不是连续量。
 */

/** 10 段均衡器：每段一根竖向滑杆，展示中心频率与当前增益。 */
@Composable
private fun EqualizerBands(
    bands: List<Float>,
    onBandChange: (Int, Float) -> Unit,
) {
    val frequencyLabels =
        listOf("31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
    ) {
        for (index in 0 until 10) {
            val gain = bands.getOrElse(index) { 0f }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
            ) {
                Text(
                    text = "${gain.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                VerticalBandSlider(
                    value = gain,
                    onValueChange = { onBandChange(index, it) },
                    modifier =
                        Modifier
                            .height(140.dp)
                            .fillMaxWidth(),
                )
                Text(
                    text = frequencyLabels[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 竖向滑杆：旋转 270° 并交换测量维度，让布局尺寸真正为"窄×高"，
 * 避免旋转后布局边界互相重叠导致触摸命中错位。
 */
@Composable
private fun VerticalBandSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -12f..12f,
            // 绕中心旋转 270°，再交换测量维度：滑杆以父容器高度为轨道长度，
            // 布局尺寸报告为"窄×高"，并把旋转后的内容中心对齐回布局框中心
            modifier =
                Modifier
                    .graphicsLayer { rotationZ = 270f }
                    .layout { measurable, constraints ->
                        val placeable =
                            measurable.measure(
                                Constraints(
                                    minWidth = constraints.minHeight,
                                    maxWidth = constraints.maxHeight,
                                    minHeight = constraints.minWidth,
                                    maxHeight = constraints.maxWidth,
                                ),
                            )
                        layout(placeable.height, placeable.width) {
                            placeable.place(
                                x = (placeable.height - placeable.width) / 2,
                                y = (placeable.width - placeable.height) / 2,
                            )
                        }
                    },
        )
    }
}

/** 恢复默认行。 */
@Composable
private fun ResetRow(onReset: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f))
                .clickHighlight(onClick = onReset)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        Text(
            text = stringResource(R.string.audio_effects_reset),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun percentText(value: Float): String = "${(value * 100).toInt()}%"
