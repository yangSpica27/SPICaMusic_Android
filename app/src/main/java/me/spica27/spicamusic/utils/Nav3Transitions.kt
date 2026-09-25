package me.spica27.spicamusic.utils

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.TransformOrigin

object Nav3Transitions {

    fun iosStyle(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(200))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it / 4 },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + scaleOut(
                    targetScale = 0.95f,
                    animationSpec = tween(400)
                ) + fadeOut(tween(200)
                ))

    fun iosStylePop(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { -it / 4 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + scaleIn(
            initialScale = 0.95f,
            animationSpec = tween(400)
        ) + fadeIn(tween(200))) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(200)))

    fun flip(): ContentTransform =
        (scaleIn(
            initialScale = 0.0f,
            animationSpec = tween(
                200,
                delayMillis = 200,
                easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
            ),
            transformOrigin = TransformOrigin(0.5f, 0.5f)
        ) + fadeIn(
            animationSpec = tween(100, delayMillis = 200)
        )) togetherWith
                (scaleOut(
                    targetScale = 0.0f,
                    animationSpec = tween(200, easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)),
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                ) + fadeOut(
                    animationSpec = tween(100)
                ))

    fun flipPop(): ContentTransform = flip()

    fun cube(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + scaleIn(
            initialScale = 0.6f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            transformOrigin = TransformOrigin(0f, 0.5f)
        ) + fadeIn(
            animationSpec = tween(300, delayMillis = 100)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                    transformOrigin = TransformOrigin(1f, 0.5f)
                ) + fadeOut(
                    animationSpec = tween(300)
                ))

    fun cubePop(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + scaleIn(
            initialScale = 0.6f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            transformOrigin = TransformOrigin(1f, 0.5f)
        ) + fadeIn(
            animationSpec = tween(300, delayMillis = 100)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(500, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
                    transformOrigin = TransformOrigin(0f, 0.5f)
                ) + fadeOut(
                    animationSpec = tween(300)
                ))

    fun slideUp(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 0.85f,
            animationSpec = tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + fadeIn(
            animationSpec = tween(400, delayMillis = 100)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { -it / 4 },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 1.05f,
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(400)
                ))

    fun slideUpPop(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { -it / 4 },
            animationSpec = tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 1.05f,
            animationSpec = tween(600, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + fadeIn(
            animationSpec = tween(400)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(400, delayMillis = 100)
                ))

    fun rocket(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { it * 2 },
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 0.6f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + fadeIn(
            animationSpec = tween(350)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(
                        400,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.7f,
                    animationSpec = tween(
                        400,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(250)
                ))

    fun rocketPop(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(400, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 0.7f,
            animationSpec = tween(400, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + fadeIn(
            animationSpec = tween(250)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { it * 2 },
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.6f,
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(350)
                ))

    fun spiral(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        ) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
            transformOrigin = TransformOrigin(0.8f, 0.2f)
        ) + fadeIn(
            animationSpec = tween(500, delayMillis = 100)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(0.2f, 0.8f)
                ) + fadeOut(
                    animationSpec = tween(500)
                ))

    fun spiralPop(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        ) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f))
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(700, easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)),
            transformOrigin = TransformOrigin(0.2f, 0.8f)
        ) + fadeIn(
            animationSpec = tween(500)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(
                        700,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(0.8f, 0.2f)
                ) + fadeOut(
                    animationSpec = tween(500, delayMillis = 100)
                ))

    fun zoom(): ContentTransform =
        (scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(450, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
        ) + fadeIn(
            animationSpec = tween(350, delayMillis = 50)
        )) togetherWith
                (scaleOut(
                    targetScale = 1.3f,
                    animationSpec = tween(
                        350,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(250)
                ))

    fun zoomPop(): ContentTransform =
        (scaleIn(
            initialScale = 1.3f,
            animationSpec = tween(350, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))
        ) + fadeIn(
            animationSpec = tween(250)
        )) togetherWith
                (scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(
                        450,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + fadeOut(
                    animationSpec = tween(350, delayMillis = 50)
                ))

    fun expand(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
            transformOrigin = TransformOrigin(0.5f, 0f)
        ) + fadeIn(
            animationSpec = tween(350, delayMillis = 100)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { -it / 2 },
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(0.5f, 1f)
                ) + fadeOut(
                    animationSpec = tween(350)
                ))

    fun expandPop(): ContentTransform =
        (slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f))
        ) + scaleIn(
            initialScale = 0.5f,
            animationSpec = tween(500, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)),
            transformOrigin = TransformOrigin(0.5f, 1f)
        ) + fadeIn(
            animationSpec = tween(350, delayMillis = 100)
        )) togetherWith
                (slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.5f,
                    animationSpec = tween(
                        500,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(0.5f, 0f)
                ) + fadeOut(
                    animationSpec = tween(350)
                ))

    fun fade(): ContentTransform =
        fadeIn(
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) togetherWith
                fadeOut(
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )

    fun fadePop(): ContentTransform = fade()

    fun rotate(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { it },
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + scaleIn(
            initialScale = 0.4f,
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            transformOrigin = TransformOrigin(0f, 0f)
        ) + fadeIn(
            animationSpec = tween(400, delayMillis = 100)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.4f,
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(1f, 1f)
                ) + fadeOut(
                    animationSpec = tween(400)
                ))

    fun rotatePop(): ContentTransform =
        (slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f))
        ) + scaleIn(
            initialScale = 0.4f,
            animationSpec = tween(600, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            transformOrigin = TransformOrigin(1f, 1f)
        ) + fadeIn(
            animationSpec = tween(400, delayMillis = 100)
        )) togetherWith
                (slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    )
                ) + scaleOut(
                    targetScale = 0.4f,
                    animationSpec = tween(
                        600,
                        easing = CubicBezierEasing(0.6f, 0.04f, 0.98f, 0.34f)
                    ),
                    transformOrigin = TransformOrigin(0f, 0f)
                ) + fadeOut(
                    animationSpec = tween(400)
                ))
}