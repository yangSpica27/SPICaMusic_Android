package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.Easing
import kotlin.math.pow
import me.spica27.navkit.motion.EaseOutEmphasized as NavkitEaseOutEmphasized
import me.spica27.navkit.motion.EaseOutStrong as NavkitEaseOutStrong

/**
 * Material emphasized-decelerate（强 ease-out）
 *
 * 快速起步、柔和落定：进/出场元素的标准缓动——用户注视的起始时刻即时响应，
 * 收尾平滑不生硬。UI 进场一律优先用它，避免 ease-in 类曲线的迟钝起步。
 *
 * 用法：
 *   tween(durationMillis = 180, easing = EaseOutEmphasized)
 */
val EaseOutEmphasized: Easing = NavkitEaseOutEmphasized

val EaseOutStrong: Easing = NavkitEaseOutStrong

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
