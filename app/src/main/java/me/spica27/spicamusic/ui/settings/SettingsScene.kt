package me.spica27.spicamusic.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Scanner
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.common.collect.ImmutableList
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.ui.scan.ScannerScene
import org.koin.compose.viewmodel.koinViewModel

class SettingsScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: SettingsViewModel = koinViewModel()

        val darkMode by viewModel.darkMode.collectAsState()
        val keepScreenOn by viewModel.keepScreenOn.collectAsState()
        val spectrumValue by viewModel.dynamicSpectrumBackground.collectAsState()
        val coverTypeValue by viewModel.dynamicCoverType.collectAsState()

        val view = LocalView.current
        LaunchedEffect(darkMode) {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkMode
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { path.popTop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = { Text("设置") },
                )
            },
        ) { paddingValues ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // 外观
                item {
                    SettingsGroupHeader(title = "外观")
                }
                item {
                    SettingsSwitchItem(
                        title = "深色模式",
                        subtitle = "切换深色 / 浅色主题",
                        icon = {
                            Icon(
                                Icons.Default.Brightness6,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        checked = darkMode,
                        onCheckedChange = {
                            viewModel.setDarkMode(it)
                        },
                    )
                }
                item {
                    SettingsSelectItem(
                        title = "动态频谱背景",
                        subtitle = DynamicSpectrumBackground.fromString(spectrumValue).name,
                        icon = {
                            Icon(
                                Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        options =
                            ImmutableList.copyOf(
                                DynamicSpectrumBackground.presets.map {
                                    SelectOption(it.value, it.name)
                                },
                            ),
                        currentValue = spectrumValue,
                        onValueChange = viewModel::setDynamicSpectrumBackground,
                    )
                }
                item {
                    SettingsSelectItem(
                        title = "动态封面效果",
                        subtitle = DynamicCoverType.fromString(coverTypeValue).name,
                        icon = {
                            Icon(
                                Icons.Default.Album,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        options =
                            ImmutableList.copyOf(
                                DynamicCoverType.presets.map {
                                    SelectOption(it.value, it.name)
                                },
                            ),
                        currentValue = coverTypeValue,
                        onValueChange = viewModel::setDynamicCoverType,
                    )
                }
                item {
                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp,
                            ),
                    )
                }

                // 播放
                item {
                    SettingsGroupHeader(title = "播放")
                }
                item {
                    SettingsSwitchItem(
                        title = "屏幕常亮",
                        subtitle = "播放时保持屏幕不熄灭",
                        icon = {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        checked = keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                    )
                }
                item {
                    HorizontalDivider(
                        modifier =
                            Modifier.padding(
                                horizontal = 16.dp,
                                vertical = 4.dp,
                            ),
                    )
                }

                // 工具
                item {
                    SettingsGroupHeader(title = "工具")
                }
                item {
                    SettingsNavigationItem(
                        title = "扫描音乐",
                        subtitle = "扫描设备上的本地音乐文件",
                        icon = {
                            Icon(
                                Icons.Default.Scanner,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = { path.push(ScannerScene()) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
    )
}

@Composable
private fun SettingsSelectItem(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    options: ImmutableList<SelectOption>,
    currentValue: String,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon,
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable { showDialog = true },
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onValueChange(option.value)
                                        showDialog = false
                                    }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RadioButton(
                                selected = currentValue == option.value,
                                onClick = {
                                    onValueChange(option.value)
                                    showDialog = false
                                },
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("关闭")
                }
            },
        )
    }
}

@Composable
private fun SettingsNavigationItem(
    title: String,
    subtitle: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = icon,
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
