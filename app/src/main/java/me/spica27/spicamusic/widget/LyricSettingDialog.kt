package me.spica27.spicamusic.widget

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.spica27.spicamusic.R
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.repository.LyricRepository
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.clickableNoRippleWithVibration
import org.koin.compose.koinInject
import kotlin.math.absoluteValue

/**
 * 歌词设置弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricSettingDialog(
    onDismissRequest: () -> Unit,
    dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
    dialogBackgroundColor: Color = MaterialTheme.colorScheme.background,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    song: Song,
    navController: NavController? = null,
    // 窗口是否透明
    dialogBackgroundIsTranslate: (Boolean) -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()

    // 字体大小设置
    val fontSize = dataStoreUtil.getLyricFontSize().collectAsState(null).value

    // 字重设置
    val fontWeight = dataStoreUtil.getLyricFontWeight().collectAsState(null).value

    val lyricRepository = koinInject<LyricRepository>()

    // 这首歌用户有没有绑定歌词
    val haveLyric =
        lyricRepository
            .getLyrics(song.mediaStoreId)
            .map { it != null }
            .collectAsState(false)
            .value
    val delay = lyricRepository.getDelay(song.mediaStoreId).collectAsState(null).value

    // 有没有在调整字号
    var isSeekFontSize by remember { mutableStateOf(false) }

    // 有没有在调整字重
    var isSeekFontWeight by remember { mutableStateOf(false) }

    // 有没有在调整歌词速度
    var isSeekLrcSpeed by remember { mutableStateOf(false) }

    // 弹窗背景颜色
    var dialogBackgroundColor by remember { mutableStateOf(dialogBackgroundColor) }

    LaunchedEffect(dialogBackgroundColor) {
        dialogBackgroundIsTranslate.invoke(dialogBackgroundColor.alpha == 1f)
    }

    val dialogBackgroundColorAnimValue = animateColorAsState(dialogBackgroundColor, tween(200))

    // 字体颜色
    var textColor by remember { mutableStateOf(textColor) }

    val textColorAnimValue = animateColorAsState(textColor, tween(200))

    var showFontWeightSlider by remember { mutableStateOf(false) }

    var showFontSizeSlider by remember { mutableStateOf(false) }

    var showSpeedSlider by remember { mutableStateOf(false) }

    LaunchedEffect(isSeekFontSize) {
        dialogBackgroundColor =
            if (isSeekFontSize) {
                dialogBackgroundColor.copy(alpha = 0f)
            } else {
                dialogBackgroundColor.copy(alpha = 1f)
            }
        textColor =
            if (isSeekFontSize) {
                textColor.copy(alpha = 0f)
            } else {
                textColor.copy(alpha = 1f)
            }
        showFontWeightSlider = !isSeekFontSize
        showSpeedSlider = !isSeekFontSize
    }

    LaunchedEffect(isSeekFontWeight) {
        dialogBackgroundColor =
            if (isSeekFontWeight) {
                dialogBackgroundColor.copy(alpha = 0f)
            } else {
                dialogBackgroundColor.copy(alpha = 1f)
            }
        textColor =
            if (isSeekFontWeight) {
                textColor.copy(alpha = 0f)
            } else {
                textColor.copy(alpha = 1f)
            }
        showFontSizeSlider = !isSeekFontWeight
        showSpeedSlider = !isSeekFontWeight
    }

    LaunchedEffect(isSeekLrcSpeed) {
    }

    if (fontWeight != null && fontSize != null) {
        AlertDialog(
            containerColor = dialogBackgroundColorAnimValue.value,
            shape = MaterialTheme.shapes.large,
            onDismissRequest = { onDismissRequest() },
            title = {
                Text(stringResource(R.string.title_dialog_lrc_ui_setting), color = textColor)
            },
            text = {
                (LocalView.current.parent as DialogWindowProvider).window.setDimAmount(0f)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TitleText(
                        stringResource(R.string.lrc_setting_font_size, fontSize),
                        textColor = textColorAnimValue.value,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .alpha(
                                    if (showFontSizeSlider) {
                                        1f
                                    } else {
                                        0f
                                    },
                                ),
                        progress = fontSize.toFloat(),
                        onProgressChange = {
                            coroutineScope.launch {
                                dataStoreUtil.setLyricFontSize(it.toInt())
                            }
                            isSeekFontSize = true
                        },
                        onProgressChangeFinished = {
                            isSeekFontSize = false
                        },
                        maxValue = 24f,
                        minValue = 16f,
                        decimalPlaces = 1,
                    )
                    Spacer(Modifier.height(8.dp))
                    TitleText(
                        stringResource(R.string.lrc_setting_font_weight, fontWeight),
                        textColor = textColorAnimValue.value,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .alpha(
                                    if (showFontWeightSlider) {
                                        1f
                                    } else {
                                        0f
                                    },
                                ),
                        progress = fontWeight.toFloat(),
                        onProgressChange = {
                            coroutineScope.launch {
                                dataStoreUtil.setLyricFontWeight(it.toInt())
                            }
                            isSeekFontWeight = true
                        },
                        onProgressChangeFinished = {
                            isSeekFontWeight = false
                        },
                        minValue = 100f,
                        maxValue = 900f,
                        decimalPlaces = 1,
                    )

                    if (haveLyric && delay != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TitleText(
                            if (delay > 0) {
                                "延迟${delay.absoluteValue}毫秒"
                            } else {
                                "加快${delay.absoluteValue}毫秒"
                            },
                            textColor = textColorAnimValue.value,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SimpleSlider(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .alpha(
                                        if (showSpeedSlider) {
                                            1f
                                        } else {
                                            0f
                                        },
                                    ),
                            value = delay.toFloat(),
                            onValueChange = {
                                val i =
                                    if (it > 0) {
                                        1
                                    } else {
                                        -1
                                    }
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (it.absoluteValue < 250) {
                                        lyricRepository.setDelay(song.mediaStoreId, 0)
                                    } else if (it.absoluteValue < 500 * i) {
                                        lyricRepository.setDelay(song.mediaStoreId, 250L * i)
                                    } else if (it.absoluteValue < 750 * i) {
                                        lyricRepository.setDelay(song.mediaStoreId, 500L * i)
                                    } else if (it.absoluteValue < 1000) {
                                        lyricRepository.setDelay(song.mediaStoreId, 750L * i)
                                    } else {
                                        lyricRepository.setDelay(
                                            song.mediaStoreId,
                                            (it / 1000).toInt() * 1000L,
                                        )
                                    }
                                }
                                isSeekLrcSpeed = true
                            },
                            onValueChangeFinished = {
                                isSeekLrcSpeed = false
                            },
                            valueRange = -8000f..8000f,
                            steps = 1000,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.lrc_setting_change_lrc),
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.clickableNoRippleWithVibration {
                                onDismissRequest.invoke()
                                navController?.clearBackStack(Routes.Main)
                                navController?.navigate(
                                    Routes.LyricsSearch(
                                        song = song,
                                    ),
                                    navOptions =
                                        NavOptions
                                            .Builder()
                                            .setLaunchSingleTop(true)
                                            .build(),
                                )
                            },
                    )
                    Text(
                        stringResource(R.string.lrc_setting_full_screen),
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier.clickableNoRippleWithVibration {
                                onDismissRequest.invoke()
                                navController?.clearBackStack(Routes.Main)
                                navController?.navigate(
                                    Routes.FullScreenLrc,
                                    navOptions =
                                        NavOptions
                                            .Builder()
                                            .setLaunchSingleTop(true)
                                            .build(),
                                )
                            },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 确认
                    onDismissRequest.invoke()
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    }
}

@Composable
private fun TitleText(
    text: String,
    textColor: Color,
) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth(),
        style =
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W500,
                color = textColor,
            ),
    )
}
