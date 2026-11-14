package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class ConverterPropertyTest {
    @Test
    fun p1() {
        assertEquals(AmharicConverter.convert("selam"), AmharicConverter.convert("SeLaM"))
        assertEquals(AmharicConverter.convert("selam"), AmharicConverter.convert("SELAM"))
        assertEquals(AmharicConverter.convert("abebech"), AmharicConverter.convert("Abebech"))
        assertEquals(AmharicConverter.convert("tsehay"), AmharicConverter.convert("TsEhAy"))
    }

    @Test
    fun p2() {
        val rng = Random(42)
        val sb = java.lang.StringBuilder()
        for (i in 0 until 10_000) {
            val len = rng.nextInt(30) + 1
            sb.setLength(0)
            for (j in 0 until len) {
                sb.append(('a'.code + rng.nextInt(26)).toChar())
            }
            val input = sb.toString()
            val output = AmharicConverter.convert(input)
            assertTrue("Output for '$input' must not be empty", output.isNotEmpty())
            for (ch in output) {
                assertFalse("Output for '$input' must not contain ASCII letter: $ch", (ch in 'a'..'z') || (ch in 'A'..'Z'))
            }
            // F-024-05: apply P2 invariant across all nBest texts
            val nbest = AmharicConverter.nBest(input, 4)
            assertTrue("nBest for '$input' must not be empty", nbest.isNotEmpty())
            for (item in nbest) {
                assertTrue("nBest item for '$input' must not be empty", item.text.isNotEmpty())
                for (ch in item.text) {
                    assertFalse("nBest item for '$input' must not contain ASCII letter: $ch", (ch in 'a'..'z') || (ch in 'A'..'Z'))
                }
            }
        }
    }

    @Test
    fun p3() {
        val rng = Random(42)
        val sb = java.lang.StringBuilder()
        for (i in 0 until 10_000) {
            val len = rng.nextInt(30) + 1
            sb.setLength(0)
            for (j in 0 until len) {
                sb.append(('a'.code + rng.nextInt(26)).toChar())
            }
            val input = sb.toString()
            val output = AmharicConverter.convert(input)
            for (ch in output) {
                val cp = ch.code
                assertTrue("Codepoint U+%04X in output of '$input' must be in U+1200..U+137F".format(cp), cp in 0x1200..0x137F)
            }
            // F-024-05: apply P3 invariant across all nBest texts
            val nbest = AmharicConverter.nBest(input, 4)
            for (item in nbest) {
                for (ch in item.text) {
                    val cp = ch.code
                    assertTrue("Codepoint U+%04X in nBest item of '$input' must be in U+1200..U+137F".format(cp), cp in 0x1200..0x137F)
                }
            }
        }
    }

    @Test
    fun p4() {
        // F-024-05: run P4 determinism over all 97 golden fixture inputs
        val stream =
            javaClass.getResourceAsStream("/golden/converter-golden.tsv")
                ?: error("golden fixture missing")
        val lines =
            stream
                .bufferedReader(Charsets.UTF_8)
                .readLines()
                .drop(2)
                .filter { it.isNotBlank() }
        for (line in lines) {
            val input = line.split('\t')[1]
            val expected = AmharicConverter.nBest(input, 4)
            for (i in 0 until 10) {
                val actual = AmharicConverter.nBest(input, 4)
                assertEquals("Run $i for golden input '$input' must match expected n-best list", expected, actual)
            }
        }
    }

    @Test
    fun p6() {
        assertEquals("ትሳ", AmharicConverter.convert("t'sa"))
        assertEquals("ሰአት", AmharicConverter.convert("se'at"))
        assertEquals("አስሁን", AmharicConverter.convert("as'hun"))

        val testCases = listOf("t'sa", "se'at", "as'hun", "'''", "a'b'c'", "'selam'")
        for (tc in testCases) {
            val output = AmharicConverter.convert(tc)
            assertFalse("Apostrophe must never be in output for '$tc': $output", output.contains('\''))
            val nbest = AmharicConverter.nBest(tc, 4)
            for (item in nbest) {
                assertFalse("Apostrophe must never be in nBest output for '$tc': ${item.text}", item.text.contains('\''))
            }
        }
    }
}
