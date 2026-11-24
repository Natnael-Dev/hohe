package app.hohe.keyboard.amharic.engine

import app.hohe.keyboard.amharic.dict.AmharicDictionary
import app.hohe.keyboard.amharic.dict.Fidel
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * `suggest` p95 ≤ 10 ms over 1000 realistic inputs (architecture §3 budget). Uses the shipped
 * dictionary when present, otherwise a generated 80k-word dictionary of the same shape.
 * Warm-up first, then up to 3 rounds; the best round must meet the budget (robust on busy CI).
 */
class EnginePerfTest {
    private val base =
        listOf(
            "selam",
            "melkam",
            "endet",
            "amesegnalehu",
            "abebe",
            "dexter",
            "betam",
            "eshi",
            "ene",
            "anchi",
            "ante",
            "yelem",
            "alew",
            "new",
            "ishi",
            "tenastilign",
            "dehna",
            "nesh",
            "nachihu",
            "ewedihalehu",
            "konjo",
            "gize",
            "sew",
            "ager",
            "ethiopia",
            "addis",
            "abeba",
            "lij",
            "bet",
            "mist",
            "bal",
            "wuha",
            "dabo",
            "buna",
            "shai",
            "chigir",
            "yikirta",
            "zare",
            "nege",
            "tilant",
            "mata",
            "keresat",
            "sira",
            "temari",
            "astemari",
            "mengist",
            "hager",
            "hizb",
            "tsehay",
            "chereka",
            "kokeb",
            "semay",
            "midr",
            "wenz",
            "bahir",
            "terara",
            "fikir",
            "tesfa",
            "selamta",
            "enkuan",
            "des",
            "alegn",
            "yihe",
            "ya",
        )

    private val inputs: List<String> =
        buildList {
            val prefixes = base.flatMap { w -> (1..w.length).map { w.substring(0, it) } }
            var k = 0
            while (size < 1000) add(prefixes[k++ % prefixes.size])
        }

    private fun syntheticDict(): AmharicDictionary {
        val rnd = Random(42)
        val fidels = (Fidel.FIRST..Fidel.LAST).map { it.toChar() }.filter { Fidel.isMapped(it) }
        // weight common orders: bias towards 1st, 4th and 6th order fidels like real text
        val common = fidels.filter { (it.code - Fidel.FIRST) % 8 in setOf(0, 3, 5) }
        val words = HashMap<String, Long>()
        while (words.size < 80_000) {
            val len = 2 + rnd.nextInt(6)
            val sb = StringBuilder()
            repeat(len) { sb.append(if (rnd.nextInt(4) == 0) fidels.random(rnd) else common.random(rnd)) }
            words[sb.toString()] = (1_000_000L / (1 + words.size)).coerceAtLeast(2L)
        }
        return TestDicts.dict(words)
    }

    @Test
    fun suggestP95Within10ms() {
        val dict = TestDicts.assetDict() ?: syntheticDict()
        val engine = HoheAmharicEngine(dict)
        val user = UserDataSnapshot.EMPTY
        repeat(3) { for (s in inputs) engine.suggest(SuggestRequest(0, s), user) }

        var best = Long.MAX_VALUE
        repeat(3) {
            val times = LongArray(inputs.size)
            for ((i, s) in inputs.withIndex()) {
                val t0 = System.nanoTime()
                engine.suggest(SuggestRequest(i.toLong(), s), user)
                times[i] = System.nanoTime() - t0
            }
            times.sort()
            val p95 = times[(times.size * 95) / 100]
            println(
                "EnginePerf words=${dict.wordCount} p50=${times[times.size / 2] / 1000}us " +
                    "p95=${p95 / 1000}us max=${times.last() / 1000}us",
            )
            best = minOf(best, p95)
            if (best <= BUDGET_NANOS) return@repeat
        }
        assertTrue("p95 ${best / 1000} us > 10 ms", best <= BUDGET_NANOS)
    }

    private companion object {
        const val BUDGET_NANOS = 10_000_000L
    }
}
