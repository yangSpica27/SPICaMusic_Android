package me.spica27.spicamusic.ui

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.spica27.spicamusic.R
import me.spica27.spicamusic.service.RefreshMusicListService
import me.spica27.spicamusic.viewModel.SettingViewModel


// 设置页面
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
  navigator: NavBackStack? = null,
) {

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

  val context = LocalContext.current
  val settingViewModel = hiltViewModel<SettingViewModel>()
  val autoPlaySettingState = settingViewModel.autoPlay.collectAsStateWithLifecycle(false)
  val autoScannerSettingState = settingViewModel.autoScanner.collectAsStateWithLifecycle(false)
  val forceDarkThemeSettingState =
    settingViewModel.forceDarkTheme.collectAsStateWithLifecycle(false).value

  val listState = rememberLazyListState()


  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Top
  ) {
    TopBar()
    LazyColumn(
      modifier = Modifier.weight(1f),
      state = listState
    ) {
      item {
        CategoryItem (
          title = if (forceDarkThemeSettingState) {
            "暗色模式"
          } else {
            "亮色模式"
          },
          icon = if (forceDarkThemeSettingState) {
            ImageVector.vectorResource(R.drawable.ic_dark_mode)
          } else {
            ImageVector.vectorResource(R.drawable.ic_outlined_sunny)
          },
          onClick = {
            settingViewModel.saveForceDarkTheme(!forceDarkThemeSettingState)
          }
        )
      }
      item {
        CategoryItem(
          title = "歌词",
          icon = Icons.Default.Menu,
          onClick = { })
      }
      item {
        CategoryItem(
          title = "扫描",
          icon = Icons.Outlined.Refresh,
          onClick = {
            if (permissionState.allPermissionsGranted) {
              // 扫描本地音乐
              Toast.makeText(context, "开始扫描本地音乐", Toast.LENGTH_SHORT).show()
              context.startService(Intent(context, RefreshMusicListService::class.java))
            } else {
              permissionState.launchMultiplePermissionRequest()
              Toast.makeText(context, "请授予权限", Toast.LENGTH_SHORT).show()
            }
          })
      }
      item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
      item {
        CategoryItem(
          title = "FAQ",
          icon = Icons.Outlined.Email,
          onClick = {

          })
      }
      item {
        CategoryItem(
          title = "关于",
          icon = Icons.Outlined.Info,
          onClick = {

          })
      }
      item {
        AppVersion(
          versionText = "Version ALPHA-3",
          copyrights = "© 2024 SPICa27",
          onClick = {

          })
      }
    }
  }
}


@Composable
private fun CategoryItem(title: String, icon: ImageVector, onClick: () -> Unit) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
      Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSurface
      )
      Text(title, style = MaterialTheme.typography.bodyLarge)
    }
  }
}


@Composable
private fun SwitchItem(
  title: String, icon: ImageVector,
  onClick: (Boolean) -> Unit,
  checked: Boolean
) {
  Surface(
    onClick = {
      onClick(!checked)
    },
    shape = MaterialTheme.shapes.medium,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSurface
      )
      Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
      Switch(
        checked = checked, onCheckedChange = {
          onClick(!checked)
        },
        thumbContent = {
          Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(SwitchDefaults.IconSize),
          )
        }
      )
    }
  }
}


@Composable
private fun AppVersion(versionText: String, copyrights: String, onClick: () -> Unit) {
  Surface(onClick = onClick) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.spacedBy(30.dp)
    ) {
      Box(
        modifier = Modifier.size(30.dp),
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          versionText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(0.44f)
        )
        Text(
          copyrights,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface.copy(0.44f)
        )
      }
    }
  }
}


@Composable
private fun TopBar(
  navigator: NavBackStack? = null,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
  ) {
    Text(
      text = "设置",
      style = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Black,
        fontSize = 22.sp
      ),
      modifier = Modifier
        .align(Alignment.Center)
    )
  }
}