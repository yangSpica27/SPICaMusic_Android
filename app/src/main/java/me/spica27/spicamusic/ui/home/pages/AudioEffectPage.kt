package me.spica27.spicamusic.ui.home.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica27.spicamusic.player.api.IAudioEffectProcessor
import org.koin.androidx.compose.koinViewModel
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 音效页面
 */
@Composable
fun AudioEffectPage(
    modifier: Modifier = Modifier,
    viewModel: AudioEffectViewModel = koinViewModel(),
) {
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqBands by viewModel.eqBands.collectAsState()
    val bassBoostEnabled by viewModel.bassBoostEnabled.collectAsState()
    val bassBoostStrength by viewModel.bassBoostStrength.collectAsState()
    val reverbEnabled by viewModel.reverbEnabled.collectAsState()
    val normalizerEnabled by viewModel.normalizerEnabled.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = "音效设置",
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 均衡器 (EQ)
            item {
                EffectCard(
                    title = "10段均衡器",
                    enabled = eqEnabled,
                    onToggle = viewModel::toggleEq,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 重置按钮
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            Button(
                                onClick = viewModel::resetEq,
                                enabled = eqEnabled,
                            ) {
                                Text("重置")
                            }
                        }

                        // 10个EQ滑块
                        eqBands.forEachIndexed { index, gain ->
                            EqBandSlider(
                                bandIndex = index,
                                frequency = viewModel.getEqBandFrequency(index),
                                gain = gain,
                                enabled = eqEnabled,
                                onGainChange = { newGain ->
                                    viewModel.setEqBandGain(index, newGain)
                                },
                            )
                        }
                    }
                }
            }

            // 低音增强
            item {
                EffectCard(
                    title = "低音增强",
                    enabled = bassBoostEnabled,
                    onToggle = viewModel::toggleBassBoost,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "强度",
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "${bassBoostStrength.toInt()}/10",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        Slider(
                            value = bassBoostStrength / IAudioEffectProcessor.BASS_BOOST_MAX,
                            onValueChange = { value ->
                                viewModel.setBassBoostStrength(
                                    value * IAudioEffectProcessor.BASS_BOOST_MAX,
                                )
                            },
                            enabled = bassBoostEnabled,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // 混响效果
            item {
                EffectCard(
                    title = "混响效果",
                    subtitle = "增加空间感和深度",
                    enabled = reverbEnabled,
                    onToggle = viewModel::toggleReverb,
                )
            }

            // 音量标准化
            item {
                EffectCard(
                    title = "音量标准化",
                    subtitle = "平衡不同歌曲的音量",
                    enabled = normalizerEnabled,
                    onToggle = viewModel::toggleNormalizer,
                )
            }
        }
    }
}

/**
 * 音效卡片组件
 */
@Composable
private fun EffectCard(
    title: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: (@Composable () -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 标题和开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                )
            }

            // 可选的内容区域
            if (content != null && enabled) {
                content()
            }
        }
    }
}

/**
 * EQ 频段滑块
 */
@Composable
private fun EqBandSlider(
    bandIndex: Int,
    frequency: Int,
    gain: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatFrequency(frequency),
                fontSize = 13.sp,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text = "${if (gain > 0) "+" else ""}${String.format("%.1f", gain)} dB",
                fontSize = 13.sp,
                color =
                    if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                fontWeight = FontWeight.Medium,
            )
        }

        Slider(
            value =
                (gain - IAudioEffectProcessor.EQ_GAIN_MIN) /
                    (IAudioEffectProcessor.EQ_GAIN_MAX - IAudioEffectProcessor.EQ_GAIN_MIN),
            onValueChange = { value ->
                val newGain =
                    IAudioEffectProcessor.EQ_GAIN_MIN +
                        value * (IAudioEffectProcessor.EQ_GAIN_MAX - IAudioEffectProcessor.EQ_GAIN_MIN)
                onGainChange(newGain)
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 格式化频率显示
 */
private fun formatFrequency(frequency: Int): String =
    if (frequency >= 1000) {
        "${frequency / 1000}kHz"
    } else {
        "${frequency}Hz"
    }
