package app.hohe.keyboard.english.emoji

import app.hohe.keyboard.core.engine.EmojiSuggester
import java.text.Normalizer
import java.util.Locale

/**
 * Offline emoji lookup over CLDR names and keywords in English and Amharic.
 *
 * - [suggest]: exact keyword or name match (REQ-062).
 * - [search]: exact matches first, then prefix matches on keywords and their words (REQ-060).
 *
 * Within a tier, an emoji whose name equals the query ranks first, then panel order.
 * Immutable after construction; safe to share between threads.
 */
class HoheEmojiSuggester(
    data: EmojiData,
) : EmojiSuggester {
    private val entries = data.entries

    /** Normalised full keyword or name -> entry indices (ascending). */
    private val exact: Map<String, IntArray>

    /** Normalised names -> entry indices, for the "name equals query" boost. */
    private val names: Map<String, IntArray>

    /** Sorted prefix keys (keywords, names and their single words) with entry indices. */
    private val prefixKeys: Array<String>
    private val prefixHits: Array<IntArray>

    init {
        val ex = HashMap<String, MutableSet<Int>>()
        val nm = HashMap<String, MutableSet<Int>>()
        val pk = HashMap<String, MutableSet<Int>>()
        entries.forEachIndexed { i, e ->
            for (lang in listOf(e.en, e.am)) {
                lang.forEachIndexed { k, raw ->
                    val key = normalize(raw)
                    if (key.isEmpty()) return@forEachIndexed
                    ex.getOrPut(key) { sortedSetOf() }.add(i)
                    if (k == 0) nm.getOrPut(key) { sortedSetOf() }.add(i)
                    pk.getOrPut(key) { sortedSetOf() }.add(i)
                    for (w in key.split(' ', '-', ':', ',')) {
                        if (w.isNotEmpty() && w != key) pk.getOrPut(w) { sortedSetOf() }.add(i)
                    }
                }
            }
        }
        exact = ex.mapValues { it.value.toIntArray() }
        names = nm.mapValues { it.value.toIntArray() }
        val sorted = pk.keys.sorted()
        prefixKeys = sorted.toTypedArray()
        prefixHits = Array(sorted.size) { pk.getValue(sorted[it]).toIntArray() }
    }

    override fun suggest(
        word: String,
        max: Int,
    ): List<String> {
        val key = normalize(word)
        if (key.isEmpty() || max <= 0) return emptyList()
        return ranked(key, exact[key] ?: return emptyList()).take(max).map { entries[it].emoji }
    }

    override fun search(
        query: String,
        max: Int,
    ): List<String> {
        val key = normalize(query)
        if (key.isEmpty() || max <= 0) return emptyList()
        val out = LinkedHashSet<Int>()
        exact[key]?.let { out.addAll(ranked(key, it)) }
        if (out.size < max) {
            val prefixed = sortedSetOf<Int>()
            var i = lowerBound(key)
            while (i < prefixKeys.size && prefixKeys[i].startsWith(key)) {
                prefixHits[i].forEach { prefixed.add(it) }
                i++
            }
            out.addAll(prefixed)
        }
        return out
            .asSequence()
            .take(max)
            .map { entries[it].emoji }
            .toList()
    }

    /** [hits] with entries named exactly [key] first, then panel order. */
    private fun ranked(
        key: String,
        hits: IntArray,
    ): List<Int> {
        val named = names[key]?.toSet().orEmpty()
        return hits.sortedWith(compareBy<Int> { if (it in named) 0 else 1 }.thenBy { it })
    }

    private fun lowerBound(key: String): Int {
        var lo = 0
        var hi = prefixKeys.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (prefixKeys[mid] < key) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        internal fun normalize(s: String): String = Normalizer.normalize(s.trim(), Normalizer.Form.NFC).lowercase(Locale.ROOT)
    }
}
