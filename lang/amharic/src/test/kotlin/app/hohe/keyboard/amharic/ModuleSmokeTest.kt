package app.hohe.keyboard.amharic

import app.hohe.keyboard.amharic.converter.AmharicConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testAmharicModule() {
        assertEquals("ሰላም", AmharicConverter.convert("selam"))
    }
}
