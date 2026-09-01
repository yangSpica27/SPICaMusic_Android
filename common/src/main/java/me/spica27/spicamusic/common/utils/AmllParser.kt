package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import kotlin.math.roundToLong
import javax.xml.parsers.DocumentBuilderFactory

/** Parser for AMLL/Apple-style TTML lyrics. */
object AmllParser {
    private const val TTML_NAMESPACE = "http://www.w3.org/ns/ttml"
    private const val TTM_NAMESPACE = "http://www.w3.org/ns/ttml#metadata"
    private const val ITUNES_NAMESPACE = "http://music.apple.com/lyric-ttml-internal"
    private const val AMLL_NAMESPACE = "http://www.example.com/ns/amll"
    private const val XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace"
    private const val TTS_NAMESPACE = "http://www.w3.org/ns/ttml#styling"

    data class Metadata(
        val language: String? = null,
        val timingMode: String? = null,
        val title: List<String> = emptyList(),
        val artists: List<String> = emptyList(),
        val album: List<String> = emptyList(),
        val isrc: List<String> = emptyList(),
        val songwriters: List<String> = emptyList(),
        val agents: List<LyricItem.Agent> = emptyList(),
        val platformIds: Map<String, List<String>> = emptyMap(),
        val rawProperties: Map<String, List<String>> = emptyMap(),
        val authorIds: List<String> = emptyList(),
        val authorNames: List<String> = emptyList(),
    )

    data class ParseResult(
        val items: List<LyricItem>,
        val metadata: Metadata = Metadata(),
        val warnings: List<String> = emptyList(),
        val error: String? = null,
    )

    private data class SidecarEntry(
        val translations: List<LyricItem.WordsLyric.Translation> = emptyList(),
        val transliterations: List<LyricItem.WordsLyric.Translation> = emptyList(),
        val backgroundTranslations: List<LyricItem.WordsLyric.Translation> = emptyList(),
        val backgroundTransliterations: List<LyricItem.WordsLyric.Translation> = emptyList(),
    )

    private data class ParsedContent(
        val text: String,
        val words: List<LyricItem.WordsLyric.WordWithTiming>,
        val translations: List<LyricItem.WordsLyric.Translation>,
        val transliterations: List<LyricItem.WordsLyric.Translation>,
        val backgrounds: List<ParsedContent>,
        val segments: List<String>,
        val declaredStart: Long? = null,
        val declaredEnd: Long? = null,
        val agent: String? = null,
    )

    private data class TimeRange(val start: Long?, val end: Long?)

    private class ContentBuilder {
        val fullText = StringBuilder()
        val words = mutableListOf<LyricItem.WordsLyric.WordWithTiming>()
        val translations = mutableListOf<LyricItem.WordsLyric.Translation>()
        val transliterations = mutableListOf<LyricItem.WordsLyric.Translation>()
        val backgrounds = mutableListOf<ParsedContent>()
        val segments = mutableListOf<String>()

        fun appendText(raw: String?) {
            if (raw.isNullOrEmpty()) return
            val formatting = raw.contains('\n') || raw.contains('\r')
            if (formatting && raw.trim().isEmpty()) return
            val normalized = normalizeWhitespace(raw, trim = false)
            val value = if (formatting) normalized.trim() else normalized
            if (value.isEmpty()) return
            fullText.append(value)
            if (!formatting && value.all { it.isWhitespace() }) markPreviousSpace()
        }

        fun addTimedWord(
            raw: String?,
            start: Long,
            end: Long,
            obscene: Boolean = false,
            emptyBeat: Int? = null,
            ruby: List<LyricItem.WordsLyric.RubyAnnotation> = emptyList(),
        ) {
            if (raw.isNullOrEmpty()) return
            val formatting = raw.contains('\n') || raw.contains('\r')
            if (formatting && raw.trim().isEmpty()) return
            val normalized = normalizeWhitespace(raw, trim = false)
            val value = if (formatting) normalized.trim() else normalized
            fullText.append(value)

            val startsWithSpace = !formatting && value.firstOrNull()?.isWhitespace() == true
            val endsWithSpace = !formatting && value.lastOrNull()?.isWhitespace() == true
            if (startsWithSpace) markPreviousSpace()

            val clean = value.trim()
            if (clean.isEmpty()) {
                if (endsWithSpace) markPreviousSpace()
                return
            }

            words += LyricItem.WordsLyric.WordWithTiming(
                content = if (endsWithSpace) "$clean " else clean,
                startTime = start,
                endTime = maxOf(start, end),
                endsWithSpace = endsWithSpace,
                obscene = obscene,
                emptyBeat = emptyBeat,
                ruby = ruby,
            )
        }

