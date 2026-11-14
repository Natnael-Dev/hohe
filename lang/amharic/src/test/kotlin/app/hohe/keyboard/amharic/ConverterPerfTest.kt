package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Arrays

class ConverterPerfTest {
    @Test
    fun perf() {
        val input = "amesegnalehualemuhunbalebetuan" // 30 letters
        // Warmup: 2,000 runs
        for (i in 0 until 2_000) {
            AmharicConverter.nBest(input, 4)
        }

        // Benchmark: 10,000 runs
        val times = LongArray(10_000)
        for (i in 0 until 10_000) {
            val t0 = System.nanoTime()
            AmharicConverter.nBest(input, 4)
            val t1 = System.nanoTime()
            times[i] = t1 - t0
        }

        Arrays.sort(times)
        val medianNanos = times[5_000]
        val medianMs = medianNanos / 1_000_000.0
        println("ConverterPerfTest: median of 10,000 nBest(30, 4) runs = %.4f ms (median = %d ns)".format(medianMs, medianNanos))

        val isNoisy = System.getProperty("perf.noisy") == "true"
        val thresholdMs = if (isNoisy) 0.5 else 0.2
        if (isNoisy && medianMs > 0.2) {
            println("NOTE: perf.noisy=true allowed median %.4f ms > 0.2 ms (within <= 0.5 ms limit)".format(medianMs))
        }

        assertTrue(
            "Median time %.4f ms must be <= %.1f ms (target <= 0.2 ms)".format(medianMs, thresholdMs),
            medianMs <= thresholdMs,
        )
    }

    @Test
    fun testNBest1000Chars() {
        // F-024-04: 1000-char nBest performance test (< 100 ms)
        val input = "selam".repeat(200) // 1000 characters
        val t0 = System.nanoTime()
        val result = AmharicConverter.nBest(input, 4)
        val elapsedMs = (System.nanoTime() - t0) / 1_000_000.0
        println("ConverterPerfTest: 1000-char nBest took %.2f ms".format(elapsedMs))
        assertTrue("Result must not be empty", result.isNotEmpty())
        assertTrue("1000-char nBest elapsed time %.2f ms must be < 100 ms".format(elapsedMs), elapsedMs < 100.0)
    }
}
