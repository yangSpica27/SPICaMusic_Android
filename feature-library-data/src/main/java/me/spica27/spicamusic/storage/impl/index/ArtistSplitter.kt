package me.spica27.spicamusic.storage.impl.index

/**
 * 艺人名单拆分。
 * - 标签里本来就是多个值：始终按原值，不再二次拆分
 * - 单个值：开关关闭时整串算一位；开启时按分隔符和 feat. 拆分，`\` 转义的分隔符不拆
 */
object ArtistSplitter {
    /** 不含逗号，避免拆错 "Earth, Wind & Fire" 这类名称 */
    private val SEPARATORS = setOf('/', '／', ';', '；', '、', '&', '＆')

    /** feat. 前须有空白或左括号，避免拆开 "Defeat." 这类词 */
    private val FEATURING = Regex("(?i)[\\s(\\[（【]+(?:feat\\.|ft\\.|featuring)\\s+")
    private val OPEN_BRACKETS = "([（【"
    private val CLOSE_BRACKETS = ")]）】"

    fun split(
        values: List<String>,
        enabled: Boolean,
    ): List<String> {
        val cleaned = values.map(String::trim).filter(String::isNotEmpty)
        if (cleaned.size != 1 || !enabled) return cleaned
        return splitSingle(cleaned.single())
    }

    private fun splitSingle(value: String): List<String> =
        splitOnSeparators(value)
            .flatMap { part -> part.split(FEATURING) }
            .map(::trimBrackets)
            .filter(String::isNotEmpty)

    private fun splitOnSeparators(value: String): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var index = 0
        while (index < value.length) {
            val char = value[index]
            when {
                char == '\\' && index + 1 < value.length && value[index + 1] in SEPARATORS -> {
                    current.append(value[index + 1])
                    index++
                }
                char in SEPARATORS -> {
                    parts.add(current.toString())
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        parts.add(current.toString())
        return parts
    }

    /** "B)" → "B"、"(A" → "A"：feat. 拆在括号里时只去掉落单的那一侧 */
    private fun trimBrackets(part: String): String {
        var text = part.trim()
        val opens = text.count { it in OPEN_BRACKETS }
        val closes = text.count { it in CLOSE_BRACKETS }
        if (closes > opens && text.lastOrNull()?.let { it in CLOSE_BRACKETS } == true) text = text.dropLast(1).trim()
        if (opens > closes && text.firstOrNull()?.let { it in OPEN_BRACKETS } == true) text = text.drop(1).trim()
        return text
    }
}
