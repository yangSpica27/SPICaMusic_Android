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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.spica27.spicamusic.feature.player.domain.PlayerUseCases
import me.spica27.spicamusic.player.api.PlayerAction

data class RemoteMusicUiState(
    val accounts: List<RemoteMusicAccount> = emptyList(),
    val selectedAccount: RemoteMusicAccount? = null,
    val isConnecting: Boolean = false,
    val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteMusicViewModel(
    val provider: RemoteMusicProvider,
    private val accountStore: CloudAccountStore,
    private val clients: RemoteMusicClientRegistry,
    private val proxy: RemoteMusicStreamProxy,
    private val player: PlayerUseCases,
) : ViewModel() {
    private val _state = MutableStateFlow(RemoteMusicUiState())
    val state = _state.asStateFlow()
    private val query = MutableStateFlow("")

    val songs: Flow<PagingData<RemoteSong>> =
        combine(state, query) { uiState, currentQuery ->
            uiState.selectedAccount to currentQuery
        }.flatMapLatest { (account, currentQuery) ->
            if (account == null) {
                flowOf(PagingData.empty())
            } else {
                Pager(
                    PagingConfig(
                        pageSize = RemoteMusicPagingSource.PAGE_SIZE,
                        initialLoadSize = RemoteMusicPagingSource.PAGE_SIZE,
                        prefetchDistance = 12,
                        enablePlaceholders = false,
                        maxSize = RemoteMusicPagingSource.PAGE_SIZE * 6,
                    ),
                ) {
                    RemoteMusicPagingSource(clients, account, currentQuery)
                }.flow
            }
        }.cachedIn(viewModelScope)

    init {
        refreshAccounts()
    }

    fun loginSubsonic(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        if (provider != RemoteMusicProvider.SUBSONIC || _state.value.isConnecting) return
        connect {
            clients.authenticateSubsonic(serverUrl, username, password)
        }
    }

    fun loginWithCookies(cookieHeader: String) {
        if (provider == RemoteMusicProvider.SUBSONIC || _state.value.isConnecting) return
        connect {
            clients.authenticateCookies(provider, cookieHeader)
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
        _state.value.selectedAccount?.let {
            clients.clearCache(it.id)
            accountStore.removeRemoteAccount(it.id)
        }
        refreshAccounts()
    }

    fun search(value: String) {
        query.value = value.trim()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun play(
        selectedSong: RemoteSong,
        visibleSnapshot: List<RemoteSong>,
    ) {
        val account = _state.value.selectedAccount ?: return
        viewModelScope.launch {
            val visible =
                visibleSnapshot
                    .distinctBy(RemoteSong::id)
                    .ifEmpty { listOf(selectedSong) }
            val items =
                visible.map { song ->
                    MediaItem
                        .Builder()
                        .setMediaId(
                            "cloud:${account.provider.name.lowercase()}:${account.id}:${song.id}",
                        ).setUri(proxy.streamUrl(account, song))
                        .setMimeType(song.mimeType)
                        .setMediaMetadata(
                            MediaMetadata
                                .Builder()
                                .setTitle(song.title)
                                .setDisplayTitle(song.title)
                                .setArtist(song.artist)
                                .setAlbumTitle(song.album)
                                .setArtworkUri(song.artworkUrl?.let(Uri::parse))
                                .setDurationMs(song.durationMs)
                                .setIsPlayable(true)
                                .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                                .setExtras(
                                    Bundle().apply {
                                        putString("cloudProvider", account.provider.name)
                                        putString("cloudAccountId", account.id)
                                    },
                                ).build(),
                        ).build()
                }
            val startIndex =
                visible.indexOfFirst { it.id == selectedSong.id }.coerceAtLeast(0)
            player.doAction(PlayerAction.PlayMediaItems(items, startIndex))
        }
    }

    private fun connect(block: suspend () -> Result<RemoteMusicAccount>) {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            block().fold(
                onSuccess = { authenticated ->
                    val account = authenticated.copy(id = accountStore.newAccountId())
                    accountStore.saveRemoteAccount(account)
                    refreshAccounts(account.id)
                },
                onFailure = { error ->
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            error = error.message ?: "Unable to connect cloud music account",
                        )
                    }
                },
            )
        }
    }

    private fun refreshAccounts(preferredId: String? = null) {
        val accounts = accountStore.getRemoteAccounts(provider)
        val currentId = preferredId ?: _state.value.selectedAccount?.id
        _state.value =
            RemoteMusicUiState(
                accounts = accounts,
                selectedAccount =
                    accounts.firstOrNull { it.id == currentId }
                        ?: accounts.firstOrNull(),
            )
    }
}
