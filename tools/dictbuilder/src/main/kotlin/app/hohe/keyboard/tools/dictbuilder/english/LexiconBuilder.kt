package app.hohe.keyboard.tools.dictbuilder.english

/**
 * Parses an AOSP LatinIME `*.combined` wordlist and selects the entries that go into
 * `en_main.bin`.
 *
 * Line format: a header `dictionary=main:en,...,version=54`, then ` word=the,f=222,flags=,...`
 * lines, each optionally followed by `  shortcut=...` / `  bigram=...` lines (ignored).
 */
object LexiconBuilder {
    const val MAX_WORD_LENGTH = 39

    data class Parsed(
        val sourceVersion: String?,
        val entries: List<WordFreq>,
        val skippedOffensive: Int,
        val skippedNotAWord: Int,
        val skippedZero: Int,
    )

    fun parse(lines: Sequence<String>): Parsed {
        var version: String? = null
        val out = ArrayList<WordFreq>()
        var offensive = 0
        var notAWord = 0
        var zero = 0
        for (line in lines) {
            if (line.startsWith("dictionary=")) {
                version = fields(line)["version"]
                continue
            }
            if (!line.startsWith(" word=")) continue
            val f = fields(line.trim())
            val word = f["word"] ?: continue
            val freq = f["f"]?.toIntOrNull() ?: 0
            when {
                f["possibly_offensive"] == "true" || f["flags"] == "offensive" -> offensive++
                f["not_a_word"] == "true" -> notAWord++
                freq <= 0 -> zero++
                word.isEmpty() || word.length > MAX_WORD_LENGTH || word.any { it.isWhitespace() } -> zero++
                else -> out.add(WordFreq(word, freq.coerceAtMost(255)))
            }
        }
        return Parsed(version, out, offensive, notAWord, zero)
    }

    /** Highest-frequency [max] entries (ties broken by spelling, so builds are deterministic). */
    fun select(
        entries: List<WordFreq>,
        max: Int,
    ): List<WordFreq> =
        entries
            .groupBy { it.word }
            .map { (w, list) -> WordFreq(w, list.maxOf { it.freq }) }
            .sortedWith(compareByDescending<WordFreq> { it.freq }.thenBy { it.word })
            .take(max)

    private fun fields(line: String): Map<String, String> =
        line
            .split(',')
            .mapNotNull { part ->
                val eq = part.indexOf('=')
                if (eq <= 0) null else part.substring(0, eq) to part.substring(eq + 1)
            }.toMap()
}
