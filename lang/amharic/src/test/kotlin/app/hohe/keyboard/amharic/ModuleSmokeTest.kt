package app.hohe.keyboard.amharic

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testAmharicModule() {
        assertEquals("AmharicEngine", AmharicEngine.NAME)
    }
}
