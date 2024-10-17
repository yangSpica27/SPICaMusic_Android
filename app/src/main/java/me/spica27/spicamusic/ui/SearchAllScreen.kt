package me.spica27.spicamusic.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import me.spica27.spicamusic.viewModel.MusicViewModel


/// 搜索所有歌曲的页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAllScreen(
  musicViewModel: MusicViewModel = hiltViewModel()
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = "搜索")
        },
        actions = { }
      )
    },
    content = { paddingValues ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues)
      ) {
        Column(modifier = Modifier.fillMaxSize()) {
          SearchBar(modifier = Modifier.padding(horizontal = 20.dp))
        }
      }
    }
  )
}

@Composable
private fun SearchBar(modifier: Modifier = Modifier) {
  val keyword = remember { mutableStateOf("") }
  Box(
    modifier = modifier.background(
      color = MaterialTheme.colorScheme.surfaceContainer,
      shape = MaterialTheme.shapes.small
    )
  ) {
    Row(
      modifier = Modifier
        .padding(vertical = 8.dp, horizontal = 16.dp)
        .fillMaxWidth(),
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.padding(8.dp)
      ) {
        Icon(
          modifier = Modifier.size(24.dp),
          imageVector = Icons.Default.Search,
          contentDescription = "search",
          tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      }

      Spacer(modifier = Modifier.width(8.dp))
      BasicTextField(
        value = keyword.value,
        onValueChange = { keyword.value = it },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.W600,
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}