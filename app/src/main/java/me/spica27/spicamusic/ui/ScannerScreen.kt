package me.spica27.spicamusic.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.spica27.spicamusic.service.RefreshMusicListService
import me.spica27.spicamusic.widget.SimpleTopBar

/**
 * 扫描页面
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(navigator: NavBackStack? = null) {

  // 权限状态
  val permissionState =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      rememberMultiplePermissionsState(
        listOf(
          android.Manifest.permission.FOREGROUND_SERVICE,
          android.Manifest.permission.READ_MEDIA_AUDIO,
          android.Manifest.permission.POST_NOTIFICATIONS,
          android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK,
        )
      )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      rememberMultiplePermissionsState(
        listOf(
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

  var isScanning by remember { mutableStateOf(false) }

  var text by remember { mutableStateOf("") }

  val context = LocalContext.current

  LaunchedEffect(
    isScanning
  ) {
    if (isScanning) {
      context.startService(Intent(context, RefreshMusicListService::class.java))
      isScanning = false
      text = "扫描完成"
    }
  }

  Scaffold(
    topBar = {
      SimpleTopBar(
        title = "扫描",
        onBack = { navigator?.removeLastOrNull() }
      )
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues),
      contentAlignment = Alignment.TopStart
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        AnimatedVisibility(
          visible = !permissionState.allPermissionsGranted
        ) {
          RequestCard(permissionState)
        }
        AnimatedVisibility(
          visible = permissionState.allPermissionsGranted
        ) {
          ElevatedButton(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp),
            onClick = {
              if (!isScanning) {
                isScanning = true
              }
            },
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.elevatedButtonColors().copy(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
          ) {
            if (isScanning) {
              Text("正在扫描")
            } else {
              Text("开始扫描")
            }
          }
        }
        Text(
          text,
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp),
        )
      }
    }
  }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestCard(permissionState: MultiplePermissionsState) {
  Column(
    modifier = Modifier
      .padding(
        horizontal = 16.dp
      )
      .background(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.shapes.medium
      )
      .padding(16.dp),
  ) {
    Text(
      "本应用需要获取本地存储权限，以访问本机存储的所有歌曲文件",
      modifier = Modifier.padding(vertical = 16.dp),
      color = MaterialTheme.colorScheme.onSurface.copy(0.44f),
      style = MaterialTheme.typography.bodyLarge
    )
    Spacer(
      modifier = Modifier.height(20.dp)
    )
    Row(
      horizontalArrangement = Arrangement.End
    ) {
      ElevatedButton(
        onClick = {
          permissionState.launchMultiplePermissionRequest()
        },
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.elevatedButtonColors().copy(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
      ) {
        Text("授权")
      }
    }
  }
}


//@OptIn(ExperimentalPermissionsApi::class)
//@Composable
//fun PermissionCard(permissionState: MultiplePermissionsState) {
//  Card(
//    modifier = Modifier.padding(16.dp)
//  ) {
//    Text(
//      """
//            使用本应用的用户应知晓以下内容：
//
//            1. 本应用所提供的网络歌词获取功能需要使用网络权限，如无此需求可拒绝网络权限授予；
//            2. 本应用所涉及的网络接口调用均不以获取用户个人唯一标识为前提，以此确保用户个人信息和隐私安全；
//            3. 本应用本体及代码基于MIT协议,任何人免费获得本软件和相关文档文件（“软件”）副本的许可，不受限制地处理本软件，包括但不限于使用、复制、修改、合并 、发布、分发、再许可的权利；
//
//            未来此协议可能有扩充的可能性，认可本协议内容即视为同意未来的变更。
//
//            SPICa27
//            """
//    )
//  }
//}