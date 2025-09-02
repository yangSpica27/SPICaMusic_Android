package me.spica27.spicamusic.widget.blur

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


fun Modifier.uniformBlur() = uniformBlurImpl()


@Composable
fun Modifier.progressiveBlur(): Modifier = progressiveBlurImpl()
