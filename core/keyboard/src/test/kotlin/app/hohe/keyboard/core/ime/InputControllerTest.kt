package app.hohe.keyboard.core.ime

import app.hohe.keyboard.core.engine.AmharicEngine
import app.hohe.keyboard.core.engine.Candidate
import app.hohe.keyboard.core.engine.EmojiSuggester
import app.hohe.keyboard.core.engine.EnglishEngine
import app.hohe.keyboard.core.engine.Source
import app.hohe.keyboard.core.engine.SuggestRequest
import app.hohe.keyboard.core.engine.SuggestionResult
import app.hohe.keyboard.core.engine.UserDataPort
import app.hohe.keyboard.core.engine.UserDataSnapshot
import app.hohe.keyboard.core.field.EnterAction
import app.hohe.keyboard.core.field.FieldPolicy
import app.hohe.keyboard.core.input.ShiftState
import app.hohe.keyboard.core.layout.KeyCodes
import app.hohe.keyboard.core.layout.KeyboardPage
import app.hohe.keyboard.core.settings.KeyboardSettings
import app.hohe.keyboard.core.settings.TypingLanguage
import app.hohe.keyboard.core.time.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeEditor : EditorPort {
    val text = StringBuilder()
    var composingStart = -1
    val actions = mutableListOf<EnterAction>()
    var cursorMoves = 0

    override fun textBeforeCursor(n: Int): CharSequence = text.substring(maxOf(0, text.length - n))

    override fun hasSelection() = false

    override fun setComposingText(text: String) {
        if (composingStart < 0) composingStart = this.text.length
        this.text.setLength(composingStart)
        this.text.append(text)
        if (text.isEmpty()) composingStart = -1
    }

    override fun finishComposingText() {
        composingStart = -1
    }

    override fun commitText(text: String) {
        if (composingStart >= 0) {
            this.text.setLength(composingStart)
            composingStart = -1
        }
        this.text.append(text)
    }

    override fun deleteBackward() {
        if (text.isNotEmpty()) text.setLength(text.length - 1)
    }

    override fun deleteBefore(chars: Int) {
        text.setLength(maxOf(0, text.length - chars))
    }

    override fun performEditorAction(action: EnterAction) {
        actions.add(action)
    }

    override fun moveCursor(delta: Int) {
        cursorMoves += delta
    }

    override fun beginBatch() = Unit

    override fun endBatch() = Unit
}

class FakeHost : ImeHost {
    var pickerShown = 0
    val panels = mutableListOf<Panel>()

    override fun showImePicker() {
        pickerShown++
    }

    override fun openPanel(panel: Panel) {
        panels.add(panel)
    }

    override fun startVoiceInput() = Unit

    override fun openSettings() = Unit

    override fun keyFeedback(code: Int) = Unit
}

class FakeAmharic : AmharicEngine {
    val calls = mutableListOf<String>()
    override val dataVersion = "test"

    override fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult {
        calls.add(req.input)
        val pick = user.amharicPicks[req.input]
        val list =
            when (req.input) {
                "sel" -> listOf("ሰላም", "ሰል", "ጸል")
                "selam" -> listOf("ሰላም", "ሰላምታ")
                else -> listOf(transliterate(req.input))
            }
        val ranked = listOfNotNull(pick) + list.filter { it != pick }
        return SuggestionResult(req.id, req.input, ranked.map { Candidate(it, Source.DICTIONARY) })
    }

    override fun transliterate(latin: String) = "T($latin)"
}

class FakeEnglish : EnglishEngine {
    override val dataVersion = "test"

    override fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult {
        val out = if (req.input.lowercase() == "helo") listOf("hello", "help") else emptyList()
        return SuggestionResult(req.id, req.input, out.map { Candidate(it, Source.DICTIONARY) })
    }

    override fun nextWords(
        contextBefore: String,
        user: UserDataSnapshot,
        max: Int,
    ) = listOf(Candidate("world", Source.NEXT_WORD))
}

class FakeUserData : UserDataPort {
    val picks = mutableMapOf<String, String>()
    val english = mutableListOf<Pair<String, String?>>()

