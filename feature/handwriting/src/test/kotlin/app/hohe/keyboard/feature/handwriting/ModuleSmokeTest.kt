package app.hohe.keyboard.feature.handwriting

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testHandwritingModule() {
        assertEquals("Handwriting", HandwritingPlaceholder.NAME)
    }
}
