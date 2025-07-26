package me.spica27.spicamusic.ui

import android.widget.EditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.db.entity.Song


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSearchScreen(
  song: Song
) {
  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          IconButton(
            onClick = {

            }
          ) {
            Icon(Icons.AutoMirrored.Default.KeyboardArrowLeft, contentDescription = "Back")
          }
        },
        title = {
          Text(
            "搜索歌词",
            style = MaterialTheme.typography.titleLarge.copy(
              color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
              fontWeight = FontWeight.ExtraBold
            ),
          )
        }
      )
    }

  ) { paddingValues ->
    Box(
      modifier = Modifier.padding(paddingValues)
    ) {

      Column(
        modifier = Modifier.fillMaxSize()
      ) {
        TopPanel(song)
        HorizontalDivider()
        ListView(
          state = STATE.LOADING,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}


@Composable
private fun TopPanel(song: Song) {

  val songName = rememberSaveable { mutableStateOf(song.displayName) }

  val artists = rememberSaveable { mutableStateOf(song.artist) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    EditText(
      leftLabel = "歌名",
      rightLabel = "请输入歌名",
      text = songName.value,
      onValueChange = {
        songName.value = it
      }
    )
    EditText(
      leftLabel = "歌手",
      rightLabel = "请输入歌手名称",
      text = artists.value,
      onValueChange = {
        artists.value = it
      }
    )
    OutlinedButton(
      onClick = {},
      modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()
    ) {
      Text("搜索")
    }
  }
}

enum class STATE {
  IDEA,
  LOADING,
  ERROR,
  SUCCESS
}

@Composable
private fun ListView(
  state: STATE,
  modifier: Modifier,
) {
  Box(
    modifier = modifier
  ) {
    when (state) {
      STATE.IDEA -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text("空空如也")
        }
      }

      STATE.LOADING -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator()
        }
      }

      STATE.ERROR -> {
        Box(
          modifier = Modifier.fillMaxSize(),
          contentAlignment = Alignment.Center
        ) {
          Text("加载失败")
        }
      }

      STATE.SUCCESS -> {
      }
    }
  }
}


@Composable
private fun EditText(
  leftLabel: String,
  rightLabel: String,
  text: String,
  onValueChange: (String) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp, horizontal = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(
      leftLabel,
      modifier = Modifier.width(60.dp),
      style = MaterialTheme.typography.bodyMedium.copy()
    )
    TextField(
      value = text,
      onValueChange = onValueChange,
      textStyle = MaterialTheme.typography.bodyMedium,
      placeholder = { Text(rightLabel) },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
      maxLines = 2,
      shape = MaterialTheme.shapes.small,
      colors = TextFieldDefaults.colors().copy(
        disabledIndicatorColor = Color.Transparent,
        errorIndicatorColor = Color.Transparent,
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
      )
    )
  }
}


@Preview()
@Composable
private fun Preview(modifier: Modifier = Modifier) {
  LyricsSearchScreen(
    song = Song(
      mediaStoreId = 1,
      songId = 1,
      path = "TODO()",
      displayName = "歌曲名称",
      artist = "",
      size = 0,
      like = true,
      duration = 0,
      sort = 1,
      playTimes = 100,
      lastPlayTime = 0,
      mimeType = "",
      albumId = 1,
      sampleRate = 1,
      bitRate = 1,
      channels = 2,
      digit = 1,
    )
  )
}