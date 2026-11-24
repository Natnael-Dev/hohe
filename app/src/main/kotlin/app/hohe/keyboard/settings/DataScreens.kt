package app.hohe.keyboard.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hohe.keyboard.R
import app.hohe.keyboard.amharic.converter.AmharicConverter
import app.hohe.keyboard.core.engine.UserWord
import app.hohe.keyboard.data.DataCategory
import app.hohe.keyboard.data.HoheData
import app.hohe.keyboard.data.SettingsStore
import kotlinx.coroutines.launch

/** Saved Amharic words: add, list, delete (REQ-025). */
@Composable
fun UserWordsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val personal = remember { HoheData.personal(context) }
    val words by personal.userWords.collectAsState()
    val scope = rememberCoroutineScope()
    var latin by remember { mutableStateOf("") }
    var ethiopic by remember { mutableStateOf("") }
    var edited by remember { mutableStateOf(false) }
    ScreenScaffold(stringResource(R.string.set_user_words), onBack) {
        Text(stringResource(R.string.user_words_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = latin,
            onValueChange = { v ->
                latin = v.filter { it.isLetter() || it == '\'' }.lowercase()
                if (!edited) ethiopic = AmharicConverter.convert(latin)
            },
            label = { Text(stringResource(R.string.user_words_latin)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = ethiopic,
            onValueChange = {
                ethiopic = it.trim()
                edited = true
            },
            label = { Text(stringResource(R.string.user_words_ethiopic)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        val valid = latin.isNotBlank() && ethiopic.isNotBlank() && ethiopic.any { it.code in 0x1200..0x137F }
        Button(
            enabled = valid,
            onClick = {
                val w = UserWord(latin, ethiopic)
                scope.launch { personal.addUserWord(w) }
                latin = ""
                ethiopic = ""
                edited = false
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.add)) }
        if (words.isEmpty()) {
            Text(stringResource(R.string.user_words_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Card {
                words.forEachIndexed { i, w ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(w.ethiopic, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                            Text(w.latin, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { scope.launch { personal.deleteUserWord(w) } }) {
                            Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

/** Clear data with per-category counts and settings reset (REQ-083). */
@Composable
fun ClearDataScreen(
    store: SettingsStore,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val personal = remember { HoheData.personal(context) }
    val scope = rememberCoroutineScope()
    var counts by remember { mutableStateOf<Map<DataCategory, Int>>(emptyMap()) }
    var refresh by remember { mutableStateOf(0) }
    var confirm by remember { mutableStateOf<DataCategory?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var resetDone by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) { counts = personal.counts() }
    val labels =
        mapOf(
            DataCategory.AMHARIC_PICKS to R.string.data_amharic_picks,
            DataCategory.USER_WORDS to R.string.data_user_words,
            DataCategory.ENGLISH_HISTORY to R.string.data_english,
            DataCategory.CLIPBOARD to R.string.data_clipboard,
            DataCategory.EMOJI_RECENTS to R.string.data_emoji,
        )
    ScreenScaffold(stringResource(R.string.clear_data_title), onBack) {
        Text(stringResource(R.string.clear_data_help), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card {
            DataCategory.entries.forEachIndexed { i, cat ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val n = counts[cat] ?: 0
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(labels.getValue(cat)), fontWeight = FontWeight.Medium)
                        Text(
                            pluralStringResource(R.plurals.items, n, n),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(enabled = n > 0, onClick = { confirm = cat }) { Text(stringResource(R.string.clear)) }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.reset_settings))
            }
            if (resetDone) Text(stringResource(R.string.reset_done), color = MaterialTheme.colorScheme.primary)
        }
    }
    confirm?.let { cat ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(labels.getValue(cat))) },
            text = { Text(stringResource(R.string.clear_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        personal.clear(cat)
                        refresh++
                    }
                    confirm = null
                }) { Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.reset_settings)) },
            text = { Text(stringResource(R.string.reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        store.reset()
                        resetDone = true
                    }
                    confirmReset = false
                }) { Text(stringResource(R.string.reset), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
