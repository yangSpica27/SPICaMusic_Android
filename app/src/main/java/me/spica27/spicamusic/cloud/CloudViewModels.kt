package me.spica27.spicamusic.cloud

import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction
import org.drinkless.tdlib.TdApi

data class MediaServerUiState(
    val accounts: List<MediaServerAccount> = emptyList(),
    val selectedAccount: MediaServerAccount? = null,
    val isConnecting: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MediaServerViewModel(
    val type: MediaServerType,
    private val accountStore: CloudAccountStore,
    private val client: MediaServerClient,
    private val player: PlayerUseCases,
) : ViewModel() {
    private val _state = MutableStateFlow(MediaServerUiState())
    val state = _state.asStateFlow()
    private val query = MutableStateFlow("")

    val songs: Flow<PagingData<CloudSong>> =
        combine(
            state,
            query,
        ) { uiState, currentQuery -> uiState.selectedAccount to currentQuery }
            .flatMapLatest { (account, currentQuery) ->
                if (account == null) {
                    flowOf(PagingData.empty())
                } else {
                    Pager(
                        PagingConfig(
                            pageSize = MediaServerClient.PAGE_SIZE,
                            initialLoadSize = MediaServerClient.PAGE_SIZE,
                            prefetchDistance = 14,
                            enablePlaceholders = false,
                            maxSize = MediaServerClient.PAGE_SIZE * 6,
                        ),
                    ) {
                        MediaServerPagingSource(client, account, currentQuery)
                    }.flow
                }
            }.cachedIn(viewModelScope)

    init {
        refreshAccounts()
    }

    fun login(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        if (_state.value.isConnecting) return
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            client.authenticate(type, serverUrl, username, password).fold(
                onSuccess = { authenticated ->
                    val account = authenticated.copy(id = accountStore.newAccountId())
                    accountStore.saveAccount(account)
                    refreshAccounts(account.id)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            error = error.message ?: "连接服务器失败",
                        )
                    }
                },
            )
        }
    }

    fun selectAccount(id: String) {
        _state.update { current ->
            current.copy(
                selectedAccount = current.accounts.firstOrNull { it.id == id },
                error = null,
            )
        }
    }

    fun removeSelectedAccount() {
        _state.value.selectedAccount?.let { accountStore.removeAccount(it.id) }
        refreshAccounts()
    }

    fun search(value: String) {
        query.value = value.trim()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun play(
        selectedSong: CloudSong,
        visibleSnapshot: List<CloudSong>,
    ) {
        val account = _state.value.selectedAccount ?: return
        val queue =
            visibleSnapshot
                .distinctBy(CloudSong::id)
                .ifEmpty { listOf(selectedSong) }
                .map { it.toMediaItem(account) }
        val startIndex = queue.indexOfFirst { it.mediaId.endsWith(":${selectedSong.id}") }.coerceAtLeast(0)
        player.doAction(PlayerAction.PlayMediaItems(queue, startIndex))
    }

    private fun CloudSong.toMediaItem(account: MediaServerAccount): MediaItem {
        val artwork =
            imageItemId?.let {
                Uri.parse(client.imageUrl(account, it))
            }
        val mediaId = "cloud:${account.type.name.lowercase()}:${account.id}:$id"
        return MediaItem
            .Builder()
            .setMediaId(mediaId)
            .setUri(client.streamUrl(account, id))
            .setMimeType(mimeType)
            .setMediaMetadata(
                MediaMetadata
                    .Builder()
                    .setTitle(title)
                    .setDisplayTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(album)
                    .setArtworkUri(artwork)
                    .setDurationMs(durationMs)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setExtras(
                        Bundle().apply {
                            putString("cloudProvider", account.type.name)
                            putString("cloudAccountId", account.id)
                        },
                    ).build(),
            ).build()
    }

    private fun refreshAccounts(preferredId: String? = null) {
        val accounts = accountStore.getAccounts(type)
        val currentId = preferredId ?: _state.value.selectedAccount?.id
        _state.value =
            MediaServerUiState(
                accounts = accounts,
                selectedAccount =
                    accounts.firstOrNull { it.id == currentId }
                        ?: accounts.firstOrNull(),
            )
    }
}

data class TelegramUiState(
    val hasConfig: Boolean = false,
    val channels: List<TelegramChannel> = emptyList(),
    val availableChannels: List<TelegramChannel> = emptyList(),
    val isChoosingChannels: Boolean = false,
    val isWorking: Boolean = false,
    val error: String? = null,
)

