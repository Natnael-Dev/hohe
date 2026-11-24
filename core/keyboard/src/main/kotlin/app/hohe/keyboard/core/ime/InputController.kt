package app.hohe.keyboard.core.ime

import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.EmojiSuggester
import app.hohe.keyboard.core.engine.EnglishEngine
import app.hohe.keyboard.core.engine.ProximityInfo
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.UserDataPort
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.field.EnterAction
import app.hohe.keyboard.core.field.FieldClass
import app.hohe.keyboard.core.field.FieldPolicy
import app.hohe.keyboard.core.field.LayoutKind
import app.hohe.keyboard.core.field.SuggestMode
import app.hohe.keyboard.core.input.AutoCap
import app.hohe.keyboard.core.input.ShiftStateMachine
import app.hohe.keyboard.core.layout.KeyCodes
import app.hohe.keyboard.core.layout.KeyboardPage
import app.hohe.keyboard.core.layout.Layout
import app.hohe.keyboard.core.layout.LayoutFactory
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage
import app.hohe.keyboard.core.time.Clock

/** Engines are created lazily by the host; any of them may be missing (typing never breaks). */
interface EngineProvider {
    val amharic: AmharicEngine?
    val english: EnglishEngine?
    val emoji: EmojiSuggester?
}

/** Runs suggestion work off the input thread and delivers results back on it. */
interface SuggestScheduler {
    fun submit(
        work: () -> StripState,
        deliver: (StripState) -> Unit,
    )

    /** Runs synchronously; for tests and as the default. */
    object Immediate : SuggestScheduler {
        override fun submit(
            work: () -> StripState,
            deliver: (StripState) -> Unit,
        ) = deliver(work())
    }
}

/**
 * Single owner of IME typing state (application-architecture §3.1). Views send events,
 * the controller applies the rules from this module and publishes [KeyboardUiState].
 * Must be called from one thread only.
 */
