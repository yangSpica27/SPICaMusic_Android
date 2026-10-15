package me.spica27.spicamusic.storage.impl.scanner

import android.icu.text.Transliterator
import me.spica27.spicamusic.storage.impl.index.IndexText
import timber.log.Timber

/** [IndexText] 的 Android 实现：繁转简与拼音 / 罗马音首字母使用 android.icu */
object AndroidIndexText {
    private const val TAG = "AndroidIndexText"

    val instance: IndexText by lazy {
        IndexText(simplify = ::simplify, initial = ::initial)
    }

    private val traditionalToSimplified by lazy { transliterator("Traditional-Simplified") }
    private val hanToLatin by lazy { transliterator("Han-Latin; Latin-ASCII") }
    private val hiraganaToLatin by lazy { transliterator("Hiragana-Latin") }
    private val katakanaToLatin by lazy { transliterator("Katakana-Latin") }

    private fun transliterator(id: String): Transliterator? =
        runCatching { Transliterator.getInstance(id) }
            .onFailure { Timber.tag(TAG).w(it, "Transliterator 不可用: $id") }
            .getOrNull()

    private fun isHan(char: Char): Boolean = char.code in 0x3400..0x9FFF || char.code in 0xF900..0xFAFF

    private fun simplify(text: String): String {
        if (text.none(::isHan)) return text
        return traditionalToSimplified?.transliterate(text) ?: text
    }

    /**
     * 排序首字母：英文取首字母，中文取拼音首字母，日文假名取罗马音首字母，其余为 #
     */
    private fun initial(name: String): String {
        val first = name.firstOrNull() ?: return "#"
        val latin =
            when {
                first.code in 0x41..0x5A || first.code in 0x61..0x7A -> first.toString()
                isHan(first) -> hanToLatin?.transliterate(first.toString())
                first.code in 0x3040..0x309F -> hiraganaToLatin?.transliterate(first.toString())
                first.code in 0x30A0..0x30FF -> katakanaToLatin?.transliterate(first.toString())
                else -> null
            }
        val letter = latin?.firstOrNull()?.uppercaseChar() ?: return "#"
        return if (letter in 'A'..'Z') letter.toString() else "#"
    }
}
