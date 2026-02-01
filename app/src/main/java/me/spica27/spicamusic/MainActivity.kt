package me.spica27.spicamusic

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import me.jessyan.autosize.internal.CustomAdapt
import me.spica27.spicamusic.ui.AppScaffold
import me.spica27.spicamusic.ui.audioeffects.AudioEffectsViewModel

/**
 * 主 Activity
 */
class MainActivity :
    ComponentActivity(),
    CustomAdapt {
    private val audioEffectsViewModel by viewModels<AudioEffectsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启用边缘到边缘显示
        enableEdgeToEdge(
            statusBarStyle =
                SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                ),
            navigationBarStyle =
                SystemBarStyle.auto(
                    Color.TRANSPARENT,
                    Color.TRANSPARENT,
                ),
        )

        setContent {
            AppScaffold()
        }
    }

    override fun isBaseOnWidth(): Boolean = true

    /**
     * 设计稿基准尺寸（dp）
     * 竖屏：375dp（手机设计稿）
     * 横屏：1024dp（平板/横屏设计稿）
     */
    override fun getSizeInDp(): Float = 375f
}
