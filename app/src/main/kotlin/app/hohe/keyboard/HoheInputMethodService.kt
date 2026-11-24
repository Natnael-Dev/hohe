package app.hohe.keyboard

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.hohe.keyboard.core.field.FieldPolicy
import app.hohe.keyboard.core.field.SuggestMode
import app.hohe.keyboard.core.ime.ImeHost
import app.hohe.keyboard.core.ime.InputController
import app.hohe.keyboard.core.ime.KeyboardUiState
import app.hohe.keyboard.core.ime.Panel
import app.hohe.keyboard.core.ime.StripItem
import app.hohe.keyboard.core.layout.KeyCodes
import app.hohe.keyboard.core.layout.KeyboardPage
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.theme.KeyboardTheme
import app.hohe.keyboard.core.theme.KeyboardThemes
import app.hohe.keyboard.core.time.Clock
import app.hohe.keyboard.data.HoheData
import app.hohe.keyboard.data.PersonalDataStore
import app.hohe.keyboard.data.SettingsStore
import app.hohe.keyboard.ime.AndroidEditor
import app.hohe.keyboard.ime.EmojiRowView
import app.hohe.keyboard.ime.EngineHost
import app.hohe.keyboard.ime.KeyActionListener
import app.hohe.keyboard.ime.KeyFeedback
import app.hohe.keyboard.ime.KeyboardView
import app.hohe.keyboard.ime.PopupOverlay
import app.hohe.keyboard.ime.StripListener
import app.hohe.keyboard.ime.StripView
import app.hohe.keyboard.ime.ToolbarAction
import app.hohe.keyboard.ime.VoiceInput
import app.hohe.keyboard.ime.panels.ImePanels
import app.hohe.keyboard.ime.panels.PanelCallbacks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Lifecycle adapter for the keyboard (keyboard-engine-architecture §8). Typing rules live in
 * [InputController]; this class only connects Android callbacks, views, storage and engines.
 */
