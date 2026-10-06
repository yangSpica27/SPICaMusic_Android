package me.spica27.spicamusic

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import me.jessyan.autosize.internal.CustomAdapt
import me.spica27.spicamusic.ui.AppScaffold
import me.spica27.spicamusic.ui.adaptive.AppWindowInfo
import me.spica27.spicamusic.ui.adaptive.LocalAppWindowInfo
import me.spica27.spicamusic.ui.adaptive.applyWindowAutoSize
import me.spica27.spicamusic.ui.adaptive.prepareAutoSize
import me.spica27.spicamusic.ui.adaptive.readAppWindowInfo
import me.spica27.spicamusic.ui.audioeffects.AudioEffectsViewModel
import kotlin.math.abs

/**
 * 主 Activity
 */
class MainActivity :
    ComponentActivity(),
    CustomAdapt {
    private val audioEffectsViewModel by viewModels<AudioEffectsViewModel>()
    private var appWindowInfo by mutableStateOf<AppWindowInfo?>(null)
    private var contentDensity by mutableStateOf<Density?>(null)
    private val windowLayoutListener =
        View.OnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                refreshWindowAdaptation()
            }
        }

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

        refreshWindowAdaptation()
        window.decorView.addOnLayoutChangeListener(windowLayoutListener)
        setContent {
            val info = appWindowInfo ?: return@setContent
            val density = contentDensity ?: return@setContent
            CompositionLocalProvider(
                LocalAppWindowInfo provides info,
                LocalDensity provides density,
            ) {
                AppScaffold()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        refreshWindowAdaptation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshWindowAdaptation()
    }

    override fun onDestroy() {
        window.decorView.removeOnLayoutChangeListener(windowLayoutListener)
        super.onDestroy()
    }

    override fun isBaseOnWidth(): Boolean = true

    override fun getSizeInDp(): Float {
        val info = readAppWindowInfo()
        info.prepareAutoSize()
        return info.mode.designWidthDp
    }

    private fun refreshWindowAdaptation() {
        val info = readAppWindowInfo()
        val expectedDensity = info.widthPx / info.mode.designWidthDp
        if (
            info == appWindowInfo &&
            contentDensity?.fontScale == resources.configuration.fontScale &&
            abs(resources.displayMetrics.density - expectedDensity) < 0.0001f
        ) {
            return
        }
        applyWindowAutoSize(info)
        appWindowInfo = info
        contentDensity = Density(this)
    }
}
