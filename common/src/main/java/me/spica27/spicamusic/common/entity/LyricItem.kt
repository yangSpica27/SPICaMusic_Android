package me.spica27.spicamusic.common.entity

import androidx.compose.runtime.Immutable

// 参考LMusic https://github.com/cy745/lmusic
@Immutable
sealed class LyricItem(
    open val time: Long = 0,
    open val key: String = "",
) : Comparable<LyricItem> {
    /** A voice declaration from AMLL's ttm:agent metadata. */
    @Immutable
    data class Agent(
        val id: String,
        val type: String? = null,
        val name: String? = null,
        val order: Int = 0,
    ) {
        val label: String
            get() = name?.takeIf { it.isNotBlank() } ?: id
    }

    override fun compareTo(other: LyricItem): Int = time.compareTo(other.time)

    override fun toString(): String = "LyricItem(time=$time, key='$key')"

    @Immutable
    data class NormalLyric(
        val content: String,
        val translation: String? = null,
        override val time: Long,
        override val key: String,
        val phonetic: String? = null,
        val agent: String = "",
        val agents: List<Agent> = emptyList(),
    ) : LyricItem()

    @Immutable
    data class WordsLyric(
        val agent: String = "",
        val words: List<WordWithTiming>,
        val translation: List<Translation>,
        val startTime: Long,
        val endTime: Long,
        override val key: String,
        val phonetic: String? = null,
        val accompaniment: List<AccompanimentLyric> = emptyList(),
        val agents: List<Agent> = emptyList(),
    ) : LyricItem(time = startTime) {

        @Immutable
        data class Translation(
            val content: String,
            val lang: String,
        )

        /**
         * A background-vocal track attached to the main line. AMLL stores these
         * tracks as nested `x-bg` spans with their own word timeline.
         */
        @Immutable
        data class AccompanimentLyric(
            val agent: String = "",
            val words: List<WordWithTiming>,
            val translation: List<Translation> = emptyList(),
            val startTime: Long,
            val endTime: Long,
            val phonetic: String? = null,
            val agents: List<Agent> = emptyList(),
        )

        @Immutable
        data class WordWithTiming(
            val content: String,
            val startTime: Long,
            val endTime: Long,
            val phonetic: String? = null,
        ) : Comparable<WordWithTiming> {
            override fun compareTo(other: WordWithTiming): Int = startTime.compareTo(other.startTime)
        }
    }
}

fun List<LyricItem>.findPlayingIndex(time: Long): Int {
    if (isEmpty()) return Int.MAX_VALUE

    var low = 0
    var high = size - 1
    var result = Int.MAX_VALUE

    while (low <= high) {
        val mid = (low + high) ushr 1
        val midVal = get(mid).time

        when {
            midVal <= time -> {
                // 记录最后一个不晚于目标时间的索引。
                // 对于多个歌手同一时刻开始的歌词，这会稳定落到同组最后一行。
                result = mid
                low = mid + 1
            }

            midVal > time -> {
                high = mid - 1
            }

        }
    }

    // 处理边界情况：
    return when {
        // 所有元素的时间都大于目标时间
        result == Int.MAX_VALUE -> Int.MAX_VALUE

        // 检查找到的索引是否有效（下一个元素时间是否超过当前时间）
        result == lastIndex || get(result + 1).time > time -> result

        // 理论上不会到达这里
        else -> Int.MAX_VALUE
    }
}

fun List<LyricItem.WordsLyric.WordWithTiming>.findPlayingIndexForWords(time: Long): Int {
    var left = 0
    var right = size - 1

    while (left <= right) {
        val mid = left + (right - left) / 2
        val midItem = this[mid]

        if (midItem.startTime <= time && midItem.endTime >= time) {
            return mid
        } else if (midItem.endTime < time) {
            left = mid + 1
        } else {
            right = mid - 1
        }
    }

    return Int.MAX_VALUE
}

fun List<LyricItem>.findPlayingItem(time: Long): LyricItem? = this.getOrNull(findPlayingIndex(time))

fun LyricItem.WordsLyric.getSentenceContent(): String = words.joinToString(separator = "") { it.content }

fun LyricItem.voiceAgents(): List<LyricItem.Agent> =
    when (this) {
        is LyricItem.NormalLyric ->
            agents.ifEmpty { agentIds(agent).map { LyricItem.Agent(it) } }
        is LyricItem.WordsLyric ->
            agents.ifEmpty { agentIds(agent).map { LyricItem.Agent(it) } }
    }

fun LyricItem.WordsLyric.AccompanimentLyric.voiceAgents(): List<LyricItem.Agent> =
    agents.ifEmpty { agentIds(agent).map { LyricItem.Agent(it) } }

private fun agentIds(value: String): List<String> =
    value.split(Regex("[\\s,]+"))
        .filter { it.isNotBlank() }
        .distinct()

fun LyricItem.toNormal(): LyricItem.NormalLyric? {
    if (this is LyricItem.NormalLyric) return this
    if (this is LyricItem.WordsLyric) {
        apply {
            val translation = translation.firstOrNull { it.content.isNotBlank() }?.content
            val sentence =
                getSentenceContent()
                    .takeIf { it.isNotBlank() }
                    ?: return null

            return LyricItem.NormalLyric(
                content = sentence,
                translation = translation,
                time = this.time,
                key = this.key,
                phonetic = phonetic,
                agent = agent,
                agents = agents,
            )
        }
    }

    return null
}
