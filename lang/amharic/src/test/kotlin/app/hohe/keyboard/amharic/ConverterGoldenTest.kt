package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.fail
import org.junit.Test

class ConverterGoldenTest {
    data class GoldenRow(
        val id: String,
        val input: String,
        val expected: String,
        val next: List<String>,
    )

    companion object {
        fun loadRows(): List<GoldenRow> {
            val stream =
                ConverterGoldenTest::class.java.getResourceAsStream("/golden/converter-golden.tsv")
                    ?: error("Missing /golden/converter-golden.tsv resource")
            return stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines
                    .map { it.trim() }
                    .filter { it.startsWith("G-") }
                    .map { line ->
                        val parts = line.split("\t")
                        val id = parts[0]
                        val input = parts[1]
                        val expected = parts[2]
                        val next =
                            if (parts.size > 3 && parts[3].isNotBlank()) {
                                parts[3].split("·").map { it.trim() }
                            } else {
                                emptyList()
                            }
                        GoldenRow(id, input, expected, next)
                    }.toList()
            }
        }
    }

    @Test
    fun slot0() {
        val rows = loadRows()
        val failures = mutableListOf<String>()
        for (row in rows) {
            val actual = AmharicConverter.convert(row.input)
            if (actual != row.expected) {
                failures.add("${row.id} input='${row.input}' expected='${row.expected}' actual='$actual'")
            }
        }
        if (failures.isNotEmpty()) {
            fail("Slot 0 failures (${failures.size}/${rows.size}):\n" + failures.joinToString("\n"))
        }
    }

    @Test
    fun nextRanked() {
        val rows = loadRows()
        val failures = mutableListOf<String>()
        for (row in rows) {
            val actualNext = AmharicConverter.nBest(row.input, 4).drop(1).map { it.text }
            if (actualNext != row.next) {
                failures.add("${row.id} input='${row.input}' expected='${row.next}' actual='$actualNext'")
            }
        }
        if (failures.isNotEmpty()) {
            fail("Next ranked failures (${failures.size}/${rows.size}):\n" + failures.joinToString("\n"))
        }
    }
}
