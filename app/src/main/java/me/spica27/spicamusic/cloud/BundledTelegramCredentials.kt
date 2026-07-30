package me.spica27.spicamusic.cloud

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import me.spica27.spicamusic.BuildConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Decrypts the optional build-time Telegram credentials.
 *
 * The APK contains only AES-GCM ciphertext. The key is derived from the installed APK signing
 * certificate, application id, and a versioned domain separator, so a repackaged APK cannot use
 * the bundled credentials. This is obfuscation for a client-distributed credential, not a
 * substitute for a server-held secret.
 */
object BundledTelegramCredentials {
    fun load(context: Context): TelegramConfig? {
        val payload = BuildConfig.TELEGRAM_API_ENCRYPTED
        if (payload.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            require(bytes.size > IV_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(deriveKey(context), "AES"),
                GCMParameterSpec(GCM_TAG_BITS, bytes.copyOfRange(0, IV_SIZE)),
            )
            val plaintext =
                String(
                    cipher.doFinal(bytes.copyOfRange(IV_SIZE, bytes.size)),
                    StandardCharsets.UTF_8,
                )
            val parts = plaintext.split('\n', limit = 2)
            val apiId = parts.getOrNull(0)?.toIntOrNull() ?: return null
            val apiHash = parts.getOrNull(1).orEmpty()
            TelegramConfig(apiId, apiHash).takeIf { apiId > 0 && apiHash.isNotBlank() }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun deriveKey(context: Context): ByteArray {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    PackageManager.GET_SIGNATURES
                },
            )
        val certificate =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                packageInfo.signatures?.firstOrNull()?.toByteArray()
            } ?: error("Application signing certificate is unavailable")
        return MessageDigest
            .getInstance("SHA-256")
            .apply {
                update(certificate)
                update(context.packageName.toByteArray(StandardCharsets.UTF_8))
                update(DOMAIN.toByteArray(StandardCharsets.UTF_8))
            }.digest()
    }

    private const val DOMAIN = "spica.telegram.credentials.v1"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val GCM_TAG_BITS = 128
}
