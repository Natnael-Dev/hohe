package app.hohe.keyboard.english.emoji

import java.io.InputStream
import java.util.zip.CRC32

/** One emoji with its CLDR name and keywords. [en]/[am] hold the name first, then keywords; [am] may be empty. */
data class EmojiEntry(
    val emoji: String,
    /** Unicode emoji version that introduced it, e.g. "15.0". */
    val version: String,
    val en: List<String>,
    val am: List<String>,
) {
    val nameEn: String get() = en.firstOrNull().orEmpty()
    val nameAm: String? get() = am.firstOrNull()

    /** Emoji version as a number for filtering by device support (e.g. 15.0). */
    val versionNumber: Double get() = version.toDoubleOrNull() ?: 0.0
}

/** One panel category in standard order. [displayName] is the Unicode group name (English). */
data class EmojiCategory(
    val id: String,
    val displayName: String,
    val entries: List<EmojiEntry>,
) {
    val emoji: List<String> get() = entries.map { it.emoji }
}

/**
 * Parsed `assets/dict/emoji.tsv` (built by `tools/dictbuilder english emoji`).
 *
 * ```
 * HOHE-EMOJI<TAB>1<TAB><dataVersion><TAB><licence><TAB><crc32 hex of the rest>
 * C<TAB><id><TAB><display name>
 * E<TAB><emoji><TAB><version><TAB><en name|kw..><TAB><am name|kw..>
 * ```
 */
class EmojiData private constructor(
    val dataVersion: String,
    val licenceId: String,
    val categories: List<EmojiCategory>,
) {
    /** All entries in panel order; the index is the catalog order used for ranking. */
    val entries: List<EmojiEntry> = categories.flatMap { it.entries }

    companion object {
        const val FORMAT_VERSION: Int = 1
        const val MAGIC: String = "HOHE-EMOJI"

        /** Empty data: used when the asset is missing or invalid (fail soft). */
        val EMPTY: EmojiData = EmojiData("none", "", emptyList())

        /**
         * Parses and checksums [raw]. Throws [IllegalArgumentException] if invalid. CRLF line
         * endings (a Windows checkout under `text=auto`) are normalised to LF before the check.
         */
        fun parse(raw: String): EmojiData {
            val text = raw.replace("\r\n", "\n")
            val nl = text.indexOf('\n')
            require(nl > 0) { "no header" }
            val header = text.substring(0, nl).split('\t')
            require(header.size == 5 && header[0] == MAGIC) { "bad header" }
            require(header[1].toIntOrNull() == FORMAT_VERSION) { "unsupported version ${header[1]}" }
            val body = text.substring(nl + 1)
            val crc = CRC32().apply { update(body.toByteArray(Charsets.UTF_8)) }.value
            require(header[4].toLongOrNull(16) == crc) { "checksum mismatch" }

            val categories = ArrayList<EmojiCategory>()
            var id: String? = null
            var name = ""
            var list = ArrayList<EmojiEntry>()

            fun flush() {
                id?.let { categories.add(EmojiCategory(it, name, list)) }
            }
            for (line in body.split('\n')) {
                if (line.isEmpty()) continue
                val f = line.split('\t')
                when (f[0]) {
                    "C" -> {
                        require(f.size == 3) { "bad category line" }
                        flush()
                        id = f[1]
                        name = f[2]
                        list = ArrayList()
                    }
                    "E" -> {
                        require(f.size == 5 && id != null) { "bad emoji line" }
                        list.add(EmojiEntry(f[1], f[2], split(f[3]), split(f[4])))
                    }
                    else -> throw IllegalArgumentException("unknown line kind '${f[0]}'")
                }
            }
            flush()
            return EmojiData(header[2], header[3], categories)
        }

        /** Reads the whole stream as UTF-8 (not closed here) and parses it. */
        fun parse(input: InputStream): EmojiData = parse(String(input.readBytes(), Charsets.UTF_8))

        /** Like [parse] but returns [EMPTY] for a missing or invalid file. */
        fun loadOrEmpty(input: InputStream?): EmojiData = input?.let { runCatching { parse(it) }.getOrNull() } ?: EMPTY

        private fun split(field: String): List<String> = if (field.isEmpty()) emptyList() else field.split('|')
    }
}

/** Emoji panel categories in standard order (Smileys & Emotion ... Flags). */
class EmojiCatalog(
    private val data: EmojiData,
) {
    val categories: List<EmojiCategory> get() = data.categories

    /**
     * Categories limited to emoji the device can show, e.g. `maxVersion = 15.0` on a device
     * whose font supports Emoji 15.0. Empty categories are dropped.
     */
    fun categories(maxVersion: Double): List<EmojiCategory> =
        data.categories
            .map { c -> c.copy(entries = c.entries.filter { it.versionNumber <= maxVersion }) }
            .filter { it.entries.isNotEmpty() }

    fun category(id: String): EmojiCategory? = data.categories.firstOrNull { it.id == id }

    companion object {
        /** Category ids in panel order. */
        val IDS: List<String> =
            listOf(
                "smileys_emotion",
                "people_body",
                "animals_nature",
                "food_drink",
                "travel_places",
                "activities",
                "objects",
                "symbols",
                "flags",
            )
    }
}
