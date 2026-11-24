package app.hohe.keyboard.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hohe.keyboard.R
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.ThemeId
import app.hohe.keyboard.data.SettingsStore
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** One row in a settings section; search, reset and screens all derive from this table. */
sealed interface SettingDef {
    @get:StringRes val title: Int

    data class Toggle(
        override val title: Int,
        @StringRes val summary: Int?,
        val get: (KeyboardSettings) -> Boolean,
        val set: (KeyboardSettings, Boolean) -> KeyboardSettings,
        val enabledWhen: (KeyboardSettings) -> Boolean = { true },
    ) : SettingDef

    data class Slider(
        override val title: Int,
        val range: ClosedFloatingPointRange<Float>,
        val steps: Int,
        val get: (KeyboardSettings) -> Float,
        val set: (KeyboardSettings, Float) -> KeyboardSettings,
        val label: (Float) -> String,
        val enabledWhen: (KeyboardSettings) -> Boolean = { true },
    ) : SettingDef

    data class Choice<T>(
        override val title: Int,
        val options: List<Pair<String, T>>,
        val get: (KeyboardSettings) -> T,
        val set: (KeyboardSettings, T) -> KeyboardSettings,
        val enabledWhen: (KeyboardSettings) -> Boolean = { true },
    ) : SettingDef

    data class Link(
        override val title: Int,
        @StringRes val summary: Int?,
        val target: Screen,
    ) : SettingDef
}

enum class SettingSection(
    @StringRes val title: Int,
    @StringRes val summary: Int,
) {
    HEIGHT(R.string.sec_height, R.string.sec_height_sub),
    SOUND(R.string.sec_sound, R.string.sec_sound_sub),
    EMOJI(R.string.sec_emoji, R.string.sec_emoji_sub),
    TYPING(R.string.sec_typing, R.string.sec_typing_sub),
    GESTURES(R.string.sec_gestures, R.string.sec_gestures_sub),
    ACCESSIBILITY(R.string.sec_accessibility, R.string.sec_accessibility_sub),
    ;

    val rows: List<SettingDef> get() = SettingsTable.rows(this)
}

