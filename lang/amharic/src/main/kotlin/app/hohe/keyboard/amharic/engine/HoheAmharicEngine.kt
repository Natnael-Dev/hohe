package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.converter.AmharicConverter
import app.hohe.keyboard.amharic.dict.AmharicDictionary
import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.Candidate
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.SuggestionResult
import app.hohe.keyboard.core.engine.UserDataSnapshot
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.Locale

/**
 * Amharic word engine (REQ-020..029, engine spec §3.3).
 *
 * Order: user words → previous pick for the exact Latin input → dictionary (exact matches,
 * then completions; a completion ≥ 20× as frequent as the best exact match may lead, D-018)
 * → letter-by-letter transliteration. When the dictionary has no close match (cost > 2.0),
 * the transliteration leads the non-personal part instead (spec §3.3 fallback placement).
 *
 * A null [dict] (missing/corrupt file) gives `dataVersion = "none"` and transliteration only.
 */
class HoheAmharicEngine(
    private val dict: AmharicDictionary?,
) : AmharicEngine {
    private val matcher = dict?.let { DictionaryMatcher(it) }

    override val dataVersion: String = dict?.dataVersion ?: NO_DATA

    override fun transliterate(latin: String): String = AmharicConverter.convert(latin)

    override fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult {
        val t0 = System.nanoTime()
        val key = normalise(req.input)
        val candidates =
            if (key.isEmpty() || req.maxResults <= 0) emptyList() else rank(key, req, user)
        return SuggestionResult(req.id, req.input, candidates, (System.nanoTime() - t0) / 1000)
    }

    private fun rank(
        key: String,
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): List<Candidate> {
        val out = LinkedHashMap<String, Source>()
        val max = req.maxResults

        fun add(
            text: String,
            source: Source,
        ) {
            if (text.isNotEmpty() && out.size < max && text !in out) out[text] = source
        }

        if (req.usePersonalData) {
            for (w in user.userWords) if (w.latin.lowercase(Locale.ROOT) == key) add(w.ethiopic, Source.USER_WORD)
            user.amharicPicks[key]?.let { add(it, Source.USER_PICK) }
        }

        val matches = matcher?.match(key.replace("'", "")).orEmpty()
        val translit = AmharicConverter.nBest(key, FALLBACK_K).map { it.text }
        if (matches.none { it.cost <= CLOSE_MATCH }) {
            translit.firstOrNull()?.let { add(it, Source.FALLBACK_TRANSLIT) }
        }

        val bestCloseExactQ = matches.filter { !it.completion && it.cost <= CLOSE_READING }.maxOfOrNull { it.freqQ }
        val ordered =
            matches.sortedWith(
                compareBy<DictionaryMatcher.Match>({ tier(it, bestCloseExactQ) }, { it.score }, { -it.freqQ }, { it.index }),
            )
        val perVariant = HashMap<String, Int>()
        for (m in ordered) {
            if (out.size >= max) break
            val word = dict!!.word(m.index)
            val norm = foldVariants(word)
            val seen = perVariant.getOrDefault(norm, 0)
            if (seen >= MAX_VARIANTS) continue
            if (word !in out) perVariant[norm] = seen + 1
            add(word, Source.DICTIONARY)
        }

        for (t in translit) add(t, Source.FALLBACK_TRANSLIT)
        if (out.isEmpty()) add(transliterate(key), Source.FALLBACK_TRANSLIT)

        var score = out.size
        return out.map { (text, source) -> Candidate(text, source, score--) }
    }

    /**
     * Ranking tier (lower first), then [DictionaryMatcher.Match.score] inside a tier:
     * 0 = dominant completion: a close reading (cost ≤ 1.0) at least 20× as frequent as the best
     *     close exact match, or any close completion when there is no close exact match
     *     (spec §3.3, D-018; 20× is `ln 20 · 64 ≈ 192` quantised units);
     * 1 = exact matches; 2 = other completions.
     */
    private fun tier(
        m: DictionaryMatcher.Match,
        bestCloseExactQ: Int?,
    ): Int {
        if (!m.completion) return 1
        val dominant = bestCloseExactQ == null || m.freqQ - bestCloseExactQ >= DOMINANT_Q
        return if (dominant && m.cost <= CLOSE_READING) 0 else 2
    }

    companion object {
        const val NO_DATA = "none"
        private const val FALLBACK_K = 4
        private const val CLOSE_MATCH = 2.0f
        private const val CLOSE_READING = 1.0f
        private val DOMINANT_Q = Math.round(64.0 * Math.log(20.0)).toInt()
        private const val MAX_VARIANTS = 2

        /** Builds an engine from a dictionary buffer; a bad or null buffer gives a translit-only engine. */
        fun fromBuffer(buffer: ByteBuffer?): HoheAmharicEngine = HoheAmharicEngine(AmharicDictionary.load(buffer))

        /** Builds an engine from a dictionary stream (closed afterwards); fails soft like [fromBuffer]. */
        fun fromStream(input: InputStream?): HoheAmharicEngine = HoheAmharicEngine(AmharicDictionary.load(input))

        internal fun normalise(input: String): String {
            val sb = StringBuilder(input.length)
            for (ch in input.lowercase(Locale.ROOT)) if (ch in 'a'..'z' || ch == '\'') sb.append(ch)
            return if (sb.any { it != '\'' }) sb.toString() else ""
        }

        private val FOLD: Map<Char, Int> =
            mapOf(
                // homophone series folded for the visible-variant cap (spec §3.6)
                0x1210 to 0x1200,
                0x1280 to 0x1200,
                0x1220 to 0x1230,
                0x12D0 to 0x12A0,
                0x1338 to 0x1340,
            ).entries.flatMap { (from, to) -> (0..7).map { (from + it).toChar() to (to + it) } }.toMap()

        private fun foldVariants(word: String): String {
            val sb = StringBuilder(word.length)
            for (ch in word) sb.appendCodePoint(FOLD[ch] ?: ch.code)
            return sb.toString()
        }
    }
}
