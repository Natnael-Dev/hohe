package app.hohe.keyboard.amharic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoldenFixtureTest {
    @Test
    fun rowCount() {
        val stream =
            javaClass.getResourceAsStream("/golden/converter-golden.tsv")
                ?: error("golden fixture /golden/converter-golden.tsv missing from test resources")

        val lines = stream.bufferedReader(Charsets.UTF_8).readLines()
        assertTrue("Fixture must not be empty", lines.isNotEmpty())

        val commentLine = lines[0]
        assertTrue("First line must be source comment", commentLine.startsWith("# source: character-mapping-tests.md sha256="))
        val sha = commentLine.substringAfter("sha256=").trim()
        assertEquals("SHA-256 must match character-mapping-tests.md", 64, sha.length)

        val headerLine = lines[1]
        assertEquals("Header must match spec", "id\tinput\texpected\tnext", headerLine)

        val rows = lines.drop(2).filter { it.isNotBlank() }
        assertEquals("Fixture must have exactly 97 golden rows", 97, rows.size)

        for ((index, row) in rows.withIndex()) {
            val fields = row.split('\t')
            assertEquals("Row $index must have exactly 4 fields", 4, fields.size)
            val expectedId = "G-%03d".format(index + 1)
            assertEquals("Row $index id should be $expectedId", expectedId, fields[0])
            assertFalse("Fixture must never contain 'oracle' data (row $expectedId)", row.contains("oracle", ignoreCase = true))
        }
    }
}