object SettingsTable {
    fun rows(section: SettingSection): List<SettingDef> =
        when (section) {
            SettingSection.HEIGHT ->
                listOf(
                    SettingDef.Choice(
                        R.string.set_height,
                        KeyboardSettings.HEIGHT_PRESETS.map { it.key to it.value },
                        { s -> KeyboardSettings.HEIGHT_PRESETS.values.minByOrNull { kotlin.math.abs(it - s.heightMultiplier) }!! },
                        { s, v -> s.copy(heightMultiplier = v) },
                    ),
                    SettingDef.Slider(
                        R.string.set_text_size,
                        0f..(KeyboardSettings.TEXT_SIZE_STEPS.size - 1).toFloat(),
                        KeyboardSettings.TEXT_SIZE_STEPS.size - 2,
                        { s ->
                            KeyboardSettings.TEXT_SIZE_STEPS
                                .indexOfFirst { it >= s.keyTextSize - 0.001f }
                                .coerceAtLeast(0)
                                .toFloat()
                        },
                        { s, v -> s.copy(keyTextSize = KeyboardSettings.TEXT_SIZE_STEPS[v.roundToInt()]) },
                        { v -> "${(KeyboardSettings.TEXT_SIZE_STEPS[v.roundToInt()] * 100).roundToInt()}%" },
                    ),
                    SettingDef.Slider(
                        R.string.set_bottom_space,
                        0f..KeyboardSettings.BOTTOM_SPACE_MAX_DP.toFloat(),
                        KeyboardSettings.BOTTOM_SPACE_MAX_DP / 4 - 1,
                        { s -> s.bottomSpaceDp.toFloat() },
                        { s, v -> s.copy(bottomSpaceDp = v.roundToInt()) },
                        { v -> if (v < 1f) "Off" else "${v.roundToInt()} dp" },
                    ),
                    SettingDef.Toggle(
                        R.string.set_key_borders,
                        R.string.set_key_borders_sub,
                        { it.keyBorders },
                        { s, v -> s.copy(keyBorders = v) },
                    ),
                )
            SettingSection.SOUND ->
                listOf(
                    SettingDef.Toggle(R.string.set_sound, R.string.set_sound_sub, { it.sound }, { s, v -> s.copy(sound = v) }),
                    SettingDef.Slider(
                        R.string.set_sound_level,
                        0f..100f,
                        19,
                        { it.soundLevel.toFloat() },
                        { s, v -> s.copy(soundLevel = v.roundToInt()) },
                        { v -> "${v.roundToInt()}" },
                        enabledWhen = { it.sound },
                    ),
                    SettingDef.Toggle(R.string.set_vibrate, R.string.set_vibrate_sub, { it.vibrate }, { s, v -> s.copy(vibrate = v) }),
                    SettingDef.Choice(
                        R.string.set_vibration_level,
                        listOf("System" to -1, "10 ms" to 10, "20 ms" to 20, "35 ms" to 35, "50 ms" to 50),
                        { it.vibrationMs },
                        { s, v -> s.copy(vibrationMs = v) },
                        enabledWhen = { it.vibrate },
                    ),
                )
            SettingSection.EMOJI ->
                listOf(
                    SettingDef.Toggle(
                        R.string.set_emoji_row,
                        R.string.set_emoji_row_sub,
                        { it.emojiRow },
                        { s, v -> s.copy(emojiRow = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_emoji_suggestions,
                        R.string.set_emoji_suggestions_sub,
                        { it.emojiSuggestions },
                        { s, v -> s.copy(emojiSuggestions = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_number_row,
                        R.string.set_number_row_sub,
                        { it.numberRow },
                        { s, v -> s.copy(numberRow = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_long_press_symbols,
                        R.string.set_long_press_symbols_sub,
                        { it.longPressForSymbols },
                        { s, v -> s.copy(longPressForSymbols = v) },
                    ),
                )
            SettingSection.TYPING ->
                listOf(
                    SettingDef.Link(R.string.set_user_words, R.string.set_user_words_sub, Screen.UserWords),
                    SettingDef.Toggle(
                        R.string.set_space_selects,
                        R.string.set_space_selects_sub,
                        { it.selectSuggestionWithSpace },
                        { s, v -> s.copy(selectSuggestionWithSpace = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_amharic_predictions,
                        R.string.set_amharic_predictions_sub,
                        { it.amharicPredictions },
                        { s, v -> s.copy(amharicPredictions = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_autospace,
                        R.string.set_autospace_sub,
                        { it.autoSpacePunctuation },
                        { s, v -> s.copy(autoSpacePunctuation = v) },
                    ),
                    SettingDef.Toggle(R.string.set_autocap, R.string.set_autocap_sub, { it.autoCap }, { s, v -> s.copy(autoCap = v) }),
                    SettingDef.Toggle(
                        R.string.set_double_space,
                        R.string.set_double_space_sub,
                        { it.doubleSpacePeriod },
                        { s, v -> s.copy(doubleSpacePeriod = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_key_popup,
                        R.string.set_key_popup_sub,
                        { it.keyPopup },
                        { s, v -> s.copy(keyPopup = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_next_word,
                        R.string.set_next_word_sub,
                        { it.nextWordSuggestions },
                        { s, v -> s.copy(nextWordSuggestions = v) },
                    ),
                    SettingDef.Slider(
                        R.string.set_long_press_delay,
                        KeyboardSettings.LONG_PRESS_MIN_MS.toFloat()..1000f,
                        16,
                        { it.longPressDelayMs.toFloat().coerceAtMost(1000f) },
                        { s, v -> s.copy(longPressDelayMs = (v / 50f).roundToInt() * 50) },
                        { v -> "${((v / 50f).roundToInt() * 50)} ms" },
                    ),
                )
            SettingSection.GESTURES ->
                listOf(
                    SettingDef.Toggle(
                        R.string.set_space_cursor,
                        R.string.set_space_cursor_sub,
                        { it.spaceCursorControl },
                        { s, v -> s.copy(spaceCursorControl = v) },
                    ),
                    SettingDef.Toggle(
                        R.string.set_swipe_delete,
                        R.string.set_swipe_delete_sub,
                        { it.swipeToDelete },
                        { s, v -> s.copy(swipeToDelete = v) },
                    ),
                )
            SettingSection.ACCESSIBILITY ->
                listOf(
                    SettingDef.Toggle(
                        R.string.set_high_contrast,
                        R.string.set_high_contrast_sub,
                        { it.highContrast },
                        { s, v -> s.copy(highContrast = v) },
                    ),
                    SettingDef.Choice(
                        R.string.set_high_contrast_theme,
                        listOf("Yellow on black" to ThemeId.HC_YELLOW, "White on black" to ThemeId.HC_WHITE),
                        { it.highContrastTheme },
                        { s, v -> s.copy(highContrastTheme = v) },
                        enabledWhen = { it.highContrast },
                    ),
                )
        }
}

// ---- screens ----

@Composable
fun ScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            val back = stringResource(R.string.back)
            Box(
                Modifier.size(56.dp).clickable(onClick = onBack).semantics { contentDescription = back },
                contentAlignment = Alignment.Center,
            ) { Text("←", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface) }
            Text(title, style = MaterialTheme.typography.titleLarge)
        }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)).padding(vertical = 4.dp),
        content = content,
    )
}

@Composable
fun SettingsTab(navigate: (Screen) -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
                .clickable { navigate(Screen.Search) }
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) { Text(stringResource(R.string.search_settings), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Card {
            SettingSection.entries.forEachIndexed { i, s ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavRow(stringResource(s.title), stringResource(s.summary)) { navigate(Screen.Section(s)) }
            }
        }
        Card {
            NavRow(stringResource(R.string.clear_data_title), stringResource(R.string.clear_data_sub)) { navigate(Screen.ClearData) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.help_title), stringResource(R.string.help_sub)) { navigate(Screen.Help) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            NavRow(stringResource(R.string.about_title), stringResource(R.string.about_sub)) { navigate(Screen.About) }
        }
    }
}

@Composable
fun NavRow(
    title: String,
    summary: String?,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 16.sp)
            if (summary != null) Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Text("›", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionScreen(
    section: SettingSection,
    settings: KeyboardSettings,
    store: SettingsStore,
    onBack: () -> Unit,
    navigate: (Screen) -> Unit,
) {
    ScreenScaffold(stringResource(section.title), onBack) {
        Card {
            section.rows.forEachIndexed { i, def ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(def, settings, store, navigate)
            }
        }
        TryKeyboardField()
    }
}

@Composable
fun SettingRow(
    def: SettingDef,
    settings: KeyboardSettings,
    store: SettingsStore,
    navigate: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()

    fun save(t: (KeyboardSettings) -> KeyboardSettings) = scope.launch { store.update(t) }
    val rowMod = Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 10.dp)
    when (def) {
        is SettingDef.Toggle -> {
            val on = def.get(settings)
            val enabled = def.enabledWhen(settings)
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled) { save { def.set(it, !on) } }.then(rowMod),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(def.title), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    def.summary?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
                }
                Switch(checked = on, enabled = enabled, onCheckedChange = { v -> save { def.set(it, v) } })
            }
        }
        is SettingDef.Slider -> {
            val stored = def.get(settings)
            var local by remember(stored) { mutableFloatStateOf(stored) }
            val enabled = def.enabledWhen(settings)
            Column(rowMod) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(def.title), fontWeight = FontWeight.Medium, fontSize = 16.sp, modifier = Modifier.weight(1f))
                    Text(def.label(local), color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = local,
                    onValueChange = { local = it },
                    onValueChangeFinished = { save { def.set(it, local) } },
                    valueRange = def.range,
                    steps = def.steps,
                    enabled = enabled,
                )
            }
        }
        is SettingDef.Choice<*> -> ChoiceRow(def, settings, rowMod) { t -> save(t) }
        is SettingDef.Link -> NavRow(stringResource(def.title), def.summary?.let { stringResource(it) }) { navigate(def.target) }
    }
}

@Composable
private fun <T> ChoiceRow(
    def: SettingDef.Choice<T>,
    settings: KeyboardSettings,
    modifier: Modifier,
    save: ((KeyboardSettings) -> KeyboardSettings) -> Unit,
) {
    val current = def.get(settings)
    val enabled = def.enabledWhen(settings)
    Column(modifier) {
        Text(stringResource(def.title), fontWeight = FontWeight.Medium, fontSize = 16.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
            def.options.forEach { (label, value) ->
                FilterChip(
                    selected = value == current,
                    enabled = enabled,
                    onClick = { save { s -> def.set(s, value) } },
                    label = { Text(label) },
                    modifier = Modifier.heightIn(min = 40.dp),
                )
            }
        }
    }
}

/** Local search over setting titles (REQ-082). Nothing about searches is stored. */
@Composable
fun SearchScreen(
    settings: KeyboardSettings,
    store: SettingsStore,
    onBack: () -> Unit,
    navigate: (Screen) -> Unit,
) {
    val res = LocalResources.current
    var query by remember { mutableStateOf("") }
    ScreenScaffold(stringResource(R.string.search_settings), onBack) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(stringResource(R.string.search_hint)) },
        )
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            val hits =
                SettingSection.entries.flatMap { s -> s.rows.map { s to it } }.filter { (_, def) ->
                    res.getString(def.title).lowercase().contains(q) ||
                        (def is SettingDef.Toggle && def.summary != null && res.getString(def.summary).lowercase().contains(q))
                }
            if (hits.isEmpty()) {
                Text(stringResource(R.string.search_none), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Card {
                    hits.forEachIndexed { i, (_, def) ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        SettingRow(def, settings, store, navigate)
                    }
                }
            }
        }
    }
}
