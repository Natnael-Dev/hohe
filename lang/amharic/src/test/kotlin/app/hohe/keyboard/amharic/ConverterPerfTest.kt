package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

class ConverterPerfTest {
    @Test
    fun perf() {
        val input = "amesegnalehualemuhunbalebetuan" // 30 letters
        val isNoisy = System.getProperty("perf.noisy") == "true"
        val thresholdMs = if (isNoisy) 0.5 else 0.2

        var passed = false
        var lastMedianMs = 0.0
        val times = LongArray(10_000)

        for (round in 1..3) {
            // Warmup: 2,000 runs
            for (i in 0 until 2_000) {
                AmharicConverter.nBest(input, 4)
            }

            // Benchmark: 10,000 runs
            for (i in 0 until 10_000) {
                val t0 = System.nanoTime()
                AmharicConverter.nBest(input, 4)
                val t1 = System.nanoTime()
                times[i] = t1 - t0
            }

            Arrays.sort(times)
            val medianNanos = times[5_000]
            val medianMs = medianNanos / 1_000_000.0
            lastMedianMs = medianMs
            println(
                "ConverterPerfTest round $round/3: median of 10,000 nBest(30, 4) runs = %.4f ms (median = %d ns)".format(
                    medianMs,
                    medianNanos,
                ),
            )

            if (medianMs <= thresholdMs) {
                if (isNoisy && medianMs > 0.2) {
                    println("NOTE: perf.noisy=true allowed median %.4f ms > 0.2 ms (within <= 0.5 ms limit)".format(medianMs))
                }
                passed = true
                break
            } else if (round < 3) {
                Thread.sleep(1000)
            }
        }

        assertTrue(
            "Median time %.4f ms must be <= %.1f ms (target <= 0.2 ms) within 3 rounds".format(lastMedianMs, thresholdMs),
            passed,
        )
    }

    @Test
    fun testNBest1000Chars() {
        // F-024-04: 1000-char nBest performance test (< 100 ms)
        val input = "selam".repeat(200) // 1000 characters

        // Warmup: 20 calls
        for (i in 0 until 20) {
            AmharicConverter.nBest(input, 4)
        }

        // Benchmark: 21 timed calls
        val times = LongArray(21)
        var result = emptyList<AmharicConverter.Scored>()
        for (i in 0 until 21) {
            val t0 = System.nanoTime()
            result = AmharicConverter.nBest(input, 4)
            val t1 = System.nanoTime()
            times[i] = t1 - t0
        }

        Arrays.sort(times)
        val medianNanos = times[10]
        val medianMs = medianNanos / 1_000_000.0
        println("ConverterPerfTest: 1000-char nBest median of 21 runs = %.2f ms (%d ns)".format(medianMs, medianNanos))
        assertTrue("Result must not be empty", result.isNotEmpty())
        assertTrue("1000-char nBest median elapsed time %.2f ms must be < 100 ms".format(medianMs), medianMs < 100.0)
    }
}
