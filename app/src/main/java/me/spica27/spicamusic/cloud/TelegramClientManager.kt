package me.spica27.spicamusic.cloud

import android.content.Context
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import me.spica27.spicamusic.BuildConfig
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TelegramClientManager(
    private val context: Context,
    private val accountStore: CloudAccountStore,
) {
    private val _authorizationState = MutableStateFlow<TdApi.AuthorizationState?>(null)
    val authorizationState = _authorizationState.asStateFlow()

    private val _errors = MutableSharedFlow<TdApi.Error>(extraBufferCapacity = 8)
    val errors = _errors.asSharedFlow()

    private val _fileUpdates = MutableSharedFlow<TdApi.File>(extraBufferCapacity = 32)
    val fileUpdates = _fileUpdates.asSharedFlow()

    @Volatile
    private var client: Client? = null

    @Volatile
    private var config: TelegramConfig? =
        accountStore.getTelegramConfig()
            ?: BundledTelegramCredentials.load(context)

    init {
        if (nativeLibraryAvailable) {
            Client.execute(TdApi.SetLogVerbosityLevel(if (BuildConfig.DEBUG) 1 else 0))
            client = Client.create(::handleUpdate, null, null)
        }
    }

    fun configure(newConfig: TelegramConfig) {
        config = newConfig
        accountStore.saveTelegramConfig(newConfig)
        if (_authorizationState.value is TdApi.AuthorizationStateWaitTdlibParameters) {
            sendTdlibParameters(newConfig)
        }
    }

    fun isAvailable(): Boolean = nativeLibraryAvailable

    fun hasConfig(): Boolean = config != null

    fun isReady(): Boolean = _authorizationState.value is TdApi.AuthorizationStateReady

    suspend fun awaitReady(timeoutMs: Long = 20_000L): Boolean {
        if (isReady()) return true
        return withTimeoutOrNull(timeoutMs) {
            authorizationState.first {
                it is TdApi.AuthorizationStateReady || it is TdApi.AuthorizationStateClosed
            }
        } is TdApi.AuthorizationStateReady
    }

    suspend fun <T : TdApi.Object> sendRequest(function: TdApi.Function<*>): T =
        suspendCancellableCoroutine { continuation ->
            val current = client
            if (current == null) {
                continuation.resumeWithException(
                    IllegalStateException("当前设备无法加载 Telegram TDLib"),
                )
                return@suspendCancellableCoroutine
            }
            current.send(function) { result ->
                when (result) {
                    is TdApi.Error -> {
                        _errors.tryEmit(result)
                        continuation.resumeWithException(
                            TelegramRequestException(result.code, result.message),
                        )
                    }
                    else -> {
                        @Suppress("UNCHECKED_CAST")
                        continuation.resume(result as T)
                    }
                }
            }
        }

    fun logout() {
        client?.send(TdApi.LogOut()) { result ->
            if (result is TdApi.Error) _errors.tryEmit(result)
        }
    }

    private fun handleUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateAuthorizationState -> {
                _authorizationState.value = update.authorizationState
                when (val state = update.authorizationState) {
                    is TdApi.AuthorizationStateWaitTdlibParameters -> {
                        config?.let(::sendTdlibParameters)
                    }
                    is TdApi.AuthorizationStateClosed -> client = null
                    else -> Unit
                }
            }
            is TdApi.UpdateFile -> _fileUpdates.tryEmit(update.file)
            is TdApi.Error -> _errors.tryEmit(update)
        }
    }

    private fun sendTdlibParameters(value: TelegramConfig) {
        val databaseDirectory = File(context.filesDir, "tdlib").apply { mkdirs() }.absolutePath
        val filesDirectory = File(context.cacheDir, "tdlib_files").apply { mkdirs() }.absolutePath
        client?.send(
            TdApi.SetTdlibParameters(
                false,
                databaseDirectory,
                filesDirectory,
                null,
                true,
                true,
                true,
                false,
                value.apiId,
                value.apiHash,
                "zh",
                android.os.Build.MODEL,
                android.os.Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME,
            ),
        ) { result ->
            if (result is TdApi.Error) {
                Timber.w("TDLib parameters rejected: ${result.code}")
                _errors.tryEmit(result)
            }
        }
    }

    companion object {
        val nativeLibraryAvailable: Boolean =
            runCatching {
                System.loadLibrary("tdjni")
                true
            }.getOrElse {
                Timber.e(it, "TDLib native library is unavailable")
                false
            }
    }
}

class TelegramRequestException(
    val errorCode: Int,
    message: String,
) : Exception(message)
