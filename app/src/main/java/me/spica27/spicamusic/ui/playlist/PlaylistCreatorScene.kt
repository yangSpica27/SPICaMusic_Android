package me.spica27.spicamusic.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.ui.widget.PlaylistCoverView
import org.koin.compose.viewmodel.koinActivityViewModel

/**
 * 创建歌单界面
 *
 * - 顶部导航栏：返回按钮 + "创建" 确认按钮
 * - 中央区域：空歌单封面预览（马赛克占位）+ 歌单名称输入框
 * - 名称为空时点确认显示错误提示；输入合法后自动清除错误状态
 */
class PlaylistCreatorScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: PlaylistViewModel = koinActivityViewModel()

        // 歌单名称本地状态
        var name by remember { mutableStateOf("") }
        // 是否显示"名称不能为空"错误
        var showError by remember { mutableStateOf(false) }
        // 输入框焦点请求器，进场后自动弹出键盘
        val focusRequester = remember { FocusRequester() }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        // 确认逻辑：校验 → 创建 → 返回
        fun confirm() {
            if (name.isBlank()) {
                showError = true
                return
            }
            viewModel.createPlaylist(name.trim())
            path.popTop()
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
                    title = { Text("创建歌单") },
                    actions = {
                        TextButton(
                            onClick = { confirm() },
                            enabled = name.isNotBlank(),
                        ) {
                            Text("创建")
                        }
                    },
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 空歌单封面预览
                Box(
                    modifier =
                        Modifier
                            .size(120.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    PlaylistCoverView(
                        albumIds = emptyList(),
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // 歌单名称输入框
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue ->
                        name = newValue
                        // 开始输入后立即清除错误提示
                        if (showError && newValue.isNotBlank()) showError = false
                    },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                    label = { Text("歌单名称") },
                    placeholder = { Text("我的歌单") },
                    isError = showError,
                    supportingText =
                        if (showError) {
                            { Text("请输入歌单名称") }
                        } else {
                            null
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                )
            }
        }
    }
}
