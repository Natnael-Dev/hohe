package app.hohe.keyboard.ime.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hohe.keyboard.MainActivity
import app.hohe.keyboard.R
import app.hohe.keyboard.core.ime.Panel
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.data.ClipItem
import app.hohe.keyboard.data.PersonalDataStore
import app.hohe.keyboard.ime.EngineHost
import kotlinx.coroutines.launch

class PanelCallbacks(
    val onText: (String, Boolean) -> Unit,
    val onDelete: () -> Unit,
    val onSpace: () -> Unit,
    val onClose: () -> Unit,
    val onLanguage: (TypingLanguage) -> Unit,
    val onVoice: () -> Unit,
    val onOpenApp: (String) -> Unit,
    val onClipboardEnabled: (Boolean) -> Unit,
)

private fun Int.c() = Color(this)

/** Non-key surfaces drawn in place of the keys, at the same height (keyboard-engine-architecture §8). */
@Composable
fun ImePanels(
    panel: Panel,
    theme: KeyboardTheme,
    language: TypingLanguage,
    personal: PersonalDataStore,
    engines: EngineHost,
    settings: KeyboardSettings,
    voiceAvailable: Boolean,
    cb: PanelCallbacks,
) {
    Box(Modifier.fillMaxSize().background(theme.background.c())) {
        when (panel) {
            Panel.EMOJI -> EmojiPanel(theme, personal, engines, cb)
            Panel.CLIPBOARD -> ClipboardPanel(theme, personal, settings.clipboardEnabled, cb)
            Panel.CHOOSER -> ChooserPanel(theme, language, settings.handwritingOptIn, voiceAvailable, cb)
            Panel.NONE -> Unit
        }
    }
}

@Composable
private fun PanelFooter(
    theme: KeyboardTheme,
    cb: PanelCallbacks,
    showSpace: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FooterKey(stringResource(R.string.key_letters_short), theme, Modifier.width(72.dp), cb.onClose)
        if (showSpace) FooterKey(" ", theme, Modifier.weight(1f), cb.onSpace, isLetter = true) else Box(Modifier.weight(1f))
        FooterKey("⌫", theme, Modifier.width(72.dp), cb.onDelete, description = stringResource(R.string.key_delete))
    }
}

@Composable
private fun FooterKey(
    label: String,
    theme: KeyboardTheme,
    modifier: Modifier,
    onClick: () -> Unit,
    isLetter: Boolean = false,
    description: String? = null,
) {
    Box(
        modifier
            .height(48.dp)
            .background((if (isLetter) theme.key else theme.fnKey).c(), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .semantics { if (description != null) contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = theme.fnText.c(), fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// ---- Emoji (REQ-060) ----

@Composable
private fun EmojiPanel(
    theme: KeyboardTheme,
    personal: PersonalDataStore,
    engines: EngineHost,
    cb: PanelCallbacks,
) {
    val recents by personal.emojiRecents.collectAsState()
    val categories = remember { engines.emojiCategories() }
    var selected by remember { mutableIntStateOf(if (recents.isEmpty()) 1 else 0) }
    val tabs = listOf(EmojiCategory("recent", "🕘", recents)) + categories
    Column(Modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().height(48.dp)) {
            itemsIndexed(tabs) { i, cat ->
                Box(
                    Modifier
                        .size(48.dp, 48.dp)
                        .clickable { selected = i }
                        .semantics { contentDescription = cat.id },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cat.icon, fontSize = 20.sp)
                    if (i == selected) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .width(28.dp)
                                .height(3.dp)
                                .background(theme.accent.c(), RoundedCornerShape(2.dp)),
                        )
                    }
                }
            }
        }
        val list = tabs.getOrNull(selected)?.emojis.orEmpty()
        if (list.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.emoji_no_recents), color = theme.hint.c(), fontSize = 14.sp)
            }
        } else {
            LazyVerticalGrid(GridCells.Fixed(8), Modifier.weight(1f).fillMaxWidth()) {
                items(list) { e ->
                    Box(
                        Modifier.height(48.dp).clickable { cb.onText(e, true) },
                        contentAlignment = Alignment.Center,
                    ) { Text(e, fontSize = 24.sp) }
                }
            }
        }
        PanelFooter(theme, cb, showSpace = true)
    }
}

