package app.hohe.keyboard.english

import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EnglishPerfTest {
    private val words =
        (
            "the people would about there their which could other these first after where think " +
                "because through something different between government important children " +
                "together tomorrow hello thanks morning evening information development another " +
                "question remember understand everything beautiful restaurant necessary keyboard " +
                "amazing probably definitely tonight weekend birthday congratulations communication"
        ).split(' ')

    /** 1000 deterministic inputs: prefixes, exact words and single typos (drop, swap, neighbour). */
    private fun inputs(): List<String> {
        val rnd = Random(42)
        return List(1000) {
            val w = words[rnd.nextInt(words.size)]
            when (rnd.nextInt(5)) {
                0 -> w.substring(0, 1 + rnd.nextInt(w.length))
                1 -> w
                2 -> w.removeRange(rnd.nextInt(w.length).let { it..it })
                3 -> if (w.length > 2) w.substring(0, 1) + w[2] + w[1] + w.substring(3) else w
                else -> {
                    val i = rnd.nextInt(w.length)
                    val n = QwertyProximity.neighboursOf(w[i])
                    if (n.isEmpty()) w else w.substring(0, i) + n[0] + w.substring(i + 1)
                }
            }
        }
    }

    private fun p95Millis(
        engine: HoheEnglishEngine,
        list: List<String>,
        user: UserDataSnapshot,
    ): Double {
        val times = LongArray(list.size)
        list.forEachIndexed { i, s ->
            val t = System.nanoTime()
            engine.suggest(SuggestRequest(i.toLong(), s, proximity = QwertyProximity.forWord(s)), user)
            times[i] = System.nanoTime() - t
        }
        times.sort()
        return times[(times.size * 95) / 100 - 1] / 1_000_000.0
    }

    @Test
    fun suggestP95Within15ms() {
        val engine = TestData.engine
        val list = inputs()
        val user = UserDataSnapshot(englishUnigrams = (0 until 2000).associate { "userword$it" to (it % 7 + 1) })
        repeat(3) { list.forEach { engine.suggest(SuggestRequest(0, it), user) } } // warm-up / JIT
        // Best of three runs keeps the test robust against a busy CI machine.
        val p95 = (1..3).minOf { p95Millis(engine, list, user) }
        println("EnglishPerfTest: suggest p95 = ${"%.2f".format(p95)} ms over ${list.size} inputs")
        assertTrue("p95 $p95 ms > 15 ms", p95 <= 15.0)
    }
}
