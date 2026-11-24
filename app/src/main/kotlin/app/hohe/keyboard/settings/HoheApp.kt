package app.hohe.keyboard.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import app.hohe.keyboard.MainActivity
import app.hohe.keyboard.R
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage
import app.hohe.keyboard.data.HoheData
import app.hohe.keyboard.data.SettingsStore
import app.hohe.keyboard.ime.Icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val STATUS_POLL_MS = 700L

/** Navigation targets (application-architecture §3.2). */
sealed interface Screen {
    data class Home(
        val tab: Tab,
    ) : Screen

    data class Section(
        val section: SettingSection,
    ) : Screen

    data object Search : Screen

    data object UserWords : Screen

    data object ClearData : Screen

    data object Help : Screen

    data object About : Screen

    data object Voice : Screen

    data object Handwriting : Screen
}

enum class Tab { LAYOUTS, THEMES, SETTINGS }

/** Whether Hohe is enabled in system settings and chosen as the current keyboard (REQ-080). */
object ImeStatus {
    fun enabled(context: Context): Boolean {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == context.packageName }
    }

    fun isDefault(context: Context): Boolean {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith(context.packageName + "/")
    }
}

@Composable
fun HoheApp(initialSection: String?) {
    HoheTheme {
        val context = LocalContext.current
        val store = remember { HoheData.settings(context) }
        val settings by store.settings.collectAsState()
        var enabled by remember { mutableStateOf(ImeStatus.enabled(context)) }
        var isDefault by remember { mutableStateOf(ImeStatus.isDefault(context)) }
        var onboardingDone by remember { mutableStateOf(enabled && isDefault) }
        OnResume {
            enabled = ImeStatus.enabled(context)
            isDefault = ImeStatus.isDefault(context)
        }
        LaunchedEffect(enabled, isDefault) {
            while (enabled && !isDefault) {
                delay(STATUS_POLL_MS)
                isDefault = ImeStatus.isDefault(context)
            }
        }
        val stack =
            remember {
                mutableStateListOf<Screen>(Screen.Home(Tab.LAYOUTS)).also { s ->
                    when (initialSection) {
                        MainActivity.SECTION_SETTINGS -> s.add(Screen.Home(Tab.SETTINGS))
                        MainActivity.SECTION_VOICE -> s.add(Screen.Voice)
                        MainActivity.SECTION_HANDWRITING -> s.add(Screen.Handwriting)
                    }
                }
            }
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .imePadding(),
        ) {
            if (!onboardingDone && initialSection == null) {
                Onboarding(enabled, isDefault, onDone = { onboardingDone = true })
            } else {
                val top = stack.last()
                BackHandler(enabled = stack.size > 1 || (top is Screen.Home && top.tab != Tab.LAYOUTS)) {
                    if (stack.size > 1) {
                        stack.removeAt(stack.size - 1)
                    } else {
                        stack[0] = Screen.Home(Tab.LAYOUTS)
                    }
                }
                val navigate: (Screen) -> Unit = { stack.add(it) }
                val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.size - 1) }
                when (top) {
                    is Screen.Home ->
                        HomeScreen(top.tab, settings, store, enabled && isDefault, navigate) { tab ->
                            stack[stack.size - 1] = Screen.Home(tab)
                        }
                    is Screen.Section -> SectionScreen(top.section, settings, store, back, navigate)
                    Screen.Search -> SearchScreen(settings, store, back, navigate)
                    Screen.UserWords -> UserWordsScreen(back)
                    Screen.ClearData -> ClearDataScreen(store, back)
                    Screen.Help -> HelpScreen(back)
                    Screen.About -> AboutScreen(back, navigate)
                    Screen.Voice -> VoiceScreen(back)
                    Screen.Handwriting -> HandwritingScreen(back)
                }
            }
        }
    }
}

@Composable
fun OnResume(block: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) block() }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
}

// ---- Onboarding (REQ-080) ----

