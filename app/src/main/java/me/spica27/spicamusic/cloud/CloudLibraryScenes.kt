package me.spica27.spicamusic.cloud

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import org.drinkless.tdlib.TdApi
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

class CloudLibraryScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        CloudSceneScaffold(
            title = stringResource(R.string.cloud_library),
            onBack = { path.popTop() },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = padding.calculateTopPadding() + 16.dp,
                        bottom = 40.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "intro", contentType = "intro") {
                    Text(
                        text = stringResource(R.string.cloud_library_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                item(key = "telegram", contentType = "provider") {
                    ProviderCard(
                        title = "Telegram",
                        subtitle = stringResource(R.string.telegram_provider_subtitle),
                        icon = Icons.Default.Send,
                        onClick = { path.push(TelegramScene()) },
                    )
                }
                item(key = "google_drive", contentType = "provider") {
                    ProviderCard(
                        title = "Google Drive",
                        subtitle = stringResource(R.string.google_drive_coming_soon),
                        icon = Icons.Default.CloudOff,
                        enabled = false,
                        onClick = {},
                    )
                }
                item(key = "subsonic", contentType = "provider") {
                    ProviderCard(
                        title = "Subsonic",
                        subtitle = stringResource(R.string.subsonic_provider_subtitle),
                        icon = Icons.Default.LibraryMusic,
                        onClick = {
                            path.push(RemoteMusicScene(RemoteMusicProvider.SUBSONIC))
                        },
                    )
                }
                item(key = "jellyfin", contentType = "provider") {
                    ProviderCard(
                        title = "Jellyfin",
                        subtitle = stringResource(R.string.jellyfin_provider_subtitle),
                        icon = Icons.Default.Storage,
                        onClick = { path.push(MediaServerScene(MediaServerType.JELLYFIN)) },
                    )
                }
                item(key = "netease", contentType = "provider") {
                    ProviderCard(
                        title = stringResource(R.string.netease_music),
                        subtitle = stringResource(R.string.netease_provider_subtitle),
                        icon = Icons.Default.MusicNote,
                        onClick = {
                            path.push(RemoteMusicScene(RemoteMusicProvider.NETEASE))
                        },
                    )
                }
                item(key = "qq_music", contentType = "provider") {
                    ProviderCard(
                        title = stringResource(R.string.qq_music),
                        subtitle = stringResource(R.string.qq_music_provider_subtitle),
                        icon = Icons.Default.Cloud,
                        onClick = {
                            path.push(RemoteMusicScene(RemoteMusicProvider.QQ_MUSIC))
                        },
                    )
                }
                item(key = "emby", contentType = "provider") {
                    ProviderCard(
                        title = "Emby",
                        subtitle = stringResource(R.string.emby_provider_subtitle),
                        icon = Icons.Default.Dns,
                        onClick = { path.push(MediaServerScene(MediaServerType.EMBY)) },
                    )
                }
                item(key = "privacy", contentType = "hint") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.cloud_credentials_local_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

class MediaServerScene(
    private val type: MediaServerType,
) : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: MediaServerViewModel =
            koinViewModel(key = "media_server_${type.name}") { parametersOf(type) }
        val state by viewModel.state.collectAsStateWithLifecycle()
        val songs = viewModel.songs.collectAsLazyPagingItems()
        var showLogin by rememberSaveable(type) { mutableStateOf(state.accounts.isEmpty()) }
        var searchText by rememberSaveable(type) { mutableStateOf("") }

        LaunchedEffect(state.accounts.size) {
            if (state.accounts.isEmpty()) showLogin = true
        }

        CloudSceneScaffold(
            title = type.displayName,
            onBack = { path.popTop() },
            actions = {
                if (state.selectedAccount != null && !showLogin) {
                    IconButton(onClick = { songs.refresh() }) {
                        Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                    }
                }
            },
        ) { padding ->
            if (showLogin) {
                MediaServerLogin(
                    type = type,
                    isConnecting = state.isConnecting,
                    error = state.error,
                    hasExistingAccount = state.accounts.isNotEmpty(),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(padding),
                    onCancel = {
                        viewModel.clearError()
                        showLogin = false
                    },
                    onLogin = viewModel::login,
                )
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
                    item(key = "account_bar", contentType = "account_bar") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            state.selectedAccount?.let { account ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = account.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = account.username,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = viewModel::removeSelectedAccount) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            stringResource(R.string.remove_account),
                                        )
                                    }
                                    IconButton(onClick = { showLogin = true }) {
                                        Icon(Icons.Default.Add, stringResource(R.string.add_account))
                                    }
                                }
                            }
                            if (state.accounts.size > 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.accounts.take(3).forEach { account ->
                                        val selected = account.id == state.selectedAccount?.id
                                        if (selected) {
                                            FilledTonalButton(
                                                onClick = { viewModel.selectAccount(account.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                            ) { Text(account.displayName, maxLines = 1) }
                                        } else {
                                            OutlinedButton(
                                                onClick = { viewModel.selectAccount(account.id) },
                                                contentPadding = PaddingValues(horizontal = 12.dp),
                                            ) { Text(account.displayName, maxLines = 1) }
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text(stringResource(R.string.search_cloud_music)) },
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                )
                                FilledTonalButton(onClick = { viewModel.search(searchText) }) {
                                    Text(stringResource(R.string.search))
                                }
                            }
                        }
                    }

                    items(
                        count = songs.itemCount,
                        key = songs.itemKey(CloudSong::id),
                        contentType = songs.itemContentType { "cloud_song" },
                    ) { index ->
                        songs[index]?.let { song ->
                            CloudSongRow(
                                title = song.title,
                                artist = song.artist,
                                album = song.album,
                                durationMs = song.durationMs,
                                onClick = {
                                    viewModel.play(song, songs.itemSnapshotList.items)
                                },
                            )
                        }
                    }

                    pagingFooter(
                        loadState = songs.loadState,
                        onRetry = songs::retry,
                    )
                }
            }
        }
    }
}

class TelegramScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: TelegramViewModel = koinViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val authState by viewModel.authorizationState.collectAsStateWithLifecycle()

        CloudSceneScaffold(
            title = "Telegram",
            onBack = { path.popTop() },
        ) { padding ->
            TelegramContent(
                state = state,
                authState = authState,
                tdlibAvailable = viewModel.tdlibAvailable(),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                onSaveConfig = viewModel::saveConfig,
                onSendPhone = viewModel::sendPhone,
                onCheckCode = viewModel::checkCode,
                onCheckPassword = viewModel::checkPassword,
                onAddChannel = viewModel::addChannel,
                onLoadJoinedChannels = viewModel::loadJoinedChannels,
                onChooseChannel = viewModel::chooseChannel,
                onCloseChannelPicker = viewModel::closeChannelPicker,
                onRemoveChannel = viewModel::removeChannel,
                onOpenChannel = { path.push(TelegramChannelScene(it)) },
                onLogout = viewModel::logout,
            )
        }
    }
}

class TelegramChannelScene(
    private val channel: TelegramChannel,
) : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: TelegramChannelViewModel =
            koinViewModel(key = "telegram_channel_${channel.chatId}") {
                parametersOf(channel.chatId)
            }
        val songs = viewModel.songs.collectAsLazyPagingItems()

        CloudSceneScaffold(
            title = channel.title,
            onBack = { path.popTop() },
            actions = {
                IconButton(onClick = { songs.refresh() }) {
                    Icon(Icons.Default.Refresh, stringResource(R.string.refresh))
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 10.dp,
                        bottom = 36.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "performance_hint", contentType = "hint") {
                    Text(
                        text = stringResource(R.string.telegram_paging_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
                items(
                    count = songs.itemCount,
                    key = songs.itemKey { "${it.chatId}_${it.messageId}" },
                    contentType = songs.itemContentType { "telegram_song" },
                ) { index ->
                    songs[index]?.let { song ->
                        CloudSongRow(
                            title = song.title,
                            artist = song.artist,
                            album = "Telegram",
                            durationMs = song.durationMs,
                            onClick = {
                                viewModel.play(song, songs.itemSnapshotList.items)
                            },
                        )
                    }
                }
                pagingFooter(songs.loadState, songs::retry)
            }
        }
    }
}

@Composable
private fun TelegramContent(
    state: TelegramUiState,
    authState: TdApi.AuthorizationState?,
    tdlibAvailable: Boolean,
    modifier: Modifier,
    onSaveConfig: (String, String) -> Unit,
    onSendPhone: (String) -> Unit,
    onCheckCode: (String) -> Unit,
    onCheckPassword: (String) -> Unit,
    onAddChannel: (String) -> Unit,
    onLoadJoinedChannels: () -> Unit,
    onChooseChannel: (TelegramChannel) -> Unit,
    onCloseChannelPicker: () -> Unit,
    onRemoveChannel: (Long) -> Unit,
    onOpenChannel: (TelegramChannel) -> Unit,
    onLogout: () -> Unit,
) {
    var firstValue by rememberSaveable { mutableStateOf("") }
    var secondValue by rememberSaveable { mutableStateOf("") }
    val authorizationStep =
        when {
            !state.hasConfig -> "config"
            authState is TdApi.AuthorizationStateWaitPhoneNumber -> "phone"
            authState is TdApi.AuthorizationStateWaitCode -> "code"
            authState is TdApi.AuthorizationStateWaitPassword -> "password"
            authState is TdApi.AuthorizationStateReady -> "ready"
            else -> "connecting"
        }

    // The same field has a different meaning in each authorization step. Clear it when TDLib
    // advances so a phone number can never be submitted as the verification code.
    LaunchedEffect(authorizationStep) {
        firstValue = ""
        secondValue = ""
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!tdlibAvailable) {
            item {
                CloudMessage(
                    icon = Icons.Default.CloudOff,
                    title = stringResource(R.string.telegram_unavailable),
                    body = stringResource(R.string.telegram_unavailable_detail),
                )
            }
            return@LazyColumn
        }

        state.error?.let { message ->
            item(key = "error", contentType = "error") {
                ErrorCard(message)
            }
        }

        if (!state.hasConfig) {
            item(key = "config", contentType = "form") {
                CloudForm(
                    title = stringResource(R.string.telegram_api_title),
                    description = stringResource(R.string.telegram_api_hint),
                    firstLabel = "API ID",
                    firstValue = firstValue,
                    onFirstChange = { firstValue = it.filter(Char::isDigit) },
                    secondLabel = "API Hash",
                    secondValue = secondValue,
                    onSecondChange = { secondValue = it },
                    secondPassword = true,
                    actionText = stringResource(R.string.save_and_continue),
                    isWorking = state.isWorking,
                    submitEnabled = firstValue.isNotBlank() && secondValue.isNotBlank(),
                    onSubmit = { onSaveConfig(firstValue, secondValue) },
                )
            }
            return@LazyColumn
        }

        when (authState) {
            is TdApi.AuthorizationStateReady -> {
                item(key = "add_channel", contentType = "form") {
                    CloudForm(
                        title = stringResource(R.string.telegram_add_channel),
                        description = stringResource(R.string.telegram_add_channel_hint),
                        firstLabel = stringResource(R.string.telegram_channel_username),
                        firstValue = firstValue,
                        onFirstChange = { firstValue = it },
                        actionText = stringResource(R.string.add),
                        isWorking = state.isWorking,
                        onSubmit = { onAddChannel(firstValue) },
                    )
                }
                item(key = "choose_joined", contentType = "action") {
                    FilledTonalButton(
                        onClick = onLoadJoinedChannels,
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.LibraryMusic, null)
                        Text(
                            stringResource(R.string.telegram_choose_joined),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                if (state.isChoosingChannels) {
                    item(key = "joined_header", contentType = "section_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.telegram_joined_channels),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onCloseChannelPicker) {
                                Text(stringResource(R.string.close))
                            }
                        }
                    }
                    if (state.availableChannels.isEmpty() && !state.isWorking) {
                        item(key = "joined_empty", contentType = "empty") {
                            Text(
                                text = stringResource(R.string.telegram_joined_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(
                            items = state.availableChannels,
                            key = { "available_${it.chatId}" },
                            contentType = { "available_channel" },
                        ) { channel ->
                            Surface(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onChooseChannel(channel) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = channel.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Icon(Icons.Default.Add, stringResource(R.string.add))
                                }
                            }
                        }
                    }
                }
                if (state.channels.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        CloudMessage(
                            icon = Icons.Default.LibraryMusic,
                            title = stringResource(R.string.no_cloud_channels),
                            body = stringResource(R.string.no_cloud_channels_hint),
                        )
                    }
                } else {
                    items(
                        items = state.channels,
                        key = TelegramChannel::chatId,
                        contentType = { "channel" },
                    ) { channel ->
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenChannel(channel) },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Column(
                                    modifier =
                                        Modifier
                                            .weight(1f)
                                            .padding(horizontal = 14.dp),
                                ) {
                                    Text(channel.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (channel.username.isNotBlank()) {
                                            "@${channel.username}"
                                        } else {
                                            stringResource(R.string.telegram_joined_channel)
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onRemoveChannel(channel.chatId) }) {
                                    Icon(Icons.Default.DeleteOutline, stringResource(R.string.remove))
                                }
                            }
                        }
                    }
                }
                item(key = "logout", contentType = "action") {
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.telegram_logout))
                    }
                }
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                item(key = "phone", contentType = "form") {
                    CloudForm(
                        title = stringResource(R.string.telegram_login),
                        description = stringResource(R.string.telegram_phone_hint),
                        firstLabel = stringResource(R.string.phone_number),
                        firstValue = firstValue,
                        onFirstChange = { firstValue = it },
                        keyboardType = KeyboardType.Phone,
                        actionText = stringResource(R.string.send_code),
                        isWorking = state.isWorking,
                        submitEnabled = firstValue.isNotBlank(),
                        onSubmit = { onSendPhone(firstValue) },
                    )
                }
            }
            is TdApi.AuthorizationStateWaitCode -> {
                item(key = "code", contentType = "form") {
                    CloudForm(
                        title = stringResource(R.string.telegram_verification),
                        description = stringResource(R.string.telegram_code_hint),
                        firstLabel = stringResource(R.string.verification_code),
                        firstValue = firstValue,
                        onFirstChange = { firstValue = it.filter(Char::isDigit) },
                        keyboardType = KeyboardType.NumberPassword,
                        actionText = stringResource(R.string.verify),
                        isWorking = state.isWorking,
                        submitEnabled = firstValue.isNotBlank(),
                        autoFocus = true,
                        onSubmit = { onCheckCode(firstValue) },
                    )
                }
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                item(key = "password", contentType = "form") {
                    CloudForm(
                        title = stringResource(R.string.telegram_two_step),
                        description = stringResource(R.string.telegram_password_hint),
                        firstLabel = stringResource(R.string.password),
                        firstValue = firstValue,
                        onFirstChange = { firstValue = it },
                        firstPassword = true,
                        actionText = stringResource(R.string.verify),
                        isWorking = state.isWorking,
                        submitEnabled = firstValue.isNotBlank(),
                        autoFocus = true,
                        onSubmit = { onCheckPassword(firstValue) },
                    )
                }
            }
            else -> {
                item(key = "loading", contentType = "loading") {
                    CloudMessage(
                        icon = Icons.Default.Cloud,
                        title = stringResource(R.string.telegram_connecting),
                        body = stringResource(R.string.telegram_connecting_hint),
                        loading = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaServerLogin(
    type: MediaServerType,
    isConnecting: Boolean,
    error: String?,
    hasExistingAccount: Boolean,
    modifier: Modifier,
    onCancel: () -> Unit,
    onLogin: (String, String, String) -> Unit,
) {
    var serverUrl by rememberSaveable(type) { mutableStateOf("") }
    var username by rememberSaveable(type) { mutableStateOf("") }
    var password by rememberSaveable(type) { mutableStateOf("") }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CloudMessage(
                icon = if (type == MediaServerType.JELLYFIN) Icons.Default.Storage else Icons.Default.Dns,
                title = stringResource(R.string.connect_to_format, type.displayName),
                body = stringResource(R.string.media_server_login_hint),
            )
        }
        error?.let { item { ErrorCard(it) } }
        item {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.server_url)) },
                placeholder = { Text("https://music.example.com") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
        }
        item {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.username)) },
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.password)) },
                singleLine = true,
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
                        enabled = !isConnecting,
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                Button(
                    onClick = { onLogin(serverUrl, username, password) },
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    enabled =
                        !isConnecting &&
                            serverUrl.isNotBlank() &&
                            username.isNotBlank(),
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.connect))
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudForm(
    title: String,
    description: String,
    firstLabel: String,
    firstValue: String,
    onFirstChange: (String) -> Unit,
    actionText: String,
    isWorking: Boolean,
    onSubmit: () -> Unit,
    secondLabel: String? = null,
    secondValue: String = "",
    onSecondChange: (String) -> Unit = {},
    firstPassword: Boolean = false,
    secondPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    submitEnabled: Boolean = true,
    autoFocus: Boolean = false,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = firstValue,
                onValueChange = onFirstChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                label = { Text(firstLabel) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Done,
                    ),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            if (!isWorking && submitEnabled) onSubmit()
                        },
                    ),
                visualTransformation =
                    if (firstPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            )
            secondLabel?.let {
                OutlinedTextField(
                    value = secondValue,
                    onValueChange = onSecondChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(it) },
                    singleLine = true,
                    visualTransformation =
                        if (secondPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                )
            }
            Button(
                onClick = onSubmit,
                enabled = !isWorking && submitEnabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (isWorking) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(actionText)
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color =
            if (enabled) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    null,
                    tint =
                        if (enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CloudSongRow(
    title: String,
    artist: String,
    album: String,
    durationMs: Long,
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.MusicNote,
                null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$artist · $album",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            formatDuration(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CloudMessage(
    icon: ImageVector,
    title: String,
    body: String,
    loading: Boolean = false,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
            } else {
                Icon(icon, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.pagingFooter(
    loadState: androidx.paging.CombinedLoadStates,
    onRetry: () -> Unit,
) {
    when {
        loadState.refresh is LoadState.Loading || loadState.append is LoadState.Loading -> {
            item(key = "paging_loading", contentType = "paging_state") {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
        }
        loadState.refresh is LoadState.Error || loadState.append is LoadState.Error -> {
            item(key = "paging_error", contentType = "paging_state") {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, null)
                        Text(stringResource(R.string.retry), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSceneScaffold(
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = actions,
            )
        },
        content = content,
    )
}

private val MediaServerType.displayName: String
    get() = if (this == MediaServerType.JELLYFIN) "Jellyfin" else "Emby"

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}
