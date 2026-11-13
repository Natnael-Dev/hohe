package app.hohe.keyboard

import org.junit.Assert.assertNotNull
import org.junit.Test

class ModuleSmokeTest {
    @Test
    fun testAppModule() {
        val service = HoheInputMethodService()
        assertNotNull(service)
    }
}
