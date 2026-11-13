package app.hohe.keyboard.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testCoreModule() {
        assertEquals("KeyboardCore", KeyboardCore.NAME)
    }
}
