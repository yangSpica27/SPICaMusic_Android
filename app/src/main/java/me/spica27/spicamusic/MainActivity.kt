package me.spica27.spicamusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
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
    enableEdgeToEdge()
    startService(Intent(this, MusicService::class.java))
    setContent {
      AppMain()
    }
  }


  @Inject
  lateinit var amplituda: Amplituda

  override fun onDestroy() {
    super.onDestroy()
    amplituda.clearCache()
  }

}

