package me.spica27.spicamusic.common.utils

import me.spica27.spicamusic.common.entity.LyricItem
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parser for AMLL/Apple-style TTML lyrics.
 */
object AmllParser {
    private const val TTML_NAMESPACE = "http://www.w3.org/ns/ttml"

    private data class TranslationData(
        val main: String?,
        val background: String?,
    )

    fun canParse(content: String): Boolean =
        content.contains(TTML_NAMESPACE) ||
            Regex("<tt(?:\\s|>)", RegexOption.IGNORE_CASE).containsMatchIn(content) &&
            content.contains("ttm:role", ignoreCase = true)

    fun parse(content: String): List<LyricItem> {
        if (content.isBlank() || !canParse(content)) return emptyList()

        return runCatching {
            val document = newDocumentBuilder().parse(InputSource(StringReader(content)))
            val root = document.documentElement ?: return@runCatching emptyList()
            val translations = parseTranslations(root)
            val transliterations = parseTransliterations(root)
            val agents = parseAgents(root)

            descendants(root, "p")
                .mapNotNull { parseLine(it, translations, transliterations, agents) }
                .sortedBy { it.time }
                .mapIndexed { index, item ->
                    when (item) {
                        is LyricItem.NormalLyric -> item.copy(key = "$index:${item.key}")
                        is LyricItem.WordsLyric -> item.copy(key = "$index:${item.key}")
                    }
                }
        }.getOrElse { emptyList() }
    }

