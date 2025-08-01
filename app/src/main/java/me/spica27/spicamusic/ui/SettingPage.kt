package me.spica27.spicamusic.ui

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import me.spica27.spicamusic.R
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.viewModel.SettingViewModel
import me.spica27.spicamusic.wrapper.activityViewModel


// 设置页面
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
  navigator: NavBackStack? = null,
) {


  val settingViewModel: SettingViewModel = activityViewModel()
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
        CategoryItem(
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
          icon = ImageVector.vectorResource(R.drawable.ic_font_download),
          onClick = { })
      }
      item {
        CategoryItem(
          title = "扫描",
          icon = Icons.Outlined.Refresh,
          onClick = {
            navigator?.add(Routes.Scanner)
//            if (permissionState.allPermissionsGranted) {
//              // 扫描本地音乐
//              Toast.makeText(context, "开始扫描本地音乐", Toast.LENGTH_SHORT).show()
//              context.startService(Intent(context, RefreshMusicListService::class.java))
//            } else {
//              permissionState.launchMultiplePermissionRequest()
//              Toast.makeText(context, "请授予权限", Toast.LENGTH_SHORT).show()
//            }
          })
      }
      item {
        CategoryItem(
          title = "音效",
          icon = ImageVector.vectorResource(R.drawable.ic_outlined_equalizer),
          onClick = {
            navigator?.add(Routes.EQ)
          })
      }
      item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
      item {
        CategoryItem(
          title = "FAQ",
          icon = Icons.Outlined.MailOutline,
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
private fun TopBar() {
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