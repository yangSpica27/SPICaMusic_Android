package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.getSentenceContent
import kotlin.math.abs

// 参考LMusic https://github.com/cy745/lmusic
object LrcParser {
  /**
   * LRC 行时间标签：支持 [mm:ss]、[mm:ss.x]、[mm:ss.xx] 等常见变体。
   * 分钟允许 1～3 位，毫秒允许省略或使用 1～6 位；解析时统一换算为毫秒。
   */
  private val REGEX_TIME = Regex("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,6}))?]")

  /** 增强型 LRC 的逐字时间标签，格式与行标签相同但使用尖括号。 */
  private val REGEX_TIME_EX = Regex("<(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,6}))?>")

  /**
   * 逐字时间戳相对[..]行时间标签允许的最大偏差（毫秒），
   * 超过则认为逐字时间轴不可信（部分歌词源生成的逐字时间戳整体错位）
   */
  private const val MAX_WORD_TIME_DEVIATION_MS = 1000L

  /**
   * 解析LRC格式的歌词字符串
   * @param lyric LRC格式的歌词字符串
   * @return 解析后的歌词项列表
   */
  fun parse(lyric: String): List<LyricItem> {
    if (lyric.isBlank()) return emptyList()

    // 首先将所有句子按单句进行解析
    val mainEntryList = parseLrc(lyric)
      ?.takeIf(Collection<*>::isNotEmpty)
      ?: return emptyList()

    // 合并相同时间的单句，其中第一个单句作为主句子，第二个单句作为翻译
    return mainEntryList.groupBy { it.time }
      .toList()
      .sortedBy { it.first }
      .mapNotNull { (time, list) ->
        val first = list.getOrNull(0) ?: return@mapNotNull null
        val second = list.getOrNull(1) ?: return@mapNotNull first
        val translationText = when (second) {
          is LyricItem.WordsLyric -> second.getSentenceContent()
          is LyricItem.NormalLyric -> second.content
          else -> return@mapNotNull null
        }

        when (first) {
          is LyricItem.WordsLyric -> first.copy(
            translation = listOf(
              LyricItem.WordsLyric.Translation(
                translationText,
                "unknown"
              )
            )
          )

          is LyricItem.NormalLyric -> first.copy(
            translation = translationText
          )

          else -> return@mapNotNull null
        }
      }.mapIndexedNotNull { index, item ->
        return@mapIndexedNotNull when (item) {
          is LyricItem.WordsLyric -> item.copy(key = "$index${item.key}")
          is LyricItem.NormalLyric -> item.copy(key = "$index${item.key}")
          else -> null
        }
      }
  }

  /**
   * 从文本解析歌词
   * @param lrcText LRC格式的歌词文本
   * @return 解析后的歌词项列表
   */
  private fun parseLrc(lrcText: String): List<LyricItem>? {
    var lyricText = lrcText.trim()
    if (lyricText.isEmpty()) return null

    if (lyricText.startsWith("\uFEFF")) {
      lyricText = lyricText.replace("\uFEFF", "")
    }

    // 针对传入 Language="Media Monkey Format"; Lyrics="......"; 的情况
    if (lyricText.contains("Lyrics=\"")) {
      lyricText = lyricText.substringAfter("Lyrics=\"")
        .substringBeforeLast("\";")
    }

    return lyricText
      .split("\n")
      .toTypedArray()
      .mapNotNull { parseLine(it)?.takeIf(Collection<*>::isNotEmpty) }
      .flatten()
      .sorted()
  }

