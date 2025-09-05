package me.spica27.spicamusic

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.spica27.spicamusic.media.SpicaPlayer
import me.spica27.spicamusic.ui.AppMain
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.doOnMainThreadIdle
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject

@UnstableApi
class MainActivity : ComponentActivity() {
    private val dataStoreUtil: DataStoreUtil by inject()

    private val player = get<SpicaPlayer>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        doOnMainThreadIdle({
            player.init()
        }, 2500)
        setContent {
            AppMain()
        }
        lifecycleScope.launch {
            dataStoreUtil.getForceDarkTheme.collectLatest {
                if (it) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                    )
                }
            }
        }
    }
}
