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

        assertTrue(
            "Median time %.4f ms must be <= 0.5 ms (target <= 0.2 ms)".format(medianMs),
            medianMs <= 0.5,
        )
    }
}
