package app.hohe.keyboard.english

import app.hohe.keyboard.core.engine.Candidate
import app.hohe.keyboard.core.engine.EnglishEngine
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.SuggestionResult
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.field.FieldClass
import java.io.InputStream
import java.nio.ByteBuffer
import kotlin.math.ln

/**
 * English word engine (REQ-040..043): proximity-aware corrections and completions over
 * `en_main.bin`, boosted by the user's English history. Suggests only; never auto-replaces
 * (D-023). Not thread-safe: confine to the engine thread.
 *
 * Fails soft: with no dictionary ([dict] null) it still offers learned user words, and
 * [dataVersion] is `"none"`.
 */
class HoheEnglishEngine(
    private val dict: EnglishDictionary?,
) : EnglishEngine {
    override val dataVersion: String = dict?.dataVersion ?: NO_DATA

    override fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult {
        val start = System.nanoTime()
        val candidates =
            try {
                compute(req, user)
            } catch (e: IndexOutOfBoundsException) {
                // A truncated or corrupt mapped buffer must never crash the keyboard.
                emptyList()
            }
        return SuggestionResult(req.id, req.input, candidates, (System.nanoTime() - start) / 1000)
    }

    override fun nextWords(
        contextBefore: String,
        user: UserDataSnapshot,
        max: Int,
    ): List<Candidate> {
        if (max <= 0) return emptyList()
        val last = WORD.findAll(contextBefore).lastOrNull() ?: return emptyList()
        // A sentence boundary after the last word means it is not a usable context.
        if (contextBefore.substring(last.range.last + 1).any { it in SENTENCE_END }) return emptyList()
        val nexts = user.englishBigrams[last.value.lowercase()] ?: return emptyList()
        return nexts.entries
            .asSequence()
            .filter { it.value > 0 && it.key.isNotBlank() }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(max)
            .map { Candidate(it.key, Source.NEXT_WORD, it.value) }
            .toList()
    }

    private fun compute(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): List<Candidate> {
        if (req.field in BLOCKED_FIELDS) return emptyList()
        val input = req.input
        if (input.isEmpty() || input.length > MAX_INPUT || input.any { it.isWhitespace() }) return emptyList()
        val personal = req.usePersonalData && req.field != FieldClass.INCOGNITO
        val neighbours = req.proximity?.neighbours ?: QwertyProximity.forWord(input).neighbours
        val search = Search(dict, input, neighbours)
        val scores = HashMap<String, Int>()
        val sources = HashMap<String, Source>()

        fun offer(
            text: String,
            score: Int,
            source: Source,
        ) {
            val old = scores[text]
            if (old == null || score > old) {
                scores[text] = score
                sources[text] = source
            }
        }

        if (dict != null) {
            search.run { text, score -> offer(text, score, Source.DICTIONARY) }
        }
        if (personal && user.englishUnigrams.isNotEmpty()) {
            for (key in scores.keys.toList()) {
                val count = user.englishUnigrams[key.lowercase()] ?: continue
                scores[key] = scores.getValue(key) + userBoost(count)
            }
            for ((word, count) in user.englishUnigrams) {
                if (count <= 0 || word.isEmpty() || word.length >= MAX_DEPTH) continue
                if (dict != null && word.replaceFirstChar { it.uppercaseChar() } in dict) continue
                val dictFreq = dict?.frequency(word) ?: 0
                val base = if (dictFreq > 0) dictFreq else USER_BASE
                val s = search.scoreWord(word, base) ?: continue
                offer(word, s + userBoost(count), if (dictFreq > 0) Source.DICTIONARY else Source.USER_WORD)
            }
        }

        val typedKnown = scores.keys.any { applyCase(it, input) == input }
        val best = HashMap<String, Candidate>()
        for ((text, score) in scores) {
            val shown = applyCase(text, input)
            if (shown == input) continue
            if (typedKnown && shown.equals(input, ignoreCase = true)) continue
            val old = best[shown]
            if (old == null || score > old.score) best[shown] = Candidate(shown, sources.getValue(text), score)
        }
        return best.values
            .sortedWith(compareByDescending<Candidate> { it.score }.thenBy { it.text })
            .take(req.maxResults.coerceAtLeast(0))
    }

    /** Weighted edit-distance search state for one typed word. */
    private class Search(
        private val dict: EnglishDictionary?,
        input: String,
        private val neighbours: List<String>,
    ) {
        private val n = input.length
        private val typed = CharArray(n) { EnglishDictionary.fold(input[it]) }
        private val maxCost = maxCostFor(n)
        private val compMax = if (n <= 2) 1 else COMPLETION_MAX_COST
        private val rows = Array(MAX_DEPTH + 1) { IntArray(n + 1) }
        private val path = CharArray(MAX_DEPTH)
        private val roots = ArrayList<Root>()

        private class Root(
            val node: Int,
            val prefix: String,
            val cost: Int,
            val maxFreq: Int,
        )

        private class Item(
            val node: Int,
            val text: String,
            val extra: Int,
            val bound: Int,
            val isWord: Boolean,
        )

        fun run(emit: (String, Int) -> Unit) {
            val d = dict ?: return
            initRow(rows[0])
            visit(d, EnglishDictionary.ROOT, 0, NO_CHAR, emit)
            roots.sortWith(compareBy<Root> { it.cost }.thenByDescending { it.maxFreq })
            for (root in roots.take(MAX_ROOTS)) complete(d, root, emit)
        }

        /** Best correction or completion score of a standalone [word] with base frequency [freq]; null if too far. */
        fun scoreWord(
            word: String,
            freq: Int,
        ): Int? {
            initRow(rows[0])
            var best: Int? = null
            var parentFold = NO_CHAR
            for (d in word.indices) {
                val ch = word[d]
                val f = EnglishDictionary.fold(ch)
                val prev = rows[d]
                val row = rows[d + 1]
                val min = fillRow(row, prev, if (d > 0) rows[d - 1] else null, f, ch.isUpperCase(), parentFold)
                if (min > maxCost) return best
                val last = row[n]
                val extra = word.length - d - 1
                val s =
                    if (extra == 0) {
                        if (last <= maxCost) freq - ALPHA * last else null
                    } else if (last <= compMax && last < prev[n] + omitCost(f, parentFold)) {
                        freq - GAMMA * extra - ALPHA * last
                    } else {
                        null
                    }
                if (s != null && (best == null || s > best)) best = s
                parentFold = f
            }
            return best
        }

        private fun initRow(r0: IntArray) {
            r0[0] = 0
            for (j in 1..n) r0[j] = r0[j - 1] + insCost(j - 1)
        }

        private fun visit(
            d: EnglishDictionary,
            node: Int,
            depth: Int,
            parentFold: Char,
            emit: (String, Int) -> Unit,
        ) {
            if (depth >= MAX_DEPTH) return
            val prev = rows[depth]
            val grand = if (depth > 0) rows[depth - 1] else null
            val row = rows[depth + 1]
            val first = d.firstChild(node)
            val count = d.childCount(node)
            for (k in 0 until count) {
                val child = first + k
                val ci = d.charIdx(child)
                val f = d.foldTable[ci]
                val min = fillRow(row, prev, grand, f, d.upperTable[ci], parentFold)
                if (min > maxCost) continue
                path[depth] = d.charAt(ci)
                val last = row[n]
                val freq = d.freq(child)
                if (freq > 0 && last <= maxCost) emit(String(path, 0, depth + 1), freq - ALPHA * last)
                if (last <= compMax && last < prev[n] + omitCost(f, parentFold) && d.childCount(child) > 0) {
                    roots.add(Root(child, String(path, 0, depth + 1), last, d.maxFreq(child)))
                }
                visit(d, child, depth + 1, f, emit)
            }
        }

        /** Fills [row] for one more dictionary char; returns the row minimum. */
        private fun fillRow(
            row: IntArray,
            prev: IntArray,
            grand: IntArray?,
            f: Char,
            upper: Boolean,
            parentFold: Char,
        ): Int {
            val omit = omitCost(f, parentFold)
            row[0] = prev[0] + omit
            var min = row[0]
            for (j in 1..n) {
                var v = prev[j - 1] + subCost(j - 1, f, upper)
                val o = prev[j] + omit
                if (o < v) v = o
                val ins = row[j - 1] + insCost(j - 1)
                if (ins < v) v = ins
                if (grand != null && j >= 2 && typed[j - 1] == parentFold && typed[j - 2] == f && f != parentFold) {
                    val t = grand[j - 2] + TRANSPOSE
                    if (t < v) v = t
                }
                row[j] = v
                if (v < min) min = v
            }
            return min
        }

        /** Best-first walk of [root]'s subtree by subtree max frequency. */
        private fun complete(
            d: EnglishDictionary,
            root: Root,
            emit: (String, Int) -> Unit,
        ) {
            val queue = java.util.PriorityQueue<Item>(64, compareByDescending { it.bound })

            fun pushChildren(
                node: Int,
                text: String,
                extra: Int,
            ) {
                if (text.length >= MAX_DEPTH) return
                val first = d.firstChild(node)
                for (k in 0 until d.childCount(node)) {
                    val c = first + k
                    val t = text + d.charAt(d.charIdx(c))
                    queue.add(Item(c, t, extra + 1, d.maxFreq(c) - GAMMA * (extra + 1), false))
                }
            }
            pushChildren(root.node, root.prefix, 0)
            var emitted = 0
            var pops = 0
            while (emitted < COMPLETIONS_PER_ROOT && pops < MAX_POPS) {
                val item = queue.poll() ?: break
                pops++
                if (item.isWord) {
                    emit(item.text, item.bound - ALPHA * root.cost)
                    emitted++
                    continue
                }
                val freq = d.freq(item.node)
                if (freq > 0) queue.add(Item(item.node, item.text, item.extra, freq - GAMMA * item.extra, true))
                pushChildren(item.node, item.text, item.extra)
            }
        }

        private fun subCost(
            i: Int,
            f: Char,
            upper: Boolean,
        ): Int {
            if (typed[i] == f) return if (upper) CASE_COST else 0
            val near = if (i < neighbours.size) neighbours[i].indexOf(f) else -1
            return if (near >= 0) PROXIMITY_COST + minOf(near, 2) else EDIT_COST
        }

        /** Cost of a typed char with no dictionary counterpart. A repeated key costs less. */
        private fun insCost(i: Int): Int = if (i > 0 && typed[i] == typed[i - 1]) REPEAT_COST else EDIT_COST

        private fun omitCost(
            f: Char,
            parentFold: Char,
        ): Int =
            when {
                f == parentFold -> CHEAP_OMIT_COST
                f == '\'' || f == '-' || f == '.' || f == '’' -> CHEAP_OMIT_COST
                else -> EDIT_COST
            }
    }

    companion object {
        const val NO_DATA: String = "none"

        /** Edit-cost units: one full edit is [EDIT_COST]; each unit lowers the score by [ALPHA]. */
        private const val EDIT_COST = 10
        private const val PROXIMITY_COST = 6
        private const val TRANSPOSE = 7
        private const val REPEAT_COST = 4
        private const val CHEAP_OMIT_COST = 1
        private const val CASE_COST = 1
        private const val COMPLETION_MAX_COST = 6
        private const val ALPHA = 12
        private const val GAMMA = 3
        private const val USER_BASE = 110
        private const val USER_BOOST = 12.0
        private const val MAX_INPUT = 32
        private const val MAX_DEPTH = 40
        private const val MAX_ROOTS = 5
        private const val COMPLETIONS_PER_ROOT = 12
        private const val MAX_POPS = 4000
        private const val NO_CHAR = '\u0000'
        private val BLOCKED_FIELDS = setOf(FieldClass.PASSWORD, FieldClass.NUMERIC, FieldClass.NO_SUGGEST)
        private val WORD = Regex("[\\p{L}\\p{M}'’-]+")
        private const val SENTENCE_END = ".!?\n"

        private fun maxCostFor(n: Int): Int =
            when {
                n <= 1 -> CASE_COST // case/accent fixes only, e.g. "i" -> "I"
                n == 2 -> 7
                n <= 4 -> 10
                n <= 7 -> 16
                else -> 20
            }

        private fun userBoost(count: Int): Int = (USER_BOOST * ln(1.0 + count) / ln(2.0)).toInt()

        /** Matches the typed word's capitalisation: all caps stays all caps, a capital first letter is kept. */
        internal fun applyCase(
            word: String,
            input: String,
        ): String {
            val letters = input.filter { it.isLetter() }
            return when {
                letters.length > 1 && letters.all { it.isUpperCase() } -> word.uppercase()
                input.first().isUpperCase() -> word.replaceFirstChar { it.titlecaseChar() }
                else -> word
            }
        }

        /** Loads from a dictionary buffer; a missing or invalid buffer yields a dictionary-less engine. */
        fun load(buffer: ByteBuffer?): HoheEnglishEngine =
            HoheEnglishEngine(buffer?.let { runCatching { EnglishDictionary.from(it) }.getOrNull() })

        /** Loads from `assets/dict/en_main.bin` opened as a stream (not closed here); fails soft like [load]. */
        fun load(input: InputStream?): HoheEnglishEngine =
            HoheEnglishEngine(input?.let { runCatching { EnglishDictionary.from(it) }.getOrNull() })
    }
}