class InputController(
    private val editor: EditorPort,
    private val host: ImeHost,
    private val clock: Clock,
    private val engines: EngineProvider,
    private val userData: () -> UserDataPort?,
    private val scheduler: SuggestScheduler = SuggestScheduler.Immediate,
    private val onLanguageChanged: (TypingLanguage) -> Unit = {},
) {
    var listener: (KeyboardUiState) -> Unit = {}

    var settings: KeyboardSettings = KeyboardSettings()
        private set
    var policy: FieldPolicy = FieldPolicy.classify(TEXT_CLASS, 0)
        private set
    var language: TypingLanguage = TypingLanguage.AMHARIC
        private set

    private var page = KeyboardPage.LETTERS
    private var panel = Panel.NONE
    private val shift = ShiftStateMachine(clock)
    private var shiftHeld = false
    private var chorded = false
    private val composing = StringBuilder()
    private val proximity = mutableListOf<String>()
    private var strip = StripState.TOOLBAR
    private var requestId = 0L
    private var lastSpaceAtMs = Long.MIN_VALUE / 2
    private var weakSpace = false
    private var previousWord: String? = null
    private var layout: Layout = LayoutFactory.build(page, policy, settings, language)
    var voiceAvailable: Boolean = false

    val composingText: String get() = composing.toString()

    val state: KeyboardUiState
        get() =
            KeyboardUiState(
                layout = layout,
                page = page,
                shift = shift.state,
                language = language,
                amharicActive = amharicActive,
                enterAction = policy.enterAction,
                strip = strip,
                panel = panel,
                incognito = policy.fieldClass == FieldClass.INCOGNITO,
                voiceAvailable = voiceAvailable && policy.voiceKey,
            )

    private val amharicActive: Boolean
        get() = language == TypingLanguage.AMHARIC && policy.amharicLookup

    private val composingAllowed: Boolean
        get() = policy.composing && (amharicActive || policy.suggestions != SuggestMode.OFF)

    private val usePersonalData: Boolean
        get() = policy.suggestions == SuggestMode.ON && policy.learning

    // ---- lifecycle -------------------------------------------------------------------------

    fun applySettings(newSettings: KeyboardSettings) {
        val s = newSettings.sanitized()
        val rebuild = s != settings
        settings = s
        if (language != s.language && composing.isEmpty()) language = s.language
        if (rebuild) rebuildLayout()
        publish()
    }

    /** A new field gained focus. Composing state from the previous field is dropped. */
    fun startInput(newPolicy: FieldPolicy) {
        policy = newPolicy
        composing.clear()
        proximity.clear()
        requestId++
        page = KeyboardPage.LETTERS
        panel = Panel.NONE
        strip = StripState.TOOLBAR
        weakSpace = false
        previousWord = null
        shift.layoutChanged()
        rebuildLayout()
        updateAutoCap()
        publish()
    }

    /** The field lost focus or the keyboard hides; leave what the user sees in the field. */
    fun finishInput() {
        if (composing.isNotEmpty()) editor.finishComposingText()
        composing.clear()
        proximity.clear()
        requestId++
        strip = StripState.TOOLBAR
        panel = Panel.NONE
    }

    /** The cursor moved without us (tap in the field, app edit): stop composing. */
    fun onExternalCursorMove() {
        if (composing.isNotEmpty()) {
            editor.finishComposingText()
            composing.clear()
            proximity.clear()
            requestId++
        }
        weakSpace = false
        strip = StripState.TOOLBAR
        updateAutoCap()
        publish()
    }

    // ---- key events ------------------------------------------------------------------------

    fun onShiftDown() {
        shift.shiftDown()
        shiftHeld = true
        chorded = false
        publish()
    }

    fun onShiftUp() {
        shift.shiftUp()
        // Shift held while letters were typed (chording): those letters were the capitals, so
        // releasing shift must not leave a one-shot capital for the next letter.
        if (chorded) shift.letterTyped()
        shiftHeld = false
        chorded = false
        publish()
    }

    /** Called while shift is held so a long hold locks caps (1200 ms). */
    fun onShiftHeld() {
        shift.tick()
        publish()
    }

    /**
     * A key was tapped. [neighbours] are nearby letters for proximity correction (letters only).
     */
    fun onKey(
        code: Int,
        neighbours: String = "",
    ) {
        when (code) {
            KeyCodes.SHIFT, KeyCodes.CAPSLOCK -> {
                shift.shiftDown()
                shift.shiftUp()
            }
            KeyCodes.DELETE -> handleDelete()
            KeyCodes.SPACE -> handleSpace()
            KeyCodes.ENTER -> handleEnter()
            KeyCodes.NEWLINE -> {
                commitComposing(Trigger.SEPARATOR)
                editor.commitText("\n")
                afterCommit(nextWords = false)
            }
            KeyCodes.SYMBOLS -> switchPage(if (page == KeyboardPage.LETTERS) KeyboardPage.SYMBOLS else KeyboardPage.LETTERS)
            KeyCodes.ABC -> switchPage(KeyboardPage.LETTERS)
            KeyCodes.SHIFT_SYMBOLS ->
                switchPage(if (page == KeyboardPage.SYMBOLS) KeyboardPage.SYMBOLS_SHIFT else KeyboardPage.SYMBOLS)
            KeyCodes.NUMPAD -> switchPage(KeyboardPage.NUMPAD)
            KeyCodes.LANGUAGE -> toggleLanguage()
            KeyCodes.LANGUAGE_CHOOSER -> openPanel(Panel.CHOOSER)
            KeyCodes.EMOJI, KeyCodes.EMOJI_LONG_PRESS, KeyCodes.COMMA_EMOJI, KeyCodes.ENTER_EMOJI -> openPanel(Panel.EMOJI)
            KeyCodes.IME_PICKER -> host.showImePicker()
            else -> if (code > 0) handleChar(code, neighbours)
        }
        publish()
    }

    /** Text from a long-press panel item, the emoji panel or the clipboard panel. */
    fun onText(text: String) {
        if (text.isEmpty()) return
        if (text.codePointCount(0, text.length) == 1) {
            onKey(text.codePointAt(0))
            return
        }
        commitComposing(Trigger.SEPARATOR)
        editor.commitText(text)
        weakSpace = false
        afterCommit(nextWords = false)
        publish()
    }

    fun onSuggestionPicked(item: StripItem) {
        val latin = composing.toString()
        // Ignore taps on an item that is no longer on the bar (a newer result replaced it).
        if (item !in strip.items || strip.forInput != latin) return
        editor.beginBatch()
        when (item.kind) {
            StripItemKind.TYPED -> {
                editor.commitText(latin)
                if (!amharicActive) recordEnglish(latin)
            }
            StripItemKind.EMOJI -> {
                if (latin.isNotEmpty()) {
                    editor.commitText(latin)
                    recordEnglish(latin)
                    editor.commitText(" ")
                }
                editor.commitText(item.text)
            }
            StripItemKind.PRIMARY, StripItemKind.CANDIDATE -> {
                editor.commitText(item.text)
                if (amharicActive && latin.isNotEmpty() && item.source != Source.TYPED) {
                    recordAmharic(latin, item.text)
                } else if (!amharicActive) {
                    recordEnglish(item.text)
                }
            }
        }
        editor.commitText(" ")
        editor.endBatch()
        composing.clear()
        proximity.clear()
        weakSpace = true
        afterCommit(nextWords = true)
        publish()
    }

    /** Space-bar slide moved [steps] characters (negative = left). Composing is finished first. */
    fun onSpaceSlide(steps: Int) {
        if (steps == 0) return
        if (composing.isNotEmpty()) {
            editor.finishComposingText()
            composing.clear()
            proximity.clear()
            requestId++
            strip = StripState.TOOLBAR
        }
        weakSpace = false
        editor.moveCursor(steps)
        publish()
    }

    /** Backspace swipe released after selecting [words] words to the left (REQ-009). */
    fun onDeleteWords(words: Int) {
        if (words <= 0) return
        var remaining = words
        if (composing.isNotEmpty()) {
            editor.setComposingText("")
            editor.finishComposingText()
            composing.clear()
            proximity.clear()
            requestId++
            remaining--
        }
        if (remaining > 0) {
            val before = editor.textBeforeCursor(MAX_CONTEXT * 4)
            val n = charsForWords(before, remaining)
            if (n > 0) editor.deleteBefore(n)
        }
        weakSpace = false
        strip = StripState.TOOLBAR
        updateAutoCap()
        publish()
    }

    fun openPanel(p: Panel) {
        if (p != Panel.NONE) commitComposing(Trigger.SEPARATOR)
        panel = p
        if (p != Panel.NONE) {
            strip = StripState.TOOLBAR
            host.openPanel(p)
        }
        publish()
    }

    fun closePanel() {
        panel = Panel.NONE
        publish()
    }

    /** Typing style chosen in the chooser panel. */
    fun chooseLanguage(target: TypingLanguage) {
        if (target != language) toggleLanguage()
        closePanel()
    }

    // ---- rules -----------------------------------------------------------------------------

    private enum class Trigger { SPACE, SEPARATOR, ENTER }

    private fun handleChar(
        code: Int,
        neighbours: String,
    ) {
        var text = String(Character.toChars(code))
        val asciiLetter = code in 'a'.code..'z'.code || code in 'A'.code..'Z'.code
        if (page == KeyboardPage.LETTERS && Character.isLetter(code) && (shift.isUppercase || shiftHeld)) {
            if (shiftHeld) chorded = true
            text = text.uppercase()
        }
        val joinsWord =
            composingAllowed && (asciiLetter || (code == '\''.code && composing.isNotEmpty()))
        if (joinsWord) {
            composing.append(text)
            proximity.add(neighbours.lowercase())
            editor.setComposingText(composing.toString())
            shift.letterTyped()
            weakSpace = false
            requestSuggestions()
            return
        }
        editor.beginBatch()
        commitComposing(Trigger.SEPARATOR)
        if (text in AUTO_SPACE_PUNCTUATION) {
            val before = editor.textBeforeCursor(2)
            if (weakSpace && before.endsWith(" ")) editor.deleteBefore(1)
            val wordBefore = editor.textBeforeCursor(1).let { it.isNotEmpty() && Character.isLetter(it[it.length - 1]) }
            editor.commitText(text)
            if (settings.autoSpacePunctuation && policy.autoSpace && wordBefore) {
                editor.commitText(" ")
                weakSpace = true
            } else {
                weakSpace = false
            }
        } else {
            editor.commitText(text)
            weakSpace = false
        }
        editor.endBatch()
        if (Character.isLetter(code)) shift.letterTyped()
        afterCommit(nextWords = false)
    }

    private fun handleSpace() {
        val now = clock.nowMs()
        editor.beginBatch()
        if (composing.isNotEmpty()) {
            commitComposing(Trigger.SPACE)
            editor.commitText(" ")
            weakSpace = true
            editor.endBatch()
            lastSpaceAtMs = now
            afterCommit(nextWords = true)
            return
        }
        val before = editor.textBeforeCursor(2)
        val doubleSpace =
            settings.doubleSpacePeriod &&
                policy.autoSpace &&
                now - lastSpaceAtMs <= DOUBLE_SPACE_MS &&
                before.length == 2 &&
                before[1] == ' ' &&
                Character.isLetterOrDigit(before[0])
        if (doubleSpace) {
            editor.deleteBefore(1)
            editor.commitText(". ")
            lastSpaceAtMs = Long.MIN_VALUE / 2
        } else {
            editor.commitText(" ")
            lastSpaceAtMs = now
        }
        editor.endBatch()
        weakSpace = false
        afterCommit(nextWords = false)
    }

    private fun handleEnter() {
        editor.beginBatch()
        commitComposing(Trigger.ENTER)
        if (policy.enterAction == EnterAction.NEWLINE) {
            editor.commitText("\n")
        } else {
            editor.performEditorAction(policy.enterAction)
        }
        editor.endBatch()
        weakSpace = false
        afterCommit(nextWords = false)
    }

    private fun handleDelete() {
        if (composing.isNotEmpty()) {
            val end = composing.length
            val start = composing.offsetByCodePoints(end, -1)
            composing.delete(start, end)
            if (proximity.isNotEmpty()) proximity.removeAt(proximity.size - 1)
            editor.setComposingText(composing.toString())
            if (composing.isEmpty()) {
                editor.finishComposingText()
                requestId++
                strip = StripState.TOOLBAR
                updateAutoCap()
            } else {
                requestSuggestions()
            }
            return
        }
        editor.deleteBackward()
        weakSpace = false
        strip = StripState.TOOLBAR
        updateAutoCap()
    }

    /** Commits the composing word. In Amharic mode, space/separators commit exactly slot 0 (REQ-021a). */
    private fun commitComposing(trigger: Trigger) {
        if (composing.isEmpty()) return
        val latin = composing.toString()
        if (amharicActive) {
            val word = if (settings.selectSuggestionWithSpace) primaryFor(latin) ?: latin else latin
            editor.commitText(word)
            if (word != latin) recordAmharic(latin, word)
        } else {
            editor.commitText(latin)
            recordEnglish(latin)
        }
        composing.clear()
        proximity.clear()
        requestId++
        if (trigger != Trigger.SPACE) weakSpace = false
    }

    /**
     * The slot-0 word for [latin]. If the strip is stale (a result is still pending), the strip is
     * computed now and published before the commit, so what is committed is what the bar shows
     * (REQ-021a, engine spec §3.4 AC 4).
     */
    private fun primaryFor(latin: String): String? {
        if (strip.forInput != latin) {
            val typedOnly = policy.suggestions == SuggestMode.TYPED_ONLY || policy.suggestions == SuggestMode.OFF
            requestId++
            strip = amharicStrip(latin, request(latin, MAX_CANDIDATES), snapshot(), typedOnly, settings.amharicPredictions)
            publish()
        }
        return strip.items.firstOrNull { it.kind == StripItemKind.PRIMARY }?.text
    }

    private fun afterCommit(nextWords: Boolean) {
        requestId++
        strip = StripState.TOOLBAR
        updateAutoCap()
        if (nextWords) requestNextWords()
    }

    private fun toggleLanguage() {
        // Any key outside [a-z'] commits slot 0 first (engine spec §3.7).
        commitComposing(Trigger.SEPARATOR)
        language = if (language == TypingLanguage.AMHARIC) TypingLanguage.ENGLISH else TypingLanguage.AMHARIC
        strip = StripState.TOOLBAR
        onLanguageChanged(language)
        rebuildLayout()
    }

    private fun switchPage(target: KeyboardPage) {
        if (policy.layout != LayoutKind.QWERTY && policy.layout != LayoutKind.PHONE) return
        commitComposing(Trigger.SEPARATOR)
        page = target
        shift.layoutChanged()
        rebuildLayout()
        updateAutoCap()
    }

    private fun rebuildLayout() {
        layout = LayoutFactory.build(page, policy, settings, language)
    }

    private fun updateAutoCap() {
        if (page != KeyboardPage.LETTERS || composing.isNotEmpty()) return
        val before = editor.textBeforeCursor(MAX_CONTEXT)
        shift.autoCapRequested(AutoCap.shouldCapitalize(before, settings.autoCap, policy.autoCap))
    }

    // ---- suggestions -----------------------------------------------------------------------

    private fun request(
        latin: String,
        max: Int,
    ): SuggestRequest =
        SuggestRequest(
            id = requestId,
            input = if (amharicActive) latin.lowercase() else latin,
            contextBefore = editor.textBeforeCursor(MAX_CONTEXT).toString(),
            proximity = if (amharicActive) null else ProximityInfo(proximity.toList()),
            field = policy.fieldClass,
            usePersonalData = usePersonalData,
            maxResults = max,
        )

    private fun snapshot(): UserDataSnapshot =
        if (usePersonalData) safely { userData()?.snapshot() } ?: UserDataSnapshot.EMPTY else UserDataSnapshot.EMPTY

    private fun requestSuggestions() {
        val latin = composing.toString()
        val id = ++requestId
        if (!amharicActive && policy.suggestions == SuggestMode.TYPED_ONLY) {
            strip = StripState(listOf(StripItem(latin, StripItemKind.TYPED, Source.TYPED)), latin)
            return
        }
        val req = request(latin, MAX_CANDIDATES)
        val snap = snapshot()
        val typedOnly = policy.suggestions == SuggestMode.TYPED_ONLY || policy.suggestions == SuggestMode.OFF
        val amharic = amharicActive
        val emojiOn = settings.emojiSuggestions && policy.suggestions == SuggestMode.ON
        val predictions = settings.amharicPredictions
        scheduler.submit(
            work = {
                if (amharic) {
                    amharicStrip(latin, req, snap, typedOnly, predictions)
                } else {
                    englishStrip(latin, req, snap, emojiOn)
                }
            },
            deliver = { result ->
                if (id == requestId && composing.toString() == latin) {
                    strip = result
                    publish()
                }
            },
        )
    }

    private fun amharicStrip(
        latin: String,
        req: SuggestRequest,
        snap: UserDataSnapshot,
        typedOnly: Boolean,
        predictions: Boolean,
    ): StripState {
        val engine = engines.amharic
        val lower = latin.lowercase()
        var candidates =
            if (engine == null) {
                emptyList()
            } else if (!predictions) {
                listOfNotNull(safely { engine.transliterate(lower) })
                    .map {
                        app.hohe.keyboard.core.engine
                            .Candidate(it, Source.FALLBACK_TRANSLIT)
                    }
            } else {
                safely { engine.suggest(req, snap).candidates }?.takeIf { it.isNotEmpty() }
                    ?: listOfNotNull(safely { engine.transliterate(lower) })
                        .map {
                            app.hohe.keyboard.core.engine
                                .Candidate(it, Source.FALLBACK_TRANSLIT)
                        }
            }
        if (typedOnly) candidates = candidates.take(1)
        val items = mutableListOf<StripItem>()
        candidates.firstOrNull()?.let { items.add(StripItem(it.text, StripItemKind.PRIMARY, it.source)) }
        items.add(StripItem(latin, StripItemKind.TYPED, Source.TYPED))
        candidates.drop(1).filter { it.text != latin }.take(VISIBLE_EXTRA).forEach {
            items.add(StripItem(it.text, StripItemKind.CANDIDATE, it.source))
        }
        return StripState(items, latin)
    }

    private fun englishStrip(
        latin: String,
        req: SuggestRequest,
        snap: UserDataSnapshot,
        emojiOn: Boolean,
    ): StripState {
        val items = mutableListOf(StripItem(latin, StripItemKind.TYPED, Source.TYPED))
        val english = engines.english
        if (english != null) {
            val cands = safely { english.suggest(req, snap).candidates } ?: emptyList()
            cands.filter { it.text != latin }.take(VISIBLE_EXTRA).forEach {
                items.add(StripItem(it.text, StripItemKind.CANDIDATE, it.source))
            }
        }
        if (emojiOn) {
            val emoji = engines.emoji
            safely { emoji?.suggest(latin.lowercase(), 2) }?.forEach {
                items.add(StripItem(it, StripItemKind.EMOJI, Source.EMOJI))
            }
        }
        return StripState(items, latin)
    }

    private fun requestNextWords() {
        if (amharicActive || !settings.nextWordSuggestions || policy.suggestions != SuggestMode.ON) return
        val english = engines.english ?: return
        val id = requestId
        val context = editor.textBeforeCursor(MAX_CONTEXT).toString()
        val snap = snapshot()
        scheduler.submit(
            work = {
                val words = safely { english.nextWords(context, snap, VISIBLE_EXTRA + 1) } ?: emptyList()
                StripState(words.map { StripItem(it.text, StripItemKind.CANDIDATE, Source.NEXT_WORD) }, "")
            },
            deliver = { result ->
                if (id == requestId && composing.isEmpty()) {
                    strip = result
                    publish()
                }
            },
        )
    }

    // ---- learning --------------------------------------------------------------------------

    private fun recordAmharic(
        latin: String,
        ethiopic: String,
    ) {
        if (!policy.learning || !usePersonalData) return
        safely { userData()?.recordAmharicPick(latin.lowercase(), ethiopic) }
        previousWord = null
    }

    private fun recordEnglish(word: String) {
        val clean = word.trim()
        if (clean.isEmpty() || clean.none { it.isLetter() }) return
        if (policy.learning && usePersonalData) {
            safely { userData()?.recordEnglishCommit(clean, previousWord) }
        }
        previousWord = clean.lowercase()
    }

    private fun publish() {
        listener(state)
    }

    private inline fun <T> safely(block: () -> T): T? =
        try {
            block()
        } catch (e: RuntimeException) {
            null
        }

    companion object {
        private const val TEXT_CLASS = 1
        const val MAX_CONTEXT = 64
        const val MAX_CANDIDATES = 12
        const val VISIBLE_EXTRA = 4
        const val DOUBLE_SPACE_MS = 1100L
        val AUTO_SPACE_PUNCTUATION = setOf(".", ",", "?", "!", "።", "፣", "፧")

        /** Number of UTF-16 chars that cover [words] words (and trailing spaces) before the cursor. */
        fun charsForWords(
            before: CharSequence,
            words: Int,
        ): Int {
            var i = before.length
            repeat(words) {
                while (i > 0 && before[i - 1].isWhitespace()) i--
                while (i > 0 && !before[i - 1].isWhitespace() && !before[i - 1].isLetterOrDigit()) i--
                while (i > 0 && before[i - 1].isLetterOrDigit()) i--
            }
            return before.length - i
        }
    }
}
