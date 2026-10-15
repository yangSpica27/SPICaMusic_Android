package me.spica27.spicamusic.storage.impl.index

import java.text.Normalizer
import java.util.Locale

/**
 * 索引用的文本处理：名称键（只用于匹配）与排序首字母。
 * 繁简转换、拼音首字母依赖 android.icu，由调用方注入，索引器本身可在 JVM 上测试。
 * 结果按原文缓存：重建关系会反复处理同一批名称。
 */
class IndexText(
    private val simplify: (String) -> String,
    private val initial: (String) -> String,
) {
    private val keyCache = HashMap<String, String>()
    private val sortCache = HashMap<String, String>()

    /** NFKC → 去零宽字符 → 合并空白 → 小写 → 繁转简 */
    @Synchronized
    fun key(name: String): String =
        keyCache.getOrPut(name) {
            simplify(clean(Normalizer.normalize(name, Normalizer.Form.NFKC)).lowercase(Locale.ROOT))
        }

    /** 显示用：只去掉零宽字符并合并空白，保留原有写法 */
    fun clean(name: String): String = name.replace(ZERO_WIDTH, "").replace(WHITESPACE, " ").trim()

    /** 首字母分组：A-Z，其余为 # */
    @Synchronized
    fun sortName(name: String): String = sortCache.getOrPut(name) { initial(name.trim()) }

    private companion object {
        val ZERO_WIDTH = Regex("[\\u200B-\\u200D\\u2060\\uFEFF]")
        val WHITESPACE = Regex("\\s+")
    }
}