class HoheInputMethodService :
    InputMethodService(),
    LifecycleOwner,
    SavedStateRegistryOwner,
    ImeHost,
    KeyActionListener,
    StripListener {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsStore: SettingsStore
    private lateinit var personal: PersonalDataStore
    private lateinit var engines: EngineHost
    private lateinit var editor: AndroidEditor
    private lateinit var controller: InputController
    private lateinit var feedback: KeyFeedback
    private lateinit var voice: VoiceInput

    private var root: FrameLayout? = null
    private var strip: StripView? = null
    private var emojiRow: EmojiRowView? = null
    private var keyboard: KeyboardView? = null
    private var panelView: ComposeView? = null
    private var shownPanel = Panel.NONE

    private var settings = KeyboardSettings()
    private var theme: KeyboardTheme = KeyboardThemes.DAYLIGHT
    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { captureClip() }

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        settingsStore = HoheData.settings(this)
        personal = HoheData.personal(this)
        engines = EngineHost(this).also { it.preload() }
        editor = AndroidEditor(this)
        feedback = KeyFeedback(this)
        voice = VoiceInput(this) { text -> controller.onText(text) }
        controller =
            InputController(
                editor = editor,
                host = this,
                clock = UptimeClock,
                engines = engines,
                userData = { personal },
                scheduler = engines,
                onLanguageChanged = { lang -> scope.launch { settingsStore.update { it.copy(language = lang) } } },
            )
        controller.listener = { render(it) }
        settings = settingsStore.settings.value
        theme = resolveTheme(settings)
        controller.applySettings(settings)
        scope.launch {
            settingsStore.settings.collect { s ->
                settings = s
                theme = resolveTheme(s)
                controller.applySettings(s)
            }
        }
        scope.launch { personal.emojiRecents.collect { emojiRow?.bind(visibleRecents(it), theme) } }
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    override fun onCreateInputView(): View {
        window.window?.decorView?.let { decor ->
            decor.setViewTreeLifecycleOwner(this)
            decor.setViewTreeSavedStateRegistryOwner(this)
        }
        val overlayView = PopupOverlay(this)
        val stripView = StripView(this).also { it.listener = this }
        val row =
            EmojiRowView(this).also {
                it.onEmoji = { e ->
                    controller.onText(e)
                    recordEmoji(e)
                }
                it.onMore = { controller.openPanel(Panel.EMOJI) }
            }
        val kb = KeyboardView(this, overlayView).also { it.listener = this }
        val panel =
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                visibility = View.GONE
            }
        val keyArea =
            FrameLayout(this).apply {
                addView(kb, FrameLayout.LayoutParams(MATCH, ViewGroup.LayoutParams.WRAP_CONTENT))
                addView(panel, FrameLayout.LayoutParams(MATCH, MATCH))
            }
        val column =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(stripView)
                addView(row)
                addView(keyArea)
            }
        val frame =
            FrameLayout(this).apply {
                addView(column)
                addView(overlayView, FrameLayout.LayoutParams(MATCH, MATCH))
            }
        root = frame
        strip = stripView
        emojiRow = row
        keyboard = kb
        panelView = panel
        shownPanel = Panel.NONE
        row.bind(visibleRecents(personal.emojiRecents.value), theme)
        render(controller.state)
        return frame
    }

    override fun onStartInputView(
        info: EditorInfo,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        editor.resetTracking()
        editor.selStart = info.initialSelStart
        editor.selEnd = info.initialSelEnd
        theme = resolveTheme(settings)
        voice.refreshAvailability()
        controller.voiceAvailable = voice.available
        controller.startInput(FieldPolicy.classify(info.inputType, info.imeOptions))
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        controller.finishInput()
        voice.cancel()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onFinishInputView(finishingInput)
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        editor.selStart = newSelStart
        editor.selEnd = newSelEnd
        val ownEdit = editor.consumeOwnSelectionUpdate()
        val composing = controller.composingText.isNotEmpty()
        if (composing) {
            // While composing, the composing region is the ground truth: the cursor must sit at its end.
            val atComposingEnd = candidatesEnd >= 0 && candidatesEnd == newSelEnd && newSelStart == newSelEnd
            if (!atComposingEnd && !(ownEdit && candidatesEnd < 0)) controller.onExternalCursorMove()
            return
        }
        if (!ownEdit) controller.onExternalCursorMove()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        theme = resolveTheme(settings)
        render(controller.state)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        voice.cancel()
        scope.cancel()
        engines.shutdown()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    // ---- rendering ----

    private fun resolveTheme(s: KeyboardSettings): KeyboardTheme {
        val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return KeyboardThemes.resolve(s.theme, night == Configuration.UI_MODE_NIGHT_YES, s.highContrast, s.highContrastTheme)
    }

    private fun render(state: KeyboardUiState) {
        val kb = keyboard ?: return
        strip?.bind(state, theme)
        kb.bind(state, settings, theme)
        val showRow =
            settings.emojiRow &&
                state.page == KeyboardPage.LETTERS &&
                state.panel == Panel.NONE &&
                controller.policy.suggestions != SuggestMode.OFF
        emojiRow?.visibility = if (showRow) View.VISIBLE else View.GONE
        emojiRow?.bind(visibleRecents(personal.emojiRecents.value), theme)
        root?.setBackgroundColor(theme.background)
        renderPanel(state, kb)
    }

    private fun renderPanel(
        state: KeyboardUiState,
        kb: KeyboardView,
    ) {
        val panel = panelView ?: return
        if (state.panel == Panel.NONE) {
            if (shownPanel != Panel.NONE) {
                panel.visibility = View.GONE
                kb.visibility = View.VISIBLE
                shownPanel = Panel.NONE
            }
            return
        }
        // The panel takes exactly the key area's height so the host app does not relayout.
        val h = if (kb.height > 0) kb.height else kb.measuredHeight
        panel.layoutParams = panel.layoutParams.apply { height = h }
        if (shownPanel != state.panel) {
            val callbacks = panelCallbacks()
            val target = state.panel
            val language = state.language
            panel.setContent {
                ImePanels(target, theme, language, personal, engines, settings, voice.available, callbacks)
            }
            shownPanel = target
        }
        panel.visibility = View.VISIBLE
        kb.visibility = View.INVISIBLE
    }

    private fun panelCallbacks() =
        PanelCallbacks(
            onText = { text, isEmoji ->
                controller.onText(text)
                if (isEmoji) recordEmoji(text)
            },
            onDelete = { controller.onKey(KeyCodes.DELETE) },
            onSpace = { controller.onKey(KeyCodes.SPACE) },
            onClose = { controller.closePanel() },
            onLanguage = { controller.chooseLanguage(it) },
            onVoice = {
                controller.closePanel()
                startVoiceInput()
            },
            onOpenApp = { section -> openApp(section) },
            onClipboardEnabled = { on -> scope.launch { settingsStore.update { it.copy(clipboardEnabled = on) } } },
        )

    /** Emoji history is personal data: never recorded or shown in private fields (REQ-017). */
    private fun recordEmoji(emoji: String) {
        if (controller.policy.learning) personal.recordEmoji(emoji)
    }

    private fun visibleRecents(recents: List<String>): List<String> = if (controller.policy.learning) recents else emptyList()

    // ---- clipboard (REQ-063/064) ----

    private fun captureClip() {
        if (!settings.clipboardEnabled) return
        // Never capture while a password or other private field is focused (REQ-017).
        if (isInputViewShown && !controller.policy.clipboardCapture) return
        val clip = clipboardManager?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val sensitive =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                clip.description.extras?.getBoolean(ClipDescription.EXTRA_IS_SENSITIVE) == true
        val text = clip.getItemAt(0).text?.toString() ?: return
        personal.addClip(text, sensitive)
    }

    // ---- ImeHost ----

    override fun showImePicker() {
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
    }

    override fun openPanel(panel: Panel) = Unit

    override fun startVoiceInput() {
        if (!voice.start(controller.state.language)) openApp(MainActivity.SECTION_VOICE)
    }

    override fun openSettings() = openApp(MainActivity.SECTION_SETTINGS)

    override fun keyFeedback(code: Int) {
        keyboard?.let { feedback.onKeyDown(it, code, settings) }
    }

    private fun openApp(section: String) {
        val intent =
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(MainActivity.EXTRA_SECTION, section)
        startActivity(intent)
        requestHideSelf(0)
    }

    // ---- KeyActionListener ----

    override fun onKeyDown(code: Int) = keyFeedback(code)

    override fun onKey(
        code: Int,
        neighbours: String,
    ) = controller.onKey(code, neighbours)

    override fun onText(text: String) = controller.onText(text)

    override fun onShiftDown() = controller.onShiftDown()

    override fun onShiftUp() = controller.onShiftUp()

    override fun onShiftHeld() = controller.onShiftHeld()

    override fun onSpaceSlide(steps: Int) = controller.onSpaceSlide(steps)

    override fun onDeleteWords(words: Int) = controller.onDeleteWords(words)

    // ---- StripListener ----

    override fun onSuggestion(item: StripItem) {
        keyFeedback(0)
        controller.onSuggestionPicked(item)
    }

    override fun onToolbar(action: ToolbarAction) {
        keyFeedback(0)
        when (action) {
            ToolbarAction.CHOOSER -> controller.openPanel(Panel.CHOOSER)
            ToolbarAction.EMOJI -> controller.openPanel(Panel.EMOJI)
            ToolbarAction.CLIPBOARD -> controller.openPanel(Panel.CLIPBOARD)
            ToolbarAction.SETTINGS -> openSettings()
            ToolbarAction.MIC -> startVoiceInput()
        }
    }

    private object UptimeClock : Clock {
        override fun nowMs(): Long = SystemClock.uptimeMillis()
    }

    private companion object {
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }
}
