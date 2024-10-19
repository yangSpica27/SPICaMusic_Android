package me.spica27.spicamusic.widget

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size


@Composable
fun VisualizerView(modifier: Modifier = Modifier) {
  var canvasSize by remember { mutableStateOf(Size(0f, 0f)) }
  Box(
    modifier = modifier
      .aspectRatio(1f)
  ) {
    Canvas(
      modifier = Modifier.fillMaxSize()
    ) {
      canvasSize = size
    }
  }
}