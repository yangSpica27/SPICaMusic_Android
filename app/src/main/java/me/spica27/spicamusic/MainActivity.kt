package me.spica27.spicamusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.service.MusicService
import me.spica27.spicamusic.ui.AppMain
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  internal lateinit var appComposeNavigator: AppComposeNavigator


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    startService(Intent(this, MusicService::class.java))
    setContent {
      AppMain(composeNavigator = appComposeNavigator)
    }
  }
}

