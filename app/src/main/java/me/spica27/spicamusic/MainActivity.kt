package me.spica27.spicamusic

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import linc.com.amplituda.Amplituda
import me.spica27.spicamusic.service.MusicService
import me.spica27.spicamusic.ui.AppMain
import me.spica27.spicamusic.utils.DataStoreUtil
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {




  @Inject
  lateinit var dataStoreUtil: DataStoreUtil


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    startService(Intent(this, MusicService::class.java))
    setContent {
      AppMain()
    }
    lifecycleScope.launch {
      dataStoreUtil.getForceDarkTheme.collectLatest {
        if (it) {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
          )
        } else {
          enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT,Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT,Color.TRANSPARENT)
          )
        }
      }
    }
  }


  @Inject
  lateinit var amplituda: Amplituda

  override fun onDestroy() {
    super.onDestroy()
    amplituda.clearCache()
  }

}

