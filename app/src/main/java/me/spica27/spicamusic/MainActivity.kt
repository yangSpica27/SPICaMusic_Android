package me.spica27.spicamusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import me.spica27.spicamusic.navigator.AppComposeNavigator
import me.spica27.spicamusic.ui.AppMain
import me.spica27.spicamusic.viewModel.MusicViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject
  internal lateinit var appComposeNavigator: AppComposeNavigator

  private val viewModel: MusicViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppMain(composeNavigator = appComposeNavigator)
    }
  }
}