data class EmojiCategory(
    val id: String,
    val icon: String,
    val emojis: List<String>,
)

// ---- Clipboard (REQ-063/064) ----

@Composable
private fun ClipboardPanel(
    theme: KeyboardTheme,
    personal: PersonalDataStore,
    enabled: Boolean,
    cb: PanelCallbacks,
) {
    val clips by personal.clipboard.collectAsState()
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.clipboard_title),
                color = theme.text.c(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = cb.onClipboardEnabled,
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = theme.enter.c(),
                        checkedThumbColor = theme.enterText.c(),
                    ),
                modifier = Modifier.semantics { contentDescription = "clipboard history" },
            )
        }
        if (!enabled || clips.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(if (enabled) R.string.clipboard_empty else R.string.clipboard_off),
                    color = theme.hint.c(),
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp)) {
                items(clips, key = { it.id }) { clip ->
                    ClipRow(clip, theme, onPaste = { cb.onText(clip.text, false) }, onPin = {
                        scope.launch { personal.setPinned(clip.id, !clip.pinned) }
                    }, onDelete = { scope.launch { personal.deleteClip(clip.id) } })
                }
            }
        }
        PanelFooter(theme, cb, showSpace = false)
    }
}

@Composable
private fun ClipRow(
    clip: ClipItem,
    theme: KeyboardTheme,
    onPaste: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(theme.key.c(), RoundedCornerShape(8.dp))
            .clickable(onClick = onPaste)
            .padding(start = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            clip.text,
            color = theme.text.c(),
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        )
        val pinLabel = stringResource(if (clip.pinned) R.string.clipboard_unpin else R.string.clipboard_pin)
        Box(
            Modifier.size(48.dp).clickable(onClick = onPin).semantics { contentDescription = pinLabel },
            contentAlignment = Alignment.Center,
        ) { Text(if (clip.pinned) "📌" else "📍", fontSize = 16.sp) }
        val delLabel = stringResource(R.string.clipboard_delete)
        Box(
            Modifier.size(48.dp).clickable(onClick = onDelete).semantics { contentDescription = delLabel },
            contentAlignment = Alignment.Center,
        ) { Text("✕", color = theme.hint.c(), fontSize = 16.sp) }
    }
}

// ---- Typing-style chooser (REQ-052) ----

@Composable
private fun ChooserPanel(
    theme: KeyboardTheme,
    language: TypingLanguage,
    handwritingOptIn: Boolean,
    voiceAvailable: Boolean,
    cb: PanelCallbacks,
) {
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleCard(
                "abc ➞ አማርኛ",
                stringResource(R.string.style_amharic_sub),
                language == TypingLanguage.AMHARIC,
                theme,
                Modifier.weight(1f),
            ) { cb.onLanguage(TypingLanguage.AMHARIC) }
            StyleCard(
                "English",
                stringResource(R.string.style_english_sub),
                language == TypingLanguage.ENGLISH,
                theme,
                Modifier.weight(1f),
            ) { cb.onLanguage(TypingLanguage.ENGLISH) }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StyleCard(
                stringResource(R.string.style_handwriting),
                stringResource(if (handwritingOptIn) R.string.style_handwriting_sub else R.string.handwriting_later_short),
                false,
                theme,
                Modifier.weight(1f),
            ) { cb.onOpenApp(MainActivity.SECTION_HANDWRITING) }
            StyleCard(
                stringResource(R.string.style_voice),
                stringResource(if (voiceAvailable) R.string.style_voice_sub else R.string.voice_unavailable),
                false,
                theme,
                Modifier.weight(1f),
            ) { cb.onVoice() }
        }
        PanelFooter(theme, cb, showSpace = false)
    }
}

@Composable
private fun StyleCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    theme: KeyboardTheme,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(theme.key.c(), RoundedCornerShape(12.dp))
            .then(if (selected) Modifier.border(2.dp, theme.accent.c(), RoundedCornerShape(12.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            (if (selected) "✓ " else "") + title,
            color = theme.text.c(),
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Text(subtitle, color = theme.hint.c(), fontSize = 12.sp, maxLines = 2)
    }
}