  /**
   * 解析一行歌词
   * @param line LRC格式的一行歌词
   * @return 解析后的歌词项列表
   */
  private fun parseLine(line: String): List<LyricItem>? {
    var lyricLine = line
    if (lyricLine.isEmpty()) return null

    lyricLine = lyricLine.trim { it <= ' ' }

    // [00:17.65]让我掉下眼泪的
    // [00:17.65]让我掉下眼泪的[00:19.66]
    var findResult = REGEX_TIME
      .findAll(lyricLine)
      .toList()

    // 增强型LRC（[行时间]<逐字时间>...）中[..]标签定义整句时间，
    // <..>标签仅用于逐字高亮；整句时间必须取行标签，
    // 才能与共享同一行时间标签的翻译行配对
    var lineTime: Long? = null
    var firstWordTagTime: Long? = null

    // 当歌词中有一个行时间标签时尝试匹配增强型 LRC 的 <..> 逐字时间标签
    if (findResult.size == 1) {
      val temp = REGEX_TIME_EX
        .findAll(lyricLine)
        .toList()

      // 当存在<00:00.000>格式的时间标签时，则使用<00:00.000>格式的时间标签
      if (temp.isNotEmpty()) {
        lineTime = timeTagToTime(findResult.first().value).takeIf { it >= 0 }
        firstWordTagTime = timeTagToTime(temp.first().value).takeIf { it >= 0 }
        findResult = temp
      }
    }

    // 若没有时间标签，则返回 null
    if (findResult.isEmpty()) {
      return null
    }

    // 拆分歌词和时间标签
    val textSplits = mutableListOf<LrcContentItem>()
    for (i in findResult.indices) {
      val item = findResult[i]
      textSplits.add(LrcContentItem.TimeTag(timeTagToTime(item.value)))

      val endIndex = findResult.getOrNull(i + 1)?.range?.first ?: (lyricLine.lastIndex + 1)
      val startIndex = item.range.last + 1

      if (startIndex <= endIndex) {
        val text = lyricLine.substring(startIndex, endIndex)
        if (text.isNotEmpty()) {
          textSplits.add(LrcContentItem.Text(text))
        }
      }
    }

    // 为歌词单词文本添加开始时间和结束时间
    val words = textSplits.mapIndexedNotNull { index, item ->
      if (item is LrcContentItem.TimeTag) return@mapIndexedNotNull null
      val text = item as? LrcContentItem.Text ?: return@mapIndexedNotNull null

      val startTime = (textSplits.getOrNull(index - 1) as? LrcContentItem.TimeTag)
        ?.time ?: return@mapIndexedNotNull null
      val endTime = (textSplits.getOrNull(index + 1) as? LrcContentItem.TimeTag)
        ?.time ?: startTime

      LyricItem.WordsLyric.WordWithTiming(
        content = text.text,
        startTime = startTime,
        endTime = endTime
      )
    }

    // 若无结果则尽早返回
    if (words.isEmpty()) return emptyList()
    val firstWord = words[0]
    val lastWord = words.last()

    // 整句时间优先取[..]行标签，否则退回第一个逐字时间
    val sentenceTime = lineTime ?: firstWord.startTime

    // 若只有一个词/句，且其开始时间等于其结束时间，则认为其就是一个普通句子
    if (words.size == 1 && firstWord.startTime == firstWord.endTime) {
      return listOf(
        LyricItem.NormalLyric(
          content = firstWord.content,
          time = sentenceTime,
          key = "$sentenceTime"
        )
      )
    }

    // 逐字时间轴与行时间严重偏离时视为不可信，
    // 降级为普通句子，避免整句在播放期间永远处于未唱状态。
    // 偏差以首个<..>标签为准：正常歌词的首个逐字标签与行标签一致
    // （人声晚进的行不受影响），错位歌词的首个标签即已偏离
    if (lineTime != null && firstWordTagTime != null &&
      abs(firstWordTagTime - lineTime) > MAX_WORD_TIME_DEVIATION_MS
    ) {
      return listOf(
        LyricItem.NormalLyric(
          content = words.joinToString(separator = "") { it.content },
          time = lineTime,
          key = "$lineTime"
        )
      )
    }

    // 否则将其输出为逐字歌词对象
    return listOf(
      LyricItem.WordsLyric(
        words = words,
        translation = emptyList(),
        startTime = sentenceTime,
        endTime = maxOf(lastWord.endTime, sentenceTime),
        key = "$sentenceTime"
      )
    )
  }

  /**
   * 负责解析并转换 `[mm:ss]` / `[mm:ss.xxx]` 和对应尖括号格式的时间标签。
   * @param str 时间标签字符串，例如 `[01:04]`、`[01:04.50]` 或 `<01:04.500>`
   * @return 转换后的时间戳（毫秒）
   */
  fun timeTagToTime(str: String): Long {

    // 匹配方括号时间标签
    var timeMatcher = REGEX_TIME.matchEntire(str)
      ?.groupValues
      ?.takeIf { it.isNotEmpty() }

    // 尝试匹配尖括号时间标签
    if (timeMatcher == null) {
      timeMatcher = REGEX_TIME_EX.matchEntire(str)
        ?.groupValues
        ?.takeIf { it.isNotEmpty() }
        ?: return -1L
    }

    val min = timeMatcher.getOrNull(1)!!.toLong()
    val sec = timeMatcher.getOrNull(2)!!.toLong()
    val milString = timeMatcher.getOrNull(3).orEmpty()

    // 统一把 1～6 位小数扩展/截断到毫秒；没有小数的 [mm:ss] 视为整秒。
    val mil = when (milString.length) {
      0 -> 0L
      1 -> milString.toLong() * 100
      2 -> milString.toLong() * 10
      3 -> milString.toLong()
      4 -> milString.toLong() / 10
      5 -> milString.toLong() / 100
      else -> milString.toLong() / 1000
    }

    return min * 60 * 1000 + sec * 1000 + mil
  }

  /**
   * LRC内容项元素接口，用于表示时间标签或文本内容
   */
  private sealed interface LrcContentItem {
    /**
     * 时间标签项
     * @param time 时间戳
     */
    data class TimeTag(val time: Long) : LrcContentItem

    /**
     * 文本内容项
     * @param text 文本内容
     */
    data class Text(val text: String) : LrcContentItem
  }
}