class TelegramViewModel(
    private val repository: TelegramRepository,
) : ViewModel() {
    val authorizationState: StateFlow<TdApi.AuthorizationState?> =
        repository.authorizationState.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null,
        )

    private val _state =
        MutableStateFlow(
            TelegramUiState(
                hasConfig = repository.hasConfig(),
                channels = repository.savedChannels(),
            ),
        )
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.errors.collect { error ->
                _state.update {
                    it.copy(
                        isWorking = false,
                        error = telegramErrorMessage(error.code, error.message),
                    )
                }
            }
        }
    }

    fun saveConfig(
        apiIdText: String,
        apiHash: String,
    ) {
        val apiId = apiIdText.trim().toIntOrNull()
        if (apiId == null || apiId <= 0 || apiHash.isBlank()) {
            _state.update { it.copy(error = "请输入有效的 API ID 和 API Hash") }
            return
        }
        repository.configure(TelegramConfig(apiId, apiHash.trim()))
        _state.update { it.copy(hasConfig = true, error = null) }
    }

    fun sendPhone(phone: String) =
        runAction {
            require(phone.trim().startsWith("+")) { "手机号需包含国际区号，例如 +86138…" }
            repository.sendPhoneNumber(phone.trim())
        }

    fun checkCode(code: String) = runAction { repository.checkCode(code.trim()) }

    fun checkPassword(password: String) = runAction { repository.checkPassword(password) }

    fun addChannel(username: String) =
        runAction {
            repository.addPublicChannel(username)
            _state.update { it.copy(channels = repository.savedChannels()) }
        }

    fun removeChannel(chatId: Long) {
        repository.removeChannel(chatId)
        _state.update { it.copy(channels = repository.savedChannels()) }
    }

    fun loadJoinedChannels() =
        runAction {
            val savedIds = repository.savedChannels().mapTo(hashSetOf()) { it.chatId }
            val available = repository.getJoinedChannels().filterNot { it.chatId in savedIds }
            _state.update {
                it.copy(
                    availableChannels = available,
                    isChoosingChannels = true,
                )
            }
        }

    fun chooseChannel(channel: TelegramChannel) {
        repository.saveChannel(channel)
        _state.update {
            it.copy(
                channels = repository.savedChannels(),
                availableChannels = it.availableChannels.filterNot { item -> item.chatId == channel.chatId },
            )
        }
    }

    fun closeChannelPicker() {
        _state.update { it.copy(isChoosingChannels = false, availableChannels = emptyList()) }
    }

    fun logout() {
        repository.logout()
        _state.update { it.copy(channels = repository.savedChannels()) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun tdlibAvailable(): Boolean = repository.isAvailable()

    private fun runAction(action: suspend () -> Unit) {
        if (_state.value.isWorking) return
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, error = null) }
            runCatching { action() }.fold(
                onSuccess = { _state.update { it.copy(isWorking = false) } },
                onFailure = { error ->
                    _state.update {
                        val message =
                            if (error is TelegramRequestException) {
                                telegramErrorMessage(error.errorCode, error.message.orEmpty())
                            } else {
                                error.message ?: "Telegram 操作失败"
                            }
                        it.copy(isWorking = false, error = message)
                    }
                },
            )
        }
    }

    private fun telegramErrorMessage(
        code: Int,
        raw: String,
    ): String =
        when {
            raw.contains("PHONE_NUMBER_INVALID", true) -> "手机号无效，请检查国家区号"
            raw.contains("PHONE_CODE_INVALID", true) -> "验证码不正确"
            raw.contains("PHONE_CODE_EXPIRED", true) -> "验证码已过期，请重新发送"
            raw.contains("PASSWORD_HASH_INVALID", true) -> "两步验证密码不正确"
            raw.contains("FLOOD_WAIT", true) -> "操作过于频繁，请稍后再试"
            else -> "Telegram 错误（$code）：$raw"
        }
}

class TelegramChannelViewModel(
    private val chatId: Long,
    private val repository: TelegramRepository,
    private val proxy: TelegramStreamProxy,
    private val player: PlayerUseCases,
) : ViewModel() {
    val songs: Flow<PagingData<TelegramSong>> =
        Pager(
            PagingConfig(
                pageSize = TelegramRepository.PAGE_SIZE,
                initialLoadSize = TelegramRepository.PAGE_SIZE,
                prefetchDistance = 10,
                enablePlaceholders = false,
                maxSize = TelegramRepository.PAGE_SIZE * 6,
            ),
        ) {
            TelegramPagingSource(repository, chatId)
        }.flow.cachedIn(viewModelScope)

    fun play(
        selectedSong: TelegramSong,
        visibleSnapshot: List<TelegramSong>,
    ) {
        viewModelScope.launch {
            val songs =
                visibleSnapshot
                    .distinctBy(TelegramSong::messageId)
                    .ifEmpty { listOf(selectedSong) }
            val items =
                songs.map { song ->
                    MediaItem
                        .Builder()
                        .setMediaId("cloud:telegram:${song.chatId}:${song.messageId}")
                        .setUri(proxy.streamUrl(song))
                        .setMimeType(song.mimeType)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle(song.title)
                                .setDisplayTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle("Telegram")
                                .setDurationMs(song.durationMs)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setExtras(
                                    Bundle().apply {
                                        putString("cloudProvider", "TELEGRAM")
                                        putLong("telegramChatId", song.chatId)
                                        putInt("telegramFileId", song.fileId)
                                    },
                                ).build(),
                        ).build()
                }
            val startIndex =
                songs.indexOfFirst { it.messageId == selectedSong.messageId }.coerceAtLeast(0)
            player.doAction(PlayerAction.PlayMediaItems(items, startIndex))
        }
    }
}
