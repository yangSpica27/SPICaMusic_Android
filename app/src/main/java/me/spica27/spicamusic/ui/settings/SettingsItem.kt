package me.spica27.spicamusic.ui.settings

import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.StateFlow

/**
 * 设置项基类
 */
sealed class SettingsItem {
    abstract val title: String
    abstract val subtitle: String?
    abstract val icon: (@Composable () -> Unit)?

    /**
     * 开关类设置项
     */
    data class SwitchItem(
        override val title: String,
        override val subtitle: String? = null,
        override val icon: (@Composable () -> Unit)? = null,
        val key: String,
        val valueFlow: StateFlow<Boolean>,
        val onValueChange: (Boolean) -> Unit,
    ) : SettingsItem()

    /**
     * 选择类设置项（单选）
     */
    data class SelectItem(
        override val title: String,
        override val subtitle: String? = null,
        override val icon: (@Composable () -> Unit)? = null,
        val key: String,
        val options: List<SelectOption>,
        val valueFlow: StateFlow<String>,
        val onValueChange: (String) -> Unit,
    ) : SettingsItem()

    /**
     * 跳转类设置项
     */
    data class NavigationItem(
        override val title: String,
        override val subtitle: String? = null,
        override val icon: (@Composable () -> Unit)? = null,
        val onClick: () -> Unit,
    ) : SettingsItem()

    /**
     * 分组标题
     */
    data class GroupHeader(
        override val title: String,
    ) : SettingsItem() {
        override val subtitle: String? = null
        override val icon: (@Composable () -> Unit)? = null
    }
}

/**
 * 选择项数据类
 */
data class SelectOption(
    val value: String,
    val label: String,
)
