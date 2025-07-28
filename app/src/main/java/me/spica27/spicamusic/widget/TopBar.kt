package me.spica27.spicamusic.widget


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleTopBar(
  onBack: () -> Unit,
  title: String,
) {
  TopAppBar(navigationIcon = {
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