package app.hohe.keyboard.data

import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import app.hohe.keyboard.core.settings.HandwritingSpeed
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.ThemeId
import app.hohe.keyboard.core.settings.TypingLanguage
import app.hohe.keyboard.core.settings.TypingStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.reflect.Modifier

class SettingsStoreTest {
    @get:Rule val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private val file: File by lazy { File(tmp.root, "datastore/settings.preferences_pb") }

    @After
    fun tearDown() {
        closeAll()
    }

    private fun closeAll() {
        runBlocking { scopes.forEach { it.coroutineContext[Job]!!.cancelAndJoin() } }
        scopes.clear()
    }

    /** DataStore allows one active instance per file: close the previous one before reopening. */
    private fun open(): DataStoreSettingsStore {
        closeAll()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scopes += scope
        return DataStoreSettingsStore.create({ file }, scope)
    }

    private fun DataStoreSettingsStore.awaitValue(expected: KeyboardSettings): KeyboardSettings =
        runBlocking { withTimeout(5_000) { settings.first { it == expected } } }

    @Test
    fun nonDefaultFixtureDiffersFromDefaultInEveryField() {
        val fields = settingsFields()
        val defaults = KeyboardSettings()
        for (f in fields) {
            assertNotEquals("field ${f.name} must be non-default in fixture", f.get(defaults), f.get(NON_DEFAULT))
        }
        assertEquals("fixture must already be sanitized", NON_DEFAULT, NON_DEFAULT.sanitized())
        assertEquals("one persisted key per field", fields.size, SettingsCodec.allKeys.size)
        assertEquals(
            "keys are unique",
            fields.size,
            SettingsCodec.allKeys
                .map { it.name }
                .toSet()
                .size,
        )
    }

    @Test
    fun startsWithDefaults() {
        val store = open()
        assertEquals(KeyboardSettings(), store.settings.value)
    }

    @Test
    fun everyFieldRoundTripsThroughDisk() =
        runBlocking {
            val store = open()
            store.update { NON_DEFAULT }
            assertEquals(NON_DEFAULT, store.settings.value)

            val reopened = open()
            assertEquals(NON_DEFAULT, reopened.awaitValue(NON_DEFAULT))
        }

    @Test
    fun updateTransformsCurrentValueAndSanitizes() =
        runBlocking {
            val store = open()
            store.update { it.copy(sound = true) }
            store.update { it.copy(heightMultiplier = 5f, longPressDelayMs = 10) }
            val v = store.settings.value
            assertTrue(v.sound)
            assertEquals(KeyboardSettings.HEIGHT_MAX, v.heightMultiplier)
            assertEquals(KeyboardSettings.LONG_PRESS_MIN_MS, v.longPressDelayMs)
        }

    @Test
    fun resetRestoresDefaultsOnDisk() =
        runBlocking {
            val store = open()
            store.update { NON_DEFAULT }
            store.reset()
            assertEquals(KeyboardSettings(), store.settings.value)

            val reopened = open()
            // An identity update reads the file, so the value below comes from disk.
            reopened.update { it }
            assertEquals(KeyboardSettings(), reopened.settings.value)
        }

    @Test
    fun unknownEnumNamesReadAsDefaults() {
        val prefs =
            preferencesOf(
                stringPreferencesKey("theme_selected") to "NO_SUCH_THEME",
                stringPreferencesKey("typing_language") to "KLINGON",
                stringPreferencesKey("handwriting_speed_name") to "",
            )
        val v = SettingsCodec.decode(prefs)
        assertEquals(ThemeId.AUTO, v.theme)
        assertEquals(TypingLanguage.AMHARIC, v.language)
        assertEquals(HandwritingSpeed.NORMAL, v.handwritingSpeed)
    }

    private fun settingsFields() =
        KeyboardSettings::class.java.declaredFields.filterNot { Modifier.isStatic(it.modifiers) }.onEach {
            it.isAccessible =
                true
        }

    companion object {
        val NON_DEFAULT =
            KeyboardSettings(
                heightMultiplier = 1.12f,
                keyTextSize = 1.3f,
                bottomSpaceDp = 24,
                keyBorders = false,
                sound = true,
                soundLevel = 80,
                vibrate = false,
                vibrationMs = 20,
                emojiRow = false,
                emojiSuggestions = false,
                numberRow = true,
                longPressForSymbols = false,
                longPressDelayMs = 500,
                selectSuggestionWithSpace = false,
                amharicPredictions = false,
                autoSpacePunctuation = false,
                autoCap = false,
                doubleSpacePeriod = true,
                keyPopup = false,
                nextWordSuggestions = false,
                handwritingSpeed = HandwritingSpeed.FAST,
                spaceCursorControl = false,
                swipeToDelete = false,
                highContrast = true,
                highContrastTheme = ThemeId.HC_WHITE,
                theme = ThemeId.BUNA,
                photoBrightness = 0.5f,
                language = TypingLanguage.ENGLISH,
                typingStyle = TypingStyle.HANDWRITING,
                clipboardEnabled = false,
                handwritingOptIn = true,
            )
    }
}
