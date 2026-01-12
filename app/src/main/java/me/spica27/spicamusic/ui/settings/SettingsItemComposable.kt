package me.spica27.spicamusic.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.extra.SuperDropdown

/**
 * 渲染单个设置项
 */
@Composable
fun SettingsItemView(
    item: SettingsItem,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is SettingsItem.GroupHeader -> {
            SmallTitle(
                text = item.title,
                modifier =
                    modifier
                        .fillMaxWidth(),
            )
        }

        is SettingsItem.SwitchItem -> {
            SwitchSettingsItem(item = item, modifier = modifier)
        }

        is SettingsItem.SelectItem -> {
            SelectSettingsItem(item = item, modifier = modifier)
        }

        is SettingsItem.NavigationItem -> {
            NavigationSettingsItem(item = item, modifier = modifier)
        }
    }
}

/**
 * 开关类设置项
 */
@Composable
private fun SwitchSettingsItem(
    item: SettingsItem.SwitchItem,
    modifier: Modifier = Modifier,
) {
    val currentValue by item.valueFlow.collectAsState()
    BasicComponent(
        modifier = modifier,
        startAction = {
            Box(
                modifier = Modifier.padding(end = 15.dp),
            ) {
                item.icon?.invoke()
            }
        },
        title = item.title,
        summary = item.subtitle,
        onClick = {
            item.onValueChange(!currentValue)
        },
        endActions = {
            Switch(
                checked = currentValue,
                onCheckedChange = item.onValueChange,
            )
        },
    )
}

/**
 * 选择类设置项
 */
@Composable
private fun SelectSettingsItem(
    item: SettingsItem.SelectItem,
    modifier: Modifier = Modifier,
) {
    val currentValue by item.valueFlow.collectAsState()

    SuperDropdown(
        modifier = modifier,
        startAction = {
            Box(
                modifier = Modifier.padding(end = 15.dp),
            ) {
                item.icon?.invoke()
            }
        },
        title = item.title,
        summary = item.subtitle,
        enabled = true,
        items =
            item.options.map {
                it.label
            },
        selectedIndex = item.options.indexOfFirst { it.value == currentValue },
        onSelectedIndexChange = { index ->
            val selectedOption = item.options.getOrNull(index)
            selectedOption?.let {
                item.onValueChange(it.value)
            }
        },
    )
}

/**
 * 跳转类设置项
 */
@Composable
private fun NavigationSettingsItem(
    item: SettingsItem.NavigationItem,
    modifier: Modifier = Modifier,
) {
    BasicComponent(
        modifier = modifier,
        startAction = {
            Box(
                modifier = Modifier.padding(end = 15.dp),
            ) {
                item.icon?.invoke()
            }
        },
        endActions = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        title = item.title,
        summary = item.subtitle,
        onClick = item.onClick,
    )
}

/**
 * 选择对话框
 */
@Composable
private fun SelectDialog(
    title: String,
    options: List<SelectOption>,
    selectedValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option.value) }
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RadioButton(
                            selected = option.value == selectedValue,
                            onClick = { onSelect(option.value) },
                        )
                        Text(text = option.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
