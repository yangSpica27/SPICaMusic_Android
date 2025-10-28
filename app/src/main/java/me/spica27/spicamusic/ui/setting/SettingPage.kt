package me.spica27.spicamusic.ui.setting

import android.os.Build
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.launch
import me.spica27.spicamusic.BuildConfig
import me.spica27.spicamusic.R
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.ToastUtils
import org.koin.compose.koinInject

// 设置页面
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingPage(
    navigator: NavController = LocalNavController.current,
    pagerState: PagerState? = null,
) {
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    val dataStoreUtil = koinInject<DataStoreUtil>()

    val forceDarkThemeSettingState =
        dataStoreUtil.getForceDarkTheme.collectAsStateWithLifecycle(false)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
    ) {
        TopBar(
            onBackClick = {
                coroutineScope.launch {
                    pagerState?.animateScrollToPage(0)
                }
            },
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
        ) {
            item {
                CategoryItem2(
                    title =
                        if (forceDarkThemeSettingState.value) {
                            stringResource(R.string.setting_dark_mode)
                        } else {
                            stringResource(R.string.setting_light_mode)
                        },
                    icon =
                        if (forceDarkThemeSettingState.value) {
                            ImageVector.vectorResource(R.drawable.ic_dark_mode)
                        } else {
                            ImageVector.vectorResource(R.drawable.ic_outlined_sunny)
                        },
                    onPoint = {
                        val isTranslateRoute = navigator.currentDestination?.hasRoute(Routes.Translate::class) == true

                        if (isTranslateRoute) {
                            return@CategoryItem2
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            navigator.navigate(
                                Routes.Translate(
                                    it.x,
                                    it.y,
                                    !forceDarkThemeSettingState.value,
                                ),
                            )
                        }
                        coroutineScope.launch {
                            dataStoreUtil.saveForceDarkTheme(!forceDarkThemeSettingState.value)
                        }
                    },
                )
            }
//      item {
//        CategoryItem(
//          title = "歌词",
//          icon = ImageVector.vectorResource(R.drawable.ic_font_download),
//          onClick = { })
//      }
            item {
                CategoryItem(
                    title = stringResource(R.string.setting_scanner),
                    icon = Icons.Outlined.Refresh,
                    onClick = {
                        navigator?.navigate(Routes.Scanner)
                    },
                )
            }
            item {
                CategoryItem(
                    title = stringResource(R.string.setting_ignore_music),
                    icon = ImageVector.vectorResource(R.drawable.ic_block),
                    onClick = {
                        navigator?.navigate(Routes.IgnoreList)
                    },
                )
            }
            item {
                CategoryItem(
                    title = stringResource(R.string.setting_eq),
                    icon = ImageVector.vectorResource(R.drawable.ic_outlined_equalizer),
                    onClick = {
                        navigator?.navigate(Routes.EQ)
                    },
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
            item {
                CategoryItem(
                    title = stringResource(R.string.setting_faq),
                    icon = Icons.Outlined.MailOutline,
                    onClick = {
                        ToastUtils.showToast("待施工")
                    },
                )
            }
            item {
                CategoryItem(
                    title = stringResource(R.string.setting_about),
                    icon = Icons.Outlined.Info,
                    onClick = {
                        ToastUtils.showToast("待施工")
                    },
                )
            }
            item {
                AppVersion(
                    versionText = "Version ${BuildConfig.VERSION_NAME}",
                    copyrights = "© 2024 SPICa27",
                    onClick = {
                    },
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit = {},
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CategoryItem2(
    title: String,
    icon: ImageVector,
    onPoint: (Offset) -> Unit = {},
) {
    var xOnScreen by remember { mutableIntStateOf(0) }

    var yOnScreen by remember { mutableIntStateOf(0) }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    xOnScreen = coordinates.positionInWindow().x.toInt()
                    yOnScreen = coordinates.positionInWindow().y.toInt()
                }.pointerInput(Unit) {
                    detectTapGestures { offsetInSurface ->
                        // 计算相对于屏幕的坐标
                        val screenX = xOnScreen + offsetInSurface.x
                        val screenY = yOnScreen + offsetInSurface.y
                        onPoint(Offset(screenX, screenY)) // 将屏幕坐标传递给回调
                    }
                },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(title, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    icon: ImageVector,
    onClick: (Boolean) -> Unit,
    checked: Boolean,
) {
    Surface(
        onClick = {
            onClick(!checked)
        },
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = {
                    onClick(!checked)
                },
                thumbContent = {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                },
            )
        }
    }
}

@Composable
private fun AppVersion(
    versionText: String,
    copyrights: String,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            Box(
                modifier = Modifier.size(30.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    versionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.44f),
                )
                Text(
                    copyrights,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.44f),
                )
            }
        }
    }
}

@Composable
private fun TopBar(onBackClick: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp),
    ) {
        IconButton(
            onClick = {
                onBackClick()
            },
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.settings),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.title_settings),
            style =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                ),
            modifier =
                Modifier
                    .align(Alignment.Center),
        )
    }
}