        fun addSegment(raw: String?) {
            val value = normalizeWhitespace(raw, trim = true)
            if (value.isNotEmpty()) segments += value
        }

        private fun markPreviousSpace() {
            val index = words.lastIndex
            if (index < 0) return
            val previous = words[index]
            words[index] = when {
                previous.content.endsWith(' ') && previous.endsWithSpace -> previous
                previous.content.endsWith(' ') -> previous.copy(endsWithSpace = true)
                else -> previous.copy(content = "${previous.content} ", endsWithSpace = true)
            }
        }
    }

    fun canParse(content: String): Boolean =
        content.contains(TTML_NAMESPACE) ||
            Regex("<tt(?:\\s|>)", RegexOption.IGNORE_CASE).containsMatchIn(content) &&
            content.contains("ttm:role", ignoreCase = true)

    /** Compatibility API used by the existing lyric pipeline. */
    fun parse(content: String): List<LyricItem> = parseDetailed(content).items

    /** Parses AMLL while retaining metadata and diagnostics for future UI consumers. */
    fun parseDetailed(content: String): ParseResult {
        if (content.isBlank() || !canParse(content)) return ParseResult(emptyList())

        return try {
            val document = newDocumentBuilder().parse(InputSource(StringReader(content)))
            val root = document.documentElement
                ?: return ParseResult(emptyList(), error = "Missing XML root element")
            val metadata = parseMetadata(root)
            val sidecars = parseSidecars(root)
            val agents = metadata.agents.associateBy { it.id }
            val lines = parseBody(root, sidecars, agents)
                .mapIndexed { index, item ->
                    when (item) {
                        is LyricItem.NormalLyric -> item.copy(key = "$index:${item.key}")
                        is LyricItem.WordsLyric -> item.copy(key = "$index:${item.key}")
                    }
                }
            val resolvedMetadata = metadata.copy(
                timingMode = metadata.timingMode ?: inferTimingMode(lines),
            )
            ParseResult(lines, resolvedMetadata)
        } catch (error: Exception) {
            ParseResult(emptyList(), error = error.message ?: error::class.simpleName)
        }
    }

