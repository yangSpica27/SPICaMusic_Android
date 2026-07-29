package me.spica27.spicamusic.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

val ListItemFadeInSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = 240, easing = FastOutSlowInEasing)

val ListItemFadeOutSpec: FiniteAnimationSpec<Float> = tween(durationMillis = 160)
