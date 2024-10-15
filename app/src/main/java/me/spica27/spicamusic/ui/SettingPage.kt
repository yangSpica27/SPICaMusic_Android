package me.spica27.spicamusic.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.spica27.spicamusic.service.RefreshMusicListService


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingPage() {

  val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    rememberMultiplePermissionsState(
      listOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.FOREGROUND_SERVICE,
        android.Manifest.permission.READ_MEDIA_AUDIO,
        android.Manifest.permission.POST_NOTIFICATIONS,
      )
    )
  } else {
    rememberMultiplePermissionsState(
      listOf(
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
      )
    )
  }


  val context = LocalContext.current


  Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {

      item {
        Text(
          text = "设置", style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.W600
          ), modifier = Modifier.padding(vertical = 20.dp)
        )
      }

      item {
        Card {
          Column {
            TextSettingItem(
              title = "申请权限", desc = "申请应用正常运行所需要的必要权限",
              onClick = {
                permissionState.launchMultiplePermissionRequest()
              }
            )
            TextSettingItem(
              title = "扫描", desc = "使用MediaStore扫描本地音乐",
              onClick = {
                if (permissionState.allPermissionsGranted) {
                  // 扫描本地音乐
                  Toast.makeText(context, "开始扫描本地音乐", Toast.LENGTH_SHORT).show()
                  context.startService(Intent(context, RefreshMusicListService::class.java))
                } else {
                  permissionState.launchMultiplePermissionRequest()
                  Toast.makeText(context, "请授予权限", Toast.LENGTH_SHORT).show()
                }
              }
            )
            TextSettingItem(
              title = "隐私政策", desc = "查看我们的隐私政策"
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(20.dp))
      }

      item {
        Card {
          Column {
            SwitchSettingItem(
              title = "自动播放", desc = "自动播放下一首", value = true
            )
            SwitchSettingItem(
              title = "自动扫描",
              desc = "定时更新/扫描本地音乐",
            )
            SwitchSettingItem(
              title = "暗色模式",
              desc = "是否启用暗色模式",
              value = false
            )
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(20.dp))
      }
      item {
        Card {
          Column {
            TextSettingItem(
              title = "主页", desc = "www.spica27.me"
            )
          }
        }
      }
      item {
        Spacer(modifier = Modifier.height(60.dp))
      }
    }
  }
}

@Composable
private fun SwitchSettingItem(
  title: String = "设置项",
  desc: String = "设置项描述",
  value: Boolean = false,
) {

  Row(
    Modifier
      .clickable { }
      .fillMaxWidth()
      .padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title, style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.W600
        )
      )
      Text(
        text = desc, style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      )
    }
    Switch(checked = value, onCheckedChange = { })
  }

}

@Composable
private fun TextSettingItem(
  title: String = "设置项", desc: String = "设置项描述", onClick: () -> Unit = {}
) {
  Row(
    Modifier
      .clickable { onClick() }
      .fillMaxWidth()
      .padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title, style = MaterialTheme.typography.bodyLarge.copy(
          fontWeight = FontWeight.W600
        )
      )
      Text(
        text = desc, style = MaterialTheme.typography.bodyMedium.copy(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
      )
    }
    Icon(imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight, contentDescription = "设置项")
  }
}

@Preview
@Composable
fun SettingPagePreview() {
  SettingPage()
}