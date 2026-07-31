package me.spica27.spicamusic.cloud

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class RemoteMusicScene(
    private val provider: RemoteMusicProvider,
) : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: RemoteMusicViewModel =
            koinViewModel(key = "remote_music_${provider.name}") {
                parametersOf(provider)
            }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val songs = viewModel.songs.collectAsLazyPagingItems()
        var showLogin by rememberSaveable(provider) { mutableStateOf(state.accounts.isEmpty()) }
        var searchText by rememberSaveable(provider) { mutableStateOf("") }

        LaunchedEffect(state.accounts.size) {
            if (state.accounts.isEmpty()) showLogin = true
            if (state.accounts.isNotEmpty() && !state.isConnecting && state.error == null) {
                showLogin = false
            }
        }

        RemoteSceneScaffold(
            title = provider.displayName,
            onBack = { path.popTop() },
            actions = {
                if (state.selectedAccount != null && !showLogin) {
                    IconButton(onClick = { songs.refresh() }) {
                        Icon(Icons.Default.Refresh, "Refresh")
                    }
                }
            },
        ) { padding ->
            if (showLogin) {
                when (provider) {
                    RemoteMusicProvider.SUBSONIC ->
                        SubsonicLogin(
                            state = state,
                            hasExistingAccount = state.accounts.isNotEmpty(),
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onCancel = {
                                viewModel.clearError()
                                showLogin = false
                            },
                            onLogin = viewModel::loginSubsonic,
                        )
                    RemoteMusicProvider.NETEASE,
                    RemoteMusicProvider.QQ_MUSIC,
                    ->
                        CookieWebLogin(
                            provider = provider,
                            state = state,
                            hasExistingAccount = state.accounts.isNotEmpty(),
                            modifier = Modifier.fillMaxSize().padding(padding),
                            onCancel = {
                                viewModel.clearError()
                                showLogin = false
                            },
                            onCookiesCaptured = viewModel::loginWithCookies,
                        )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding =
                        PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = padding.calculateTopPadding() + 10.dp,
                            bottom = 36.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "account", contentType = "account") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.selectedAccount?.let { account ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            account.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            provider.accountSubtitle(account),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = viewModel::removeSelectedAccount) {
                                        Icon(Icons.Default.DeleteOutline, "Remove account")
                                    }
                                    IconButton(onClick = { showLogin = true }) {
                                        Icon(Icons.Default.Add, "Add account")
                                    }
                                }
                            }
                            if (state.accounts.size > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.accounts.take(3).forEach { account ->
                                        if (account.id == state.selectedAccount?.id) {
                                            FilledTonalButton(
                                                onClick = { viewModel.selectAccount(account.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                            ) {
                                                Text(account.displayName, maxLines = 1)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { viewModel.selectAccount(account.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                            ) {
                                                Text(account.displayName, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("搜索云端音乐") },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                )
                                FilledTonalButton(onClick = { viewModel.search(searchText) }) {
                                    Text("搜索")
                                }
                            }
                        }
                    }

                    items(
                        count = songs.itemCount,
                        key = songs.itemKey(RemoteSong::id),
                        contentType = songs.itemContentType { "remote_song" },
                    ) { index ->
                        songs[index]?.let { song ->
                            RemoteSongRow(song) {
                                viewModel.play(song, songs.itemSnapshotList.items)
                            }
                        }
                    }

                    when {
                        songs.loadState.refresh is LoadState.Loading ||
                            songs.loadState.append is LoadState.Loading -> {
                            item(key = "loading", contentType = "paging") {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                        songs.loadState.refresh is LoadState.Error ||
                            songs.loadState.append is LoadState.Error -> {
                            item(key = "error", contentType = "paging") {
                                val error =
                                    (songs.loadState.refresh as? LoadState.Error)?.error
                                        ?: (songs.loadState.append as? LoadState.Error)?.error
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        error?.message ?: "加载云端音乐失败",
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    OutlinedButton(onClick = songs::retry) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubsonicLogin(
    state: RemoteMusicUiState,
    hasExistingAccount: Boolean,
    modifier: Modifier,
    onCancel: () -> Unit,
    onLogin: (String, String, String) -> Unit,
) {
    var serverUrl by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LoginIntro(
                title = "连接 Subsonic",
                body = "兼容 Navidrome、Airsonic、Gonic 及其他 Subsonic/OpenSubsonic 服务器。",
            )
        }
        state.error?.let { message ->
            item { LoginError(message) }
        }
        item {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("服务器地址") },
                placeholder = { Text("https://music.example.com") },
                keyboardOptions = KeyboardOptions.Default,
            )
        }
        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("用户名") },
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("密码") },
                visualTransformation = PasswordVisualTransformation(),
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hasExistingAccount) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !state.isConnecting,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Text("取消")
                    }
                }
                Button(
                    onClick = { onLogin(serverUrl, username, password) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    enabled =
                        !state.isConnecting &&
                            serverUrl.isNotBlank() &&
                            username.isNotBlank() &&
                            password.isNotBlank(),
                ) {
                    if (state.isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("连接")
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CookieWebLogin(
    provider: RemoteMusicProvider,
    state: RemoteMusicUiState,
    hasExistingAccount: Boolean,
    modifier: Modifier,
    onCancel: () -> Unit,
    onCookiesCaptured: (String) -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LoginIntro(
            title = "登录 ${provider.displayName}",
            body = "请在下方官方网页完成登录。应用只读取登录 Cookie，不会读取或保存你的密码。",
        )
        state.error?.let { LoginError(it) }
        AndroidView(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
            factory = { context ->
                WebView(context).apply {
                    val loginWebView = this
                    setBackgroundColor(Color.TRANSPARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.userAgentString =
                        if (provider == RemoteMusicProvider.QQ_MUSIC) {
                            DESKTOP_USER_AGENT
                        } else {
                            settings.userAgentString
                        }
                    settings.useWideViewPort = provider == RemoteMusicProvider.QQ_MUSIC
                    settings.loadWithOverviewMode = provider == RemoteMusicProvider.QQ_MUSIC
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    CookieManager.getInstance().apply {
                        setAcceptCookie(true)
                        setAcceptThirdPartyCookies(loginWebView, true)
                    }
                    loadUrl(provider.loginUrl)
                }
            },
            onRelease = WebView::releaseAfterLayout,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        ) {
            if (hasExistingAccount) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !state.isConnecting,
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) {
                    Text("取消")
                }
            }
            Button(
                enabled = !state.isConnecting,
                modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                onClick = {
                    CookieManager.getInstance().flush()
                    val cookies = collectCookies(provider.cookieUrls)
                    onCookiesCaptured(cookies)
                },
            ) {
                if (state.isConnecting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("我已完成登录")
                }
            }
        }
    }
}

/**
 * AndroidView can leave composition while Compose is still completing a layout pass.
 * WebView.destroy() may request another layout synchronously, so release it on the next
 * main-loop turn instead of re-entering Compose's active measure.
 */
private fun WebView.releaseAfterLayout() {
    Handler(Looper.getMainLooper()).post {
        stopLoading()
        webChromeClient = null
        webViewClient = WebViewClient()
        removeAllViews()
        destroy()
    }
}

@Composable
private fun LoginIntro(
    title: String,
    body: String,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Storage, null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LoginError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun RemoteSongRow(
    song: RemoteSong,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                        RoundedCornerShape(13.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${song.artist} · ${song.album}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatRemoteDuration(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemoteSceneScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

private val RemoteMusicProvider.displayName: String
    get() =
        when (this) {
            RemoteMusicProvider.SUBSONIC -> "Subsonic"
            RemoteMusicProvider.NETEASE -> "网易云音乐"
            RemoteMusicProvider.QQ_MUSIC -> "QQ 音乐"
        }

private val RemoteMusicProvider.loginUrl: String
    get() =
        when (this) {
            RemoteMusicProvider.NETEASE -> "https://music.163.com/m/login"
            RemoteMusicProvider.QQ_MUSIC -> "https://y.qq.com/"
            RemoteMusicProvider.SUBSONIC -> error("Subsonic does not use web login")
        }

private val RemoteMusicProvider.cookieUrls: List<String>
    get() =
        when (this) {
            RemoteMusicProvider.NETEASE ->
                listOf(
                    "https://music.163.com/",
                    "https://interface.music.163.com/",
                )
            RemoteMusicProvider.QQ_MUSIC ->
                listOf(
                    "https://y.qq.com/",
                    "https://u.y.qq.com/",
                    "https://u6.y.qq.com/",
                    "https://c.y.qq.com/",
                )
            RemoteMusicProvider.SUBSONIC -> emptyList()
        }

private fun RemoteMusicProvider.accountSubtitle(account: RemoteMusicAccount): String =
    when (this) {
        RemoteMusicProvider.SUBSONIC -> account.normalizedServerUrl
        RemoteMusicProvider.NETEASE,
        RemoteMusicProvider.QQ_MUSIC,
        -> "网页登录会话已加密保存在本机"
    }

private fun collectCookies(urls: List<String>): String {
    val values = LinkedHashMap<String, String>()
    val manager = CookieManager.getInstance()
    urls.forEach { url ->
        manager
            .getCookie(url)
            .orEmpty()
            .split(';')
            .map(String::trim)
            .filter { '=' in it }
            .forEach { entry ->
                val name = entry.substringBefore('=').trim()
                val value = entry.substringAfter('=', "")
                if (name.isNotBlank()) values[name] = value
            }
    }
    return values.entries.joinToString("; ") { (name, value) -> "$name=$value" }
}

private fun formatRemoteDuration(durationMs: Long): String {
    val seconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/120.0.0.0 Safari/537.36"
