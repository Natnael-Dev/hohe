package app.hohe.keyboard.amharic.dict

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FidelTest {
    @Test
    fun wordSkeletonsMergeClassesAndDropVowelSeries() {
        assertEquals("slm", Fidel.skeletonOf("ሰላም"))
        assertEquals("slm", Fidel.skeletonOf("ሠላም"))
        assertEquals("msgnlh", Fidel.skeletonOf("አመሰግናለሁ"))
        assertEquals("ndt", Fidel.skeletonOf("እንዴት"))
        assertEquals("kw", Fidel.skeletonOf("ቋ"))
        assertEquals("lw", Fidel.skeletonOf("ሏ"))
        assertEquals("Zhy", Fidel.skeletonOf("ፀሐይ"))
        assertEquals("gn", Fidel.skeletonOf("ኝ"))
        assertEquals("", Fidel.skeletonOf("ኧ"))
    }

    @Test
    fun unmappedCharactersRejectWord() {
        assertNull(Fidel.skeletonOf("ቐ"))
        assertNull(Fidel.skeletonOf("ሰa"))
        assertNull(Fidel.skeletonOf("ቇ"))
        assertFalse(Fidel.isMapped('፡'))
    }

    @Test
    fun latinSkeletonsMatchWordSkeletons() {
        assertEquals(listOf("msgnlh"), Fidel.latinSkeletons("amesegnalehu"))
        assertEquals(listOf("slm"), Fidel.latinSkeletons("selam"))
        assertEquals("ndt", Fidel.latinSkeletons("endet").first())
        assertEquals(listOf("Zhy"), Fidel.latinSkeletons("tsehay"))
        assertEquals(listOf("kw"), Fidel.latinSkeletons("qwa"))
        assertEquals(listOf("lw"), Fidel.latinSkeletons("lua"))
        assertEquals("Smt", Fidel.latinSkeletons("xemt").first())
    }

    @Test
    fun trailingDigraphStartAddsAlternative() {
        assertEquals(listOf("ms", "mS"), Fidel.latinSkeletons("mes"))
        assertEquals(listOf("bt", "bZ"), Fidel.latinSkeletons("bet"))
        assertEquals(listOf("bS"), Fidel.latinSkeletons("besh"))
    }

    @Test
    fun repeatedConsonantsAlsoGiveCollapsedSkeleton() {
        assertEquals(listOf("dds", "ddS", "ds", "dS"), Fidel.latinSkeletons("addis"))
        assertEquals(listOf("ddd"), Fidel.latinSkeletons("dedede"))
        assertEquals(listOf("kkk", "k"), Fidel.latinSkeletons("kkk"))
    }

    @Test
    fun everyMappedFidelHasSpellings() {
        var mapped = 0
        for (cp in Fidel.FIRST..Fidel.LAST) {
            val ch = cp.toChar()
            if (!Fidel.isMapped(ch)) continue
            mapped++
            val sp = Fidel.spellingsOf(ch)!!
            assertTrue("no spelling for ${Integer.toHexString(cp)}", sp.isNotEmpty())
            assertTrue(sp.none { it.isEmpty() })
        }
        assertTrue("mapped=$mapped", mapped > 250)
    }
}
