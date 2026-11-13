package app.hohe.keyboard.english

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testEnglishModule() {
        assertEquals("EnglishEngine", EnglishEngine.NAME)
    }
}
