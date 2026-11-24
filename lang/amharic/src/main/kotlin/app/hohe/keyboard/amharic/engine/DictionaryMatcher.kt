package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.dict.AmharicDictionary
import app.hohe.keyboard.amharic.dict.Fidel
import java.util.PriorityQueue

/**
 * Maps loose Latin onto dictionary words (engine spec §3.2): consonant-skeleton prefix lookup,
 * then a syllable alignment DP that prices how well the typed Latin reads as each word.
 * Not thread-safe (reuses scratch arrays); confined to the engine thread.
 */
internal class DictionaryMatcher(
    private val dict: AmharicDictionary,
) {
    class Match(
        val index: Int,
        val cost: Float,
        val completion: Boolean,
        val freqQ: Int,
    ) {
        /** Spec §3.2: `s = match_cost - λ·ln(freq + 1)`, λ = 0.5; lower is better. */
        val score: Float get() = cost - LAMBDA * freqQ / 64f
    }

    private var cur = FloatArray(MAX_INPUT + 1)
    private var nxt = FloatArray(MAX_INPUT + 1)

    /** All dictionary words that align with [latin] at cost ≤ [MAX_COST], unordered. */
    fun match(latin: String): List<Match> {
        if (latin.isEmpty() || latin.length > MAX_INPUT) return emptyList()
        val vowelInitial = latin[0] in VOWELS
        val picked = LinkedHashSet<Int>()
        for (skel in Fidel.latinSkeletons(latin)) {
            val exact = dict.exactRange(skel)
            var taken = 0
            for (i in exact) {
                if (taken >= EXACT_CAP) break
                if (Fidel.isGlottal(dict.charAt(i, 0)) == vowelInitial) {
                    picked.add(i)
                    taken++
                }
            }
            topByFrequency(dict.prefixRange(skel), vowelInitial, picked)
        }
        val out = ArrayList<Match>()
        for (i in picked) {
            align(latin, i)?.let { out.add(it) }
        }
        return out
    }

    private fun topByFrequency(
        range: IntRange,
        vowelInitial: Boolean,
        into: MutableSet<Int>,
    ) {
        if (range.isEmpty()) return
        // min-heap on frequency keeps the PREFIX_CAP most frequent words in the range
        val heap = PriorityQueue<Long>(PREFIX_CAP + 1)
        for (i in range) {
            val q = dict.freqQ(i)
            if (heap.size >= PREFIX_CAP && q <= (heap.peek() ushr 32).toInt()) continue
            if (Fidel.isGlottal(dict.charAt(i, 0)) != vowelInitial) continue
            heap.add((q.toLong() shl 32) or i.toLong())
            if (heap.size > PREFIX_CAP) heap.poll()
        }
        for (e in heap) into.add((e and 0xFFFFFFFFL).toInt())
    }

    /** Alignment DP over (typed position) per fidel; null if the word cannot be read as [t]. */
    fun align(
        t: String,
        index: Int,
    ): Match? {
        val m = t.length
        val n = dict.wordLength(index)
        if (n == 0) return null
        cur.fill(INF, 0, m + 1)
        cur[0] = 0f
        var bestCompletion = INF
        for (j in 0 until n) {
            val ch = dict.charAt(index, j)
            val sp = Fidel.spellingsOf(ch) ?: return null
            val cs = Fidel.costsOf(ch)!!
            nxt.fill(INF, 0, m + 1)
            var live = false
            for (i in 0..m) {
                val base = cur[i]
                if (base >= INF) continue
                if (i == m) {
                    // input exhausted: remaining fidels are a completion
                    val c = base + COMPLETION_PER_FIDEL * (n - j)
                    if (c < bestCompletion) bestCompletion = c
                    continue
                }
                val rest = m - i
                for (r in sp.indices) {
                    val s = sp[r]
                    val c = base + cs[r]
                    if (c > MAX_COST) continue
                    if (s.length <= rest) {
                        if (t.regionMatches(i, s, 0, s.length) && c < nxt[i + s.length]) {
                            nxt[i + s.length] = c
                            live = true
                        }
                    } else if (s.regionMatches(0, t, i, rest)) {
                        // input ends inside this fidel's spelling
                        val cc = c + PARTIAL + COMPLETION_PER_FIDEL * (n - j - 1)
                        if (cc < bestCompletion) bestCompletion = cc
                    }
                }
            }
            if (!live) {
                return if (bestCompletion <= MAX_COST) {
                    Match(index, bestCompletion, true, dict.freqQ(index))
                } else {
                    null
                }
            }
            val tmp = cur
            cur = nxt
            nxt = tmp
        }
        val exact = cur[m]
        return when {
            exact <= MAX_COST -> Match(index, exact, false, dict.freqQ(index))
            bestCompletion <= MAX_COST -> Match(index, bestCompletion, true, dict.freqQ(index))
            else -> null
        }
    }

    companion object {
        const val MAX_INPUT = 32
        const val MAX_COST = 4.0f
        const val LAMBDA = 0.5f
        const val COMPLETION_PER_FIDEL = 0.3f
        const val PARTIAL = 0.15f
        const val EXACT_CAP = 64
        const val PREFIX_CAP = 300
        private const val INF = Float.MAX_VALUE
        private const val VOWELS = "aeiou"
    }
}
