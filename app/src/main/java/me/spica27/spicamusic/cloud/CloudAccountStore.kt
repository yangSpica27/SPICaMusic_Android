package me.spica27.spicamusic.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 云端账号只保存在设备上。令牌和 Telegram API 凭据由 Android Keystore 加密，
 * 不写入日志，也不会进入 Room 的本地音乐资料库。
 */
class CloudAccountStore(
    context: Context,
) {
    private val preferences =
        context.getSharedPreferences("cloud_library_accounts", Context.MODE_PRIVATE)

    fun getAccounts(type: MediaServerType? = null): List<MediaServerAccount> {
        val payload = decrypt(preferences.getString(KEY_MEDIA_SERVERS, null)) ?: return emptyList()
        val array = runCatching { JSONArray(payload) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val account =
                    runCatching {
                        MediaServerAccount(
                            id = item.getString("id"),
                            type = MediaServerType.valueOf(item.getString("type")),
                            displayName = item.getString("displayName"),
                            serverUrl = item.getString("serverUrl"),
                            username = item.getString("username"),
                            userId = item.getString("userId"),
                            accessToken = item.getString("accessToken"),
                        )
                    }.getOrNull() ?: continue
                if (type == null || account.type == type) add(account)
            }
        }
    }

    fun saveAccount(account: MediaServerAccount) {
        val accounts = getAccounts().filterNot { it.id == account.id } + account
        val array = JSONArray()
        accounts.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("type", it.type.name)
                    .put("displayName", it.displayName)
                    .put("serverUrl", it.serverUrl)
                    .put("username", it.username)
                    .put("userId", it.userId)
                    .put("accessToken", it.accessToken),
            )
        }
        preferences.edit().putString(KEY_MEDIA_SERVERS, encrypt(array.toString())).apply()
    }

    fun removeAccount(id: String) {
        val remaining = getAccounts().filterNot { it.id == id }
        val array = JSONArray()
        remaining.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("type", it.type.name)
                    .put("displayName", it.displayName)
                    .put("serverUrl", it.serverUrl)
                    .put("username", it.username)
                    .put("userId", it.userId)
                    .put("accessToken", it.accessToken),
            )
        }
        preferences.edit().putString(KEY_MEDIA_SERVERS, encrypt(array.toString())).apply()
    }

    fun newAccountId(): String = UUID.randomUUID().toString()

    fun getRemoteAccounts(provider: RemoteMusicProvider? = null): List<RemoteMusicAccount> {
        val payload = decrypt(preferences.getString(KEY_REMOTE_MUSIC, null)) ?: return emptyList()
        val array = runCatching { JSONArray(payload) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val account =
                    runCatching {
                        RemoteMusicAccount(
                            id = item.getString("id"),
                            provider = RemoteMusicProvider.valueOf(item.getString("provider")),
                            displayName = item.getString("displayName"),
                            serverUrl = item.optString("serverUrl"),
                            username = item.optString("username"),
                            secret = item.getString("secret"),
                            userId = item.optString("userId"),
                        )
                    }.getOrNull() ?: continue
                if (provider == null || account.provider == provider) add(account)
            }
        }
    }

    fun saveRemoteAccount(account: RemoteMusicAccount) {
        writeRemoteAccounts(getRemoteAccounts().filterNot { it.id == account.id } + account)
    }

    fun removeRemoteAccount(id: String) {
        writeRemoteAccounts(getRemoteAccounts().filterNot { it.id == id })
    }

    private fun writeRemoteAccounts(accounts: List<RemoteMusicAccount>) {
        val array = JSONArray()
        accounts.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("provider", it.provider.name)
                    .put("displayName", it.displayName)
                    .put("serverUrl", it.serverUrl)
                    .put("username", it.username)
                    .put("secret", it.secret)
                    .put("userId", it.userId),
            )
        }
        preferences.edit().putString(KEY_REMOTE_MUSIC, encrypt(array.toString())).apply()
    }

    fun getTelegramConfig(): TelegramConfig? {
        val payload = decrypt(preferences.getString(KEY_TELEGRAM_CONFIG, null)) ?: return null
        val json = runCatching { JSONObject(payload) }.getOrNull() ?: return null
        val apiId = json.optInt("apiId")
        val apiHash = json.optString("apiHash")
        return if (apiId > 0 && apiHash.isNotBlank()) TelegramConfig(apiId, apiHash) else null
    }

    fun saveTelegramConfig(config: TelegramConfig) {
        val json = JSONObject().put("apiId", config.apiId).put("apiHash", config.apiHash)
        preferences.edit().putString(KEY_TELEGRAM_CONFIG, encrypt(json.toString())).apply()
    }

    fun getTelegramChannels(): List<TelegramChannel> {
        val payload = decrypt(preferences.getString(KEY_TELEGRAM_CHANNELS, null)) ?: return emptyList()
        val array = runCatching { JSONArray(payload) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    TelegramChannel(
                        chatId = item.optLong("chatId"),
                        title = item.optString("title"),
                        username = item.optString("username"),
                    ),
                )
            }
        }
    }

    fun saveTelegramChannel(channel: TelegramChannel) {
        val channels = getTelegramChannels().filterNot { it.chatId == channel.chatId } + channel
        writeTelegramChannels(channels)
    }

    fun removeTelegramChannel(chatId: Long) {
        writeTelegramChannels(getTelegramChannels().filterNot { it.chatId == chatId })
    }

    private fun writeTelegramChannels(channels: List<TelegramChannel>) {
        val array = JSONArray()
        channels.forEach {
            array.put(
                JSONObject()
                    .put("chatId", it.chatId)
                    .put("title", it.title)
                    .put("username", it.username),
            )
        }
        preferences.edit().putString(KEY_TELEGRAM_CHANNELS, encrypt(array.toString())).apply()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
    }

    private fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            if (bytes.size <= IV_SIZE) return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                GCMParameterSpec(GCM_TAG_BITS, bytes.copyOfRange(0, IV_SIZE)),
            )
            String(cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size)), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_MEDIA_SERVERS = "media_servers"
        const val KEY_REMOTE_MUSIC = "remote_music_accounts"
        const val KEY_TELEGRAM_CONFIG = "telegram_config"
        const val KEY_TELEGRAM_CHANNELS = "telegram_channels"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "spica_cloud_library_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val GCM_TAG_BITS = 128
    }
}
