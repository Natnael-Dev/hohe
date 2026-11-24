package app.hohe.keyboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.hohe.keyboard.settings.HoheApp

/** Settings app host (REQ-080..088). [EXTRA_SECTION] deep-links from the keyboard. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val section = intent?.getStringExtra(EXTRA_SECTION)
        setContent { HoheApp(initialSection = section) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val section = intent.getStringExtra(EXTRA_SECTION)
        setContent { HoheApp(initialSection = section) }
    }

    companion object {
        const val EXTRA_SECTION = "section"
        const val SECTION_SETTINGS = "settings"
        const val SECTION_VOICE = "voice"
        const val SECTION_HANDWRITING = "handwriting"
    }
}
