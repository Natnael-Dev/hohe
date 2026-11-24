package app.hohe.keyboard.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class HoheDataTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun storesAreProcessSingletonsWritingTheBackedUpFiles() {
        val settings = HoheData.settings(context)
        assertSame(settings, HoheData.settings(context))
        val personal = HoheData.personal(context)
        assertSame(personal, HoheData.personal(context))

        runBlocking { settings.update { it.copy(sound = true) } }
        assertTrue(settings.settings.value.sound)
        assertTrue(context.filesDir.resolve(DataStoreSettingsStore.BACKUP_PATH).isFile)
    }
}
