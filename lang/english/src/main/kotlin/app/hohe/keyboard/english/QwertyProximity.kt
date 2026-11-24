package app.hohe.keyboard.english

import app.hohe.keyboard.core.engine.ProximityInfo
import kotlin.math.sqrt

/**
 * Static QWERTY key neighbours, used when a request carries no [ProximityInfo]
 * (tests, or an IME that has not measured its key geometry yet).
 *
 * Key centres in key-width units: top row x = i + 0.5, home row x = i + 1.0, bottom row
 * x = i + 2.0 (after a 1.5-wide shift key); rows one unit apart. Neighbours are the keys
 * within [RADIUS], nearest first.
 */
object QwertyProximity {
    private const val RADIUS = 1.6
    private val rows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val offsets = doubleArrayOf(0.5, 1.0, 2.0)

    private val table: Map<Char, String> by lazy {
        val pos = HashMap<Char, Pair<Double, Double>>()
        rows.forEachIndexed { r, row ->
            row.forEachIndexed { i, c -> pos[c] = Pair(i + offsets[r], r.toDouble()) }
        }
        pos.keys.associateWith { c ->
            val (x, y) = pos.getValue(c)
            pos.entries
                .asSequence()
                .filter { it.key != c }
                .map { (k, p) -> k to sqrt((p.first - x) * (p.first - x) + (p.second - y) * (p.second - y)) }
                .filter { it.second <= RADIUS }
                .sortedWith(compareBy({ it.second }, { it.first }))
                .map { it.first }
                .joinToString("")
        }
    }

    /** Neighbours of one letter (lowercase, nearest first); empty for non-letters. */
    fun neighboursOf(c: Char): String = table[c.lowercaseChar()] ?: ""

    /** Proximity info for a typed word, one entry per char. */
    fun forWord(word: String): ProximityInfo = ProximityInfo(word.map { neighboursOf(it) })
}
