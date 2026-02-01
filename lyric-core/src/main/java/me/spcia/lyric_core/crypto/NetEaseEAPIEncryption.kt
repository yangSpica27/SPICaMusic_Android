package me.spcia.lyric_core.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * EAPI 加密/解密工具
 * 
 * 参考 LDDC 项目实现：
 * https://github.com/chenmozhijin/LDDC/blob/main/LDDC/core/decryptor/eapi.py
 */
object NetEaseEAPIEncryption {
    
    // EAPI 密钥常量
    private const val EAPI_KEY = "e82ckenh8dichen8"
    
    // EAPI 加密 URL 填充字符串
    private const val EAPI_URL = "-36cd479b6b5-"
    
    /**
     * 加密 EAPI 参数
     * 
     * @param url API 路径 (如 /api/song/lyric/v1)
     * @param params 参数 Map
     * @return 加密后的字节数组
     */
    fun encryptParams(url: String, params: Map<String, Any>): ByteArray {
        // 1. 构造参数字符串
        val paramsStr = buildParamsString(params)
        
        // 2. 计算消息摘要
        val message = "nobody${url}use${paramsStr}md5forencrypt"
        val digest = md5(message)
        
        // 3. 构造完整文本
        val text = "$url-36cd479b6b5-$paramsStr-36cd479b6b5-$digest"
        
        // 4. AES ECB 加密
        return aesEcbEncrypt(text.toByteArray(Charsets.UTF_8), EAPI_KEY)
    }
    
    /**
     * 解密 EAPI 响应
     * 
     * @param data 加密的响应数据
     * @return 解密后的 JSON 字符串
     */
    fun decryptResponse(data: ByteArray): String {
        return try {
            val decrypted = aesEcbDecrypt(data, EAPI_KEY)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            throw IllegalStateException("EAPI 响应解密失败", e)
        }
    }
    
    /**
     * 生成匿名用户名
     * 
     * @param deviceId 设备ID
     * @return Base64编码的用户名
     */
    fun getAnonymousUsername(deviceId: String): String {
        val username = deviceId
        return android.util.Base64.encodeToString(
            username.toByteArray(Charsets.UTF_8),
            android.util.Base64.NO_WRAP
        )
    }
    
    /**
     * 生成缓存键
     * 
     * @param params 参数字符串
     * @return MD5 哈希值
     */
    fun getCacheKey(params: String): String {
        return md5(params)
    }
    
    // =============== 私有辅助方法 ===============
    
    /**
     * 构建参数字符串 (key1=value1&key2=value2)
     */
    private fun buildParamsString(params: Map<String, Any>): String {
        return params.entries
            .sortedBy { it.key }
            .joinToString("&") { (key, value) ->
                "$key=${encodeValue(value)}"
            }
    }
    
    /**
     * 编码参数值
     */
    private fun encodeValue(value: Any): String {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> value.joinToString(",")
            is Map<*, *> -> value.entries.joinToString(",") { "${it.key}:${it.value}" }
            else -> value.toString()
        }
    }
    
    /**
     * MD5 哈希
     */
    private fun md5(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(text.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * AES ECB 模式加密
     */
    private fun aesEcbEncrypt(data: ByteArray, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }
    
    /**
     * AES ECB 模式解密
     */
    private fun aesEcbDecrypt(data: ByteArray, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec)
        return cipher.doFinal(data)
    }
}
