package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * 列表项淡入。
 *
 * 进场用强 ease-out：起步即动，用户注视的第一帧就有反应。
 * 原先的 FastOutSlowInEasing 是缓入缓出，起步慢会延迟这一刻。
 */
val ListItemFadeInSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = 220, easing = EaseOutEmphasized)

/**
 * 列表项淡出。
 *
 * 时长比淡入短：移除是系统对用户操作的响应，不该让用户等。
 * 必须显式写 easing——tween 的默认值是 FastOutSlowInEasing（缓入缓出）。
 */
val ListItemFadeOutSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = 160, easing = EaseOutEmphasized)

/** 入场缩放起点：AUDIT 区间 0.9–0.97；取值对齐 MusicPage 既有范本 */
val ScaleEnterFrom = 0.92f

/** 退场缩放终点：退场更短，幅度更收敛 */
val ScaleExitTo = 0.94f

/** 移除/消失缩放终点（必须配合 alpha 淡出，不允许缩到 0） */
val ScaleDismissTo = 0.92f
