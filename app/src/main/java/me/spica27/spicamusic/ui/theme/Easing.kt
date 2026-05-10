package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.Easing
import kotlin.math.pow

/**
 * 三次方缓入缓出（EaseInOutCubic）
 *
 * 前半段（0..0.5）用加速立方曲线，后半段（0.5..1）用减速立方曲线，
 * 衔接处导数连续（速度平滑），适合页面切换、抽屉展开等需要自然感的动画。
 *
 * 用法：
 *   tween(durationMillis = 300, easing = EaseInOutCubic)
 *   animationSpec = tween(easing = EaseInOutCubic)
 */
val EaseInOutCubic: Easing =
    Easing { fraction ->
        if (fraction < 0.5f) {
            4f * fraction * fraction * fraction
        } else {
            1f - (-2f * fraction + 2f).pow(3) / 2f
        }
    }
