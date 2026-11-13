package app.hohe.keyboard

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ManifestDeclarationTest {
    @Test
    fun testHoheInputMethodServiceDeclaredWithBindInputMethodPermission() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val packageManager = context.packageManager
        val componentName = ComponentName(context, HoheInputMethodService::class.java)
        val serviceInfo = packageManager.getServiceInfo(componentName, PackageManager.GET_META_DATA)

        assertNotNull("HoheInputMethodService must be declared in manifest", serviceInfo)
        assertEquals(
            "Service must require android.permission.BIND_INPUT_METHOD",
            "android.permission.BIND_INPUT_METHOD",
            serviceInfo.permission,
        )
    }
}
