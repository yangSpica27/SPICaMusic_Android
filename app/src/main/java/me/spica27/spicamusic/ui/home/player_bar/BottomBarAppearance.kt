package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.common.entity.BottomBarLayout
import me.spica27.spicamusic.ui.theme.Shapes

/** 只切换外观参数，两种布局共用播放、导航和拖拽状态。 */
@Immutable
internal class BottomBarAppearance private constructor(
    val isFloating: Boolean,
) {
    val horizontalMargin = if (isFloating) FloatingBarDefaults.HorizontalMargin else 0.dp
    val bottomMargin = if (isFloating) FloatingBarDefaults.BottomMargin else 0.dp
    val gap = if (isFloating) FloatingBarDefaults.Gap else 0.dp
    val miniPlayerMinHeight = if (isFloating) 60.dp else 70.dp
    val navigationMinHeight = if (isFloating) 66.dp else 76.dp
    val navigationVerticalPadding = if (isFloating) 8.dp else 12.dp
    val revealTopRadius = if (isFloating) FloatingBarDefaults.CornerRadius else 20.dp
    val revealBottomRadius = if (isFloating) FloatingBarDefaults.CornerRadius else 0.dp
    val miniPlayerShape =
        if (isFloating) FloatingBarDefaults.Shape else RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    val navigationShape = if (isFloating) FloatingBarDefaults.Shape else RoundedCornerShape(0.dp)
    val indicatorShape = if (isFloating) Shapes.ExtraLarge2CornerBasedShape else RoundedCornerShape(20.dp)

    fun miniPlayerContainerColor(colors: ColorScheme) = if (isFloating) colors.surfaceContainer else colors.primaryContainer

    fun miniPlayerContentColor(colors: ColorScheme) = if (isFloating) colors.onSecondaryContainer else colors.onPrimaryContainer

    fun navigationContainerColor(colors: ColorScheme) = if (isFloating) colors.surfaceContainer else colors.surface

    companion object {
        private val traditional = BottomBarAppearance(isFloating = false)
        private val floating = BottomBarAppearance(isFloating = true)

        fun forLayout(layout: BottomBarLayout): BottomBarAppearance =
            when (layout) {
                BottomBarLayout.Traditional -> traditional
                BottomBarLayout.Floating -> floating
            }
    }
}