    private fun newDocumentBuilder() =
        DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            runCatching {
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            }
            runCatching {
                setFeature("http://xml.org/sax/features/external-general-entities", false)
            }
            runCatching {
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            runCatching {
                setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            runCatching { isXIncludeAware = false }
            runCatching { isExpandEntityReferences = false }
        }.newDocumentBuilder()

    private fun parseLine(
        element: Element,
        translations: Map<String, TranslationData>,
        transliterations: Map<String, List<String>>,
        agents: Map<String, LyricItem.Agent>,
    ): LyricItem? {
        val key = attribute(element, "itunes:key", "key").orEmpty()
        val agentValue = attribute(element, "ttm:agent", "agent").orEmpty()
        val lineAgents = resolveAgents(agentValue, agents)
        val lineTranslation = directElements(element)
            .firstOrNull { it.isRole("x-translation") && !it.isRole("x-bg") }
            ?.let { translation(it) }
        val metadataTranslation = translations[key]?.main?.takeIf { it.isNotBlank() }
        val translation = lineTranslation ?: metadataTranslation
        val linePhonetic = directElements(element)
            .firstOrNull { it.isRole("x-roman") }
            ?.textContent
            ?.trim()

        var words = parseSyllables(element)
        transliterations[key]?.let { phonetics ->
            if (phonetics.size == words.size) {
                words = words.mapIndexed { index, word ->
                    word.copy(phonetic = phonetics[index].takeIf { it.isNotBlank() })
                }
            }
        }

        val accompaniment = directElements(element)
            .filter { it.isRole("x-bg") }
            .mapNotNull { parseAccompaniment(it, key, element, translations, agents) }

        val firstWordStart = words.firstOrNull()?.startTime
        val lastWordEnd = words.lastOrNull()?.endTime
        val start = attribute(element, "begin")?.let(::parseTime) ?: firstWordStart ?: return null
        val end = listOfNotNull(
            attribute(element, "end")?.let(::parseTime),
            lastWordEnd,
            accompaniment.maxOfOrNull { it.endTime },
        ).maxOrNull() ?: start

        if (words.isEmpty()) {
            val plainContent = plainLineText(element)
            if (plainContent.isBlank()) return null
            return LyricItem.NormalLyric(
                content = plainContent,
                translation = translation,
                time = start,
                key = key.ifBlank { start.toString() },
                phonetic = linePhonetic,
                agent = agentValue,
                agents = lineAgents,
            )
        }

        return LyricItem.WordsLyric(
            agent = agentValue,
            words = words,
            translation = translation?.let { listOf(LyricItem.WordsLyric.Translation(it, "unknown")) }
                ?: emptyList(),
            startTime = start,
            endTime = maxOf(start, end),
            key = key.ifBlank { start.toString() },
            phonetic = linePhonetic,
            accompaniment = accompaniment,
            agents = lineAgents,
        )
    }

    private fun parseAccompaniment(
        element: Element,
        parentKey: String,
        parent: Element,
        translations: Map<String, TranslationData>,
        agents: Map<String, LyricItem.Agent>,
    ): LyricItem.WordsLyric.AccompanimentLyric? {
        val key = attribute(element, "itunes:key", "key") ?: parentKey
        val inlineTranslation = directElements(element)
            .firstOrNull { it.isRole("x-translation") }
            ?.let { translation(it) }
        val metadata = translations[key]
        val translation = inlineTranslation ?: metadata?.background
        val declaredStart = attribute(element, "begin")?.let(::parseTime)
        val declaredEnd = attribute(element, "end")?.let(::parseTime)
        val words =
            parseSyllables(element).ifEmpty {
                val plainText = ownText(element).normalizeWhitespace()
                if (plainText.isBlank() || declaredStart == null || declaredEnd == null) {
                    emptyList()
                } else {
                    listOf(
                        LyricItem.WordsLyric.WordWithTiming(
                            content = plainText,
                            startTime = declaredStart,
                            endTime = maxOf(declaredStart, declaredEnd),
                        ),
                    )
                }
            }
        if (words.isEmpty()) return null

        val start = declaredStart ?: words.first().startTime
        val end = declaredEnd ?: words.last().endTime
        val agentValue = attribute(element, "ttm:agent", "agent")
            ?: attribute(parent, "ttm:agent", "agent").orEmpty()

        return LyricItem.WordsLyric.AccompanimentLyric(
            agent = agentValue,
            words = words,
            translation = translation?.let {
                listOf(LyricItem.WordsLyric.Translation(it, "unknown"))
            } ?: emptyList(),
            startTime = start,
            endTime = maxOf(start, end),
            phonetic = directElements(element)
                .firstOrNull { it.isRole("x-roman") }
                ?.textContent
                ?.trim(),
            agents = resolveAgents(agentValue, agents),
        )
    }

    private fun parseSyllables(parent: Element): List<LyricItem.WordsLyric.WordWithTiming> {
        val result = mutableListOf<LyricItem.WordsLyric.WordWithTiming>()
        val children = parent.childNodes
        for (index in 0 until children.length) {
            val child = children.item(index)
            if (child !is Element || child.name() != "span") continue
            if (child.isRole("x-bg") || child.isRole("x-translation") || child.isRole("x-roman")) {
                continue
            }

            val start = attribute(child, "begin")?.let(::parseTime) ?: continue
            val end = attribute(child, "end")?.let(::parseTime) ?: continue
            var text = child.textContent.orEmpty()
            val next = children.item(index + 1)
            val separator = next?.takeIf { it.nodeType == Node.TEXT_NODE }?.nodeValue.orEmpty()
            if (separator.isNotEmpty() && '\n' !in separator && '\r' !in separator) text += separator
            if (text.isEmpty()) continue

            result += LyricItem.WordsLyric.WordWithTiming(
                content = text,
                startTime = start,
                endTime = maxOf(start, end),
            )
        }
        return result
    }

    private fun parseTranslations(root: Element): Map<String, TranslationData> {
        val result = mutableMapOf<String, TranslationData>()
        descendants(root, "translation").forEach { translationElement ->
            directElements(translationElement)
                .filter { it.name() == "text" }
                .forEach { textElement ->
                    val key = attribute(textElement, "for") ?: return@forEach
                    val main = ownText(textElement).trim().takeIf { it.isNotEmpty() }
                    val background = directElements(textElement)
                        .firstOrNull { it.isRole("x-bg") }
                        ?.textContent
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    if (main != null || background != null) {
                        result[key] = TranslationData(main, background)
                    }
                }
        }
        return result
    }

    private fun parseAgents(root: Element): Map<String, LyricItem.Agent> {
        val result = linkedMapOf<String, LyricItem.Agent>()
        descendants(root, "agent").forEachIndexed { index, element ->
            val id = attribute(element, "xml:id", "id")?.trim().orEmpty()
            if (id.isBlank()) return@forEachIndexed
            val name =
                attribute(element, "name", "label")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: ownText(element).normalizeWhitespace().takeIf { it.isNotBlank() }
            result[id] =
                LyricItem.Agent(
                    id = id,
                    type = attribute(element, "type")?.trim()?.takeIf { it.isNotBlank() },
                    name = name,
                    order = index,
                )
        }
        return result
    }

    private fun resolveAgents(
        value: String,
        definitions: Map<String, LyricItem.Agent>,
    ): List<LyricItem.Agent> =
        value
            .trim()
            .split(Regex("[\\s,]+"))
            .filter { it.isNotBlank() }
            .distinct()
            .mapIndexed { index, id ->
                definitions[id] ?: LyricItem.Agent(id = id, order = definitions.size + index)
            }

    private fun parseTransliterations(root: Element): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        descendants(root, "transliterations").forEach { container ->
            descendants(container, "transliteration").forEach { transliteration ->
                directElements(transliteration)
                    .filter { it.name() == "text" }
                    .forEach { textElement ->
                        val key = attribute(textElement, "for") ?: return@forEach
                        val spans = descendants(textElement, "span")
                            .filterNot { it.isRole("x-bg") || it.isRole("x-translation") }
                            .map { it.textContent.trim() }
                            .filter { it.isNotEmpty() }
                        if (spans.isNotEmpty()) result[key] = spans
                    }
            }
        }
        return result
    }