    override fun recordAmharicPick(
        latin: String,
        ethiopic: String,
    ) {
        picks[latin] = ethiopic
    }

    override fun recordEnglishCommit(
        word: String,
        previous: String?,
    ) {
        english.add(word to previous)
    }

    override fun snapshot() = UserDataSnapshot(amharicPicks = picks.toMap())
}

/** Holds deferred work so tests can check the stale-strip path. */
class ManualScheduler : SuggestScheduler {
    val pending = mutableListOf<Pair<() -> StripState, (StripState) -> Unit>>()

    override fun submit(
        work: () -> StripState,
        deliver: (StripState) -> Unit,
    ) {
        pending.add(work to deliver)
    }

    fun runAll() {
        val copy = pending.toList()
        pending.clear()
        copy.forEach { (w, d) -> d(w()) }
    }
}

class InputControllerTest {
    private lateinit var editor: FakeEditor
    private lateinit var host: FakeHost
    private lateinit var amharic: FakeAmharic
    private lateinit var user: FakeUserData
    private lateinit var clock: FakeClock
    private var states = mutableListOf<KeyboardUiState>()

    private val text = 1
    private val normal = FieldPolicy.classify(text, 0)

    private fun controller(scheduler: SuggestScheduler = SuggestScheduler.Immediate): InputController {
        val engines =
            object : EngineProvider {
                override val amharic: AmharicEngine = this@InputControllerTest.amharic
                override val english: EnglishEngine = FakeEnglish()
                override val emoji: EmojiSuggester? = null
            }
        return InputController(editor, host, clock, engines, { user }, scheduler).also {
            it.listener = { s -> states.add(s) }
            it.applySettings(KeyboardSettings())
            it.startInput(normal)
        }
    }

    @Before
    fun setUp() {
        editor = FakeEditor()
        host = FakeHost()
        amharic = FakeAmharic()
        user = FakeUserData()
        clock = FakeClock(1_000)
        states.clear()
    }

    private fun InputController.type(s: String) = s.forEach { onKey(it.code) }

    @Test
    fun amharicWordShowsLatinWhileComposingAndCommitsSlotZeroOnSpace() {
        val c = controller()
        c.type("sel")
        assertEquals("Sel", editor.text.toString())
        val strip = c.state.strip
        assertEquals("Sel", strip.forInput)
        assertEquals(listOf("ሰላም", "Sel", "ሰል", "ጸል"), strip.items.map { it.text })
        assertEquals(StripItemKind.PRIMARY, strip.items[0].kind)
        assertEquals(StripItemKind.TYPED, strip.items[1].kind)
        c.onKey(KeyCodes.SPACE)
        assertEquals("ሰላም ", editor.text.toString())
        assertEquals("ሰላም", user.picks["sel"])
        assertTrue(c.state.strip.showsToolbar)
    }

    @Test
    fun spaceCommitsExactlyTheShownSlotZero() {
        val sched = ManualScheduler()
        val c = controller(sched)
        c.type("se")
        sched.runAll()
        val shown =
            c.state.strip.items
                .first()
                .text
        c.onKey('l'.code)
        // strip still shows the result for "Se" while "Sel" is pending: compute synchronously
        c.onKey(KeyCodes.SPACE)
        assertEquals("ሰላም ", editor.text.toString())
        assertEquals("T(se)", shown)
    }

    @Test
    fun selectSuggestionWithSpaceOffCommitsLatin() {
        val c = controller()
        c.applySettings(KeyboardSettings(selectSuggestionWithSpace = false))
        c.type("sel")
        c.onKey(KeyCodes.SPACE)
        assertEquals("Sel ", editor.text.toString())
        assertTrue(user.picks.isEmpty())
    }

    @Test
    fun tappingSuggestionCommitsItWithSpaceAndLearnsPick() {
        val c = controller()
        c.type("sel")
        val item =
            c.state.strip.items
                .first { it.text == "ሰል" }
        c.onSuggestionPicked(item)
        assertEquals("ሰል ", editor.text.toString())
        assertEquals("ሰል", user.picks["sel"])
        // next time the pick ranks first
        c.type("sel")
        assertEquals(
            "ሰል",
            c.state.strip.items
                .first()
                .text,
        )
    }

