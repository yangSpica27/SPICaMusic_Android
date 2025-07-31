package me.spica27.spicamusic.widget


import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(
  onBack: () -> Unit,
  title: String,
  lazyListState: LazyListState? = null,
  backgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
  contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
  scrolledBackgroundColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainerLow,
  scrolledContentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {

  var scrolled by remember { mutableStateOf(false) }

  val backgroundColor = animateColorAsState(
    if (scrolled) {
      scrolledBackgroundColor
    } else {
      backgroundColor
    }
  )

  val contentColor = animateColorAsState(
    if (scrolled) {
      scrolledContentColor
    } else {
      contentColor
    }
  )


  if (lazyListState != null) {
    val isScrolled by remember {
      derivedStateOf {
        lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
      }
    }
    LaunchedEffect(isScrolled) {
      scrolled = isScrolled
    }
  }


  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = backgroundColor.value,
      titleContentColor = contentColor.value,
      navigationIconContentColor = contentColor.value
    ),
    navigationIcon = {
      IconButton(
        onClick = {
          onBack()
        }) {
        Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
      }
    }, title = {
      Text(
        title,
        style = MaterialTheme.typography.titleLarge.copy(
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.W500
        ),
      )
    })
}