    private fun translation(element: Element): String? =
        element.textContent.trim().takeIf { it.isNotEmpty() }

    private fun plainLineText(element: Element): String =
        buildString {
            val children = element.childNodes
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child.nodeType == Node.TEXT_NODE) {
                    append(child.nodeValue.orEmpty())
                } else if (child is Element &&
                    !child.isRole("x-bg") &&
                    !child.isRole("x-translation") &&
                    !child.isRole("x-roman")
                ) {
                    append(child.textContent.orEmpty())
                }
            }
        }.normalizeWhitespace()

    private fun ownText(element: Element): String =
        buildString {
            val children = element.childNodes
            for (index in 0 until children.length) {
                val child = children.item(index)
                if (child.nodeType == Node.TEXT_NODE) append(child.nodeValue.orEmpty())
            }
        }

    private fun String.normalizeWhitespace(): String = replace(Regex("\\s+"), " ").trim()

    private fun descendants(root: Element, name: String): List<Element> {
        val result = mutableListOf<Element>()
        fun visit(element: Element) {
            if (element.name() == name) result += element
            directElements(element).forEach(::visit)
        }
        visit(root)
        return result
    }

    private fun directElements(element: Element): List<Element> =
        (0 until element.childNodes.length)
            .map { element.childNodes.item(it) }
            .filterIsInstance<Element>()

    private fun Element.name(): String =
        (localName ?: nodeName.substringAfter(':')).lowercase()

    private fun Element.isRole(role: String): Boolean =
        attribute(this, "ttm:role", "role") == role

    private fun attribute(element: Element, vararg names: String): String? {
        val wanted = names.map { it.substringAfter(':') }
        val attributes = element.attributes
        for (index in 0 until attributes.length) {
            val attribute = attributes.item(index)
            val name = (attribute.localName ?: attribute.nodeName.substringAfter(':')).lowercase()
            if (name in wanted) return attribute.nodeValue
        }
        return null
    }

    private fun parseTime(value: String): Long? {
        val parts = value.trim().split(':')
        if (parts.size !in 2..3) return null
        val secondsPart = parts.last()
        val seconds = secondsPart.substringBefore('.').toLongOrNull() ?: return null
        val fraction = secondsPart.substringAfter('.', "")
        val millis = when {
            fraction.isEmpty() -> 0L
            fraction.length >= 3 -> fraction.take(3).toLongOrNull() ?: return null
            else -> (fraction + "000").take(3).toLongOrNull() ?: return null
        }
        val minuteIndex = parts.size - 2
        val minutes = parts[minuteIndex].toLongOrNull() ?: return null
        val hours = if (parts.size == 3) parts[0].toLongOrNull() ?: return null else 0L
        return hours * 3_600_000L + minutes * 60_000L + seconds * 1_000L + millis
    }
}
