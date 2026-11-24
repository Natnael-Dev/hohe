package app.hohe.keyboard.core.engine

import app.hohe.keyboard.core.field.FieldClass

/** Where a suggestion candidate came from. Drives styling and learning. */
enum class Source { USER_WORD, USER_PICK, DICTIONARY, FALLBACK_TRANSLIT, TYPED, NEXT_WORD, EMOJI }

/** One strip candidate. Higher [score] ranks first. */
data class Candidate(
    val text: String,
    val source: Source,
    val score: Int = 0,
)

/**
 * Nearest keys per typed character, used by the English engine for proximity correction.
 * `neighbours[i]` holds lowercase letters close to the key hit for the i-th typed character,
 * nearest first, excluding the typed letter itself.
 */
data class ProximityInfo(
    val neighbours: List<String>,
)

/** One request to a language engine. Engines must never receive password-field text. */
data class SuggestRequest(
    val id: Long,
    val input: String,
    val contextBefore: String = "",
    val proximity: ProximityInfo? = null,
    val field: FieldClass = FieldClass.NORMAL,
    val usePersonalData: Boolean = true,
    val maxResults: Int = 12,
)

data class SuggestionResult(
    val id: Long,
    val forInput: String,
    val candidates: List<Candidate>,
    val elapsedMicros: Long = 0,
)

/** A user-authored Amharic word: the Latin key the user types and the Ethiopic word to offer. */
data class UserWord(
    val latin: String,
    val ethiopic: String,
)

/**
 * Immutable in-memory view of personal data. Engines read only this, never storage.
 * Keys are lowercase.
 */
data class UserDataSnapshot(
    val userWords: List<UserWord> = emptyList(),
    /** Latin input -> last Ethiopic word the user picked for it. */
    val amharicPicks: Map<String, String> = emptyMap(),
    /** English word (lowercase) -> times committed. */
    val englishUnigrams: Map<String, Int> = emptyMap(),
    /** Previous English word (lowercase) -> (next word -> count). */
    val englishBigrams: Map<String, Map<String, Int>> = emptyMap(),
) {
    companion object {
        val EMPTY = UserDataSnapshot()
    }
}

/** Write side of personal data. Implemented in `:data`; calls must not block the caller. */
interface UserDataPort {
    fun recordAmharicPick(
        latin: String,
        ethiopic: String,
    )

    fun recordEnglishCommit(
        word: String,
        previous: String?,
    )

    fun snapshot(): UserDataSnapshot
}

/** Amharic word engine (REQ-020..029). Pure; confined to the engine thread except [transliterate]. */
interface AmharicEngine {
    val dataVersion: String

    /**
     * Ranked Ethiopic candidates for a lowercase Latin input. Order (REQ-022): user words,
     * previous pick, dictionary exact, dictionary prefix completions, then the letter-by-letter
     * transliteration ([Source.FALLBACK_TRANSLIT]) if not already present. Never empty for
     * non-empty `a-z` input. Does not add the typed Latin slot.
     */
    fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult

    /** Deterministic letter-by-letter transliteration (REQ-028). Thread-safe, total. */
    fun transliterate(latin: String): String
}

/** English word engine (REQ-040..043). Suggests, never auto-replaces (D-023). */
interface EnglishEngine {
    val dataVersion: String

    /** Corrections and completions for the typed word; the typed word itself is not included. */
    fun suggest(
        req: SuggestRequest,
        user: UserDataSnapshot,
    ): SuggestionResult

    /** Next-word predictions for the text before the cursor (REQ-041). */
    fun nextWords(
        contextBefore: String,
        user: UserDataSnapshot,
        max: Int,
    ): List<Candidate>
}

/** Emoji for a typed word (REQ-062). Exact keyword match, offline. */
interface EmojiSuggester {
    fun suggest(
        word: String,
        max: Int = 3,
    ): List<String>

    /** Keyword search for the emoji panel (REQ-060). */
    fun search(
        query: String,
        max: Int = 64,
    ): List<String>
}