    @Test
    fun tappingTypedSlotKeepsLatin() {
        val c = controller()
        c.type("sel")
        c.onSuggestionPicked(
            c.state.strip.items
                .first { it.kind == StripItemKind.TYPED },
        )
        assertEquals("Sel ", editor.text.toString())
    }

    @Test
    fun punctuationAfterWordSwapsTheAutoSpace() {
        val c = controller()
        c.type("selam")
        c.onKey(KeyCodes.SPACE)
        c.onKey('.'.code)
        assertEquals("ሰላም. ", editor.text.toString())
        // the next sentence is auto-capitalised
        assertEquals(ShiftState.AUTO, c.state.shift)
    }

    @Test
    fun punctuationWhileComposingCommitsSlotZeroFirst() {
        val c = controller()
        c.type("selam")
        c.onKey(','.code)
        assertEquals("ሰላም, ", editor.text.toString())
    }

    @Test
    fun englishModeNeverAutoReplaces() {
        val c = controller()
        c.onKey(KeyCodes.LANGUAGE)
        assertEquals(TypingLanguage.ENGLISH, c.state.language)
        c.type("helo")
        assertEquals(
            listOf("Helo", "hello", "help"),
            c.state.strip.items
                .map { it.text },
        )
        c.onKey(KeyCodes.SPACE)
        assertEquals("Helo ", editor.text.toString())
        assertEquals("Helo" to null, user.english.single())
        // next-word predictions after the commit
        assertEquals(
            listOf("world"),
            c.state.strip.items
                .map { it.text },
        )
    }

    @Test
    fun englishSuggestionTapReplacesTypedWord() {
        val c = controller()
        c.onKey(KeyCodes.LANGUAGE)
        c.type("helo")
        c.onSuggestionPicked(
            c.state.strip.items
                .first { it.text == "hello" },
        )
        assertEquals("hello ", editor.text.toString())
    }

    @Test
    fun backspaceWhileComposingEditsTheWord() {
        val c = controller()
        c.type("selx")
        c.onKey(KeyCodes.DELETE)
        assertEquals("Sel", editor.text.toString())
        assertEquals(
            "ሰላም",
            c.state.strip.items
                .first()
                .text,
        )
        repeat(3) { c.onKey(KeyCodes.DELETE) }
        assertEquals("", editor.text.toString())
        assertTrue(c.state.strip.showsToolbar)
    }

    @Test
    fun passwordFieldCommitsLettersDirectlyAndNeverLearns() {
        val c = controller()
        c.startInput(FieldPolicy.classify(text or 0x80, 0))
        c.type("abc")
        c.onKey(KeyCodes.SPACE)
        assertEquals("abc ", editor.text.toString())
        assertTrue(user.picks.isEmpty())
        assertTrue(user.english.isEmpty())
        assertFalse(c.state.amharicActive)
        assertTrue(amharic.calls.isEmpty())
    }

    @Test
    fun incognitoFieldConvertsButDoesNotLearn() {
        val c = controller()
        c.startInput(FieldPolicy.classify(text, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING))
        assertTrue(c.state.incognito)
        c.type("sel")
        c.onKey(KeyCodes.SPACE)
        assertEquals("ሰላም ", editor.text.toString())
        assertTrue(user.picks.isEmpty())
    }

    @Test
    fun doubleSpaceInsertsFullStopWhenEnabled() {
        val c = controller()
        c.applySettings(KeyboardSettings(doubleSpacePeriod = true, language = TypingLanguage.ENGLISH))
        c.type("hi")
        c.onKey(KeyCodes.SPACE)
        clock.advanceBy(300)
        c.onKey(KeyCodes.SPACE)
        assertEquals("Hi. ", editor.text.toString())
    }

    @Test
    fun doubleSpaceIsOffByDefault() {
        val c = controller()
        c.applySettings(KeyboardSettings(language = TypingLanguage.ENGLISH))
        c.type("hi")
        c.onKey(KeyCodes.SPACE)
        c.onKey(KeyCodes.SPACE)
        assertEquals("Hi  ", editor.text.toString())
    }