@Composable
private fun Onboarding(
    enabled: Boolean,
    isDefault: Boolean,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val step =
        when {
            !enabled -> 1
            !isDefault -> 2
            else -> 3
        }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text("ሆሄ", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.onboarding_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        StepCard(1, stringResource(R.string.onboarding_step_enable), step, stringResource(R.string.onboarding_enable_button)) {
            context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        StepCard(2, stringResource(R.string.onboarding_step_choose), step, stringResource(R.string.onboarding_choose_button)) {
            (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
        StepCard(3, stringResource(R.string.onboarding_step_done), step, stringResource(R.string.onboarding_done_button), onDone)
        Text(stringResource(R.string.onboarding_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    }
}

@Composable
private fun StepCard(
    number: Int,
    title: String,
    current: Int,
    action: String,
    onClick: () -> Unit,
) {
    val done = number < current
    val active = number == current
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .then(if (active) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(32.dp)
                .background(
                    if (done || active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (done) "✓" else "$number", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (active) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(action) }
            }
        }
    }
}

// ---- Home (REQ-081) ----

@Composable
private fun HomeScreen(
    tab: Tab,
    settings: KeyboardSettings,
    store: SettingsStore,
    ready: Boolean,
    navigate: (Screen) -> Unit,
    selectTab: (Tab) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(56.dp).padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.app_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            val help = stringResource(R.string.help_title)
            Box(
                Modifier.size(48.dp).clickable { navigate(Screen.Help) }.semantics { contentDescription = help },
                contentAlignment = Alignment.Center,
            ) { Text("?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.LAYOUTS -> LayoutsTab(settings, store, ready, navigate)
                Tab.THEMES -> ThemesTab(settings, store)
                Tab.SETTINGS -> SettingsTab(navigate)
            }
        }
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            NavItem(tab == Tab.LAYOUTS, stringResource(R.string.tab_layouts), Icon.KEYBOARD) { selectTab(Tab.LAYOUTS) }
            NavItem(tab == Tab.THEMES, stringResource(R.string.tab_themes), Icon.EMOJI) { selectTab(Tab.THEMES) }
            NavItem(tab == Tab.SETTINGS, stringResource(R.string.tab_settings), Icon.SETTINGS) { selectTab(Tab.SETTINGS) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    selected: Boolean,
    label: String,
    icon: Icon,
    onClick: () -> Unit,
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            HoheIcon(icon, if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        },
        label = { Text(label) },
    )
}

/** Layouts tab: typing-style cards (REQ-052) and the try-keyboard field (REQ-084). */
@Composable
private fun LayoutsTab(
    settings: KeyboardSettings,
    store: SettingsStore,
    ready: Boolean,
    navigate: (Screen) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!ready) {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.not_default_banner), modifier = Modifier.weight(1f))
                OutlinedButton(onClick = {
                    if (!ImeStatus.enabled(context)) {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } else {
                        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
                    }
                }) { Text(stringResource(R.string.fix)) }
            }
        }
        Text(stringResource(R.string.layouts_heading), style = MaterialTheme.typography.titleMedium)
        StyleRow(
            "abc ➞ አማርኛ",
            stringResource(R.string.style_amharic_sub),
            settings.language == TypingLanguage.AMHARIC,
        ) { scope.launch { store.update { it.copy(language = TypingLanguage.AMHARIC) } } }
        StyleRow("English", stringResource(R.string.style_english_sub), settings.language == TypingLanguage.ENGLISH) {
            scope.launch { store.update { it.copy(language = TypingLanguage.ENGLISH) } }
        }
        StyleRow(stringResource(R.string.style_voice), stringResource(R.string.style_voice_sub), false) { navigate(Screen.Voice) }
        StyleRow(stringResource(R.string.style_handwriting), stringResource(R.string.handwriting_later_short), false) {
            navigate(Screen.Handwriting)
        }
        Spacer(Modifier.height(4.dp))
        TryKeyboardField()
    }
}

@Composable
private fun StyleRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)) else Modifier)
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(16.dp),
    ) {
        Text((if (selected) "✓  " else "") + title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 17.sp)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
    }
}

/** A field that raises the keyboard so changes can be tried right away (REQ-084). */
@Composable
fun TryKeyboardField() {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.try_keyboard)) },
        minLines = 2,
    )
}
