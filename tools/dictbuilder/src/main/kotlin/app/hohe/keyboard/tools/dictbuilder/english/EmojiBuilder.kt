package app.hohe.keyboard.tools.dictbuilder.english

import org.w3c.dom.Element
import java.io.File
import java.io.InputStream
import java.util.zip.CRC32
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Builds `emoji.tsv` from Unicode `emoji-test.txt` (categories, order, versions) and CLDR
 * annotations (keywords + names) for English and Amharic.
 *
 * Output (UTF-8, LF), read by `EmojiData` in `:lang:english`:
 * ```
 * HOHE-EMOJI<TAB>1<TAB><dataVersion><TAB><licenceId><TAB><crc32 hex of all following bytes>
 * C<TAB><categoryId><TAB><display name>
 * E<TAB><emoji><TAB><emoji version><TAB><en name|kw|kw..><TAB><am name|kw|kw..>
 * ```
 * Emoji lines belong to the preceding category. Only fully-qualified emoji are kept;
 * skin-tone variants and the Component group are dropped.
 */
object EmojiBuilder {
    const val FORMAT_VERSION = 1

    /** emoji-test.txt group -> category id, in panel order. */
    val CATEGORIES: List<Pair<String, String>> =
        listOf(
            "Smileys & Emotion" to "smileys_emotion",
            "People & Body" to "people_body",
            "Animals & Nature" to "animals_nature",
            "Food & Drink" to "food_drink",
            "Travel & Places" to "travel_places",
            "Activities" to "activities",
            "Objects" to "objects",
            "Symbols" to "symbols",
            "Flags" to "flags",
        )
    private const val SKIPPED_GROUP = "Component"

    data class Emoji(
        val emoji: String,
        val version: String,
        val name: String,
    )

    data class Annotation(
        val name: String?,
        val keywords: List<String>,
    )

    /** Parses emoji-test.txt into group name -> emoji in file order. */
    fun parseEmojiTest(lines: Sequence<String>): LinkedHashMap<String, MutableList<Emoji>> {
        val groups = LinkedHashMap<String, MutableList<Emoji>>()
        var group: String? = null
        for (line in lines) {
            if (line.startsWith("# group:")) {
                group = line.substringAfter(':').trim()
                continue
            }
            if (line.isBlank() || line.startsWith("#")) continue
            val g = group ?: continue
            val status = line.substringAfter(';').substringBefore('#').trim()
            if (status != "fully-qualified" || g == SKIPPED_GROUP) continue
            val cps =
                line
                    .substringBefore(';')
                    .trim()
                    .split(' ')
                    .filter { it.isNotEmpty() }
                    .map { it.toInt(16) }
            if (cps.any { it in 0x1F3FB..0x1F3FF }) continue
            val emoji = cps.joinToString("") { String(Character.toChars(it)) }
            val comment = line.substringAfter('#').trim()
            val m = Regex("^\\S+\\s+E(\\d+\\.\\d+)\\s+(.*)$").find(comment) ?: error("bad emoji-test line: $line")
            groups.getOrPut(g) { ArrayList() }.add(Emoji(emoji, m.groupValues[1], m.groupValues[2]))
        }
        return groups
    }

    /** Parses a CLDR annotations file into key (emoji without U+FE0F) -> annotation. */
    fun parseAnnotations(input: InputStream): Map<String, Annotation> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isValidating = false
        factory.isNamespaceAware = false
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        val doc = factory.newDocumentBuilder().parse(input)
        val names = HashMap<String, String>()
        val keywords = HashMap<String, List<String>>()
        val nodes = doc.getElementsByTagName("annotation")
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            val cp = el.getAttribute("cp")
            val text = el.textContent.trim()
            if (text.isEmpty() || text == "↑↑↑") continue
            if (el.getAttribute("type") == "tts") {
                names[cp] = text
            } else {
                keywords[cp] = text.split('|').map { it.trim() }.filter { it.isNotEmpty() && it != "↑↑↑" }
            }
        }
        return (names.keys + keywords.keys).associateWith { Annotation(names[it], keywords[it].orEmpty()) }
    }

    fun build(
        groups: Map<String, List<Emoji>>,
        en: List<Map<String, Annotation>>,
        am: List<Map<String, Annotation>>,
        dataVersion: String,
        licenceId: String,
    ): String {
        val unknown = groups.keys - CATEGORIES.map { it.first }.toSet()
        require(unknown.isEmpty()) { "unknown emoji groups: $unknown" }
        val body = StringBuilder()
        for ((group, id) in CATEGORIES) {
            val list = groups[group] ?: continue
            body
                .append("C\t")
                .append(id)
                .append('\t')
                .append(group)
                .append('\n')
            for (e in list) {
                val enField = field(e, en, fallbackName = e.name)
                val amField = field(e, am, fallbackName = null)
                body
                    .append("E\t")
                    .append(e.emoji)
                    .append('\t')
                    .append(e.version)
                    .append('\t')
                body
                    .append(enField)
                    .append('\t')
                    .append(amField)
                    .append('\n')
            }
        }
        val bytes = body.toString().toByteArray(Charsets.UTF_8)
        val crc = CRC32().apply { update(bytes) }.value
        val header = "HOHE-EMOJI\t$FORMAT_VERSION\t$dataVersion\t$licenceId\t${"%08x".format(crc)}\n"
        return header + body
    }

    private fun field(
        e: Emoji,
        sources: List<Map<String, Annotation>>,
        fallbackName: String?,
    ): String {
        val key = e.emoji.replace("️", "")
        val a = sources.firstNotNullOfOrNull { it[key] ?: it[e.emoji] }
        val name = clean(a?.name ?: fallbackName ?: return "")
        val kws =
            a
                ?.keywords
                .orEmpty()
                .map(::clean)
                .filter { it.isNotEmpty() && it != name }
                .distinct()
        return (listOf(name) + kws).joinToString("|")
    }

    private fun clean(s: String): String =
        s
            .replace('\t', ' ')
            .replace('\n', ' ')
            .replace('|', '/')
            .trim()

    fun buildFromFiles(
        emojiTest: File,
        cldrDir: File,
        dataVersion: String,
        licenceId: String,
    ): String {
        val groups = emojiTest.useLines { parseEmojiTest(it) }

        fun load(name: String): Map<String, Annotation> {
            val f = File(cldrDir, name)
            return if (f.isFile) f.inputStream().use { parseAnnotations(it) } else emptyMap()
        }
        val en = listOf(load("annotations_en.xml"), load("annotationsDerived_en.xml"))
        val am = listOf(load("annotations_am.xml"), load("annotationsDerived_am.xml"))
        require(en[0].isNotEmpty()) { "missing annotations_en.xml in $cldrDir" }
        return build(groups, en, am, dataVersion, licenceId)
    }
}