    @Test
    fun enterRunsTheFieldActionAfterCommitting() {
        val c = controller()
        c.startInput(FieldPolicy.classify(text, 3))
        c.type("sel")
        c.onKey(KeyCodes.ENTER)
        assertEquals("ሰላም", editor.text.toString())
        assertEquals(listOf(EnterAction.SEARCH), editor.actions)
    }

    @Test
    fun enterInMultilineFieldInsertsNewline() {
        val c = controller()
        c.onKey(KeyCodes.ENTER)
        assertEquals("\n", editor.text.toString())
    }

    @Test
    fun autoCapAtFieldStartAndOneShotShift() {
        val c = controller()
        assertEquals(ShiftState.AUTO, c.state.shift)
        c.onKey(KeyCodes.LANGUAGE)
        c.type("a")
        assertEquals(ShiftState.OFF, c.state.shift)
        c.onShiftDown()
        c.onShiftUp()
        c.type("b")
        assertEquals("AB", editor.text.toString())
    }

    @Test
    fun symbolsPageAndBack() {
        val c = controller()
        c.onKey(KeyCodes.SYMBOLS)
        assertEquals(KeyboardPage.SYMBOLS, c.state.page)
        c.onKey(KeyCodes.SHIFT_SYMBOLS)
        assertEquals(KeyboardPage.SYMBOLS_SHIFT, c.state.page)
        c.onKey(KeyCodes.ABC)
        assertEquals(KeyboardPage.LETTERS, c.state.page)
    }

    @Test
    fun longPressActionsReachTheHost() {
        val c = controller()
        c.onKey(KeyCodes.IME_PICKER)
        assertEquals(1, host.pickerShown)
        c.onKey(KeyCodes.COMMA_EMOJI)
        assertEquals(Panel.EMOJI, c.state.panel)
        c.closePanel()
        c.onKey(KeyCodes.LANGUAGE_CHOOSER)
        assertEquals(Panel.CHOOSER, c.state.panel)
        c.chooseLanguage(TypingLanguage.ENGLISH)
        assertEquals(Panel.NONE, c.state.panel)
        assertEquals(TypingLanguage.ENGLISH, c.state.language)
    }

    @Test
    fun multiCharacterTextCommitsAfterComposing() {
        val c = controller()
        c.type("sel")
        c.onText("ብር")
        assertEquals("ሰላምብር", editor.text.toString())
    }

    @Test
    fun deleteWordsRemovesWholeWords() {
        val c = controller()
        editor.text.append("one two three")
        c.onDeleteWords(2)
        assertEquals("one ", editor.text.toString())
        assertEquals(4, InputController.charsForWords("a b, c", 2))
    }

    @Test
    fun spaceSlideFinishesComposingThenMovesCursor() {
        val c = controller()
        c.type("sel")
        c.onSpaceSlide(-2)
        assertEquals(-2, editor.cursorMoves)
        assertEquals("Sel", editor.text.toString())
        assertTrue(c.composingText.isEmpty())
    }

    @Test
    fun externalCursorMoveStopsComposing() {
        val c = controller()
        c.type("sel")
        c.onExternalCursorMove()
        assertTrue(c.composingText.isEmpty())
        assertTrue(c.state.strip.showsToolbar)
    }

    @Test
    fun engineFailureNeverBreaksTyping() {
        val broken =
            object : AmharicEngine {
                override val dataVersion = "x"

                override fun suggest(
                    req: SuggestRequest,
                    user: UserDataSnapshot,
                ): SuggestionResult = throw IllegalStateException("boom")

                override fun transliterate(latin: String) = "ት"
            }
        val engines =
            object : EngineProvider {
                override val amharic: AmharicEngine = broken
                override val english: EnglishEngine? = null
                override val emoji: EmojiSuggester? = null
            }
        val c = InputController(editor, host, clock, engines, { user })
        c.startInput(normal)
        c.type("t")
        c.onKey(KeyCodes.SPACE)
        assertEquals("ት ", editor.text.toString())
    }
}
