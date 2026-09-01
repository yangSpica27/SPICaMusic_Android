package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import me.spica27.spicamusic.common.entity.getSentenceContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        assertEquals(listOf("你好世界", "Metadata translation"), line.translation.map { it.content })
        assertEquals("Halo waludo", line.phonetic)
        assertEquals(1, line.accompaniment.size)
        assertEquals("Ooh", line.accompaniment.single().words.single().content)
        assertEquals(
            listOf("哦", "Background translation"),
            line.accompaniment.single().translation.map { it.content },
        )
    }

    @Test
    fun parsesMultipleAgentsAndExtendsMainLineForLateAccompaniment() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
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
        assertEquals("oh", line.accompaniment[1].words.single().content)
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

        val line = parsed.single() as LyricItem.NormalLyric
        assertEquals("Plain line", line.content)
        assertEquals(4_000L, line.time)
    }

    @Test
    fun inheritsParentTimingForDurationOnlyLineAndBackgroundAgent() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
                <head><metadata><ttm:agent xml:id="v1" type="person" /></metadata></head>
                <body begin="00:10.000"><div dur="2.000" ttm:agent="v1">
                    <p dur="1.000" itunes:key="L1">Plain <span ttm:role="x-bg"><span dur="0.250">(echo)</span></span></p>
                </div></body>
            </tt>
        """.trimIndent()

        val line = AmllParser.parseDetailed(content).items.single() as LyricItem.WordsLyric

        assertEquals(10_000L, line.startTime)
        assertEquals(11_000L, line.endTime)
        assertTrue(line.words.isEmpty())
        assertEquals("Plain", line.content)
        assertEquals("echo", line.accompaniment.single().words.single().content)
        assertEquals(10_250L, line.accompaniment.single().words.single().endTime)
        assertEquals(listOf("v1"), line.accompaniment.single().agents.map { it.id })
    }

    @Test
    fun mergesInlineAndSidecarTranslations() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal">
                <head><metadata><iTunesMetadata>
                    <translations><translation xml:lang="en"><text for="L1">sidecar</text></translation></translations>
                </iTunesMetadata></metadata></head>
                <body><div><p begin="1.0" end="2.0" itunes:key="L1">
                    <span begin="1.0" end="2.0">主句</span>
                    <span ttm:role="x-translation" xml:lang="en">inline</span>
                </p></div></body>
            </tt>
        """.trimIndent()

        val line = AmllParser.parse(content).single() as LyricItem.WordsLyric

        assertEquals(listOf("inline", "sidecar"), line.translation.map { it.content })
        assertEquals(listOf("inline", "sidecar"), line.translationVariants.map { it.content })
    }

    @Test
    fun preservesUntimedBackgroundTextUsingLineRange() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata">
                <body><div><p begin="2.0" end="3.0">
                    <span ttm:role="x-bg">(ah)</span>
                </p></div></body>
            </tt>
        """.trimIndent()

        val line = AmllParser.parse(content).single() as LyricItem.WordsLyric

        assertEquals(1, line.accompaniment.size)
        assertEquals("ah", line.accompaniment.single().words.single().content)
        assertEquals(2_000L, line.accompaniment.single().startTime)
        assertEquals(3_000L, line.accompaniment.single().endTime)
    }

    @Test
    fun retainsFullTextWhenTimedAndUntimedNodesAreMixed() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml">
                <body><div><p begin="1.0" end="2.0">前<span begin="1.0" end="1.5">中</span>后</p></div></body>
            </tt>
        """.trimIndent()

        val line = AmllParser.parse(content).single() as LyricItem.WordsLyric

        assertEquals("前中后", line.content)
        assertEquals("前中后", line.getSentenceContent())
        assertEquals(listOf("中"), line.words.map { it.content })
    }

    @Test
    fun ignoresNonTtmlContent() {
        assertTrue(AmllParser.parse("[00:01.00]hello").isEmpty())
    }

    @Test
    fun parsesAmllTimeExtensionsSidecarsRubyMetadataAndAttributes() {
        val content = """
            <tt xmlns="http://www.w3.org/ns/ttml"
                xmlns:ttm="http://www.w3.org/ns/ttml#metadata"
                xmlns:itunes="http://music.apple.com/lyric-ttml-internal"
                xmlns:amll="http://www.example.com/ns/amll"
                xmlns:tts="http://www.w3.org/ns/ttml#styling"
                xml:lang="ja" itunes:timing="Word">
                <head><metadata>
                    <ttm:title>Extended song</ttm:title>
                    <ttm:agent type="person" xml:id="v1"><ttm:name type="full">Singer A</ttm:name></ttm:agent>
                    <amll:meta key="isrc" value="TEST123" />
                    <amll:meta key="ttmlAuthorGithub" value="amll-dev" />
                    <amll:meta key="ttmlAuthorGithubLogin" value="amll" />
                    <iTunesMetadata>
                        <translations><translation type="subtitle" xml:lang="zh-Hans">
                            <text for="L1"><span begin="1.0" end="1.5">翻</span><span begin="1.5" end="2.0">译</span></text>
                        </translation></translations>
                    </iTunesMetadata>
                </metadata></head>
                <body><div itunes:songPart="Verse">
                    <p begin="1.0" dur="1.0" itunes:key="L1" ttm:agent="v1">
                        <span begin="1.0" dur="0.5" amll:obscene="true" amll:empty-beat="2">歌</span>
                        <span tts:ruby="container">
                            <span tts:ruby="base">詞</span>
                            <span tts:ruby="textContainer"><span tts:ruby="text" begin="1.5" end="1.8">し</span></span>
                        </span>
                    </p>
                </div></body>
            </tt>
        """.trimIndent()

        val result = AmllParser.parseDetailed(content)
        assertFalse(result.error ?: "", result.error != null)
        assertEquals("ja", result.metadata.language)
        assertEquals("Word", result.metadata.timingMode)
        assertEquals(listOf("Extended song"), result.metadata.title)
        assertEquals(listOf("TEST123"), result.metadata.isrc)
        assertEquals(listOf("amll-dev"), result.metadata.authorIds)
        assertEquals(listOf("amll"), result.metadata.authorNames)
        assertEquals("Singer A", result.metadata.agents.single().name)

        val line = result.items.single() as LyricItem.WordsLyric
        assertEquals("Verse", line.songPart)
        assertEquals(1, line.blockIndex)
        assertEquals(2_000L, line.endTime)
        assertEquals(listOf("歌", "詞"), line.words.map { it.content })
        assertTrue(line.words.first().obscene)
        assertEquals(2, line.words.first().emptyBeat)
        assertEquals("し", line.words[1].ruby.single().text)
        assertEquals(1_800L, line.words[1].endTime)
        assertEquals("翻译", line.translation.single().content)
        assertEquals("zh-Hans", line.translation.single().lang)
        assertEquals(listOf(1_000L, 1_500L), line.translation.single().words.map { it.startTime })
        assertNotNull(line.translationVariants.singleOrNull())
    }
}
