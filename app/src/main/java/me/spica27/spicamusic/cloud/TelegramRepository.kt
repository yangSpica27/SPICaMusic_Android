package me.spica27.spicamusic.cloud

import kotlinx.coroutines.flow.Flow
import org.drinkless.tdlib.TdApi

class TelegramRepository(
    private val client: TelegramClientManager,
    private val accountStore: CloudAccountStore,
) {
    val authorizationState: Flow<TdApi.AuthorizationState?> = client.authorizationState
    val errors = client.errors

    fun isAvailable(): Boolean = client.isAvailable()

    fun configure(config: TelegramConfig) = client.configure(config)

    fun hasConfig(): Boolean = client.hasConfig()

    fun savedChannels(): List<TelegramChannel> = accountStore.getTelegramChannels()

    suspend fun sendPhoneNumber(phone: String) {
        client.sendRequest<TdApi.Ok>(
            TdApi.SetAuthenticationPhoneNumber(
                phone,
                TdApi.PhoneNumberAuthenticationSettings(),
            ),
        )
    }

    suspend fun checkCode(code: String) {
        client.sendRequest<TdApi.Ok>(TdApi.CheckAuthenticationCode(code))
    }

    suspend fun checkPassword(password: String) {
        client.sendRequest<TdApi.Ok>(TdApi.CheckAuthenticationPassword(password))
    }

    fun logout() = client.logout()

    suspend fun addPublicChannel(username: String): TelegramChannel {
        check(client.awaitReady()) { "Telegram 尚未连接" }
        val normalized = username.trim().removePrefix("@")
        require(normalized.isNotBlank()) { "请输入频道用户名" }
        val chat = client.sendRequest<TdApi.Chat>(TdApi.SearchPublicChat(normalized))
        val channel = TelegramChannel(chat.id, chat.title, normalized)
        accountStore.saveTelegramChannel(channel)
        return channel
    }

    fun removeChannel(chatId: Long) = accountStore.removeTelegramChannel(chatId)

    fun saveChannel(channel: TelegramChannel) = accountStore.saveTelegramChannel(channel)

    suspend fun getJoinedChannels(limit: Int = 100): List<TelegramChannel> {
        check(client.awaitReady()) { "Telegram 尚未连接" }
        val chatList = TdApi.ChatListMain()
        runCatching {
            client.sendRequest<TdApi.Ok>(TdApi.LoadChats(chatList, limit.coerceIn(1, 100)))
        }
        val chats =
            client.sendRequest<TdApi.Chats>(
                TdApi.GetChats(chatList, limit.coerceIn(1, 100)),
            )
        val channels = mutableListOf<TelegramChannel>()
        for (chatId in chats.chatIds) {
            val chat =
                runCatching {
                    client.sendRequest<TdApi.Chat>(TdApi.GetChat(chatId))
                }.getOrNull() ?: continue
            if ((chat.type as? TdApi.ChatTypeSupergroup)?.isChannel != true) continue
            channels +=
                TelegramChannel(
                    chatId = chat.id,
                    title = chat.title,
                    username = "",
                )
        }
        return channels.sortedBy { it.title.lowercase() }
    }

    suspend fun getAudioPage(
        chatId: Long,
        fromMessageId: Long = 0L,
        limit: Int = PAGE_SIZE,
    ): TelegramSongPage {
        check(client.awaitReady()) { "Telegram 尚未连接" }
        runCatching { client.sendRequest<TdApi.Ok>(TdApi.OpenChat(chatId)) }
        val request =
            TdApi.SearchChatMessages().apply {
                this.chatId = chatId
                query = ""
                senderId = null
                this.fromMessageId = fromMessageId
                offset = 0
                this.limit = limit.coerceIn(1, PAGE_SIZE)
                filter = TdApi.SearchMessagesFilterAudio()
            }
        val response = client.sendRequest<TdApi.FoundChatMessages>(request)
        return TelegramSongPage(
            songs = response.messages.mapNotNull(::mapMessage),
            nextFromMessageId = response.nextFromMessageId.takeIf { it != 0L },
        )
    }

    suspend fun download(
        fileId: Int,
        offset: Long,
        limit: Long,
    ): TdApi.File =
        client.sendRequest(
            TdApi.DownloadFile(
                fileId,
                DOWNLOAD_PRIORITY,
                offset.coerceAtLeast(0L),
                limit.coerceAtLeast(0L),
                false,
            ),
        )

    suspend fun getFile(fileId: Int): TdApi.File = client.sendRequest(TdApi.GetFile(fileId))

    private fun mapMessage(message: TdApi.Message): TelegramSong? =
        when (val content = message.content) {
            is TdApi.MessageAudio -> {
                val audio = content.audio
                TelegramSong(
                    messageId = message.id,
                    chatId = message.chatId,
                    fileId = audio.audio.id,
                    fileSize =
                        audio.audio.expectedSize
                            .takeIf { it > 0L }
                            ?: audio.audio.size,
                    title =
                        audio.title
                            .ifBlank { audio.fileName.substringBeforeLast('.') }
                            .ifBlank { "Unknown title" },
                    artist = audio.performer.ifBlank { "Unknown artist" },
                    durationMs = audio.duration * 1_000L,
                    mimeType = audio.mimeType.ifBlank { "audio/mpeg" },
                )
            }
            else -> null
        }

    companion object {
        const val PAGE_SIZE = 60
        private const val DOWNLOAD_PRIORITY = 16
    }
}
