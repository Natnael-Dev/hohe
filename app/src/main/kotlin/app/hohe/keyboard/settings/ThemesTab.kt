package app.hohe.keyboard.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hohe.keyboard.R
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.ThemeId
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.core.theme.KeyboardThemes
import app.hohe.keyboard.data.SettingsStore
import kotlinx.coroutines.launch

private val names =
    mapOf(
        ThemeId.AUTO to "Auto",
        ThemeId.DAYLIGHT to "Daylight",
        ThemeId.NIGHT to "Night",
        ThemeId.HIGHLAND to "Highland",
        ThemeId.LAKE_TANA to "Lake Tana",
        ThemeId.BUNA to "Buna",
        ThemeId.TEFF to "Teff",
        ThemeId.SLATE to "Slate",
    )

/** Themes tab (REQ-070, REQ-073): selectable previews drawn from the real keyboard tokens. */
@Composable
fun ThemesTab(
    settings: KeyboardSettings,
    store: SettingsStore,
) {
    val scope = rememberCoroutineScope()
    val dark = isSystemInDarkTheme()
    val choices = names.keys.toList()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (settings.highContrast) {
            Text(stringResource(R.string.themes_hc_note), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        choices.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { id ->
                    val t = KeyboardThemes.resolve(id, dark)
                    ThemeCard(names.getValue(id), t, settings.theme == id, Modifier.weight(1f)) {
                        scope.launch { store.update { it.copy(theme = id, highContrast = false) } }
                    }
                }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .clickable { scope.launch { store.update { it.copy(keyBorders = !it.keyBorders) } } }
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.set_key_borders), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Switch(checked = settings.keyBorders, onCheckedChange = { v -> scope.launch { store.update { it.copy(keyBorders = v) } } })
        }
        TryKeyboardField()
    }
}

@Composable
private fun ThemeCard(
    name: String,
    t: KeyboardTheme,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().background(Color(t.background), RoundedCornerShape(10.dp)).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { Box(Modifier.weight(1f).height(16.dp).background(Color(t.key), RoundedCornerShape(3.dp))) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.weight(1f).height(16.dp).background(Color(t.fnKey), RoundedCornerShape(3.dp)))
                Box(
                    Modifier.weight(3f).height(16.dp).background(Color(t.key), RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("አማርኛ", fontSize = 9.sp, color = Color(t.accent))
                }
                Box(Modifier.weight(1f).height(16.dp).background(Color(t.enter), RoundedCornerShape(3.dp)))
            }
        }
        Text(
            (if (selected) "✓ " else "") + name,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
