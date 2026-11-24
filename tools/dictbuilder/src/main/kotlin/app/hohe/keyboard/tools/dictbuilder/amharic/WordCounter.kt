package app.hohe.keyboard.tools.dictbuilder.amharic

/**
 * Word frequency counter with a memory bound. When more than [maxEntries] distinct words are
 * held, every word whose count is at or below the current floor is dropped and the floor rises
 * (lossy counting). A word dropped that way can lose at most [floor] occurrences, far below the
 * cut-off frequency of a ~80k-word list, so the final ranking is unaffected in practice.
 */
class WordCounter(
    private val maxEntries: Int = 1_500_000,
) {
    private val counts = HashMap<String, LongArray>(1 shl 16)
    var floor: Long = 0
        private set
    var tokens: Long = 0
        private set
    var prunes: Int = 0
        private set

    fun add(word: String) {
        tokens++
        val c = counts[word]
        if (c != null) {
            c[0]++
            return
        }
        counts[word] = longArrayOf(1)
        if (counts.size > maxEntries) prune()
    }

    private fun prune() {
        do {
            floor++
            counts.values.removeIf { it[0] <= floor }
        } while (counts.size > maxEntries * 3 / 4)
        prunes++
    }

    val size: Int get() = counts.size

    fun snapshot(): Map<String, Long> = counts.mapValues { it.value[0] }
}
