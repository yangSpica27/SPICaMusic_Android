package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

  @Test
  fun parse_pairsTranslationWithPlainLrcLine() {
    val lyric = """
      [00:17.65]让我掉下眼泪的
      [00:17.65]Not only the wine of last night
    """.trimIndent()

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.NormalLyric
    assertEquals(17650L, item.time)
    assertEquals("让我掉下眼泪的", item.content)
    assertEquals("Not only the wine of last night", item.translation)
  }

  @Test
  fun parse_pairsTranslationWithEnhancedLrcLine() {
    // 标准增强型LRC：行标签与第一个逐字标签一致
    val lyric = """
      [00:13.20]<00:13.20>Hello <00:13.55>world<00:14.00>
      [00:13.20]你好世界
    """.trimIndent()

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.WordsLyric
    assertEquals(13200L, item.time)
    assertEquals(13200L, item.startTime)
    assertEquals(14000L, item.endTime)
    assertEquals(listOf("Hello ", "world"), item.words.map { it.content })
    assertEquals(listOf(13200L, 13550L), item.words.map { it.startTime })
    assertEquals("你好世界", item.translation.single().content)
  }

  @Test
  fun parse_usesLineTagTimeWhenWordTimingSlightlyDeviates() {
    // 逐字时间与行标签存在少量偏差（容差内）：保留逐字信息，整句时间取行标签
    val lyric = """
      [00:10.00]<00:10.40>Late <00:10.80>start<00:11.20>
      [00:10.00]晚一点开始
    """.trimIndent()

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.WordsLyric
    assertEquals(10000L, item.time)
    assertEquals(listOf(10400L, 10800L), item.words.map { it.startTime })
    assertEquals("晚一点开始", item.translation.single().content)
  }

  @Test
  fun parse_keepsWordTimingForVocalLeadIn() {
    // 人声晚进：首个<..>标签与行标签一致（时间轴可信），
    // 首个实际唱词晚于行时间1.5s，不应被误降级
    val lyric = """
      [00:10.00]<00:10.00><00:11.50>Wait <00:12.00>for it<00:12.50>
      [00:10.00]等一下
    """.trimIndent()

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.WordsLyric
    assertEquals(10000L, item.time)
    assertEquals(listOf("Wait ", "for it"), item.words.map { it.content })
    assertEquals(listOf(11500L, 12000L), item.words.map { it.startTime })
    assertEquals("等一下", item.translation.single().content)
  }

  @Test
  fun parse_clampsEndTimeWhenWordTimelinePrecedesLineTag() {
    // 逐字时间早于行标签但在容差内：endTime不得小于startTime
    val lyric = "[00:11.00]<00:10.20>Hi<00:10.80>"

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.WordsLyric
    assertEquals(11000L, item.startTime)
    assertTrue(item.endTime >= item.startTime)
  }

  @Test
  fun parse_degradesToNormalLyricWhenWordTimingIsBroken() {
    // 复现线上问题：歌词源生成的逐字时间戳整体错位（恰好为行时间的两倍），
    // 修复前主句时间取自逐字标签，无法与翻译行按时间配对，被拆成两组
    val lyric = """
      [00:00.00]<00:00.00>Produced <00:00.15><00:00.15>by<00:00.31><00:00.31>：<00:00.47><00:00.47>ODESZA<00:00.63>
      [00:41.76]<01:23.52>I <01:23.55><01:23.55>loved <01:24.18><01:24.18>you <01:24.24><01:24.24>most<01:24.75><01:24.75>.<01:26.93><01:26.93>.<01:29.11><01:29.11>.<01:31.29>
      [00:41.76]我最爱你
      [00:49.86]<01:39.72>I <01:39.75><01:39.75>loved <01:40.26><01:40.26>you <01:40.38><01:40.38>most<01:40.98><01:40.98>, <01:41.64><01:41.64>I <01:41.94><01:41.94>love <01:42.24><01:42.24>you <01:42.39><01:42.39>more <01:42.78><01:42.78>now<01:42.90><01:42.90>.<01:42.99>
      [00:49.86]你是我最爱，如今更爱你了
    """.trimIndent()

    val parsed = LrcParser.parse(lyric)

    // 主句与翻译两两合并，而不是拆成互不相干的两组
    assertEquals(3, parsed.size)

    // 逐字时间与行标签一致的行保持逐字歌词
    val intro = parsed[0] as LyricItem.WordsLyric
    assertEquals(0L, intro.time)
    assertTrue(intro.translation.isEmpty())

    // 逐字时间错位的行降级为普通句子，时间取行标签，翻译正确挂载
    val first = parsed[1] as LyricItem.NormalLyric
    assertEquals(41760L, first.time)
    assertEquals("I loved you most...", first.content)
    assertEquals("我最爱你", first.translation)

    val second = parsed[2] as LyricItem.NormalLyric
    assertEquals(49860L, second.time)
    assertEquals("I loved you most, I love you more now.", second.content)
    assertEquals("你是我最爱，如今更爱你了", second.translation)
  }

  @Test
  fun parse_enhancedLineWithoutTranslationKeepsWordTiming() {
    val lyric = "[00:05.00]<00:05.00>Solo <00:05.50>line<00:06.00>"

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.WordsLyric
    assertEquals(5000L, item.time)
    assertEquals(6000L, item.endTime)
    assertTrue(item.translation.isEmpty())
  }

  @Test
  fun parse_normalLineWithoutTranslationHasNullTranslation() {
    val lyric = "[00:01.00]hello"

    val parsed = LrcParser.parse(lyric)

    assertEquals(1, parsed.size)
    val item = parsed.single() as LyricItem.NormalLyric
    assertNull(item.translation)
  }

  @Test
  fun parse_realWorldBrokenEnhancedLrcPairsAllTranslations() {
    // 线上真实数据：A Moment Apart - ODESZA（歌词源逐字时间戳整体为行时间两倍）
    // 共20句主歌词，除第一句制作人信息外每句都跟随一行同时间的翻译
    val lyric = javaClass.classLoader!!
      .getResourceAsStream("lyrics_a_moment_apart.lrc")!!
      .readBytes()
      .toString(Charsets.UTF_8)

    val parsed = LrcParser.parse(lyric)

    // 主句与翻译一一合并为20项，而不是主句、翻译各占一组共39项
    assertEquals(20, parsed.size)

    // 时间严格递增，且全部落在行时间轴内（未混入两倍时间轴的句子）
    assertTrue(parsed.zipWithNext().all { (a, b) -> a.time < b.time })
    assertTrue(parsed.all { it.time <= 193830L })

    // 除第一句外，每句都带翻译
    val withTranslation = parsed.count { item ->
      when (item) {
        is LyricItem.NormalLyric -> !item.translation.isNullOrBlank()
        is LyricItem.WordsLyric -> item.translation.any { it.content.isNotBlank() }
        else -> false
      }
    }
    assertEquals(19, withTranslation)
  }
}
