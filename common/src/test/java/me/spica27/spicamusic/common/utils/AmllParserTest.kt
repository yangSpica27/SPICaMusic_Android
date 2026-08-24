package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmllParserTest {
    @Test
    fun parsesWordTimingTranslationPhoneticAndAccompaniment() {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1" />
                    <iTunesMetadata>
                        <translation><text for="L1">Metadata translation<span ttm:role="x-bg">Background translation</span></text></translation>
                        <transliterations><transliteration><text for="L1">
                            <span>Halo</span><span>waludo</span>
                        </text></transliteration></transliterations>
                    </iTunesMetadata>
                </metadata></head>
                <body><div>
                    <p begin="00:01.000" end="00:03.000" ttm:agent="v1" itunes:key="L1">
                        <span begin="00:01.000" end="00:01.500">Hello </span>
                        <span begin="00:01.500" end="00:02.000">world</span>
                        <span ttm:role="x-translation">你好世界</span>
                        <span ttm:role="x-roman">Halo waludo</span>
                        <span ttm:role="x-bg" begin="00:01.200" end="00:02.500">
                            <span begin="00:01.200" end="00:01.800">Ooh</span>
                            <span ttm:role="x-translation">哦</span>
                        </span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val parsed = AmllParser.parse(content)

        assertEquals(1, parsed.size)
        val line = parsed.single() as LyricItem.WordsLyric
        assertEquals("v1", line.agent)
        assertEquals(listOf("v1"), line.agents.map { it.id })
        assertEquals("person", line.agents.single().type)
        assertEquals(listOf("Hello ", "world"), line.words.map { it.content })
        assertEquals(listOf("Halo", "waludo"), line.words.map { it.phonetic })
        assertEquals("你好世界", line.translation.single().content)
        assertEquals("Halo waludo", line.phonetic)
        assertEquals(1, line.accompaniment.size)
        assertEquals("Ooh", line.accompaniment.single().words.single().content)
        assertEquals("哦", line.accompaniment.single().translation.single().content)
    }

    @Test
    fun parsesMultipleAgentsAndExtendsMainLineForLateAccompaniment() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <head><metadata>
                    <ttm:agent type="person" xml:id="v1" />
                    <ttm:agent type="person" xml:id="v2" />
                </metadata></head>
                <body><div>
                    <p begin="00:01.000" end="00:02.000" ttm:agent="v1 v2">
                        <span begin="00:01.000" end="00:01.500">Main</span>
                        <span ttm:role="x-bg" begin="00:02.500" end="00:03.000" ttm:agent="v2">
                            <span begin="00:02.500" end="00:03.000">Echo</span>
                        </span>
                        <span ttm:role="x-bg" begin="00:03.100" end="00:03.400">(oh)</span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val line = AmllParser.parse(content).single() as LyricItem.WordsLyric

        assertEquals(listOf("v1", "v2"), line.agents.map { it.id })
        assertEquals(listOf("v2"), line.accompaniment[0].agents.map { it.id })
        assertEquals("(oh)", line.accompaniment[1].words.single().content)
        assertEquals(3_400L, line.endTime)
    }

    @Test
    fun parsesPlainTimedTtmlLine() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"><body><div>
                <p begin="00:04.000" end="00:05.000">Plain line</p>
            </div></body></tt>
        """.trimIndent()

        val parsed = AmllParser.parse(content)

        assertTrue(parsed.single() is LyricItem.NormalLyric)
        assertEquals("Plain line", (parsed.single() as LyricItem.NormalLyric).content)
    }

    @Test
    fun ignoresNonTtmlContent() {
        assertTrue(AmllParser.parse("[00:01.00]hello").isEmpty())
    }
}