    private fun newDocumentBuilder() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false) }
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
        }.newDocumentBuilder()

    private fun parseMetadata(root: Element): Metadata {
        val titles = descendants(root, "title")
            .mapNotNull { it.textContent?.trim()?.takeIf(String::isNotEmpty) }
        val songwriters = descendants(root, "songwriter")
            .mapNotNull { it.textContent?.trim()?.takeIf(String::isNotEmpty) }
        val agents = parseAgents(root)

        val metaValues = descendants(root, "meta").mapNotNull { element ->
            val key = attribute(element, "amll:key", "key")?.trim().orEmpty()
            val value = attribute(element, "amll:value", "value")?.trim().orEmpty()
            if (key.isBlank() || value.isBlank()) null else key to value
        }
        fun values(key: String): List<String> =
            metaValues.filter { it.first == key }.map { it.second }.distinct()

        val platformIds = linkedMapOf<String, MutableList<String>>()
        val rawProperties = linkedMapOf<String, MutableList<String>>()
        metaValues.forEach { (key, value) ->
            when (key) {
                "ncmMusicId", "qqMusicId", "spotifyId", "appleMusicId" ->
                    platformIds.getOrPut(key) { mutableListOf() } += value
                "musicName", "artists", "album", "isrc",
                "ttmlAuthorGithub", "ttmlAuthorGithubLogin" -> Unit
                else -> rawProperties.getOrPut(key) { mutableListOf() } += value
            }
        }

        return Metadata(
            language = attribute(root, "xml:lang", "lang"),
            timingMode = attribute(root, "itunes:timing", "timing"),
            title = (titles + values("musicName")).distinct(),
            artists = values("artists"),
            album = values("album"),
            isrc = values("isrc"),
            songwriters = songwriters.distinct(),
            authorIds = values("ttmlAuthorGithub"),
            authorNames = values("ttmlAuthorGithubLogin"),
            agents = agents,
            platformIds = platformIds.mapValues { it.value.distinct() },
            rawProperties = rawProperties.mapValues { it.value.distinct() },
        )
    }

    private fun inferTimingMode(items: List<LyricItem>): String? {
        if (items.isEmpty()) return null
        val hasWordTiming = items.any { item ->
            item is LyricItem.WordsLyric &&
                (item.words.size > 1 || item.accompaniment.any { it.words.size > 1 })
        }
        return if (hasWordTiming) "Word" else "Line"
    }

    private fun parseAgents(root: Element): List<LyricItem.Agent> =
        descendants(root, "agent").mapIndexedNotNull { index, element ->
            val id = attribute(element, "xml:id", "id")?.trim().orEmpty()
            if (id.isBlank()) return@mapIndexedNotNull null
            val name =
                attribute(element, "name", "label")?.trim()?.takeIf(String::isNotEmpty)
                    ?: descendants(element, "name")
                        .firstOrNull()
                        ?.textContent
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
            LyricItem.Agent(
                id = id,
                type = attribute(element, "ttm:type", "type")?.trim()?.takeIf(String::isNotEmpty),
                name = name,
                order = index,
            )
        }

    private fun parseSidecars(root: Element): Map<String, SidecarEntry> {
        val translations = linkedMapOf<String, MutableList<LyricItem.WordsLyric.Translation>>()
        val transliterations = linkedMapOf<String, MutableList<LyricItem.WordsLyric.Translation>>()
        val backgroundTranslations = linkedMapOf<String, MutableList<LyricItem.WordsLyric.Translation>>()
        val backgroundTransliterations = linkedMapOf<String, MutableList<LyricItem.WordsLyric.Translation>>()

        descendants(root, "translation").forEach { element ->
            val lang = attribute(element, "xml:lang", "lang") ?: "unknown"
            val type = attribute(element, "type")
            descendants(element, "text").forEach { textElement ->
                val key = attribute(textElement, "for")?.trim().orEmpty()
                if (key.isBlank()) return@forEach
                val parsed = parseContent(textElement)
                if (parsed.text.isNotBlank() || parsed.words.isNotEmpty()) {
                    translations.getOrPut(key) { mutableListOf() } +=
                        LyricItem.WordsLyric.Translation(
                            content = parsed.text,
                            lang = lang,
                            words = parsed.words,
                            type = type,
                            segments = parsed.segments,
                        )
                }
                parsed.backgrounds.forEach { background ->
                    if (background.text.isNotBlank() || background.words.isNotEmpty()) {
                        backgroundTranslations.getOrPut(key) { mutableListOf() } +=
                            LyricItem.WordsLyric.Translation(
                                content = background.text,
                                lang = lang,
                                words = background.words,
                                type = type,
                                isBackground = true,
                                segments = background.segments,
                            )
                    }
                }
            }
        }

        descendants(root, "transliteration").forEach { element ->
            val lang = attribute(element, "xml:lang", "lang") ?: "unknown"
            val type = attribute(element, "type")
            descendants(element, "text").forEach { textElement ->
                val key = attribute(textElement, "for")?.trim().orEmpty()
                if (key.isBlank()) return@forEach
                val parsed = parseContent(textElement)
                if (parsed.text.isNotBlank() || parsed.words.isNotEmpty()) {
                    transliterations.getOrPut(key) { mutableListOf() } +=
                        LyricItem.WordsLyric.Translation(
                            content = parsed.text,
                            lang = lang,
                            words = parsed.words,
                            type = type,
                            segments = parsed.segments,
                        )
                }
                parsed.backgrounds.forEach { background ->
                    if (background.text.isNotBlank() || background.words.isNotEmpty()) {
                        backgroundTransliterations.getOrPut(key) { mutableListOf() } +=
                            LyricItem.WordsLyric.Translation(
                                content = background.text,
                                lang = lang,
                                words = background.words,
                                type = type,
                                isBackground = true,
                                segments = background.segments,
                            )
                    }
                }
            }
        }

        val keys = (translations.keys + transliterations.keys + backgroundTranslations.keys + backgroundTransliterations.keys)
            .toSet()
        return keys.associateWith { key ->
            SidecarEntry(
                translations = translations[key].orEmpty(),
                transliterations = transliterations[key].orEmpty(),
                backgroundTranslations = backgroundTranslations[key].orEmpty(),
                backgroundTransliterations = backgroundTransliterations[key].orEmpty(),
            )
        }
    }

    private fun parseBody(
        root: Element,
        sidecars: Map<String, SidecarEntry>,
        agents: Map<String, LyricItem.Agent>,
    ): List<LyricItem> {
        val body = descendants(root, "body").firstOrNull() ?: return emptyList()
        val result = mutableListOf<LyricItem>()
        var blockIndex = 0

        fun visit(
            element: Element,
            inheritedPart: String?,
            inheritedAgent: String?,
            block: Int?,
            inheritedStart: Long?,
            inheritedEnd: Long?,
        ) {
            when (element.name()) {
                "div" -> {
                    blockIndex += 1
                    val part = attribute(element, "itunes:song-part", "itunes:songPart", "song-part", "songPart")
                        ?: inheritedPart
                    val agent = attribute(element, "ttm:agent", "agent") ?: inheritedAgent
                    val range = elementTimeRange(element, inheritedStart, inheritedEnd)
                    directElements(element).forEach { child ->
                        visit(child, part, agent, blockIndex, range.start ?: inheritedStart, range.end ?: inheritedEnd)
                    }
                }
                "p" -> {
                    val lineBlock = block ?: run {
                        blockIndex += 1
                        blockIndex
                    }
                    parseLine(
                        element,
                        inheritedPart,
                        inheritedAgent,
                        lineBlock,
                        inheritedStart,
                        inheritedEnd,
                        sidecars,
                        agents,
                    )?.let(result::add)
                }
                else -> {
                    val range = elementTimeRange(element, inheritedStart, inheritedEnd)
                    directElements(element).forEach { child ->
                        visit(child, inheritedPart, inheritedAgent, block, range.start ?: inheritedStart, range.end ?: inheritedEnd)
                    }
                }
            }
        }

        val bodyRange = elementTimeRange(body)
        directElements(body).forEach {
            visit(it, null, null, null, bodyRange.start, bodyRange.end)
        }
        return result
    }

    private fun parseLine(
        element: Element,
        songPart: String?,
        inheritedAgent: String?,
        blockIndex: Int?,
        inheritedStart: Long?,
        inheritedEnd: Long?,
        sidecars: Map<String, SidecarEntry>,
        agents: Map<String, LyricItem.Agent>,
    ): LyricItem? {
        val key = attribute(element, "itunes:key", "key").orEmpty()
        val agentValue = attribute(element, "ttm:agent", "agent") ?: inheritedAgent.orEmpty()
        val lineAgents = resolveAgents(agentValue, agents)
        val lineRange = elementTimeRange(element, inheritedStart, inheritedEnd)
        val parsed = parseContent(element, lineRange.start ?: inheritedStart, lineRange.end ?: inheritedEnd, agentValue)
        val sidecar = sidecars[key]

        val inlineTranslations = parsed.translations
        val sidecarTranslations = sidecar?.translations.orEmpty()
        val allTranslations = inlineTranslations + sidecarTranslations

        val inlineRomanizations = parsed.transliterations
        val sidecarRomanizations = sidecar?.transliterations.orEmpty()
        val allRomanizations = inlineRomanizations + sidecarRomanizations

        val words = applyRomanizations(parsed.words, allRomanizations)
        val accompaniment = parsed.backgrounds.mapNotNull { background ->
            parseAccompaniment(
                background,
                key,
                agentValue,
                sidecar,
                agents,
                fallbackStart = lineRange.start ?: words.firstOrNull()?.startTime,
                fallbackEnd = lineRange.end ?: words.lastOrNull()?.endTime,
            )
        }

        val childStart = listOfNotNull(
            words.firstOrNull()?.startTime,
            accompaniment.minOfOrNull { it.startTime },
        ).minOrNull()
        val start = resolveStart(lineRange.start, childStart) ?: return null
        val end = listOfNotNull(
            lineRange.end,
            words.lastOrNull()?.endTime,
            accompaniment.maxOfOrNull { it.endTime },
        ).maxOrNull() ?: start

        val primaryTranslation = allTranslations.map { it.copy(isBackground = false) }
        val primaryRomanization = inlineRomanizations.ifEmpty { sidecarRomanizations }
        val phonetic = primaryRomanization.firstOrNull()?.content

        if (words.isEmpty()) {
            val plainContent = parsed.text.trim()
            if (plainContent.isBlank() && accompaniment.isEmpty()) return null

            if (accompaniment.isEmpty()) {
                return LyricItem.NormalLyric(
                    content = plainContent,
                    translation = primaryTranslation.firstOrNull()?.content,
                    time = start,
                    key = key.ifBlank { start.toString() },
                    phonetic = phonetic,
                    agent = agentValue,
                    agents = lineAgents,
                    translationVariants = allTranslations,
                    transliterations = allRomanizations,
                    songPart = songPart,
                    blockIndex = blockIndex,
                )
            }

            return LyricItem.WordsLyric(
                agent = agentValue,
                words = emptyList(),
                translation = primaryTranslation,
                startTime = start,
                endTime = maxOf(start, end),
                key = key.ifBlank { start.toString() },
                phonetic = phonetic,
                accompaniment = accompaniment,
                agents = lineAgents,
                transliterations = allRomanizations,
                translationVariants = allTranslations,
                songPart = songPart,
                blockIndex = blockIndex,
                content = plainContent,
            )
        }

        return LyricItem.WordsLyric(
            agent = agentValue,
            words = words,
            translation = primaryTranslation,
            startTime = start,
            endTime = maxOf(start, end),
            key = key.ifBlank { start.toString() },
            phonetic = phonetic,
            accompaniment = accompaniment,
            agents = lineAgents,
            transliterations = allRomanizations,
            translationVariants = allTranslations,
            songPart = songPart,
            blockIndex = blockIndex,
            content = parsed.text.trim().takeIf { it.isNotBlank() },
        )
    }

    private fun parseAccompaniment(
        background: ParsedContent,
        parentKey: String,
        parentAgent: String,
        sidecar: SidecarEntry?,
        agents: Map<String, LyricItem.Agent>,
        fallbackStart: Long?,
        fallbackEnd: Long?,
    ): LyricItem.WordsLyric.AccompanimentLyric? {
        val words = background.words.ifEmpty {
            val start = background.declaredStart ?: fallbackStart
            val end = background.declaredEnd ?: fallbackEnd ?: start
            if (background.text.isBlank() || start == null || end == null) emptyList()
            else listOf(
                LyricItem.WordsLyric.WordWithTiming(
                    content = background.text,
                    startTime = start,
                    endTime = maxOf(start, end),
                ),
            )
        }
        if (words.isEmpty()) return null

        val inlineTranslations = background.translations
        val sidecarTranslations = sidecar?.backgroundTranslations.orEmpty()
        val allTranslations = (inlineTranslations + sidecarTranslations)
            .map { it.copy(isBackground = true) }
        val inlineRomanizations = background.transliterations
        val sidecarRomanizations = sidecar?.backgroundTransliterations.orEmpty()
        val allRomanizations = inlineRomanizations + sidecarRomanizations

        val start = resolveStart(background.declaredStart, words.first().startTime) ?: words.first().startTime
        val end = maxOf(background.declaredEnd ?: words.last().endTime, words.last().endTime)
        return LyricItem.WordsLyric.AccompanimentLyric(
            agent = background.agent ?: parentAgent,
            words = words,
            translation = allTranslations,
            startTime = start,
            endTime = maxOf(start, end),
            phonetic = allRomanizations.firstOrNull()?.content,
            agents = resolveAgents(background.agent ?: parentAgent, agents),
            transliterations = allRomanizations,
            translationVariants = allTranslations,
            key = parentKey.ifBlank { null },
            content = background.text.trim().takeIf { it.isNotBlank() },
        )
    }

    private fun parseContent(
        element: Element,
        inheritedStart: Long? = null,
        inheritedEnd: Long? = null,
        inheritedAgent: String? = null,
    ): ParsedContent {
        val range = elementTimeRange(element, inheritedStart, inheritedEnd)
        val builder = ContentBuilder()
        walkChildren(
            element,
            builder,
            range.start ?: inheritedStart,
            range.end ?: inheritedEnd,
            attribute(element, "ttm:agent", "agent") ?: inheritedAgent,
        )
        val declaredRange = declaredTimeRange(element, inheritedStart)
        return ParsedContent(
            text = builder.fullText.toString(),
            words = builder.words.toList(),
            translations = builder.translations.toList(),
            transliterations = builder.transliterations.toList(),
            backgrounds = builder.backgrounds.toList(),
            segments = builder.segments.toList(),
            declaredStart = declaredRange.start,
            declaredEnd = declaredRange.end,
            agent = attribute(element, "ttm:agent", "agent") ?: inheritedAgent,
        )
    }

    private fun walkChildren(
        parent: Element,
        builder: ContentBuilder,
        inheritedStart: Long?,
        inheritedEnd: Long?,
        inheritedAgent: String?,
    ) {
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child is Element) walkElement(child, builder, inheritedStart, inheritedEnd, inheritedAgent)
            else if (child.nodeType == Node.TEXT_NODE) builder.appendText(child.nodeValue)
        }
    }

    private fun walkElement(
        element: Element,
        builder: ContentBuilder,
        inheritedStart: Long?,
        inheritedEnd: Long?,
        inheritedAgent: String?,
    ) {
        when (role(element)) {
            "x-bg" -> builder.backgrounds += parseContent(
                element,
                inheritedStart,
                inheritedEnd,
                inheritedAgent,
            ).cleanBackground()
            "x-translation" -> {
                val parsed = parseContent(element, inheritedStart, inheritedEnd, inheritedAgent)
                if (parsed.text.isNotBlank() || parsed.words.isNotEmpty()) {
                    builder.translations += LyricItem.WordsLyric.Translation(
                        content = parsed.text.trim(),
                        lang = attribute(element, "xml:lang", "lang") ?: "unknown",
                        words = parsed.words,
                        type = attribute(element, "type"),
                        segments = parsed.segments,
                    )
                }
            }
            "x-roman" -> {
                val parsed = parseContent(element, inheritedStart, inheritedEnd, inheritedAgent)
                if (parsed.text.isNotBlank() || parsed.words.isNotEmpty()) {
                    builder.transliterations += LyricItem.WordsLyric.Translation(
                        content = parsed.text.trim(),
                        lang = attribute(element, "xml:lang", "lang") ?: "unknown",
                        words = parsed.words,
                        type = attribute(element, "type"),
                        segments = parsed.segments,
                    )
                }
            }
            else -> {
                if (attribute(element, "tts:ruby", "ruby") == "container") {
                    parseRuby(element, builder, inheritedStart, inheritedEnd)
                    return
                }

                val range = elementTimeRange(element, inheritedStart, inheritedEnd)
                val childElements = directElements(element)
                if (childElements.isEmpty()) {
                    if (range.start != null && range.end != null) {
                        builder.addTimedWord(
                            raw = element.textContent,
                            start = range.start,
                            end = range.end,
                            obscene = isTrue(attribute(element, "amll:obscene", "obscene")),
                            emptyBeat = attribute(element, "amll:empty-beat", "empty-beat")?.toIntOrNull(),
                        )
                    } else {
                        builder.addSegment(element.textContent)
                        builder.appendText(element.textContent)
                    }
                } else {
                    val beforeWords = builder.words.size
                    walkChildren(
                        element,
                        builder,
                        range.start ?: inheritedStart,
                        range.end ?: inheritedEnd,
                        attribute(element, "ttm:agent", "agent") ?: inheritedAgent,
                    )
                    if (
                        builder.words.size == beforeWords &&
                        range.start != null &&
                        range.end != null &&
                        element.textContent.orEmpty().isNotBlank()
                    ) {
                        builder.addTimedWord(
                            raw = element.textContent,
                            start = range.start,
                            end = range.end,
                            obscene = isTrue(attribute(element, "amll:obscene", "obscene")),
                            emptyBeat = attribute(element, "amll:empty-beat", "empty-beat")?.toIntOrNull(),
                        )
                    }
                }
            }
        }
    }

    private fun parseRuby(
        element: Element,
        builder: ContentBuilder,
        inheritedStart: Long?,
        inheritedEnd: Long?,
    ) {
        val base = directElements(element)
            .firstOrNull { attribute(it, "tts:ruby", "ruby") == "base" }
        val baseText = base?.textContent.orEmpty()
        if (baseText.isBlank()) return

        val rubyAnnotations = directElements(element)
            .firstOrNull { attribute(it, "tts:ruby", "ruby") == "textContainer" }
            ?.let { container ->
                descendants(container, "span").mapNotNull { rubyText ->
                    if (attribute(rubyText, "tts:ruby", "ruby") != "text") return@mapNotNull null
                    val range = elementTimeRange(rubyText, inheritedStart, inheritedEnd)
                    val start = range.start
                    val end = range.end
                    if (start == null || end == null || rubyText.textContent.isNullOrBlank()) return@mapNotNull null
                    LyricItem.WordsLyric.RubyAnnotation(
                        text = normalizeWhitespace(rubyText.textContent, trim = true),
                        startTime = start,
                        endTime = maxOf(start, end),
                    )
                }
            }
            .orEmpty()

        val declaredRange = declaredTimeRange(element, inheritedStart)
        val rubyStart = rubyAnnotations.minOfOrNull { it.startTime }
        val rubyEnd = rubyAnnotations.maxOfOrNull { it.endTime }
        val start = resolveStart(declaredRange.start, rubyStart)
        val end = listOfNotNull(declaredRange.end, rubyEnd)
            .maxOrNull()
            ?: inheritedEnd
        if (start != null && end != null) {
            builder.addTimedWord(
                raw = baseText,
                start = start,
                end = end,
                obscene = isTrue(attribute(element, "amll:obscene", "obscene")),
                emptyBeat = attribute(element, "amll:empty-beat", "empty-beat")?.toIntOrNull(),
                ruby = rubyAnnotations,
            )
        } else {
            builder.appendText(baseText)
        }
    }

    private fun ParsedContent.cleanBackground(): ParsedContent {
        val cleanOuter = { value: String ->
            value
                .replace(Regex("^\\s*[\\(（]+"), "")
                .replace(Regex("[\\)）]+\\s*$"), "")
        }
        val cleanedWords = words.mapIndexedNotNull { index, word ->
            var content = word.content
            if (index == 0) content = content.replace(Regex("^\\s*[\\(（]+"), "")
            if (index == words.lastIndex) content = content.replace(Regex("[\\)）]+\\s*$"), "")
            val hadTrailingSpace = content.lastOrNull()?.isWhitespace() == true || word.endsWithSpace
            val normalized = content.trim()
            if (normalized.isBlank()) return@mapIndexedNotNull null
            word.copy(
                content = normalized + if (hadTrailingSpace && index != words.lastIndex) " " else "",
                endsWithSpace = hadTrailingSpace && index != words.lastIndex,
            )
        }
        return copy(
            text = cleanOuter(text).trim(),
            words = cleanedWords,
        )
    }

    private fun applyRomanizations(
        words: List<LyricItem.WordsLyric.WordWithTiming>,
        romanizations: List<LyricItem.WordsLyric.Translation>,
    ): List<LyricItem.WordsLyric.WordWithTiming> {
        val source = romanizations.firstOrNull { it.words.size == words.size && it.words.isNotEmpty() }
            ?: romanizations.firstOrNull { it.segments.size == words.size }
            ?: return words
        val phonetics = if (source.words.size == words.size) source.words.map { it.content.trim() } else source.segments
        return words.mapIndexed { index, word ->
            word.copy(phonetic = phonetics.getOrNull(index)?.takeIf(String::isNotBlank))
        }
    }

    private fun resolveAgents(value: String, definitions: Map<String, LyricItem.Agent>): List<LyricItem.Agent> =
        value.trim()
            .split(Regex("[\\s,]+"))
            .filter(String::isNotBlank)
            .distinct()
            .mapIndexed { index, id -> definitions[id] ?: LyricItem.Agent(id = id, order = definitions.size + index) }

    private fun resolveStart(declared: Long?, child: Long?): Long? = when {
        declared == null -> child
        child == null -> declared
        declared == 0L -> child
        child < declared -> child
        else -> declared
    }

    private fun elementTimeRange(element: Element, inheritedStart: Long? = null): TimeRange {
        return elementTimeRange(element, inheritedStart, null)
    }

    private fun declaredTimeRange(element: Element, inheritedStart: Long? = null): TimeRange {
        val explicitStart = attribute(element, "begin")?.let(::parseTime)
        val duration = attribute(element, "dur")?.let(::parseTime)
        val explicitEnd = attribute(element, "end")?.let(::parseTime)
        val start = explicitStart
            ?: if (duration != null || explicitEnd != null) inheritedStart else null
        val end = explicitEnd ?: duration?.let { durationValue -> start?.plus(durationValue) }
        return TimeRange(start, end)
    }

    private fun elementTimeRange(
        element: Element,
        inheritedStart: Long?,
        inheritedEnd: Long?,
    ): TimeRange {
        val explicitStart = attribute(element, "begin")?.let(::parseTime)
        val duration = attribute(element, "dur")?.let(::parseTime)
        val explicitEnd = attribute(element, "end")?.let(::parseTime)
        val start = explicitStart
            ?: if (duration != null || explicitEnd != null) inheritedStart else null
        val end = explicitEnd
            ?: duration?.let { durationValue -> start?.plus(durationValue) }
            ?: inheritedEnd
        return TimeRange(start, end)
    }

    private fun parseTime(value: String): Long? {
        val clean = value.trim()
        if (clean.isEmpty()) return null
        val secondsValue = if (clean.endsWith('s', ignoreCase = true)) clean.dropLast(1) else clean

        return if (secondsValue.contains(':')) {
            val parts = secondsValue.split(':')
            if (parts.size !in 2..3) return null
            val seconds = parts.last().toDoubleOrNull() ?: return null
            val minutes = parts[parts.size - 2].toLongOrNull() ?: return null
            val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
            ((hours * 3_600L + minutes * 60L + seconds) * 1_000.0).roundToLong()
        } else {
            val seconds = secondsValue.toDoubleOrNull() ?: return null
            (seconds * 1_000.0).roundToLong()
        }
    }

    private fun role(element: Element): String? = attribute(element, "ttm:role", "role")

    private fun descendants(root: Element, name: String): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(element: Element) {
            if (element.name() == name.lowercase()) result += element
            directElements(element).forEach(::visit)
        }
        visit(root)
        return result
    }

    private fun directElements(element: Element): List<Element> =
        (0 until element.childNodes.length)
            .map { element.childNodes.item(it) }
            .filterIsInstance<Element>()

    private fun Element.name(): String = (localName ?: nodeName.substringAfter(':')).lowercase()

    private fun attribute(element: Element, vararg names: String): String? {
        for (requested in names) {
            val prefix = requested.substringBefore(':', "").lowercase()
            val local = requested.substringAfter(':').lowercase()
            val namespace = when (prefix) {
                "xml" -> XML_NAMESPACE
                "ttm" -> TTM_NAMESPACE
                "itunes" -> ITUNES_NAMESPACE
                "amll" -> AMLL_NAMESPACE
                "tts" -> TTS_NAMESPACE
                else -> null
            }
            if (namespace != null) {
                element.getAttributeNS(namespace, local).takeIf(String::isNotEmpty)?.let { return it }
            }
            element.getAttribute(requested).takeIf(String::isNotEmpty)?.let { return it }
        }

        for (index in 0 until element.attributes.length) {
            val attr = element.attributes.item(index)
            val local = (attr.localName ?: attr.nodeName.substringAfter(':')).lowercase()
            if (names.any { it.substringAfter(':').lowercase() == local }) return attr.nodeValue
        }
        return null
    }

    private fun isTrue(value: String?): Boolean = value.equals("true", ignoreCase = true) || value == "1"

    private fun normalizeWhitespace(value: String?, trim: Boolean): String {
        if (value.isNullOrEmpty()) return ""
        val normalized = value.replace(Regex("\\s+"), " ")
        return if (trim) normalized.trim() else normalized
    }
}
