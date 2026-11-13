package app.hohe.keyboard.core.field

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldPolicyTest {
    // --- Mirrored Constants Verification ---
    @Test
    fun constantsMatchAndroidMirroredHexValues() {
        assertEquals(0xF, FieldPolicy.TYPE_MASK_CLASS)
        assertEquals(1, FieldPolicy.TYPE_CLASS_TEXT)
        assertEquals(2, FieldPolicy.TYPE_CLASS_NUMBER)
        assertEquals(3, FieldPolicy.TYPE_CLASS_PHONE)
        assertEquals(4, FieldPolicy.TYPE_CLASS_DATETIME)
        assertEquals(0xFF0, FieldPolicy.TYPE_MASK_VARIATION)
        assertEquals(0x10, FieldPolicy.TEXT_VARIATION_URI)
        assertEquals(0x20, FieldPolicy.TEXT_VARIATION_EMAIL_ADDRESS)
        assertEquals(0x80, FieldPolicy.TEXT_VARIATION_PASSWORD)
        assertEquals(0x90, FieldPolicy.TEXT_VARIATION_VISIBLE_PASSWORD)
        assertEquals(0xD0, FieldPolicy.TEXT_VARIATION_WEB_EMAIL_ADDRESS)
        assertEquals(0xE0, FieldPolicy.TEXT_VARIATION_WEB_PASSWORD)
        assertEquals(0x10, FieldPolicy.NUMBER_VARIATION_PASSWORD)
        assertEquals(0x10, FieldPolicy.DATETIME_VARIATION_DATE)
        assertEquals(0x20, FieldPolicy.DATETIME_VARIATION_TIME)
        assertEquals(0x80000, FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
        assertEquals(0xFF, FieldPolicy.IME_MASK_ACTION)
        assertEquals(0x40000000, FieldPolicy.IME_FLAG_NO_ENTER_ACTION)
        assertEquals(0x1000000, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
    }

    // --- Password Tests (Section 8 Security) ---
    @Test
    fun textPasswordVariation0x80() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
    }

    @Test
    fun visiblePasswordVariation0x90() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_VISIBLE_PASSWORD
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
    }

    @Test
    fun webPasswordVariation0xE0() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_WEB_PASSWORD
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
    }

    @Test
    fun numberPasswordVariation0x10() {
        val inputType = FieldPolicy.TYPE_CLASS_NUMBER or FieldPolicy.NUMBER_VARIATION_PASSWORD
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.NUMBER_PASSWORD, p.layout)
    }

    // --- Numeric Tests ---
    @Test
    fun numericClassNumber() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_NUMBER, 0)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture) // D-034 Q7
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.NUMBER, p.layout)
        assertEquals(CommaVariant.NORMAL, p.commaVariant)
    }

    @Test
    fun numericClassPhone() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_PHONE, 0)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture) // D-034 Q7
        assertFalse(p.amharicLookup)
        assertFalse(p.voiceKey)
        assertFalse(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace)
        assertEquals(LayoutKind.PHONE, p.layout)
    }

    @Test
    fun numericClassDatetimePlain() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_DATETIME, 0)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertEquals(LayoutKind.NUMBER, p.layout)
    }

    @Test
    fun numericClassDatetimeDate() {
        val inputType = FieldPolicy.TYPE_CLASS_DATETIME or FieldPolicy.DATETIME_VARIATION_DATE
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.clipboardCapture)
        assertEquals(LayoutKind.NUMBER, p.layout)
    }

    @Test
    fun numericClassDatetimeTime() {
        val inputType = FieldPolicy.TYPE_CLASS_DATETIME or FieldPolicy.DATETIME_VARIATION_TIME
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.clipboardCapture)
        assertEquals(LayoutKind.NUMBER, p.layout)
    }

    // --- Address Tests ---
    @Test
    fun addressUri() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_URI
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.ADDRESS, p.fieldClass)
        assertEquals(SuggestMode.ENGLISH_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture) // D-034 Q7
        assertFalse(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertFalse(p.autoCap)
        assertFalse(p.autoSpace) // URI: no autoSpace
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.URL, p.commaVariant)
    }

    @Test
    fun addressEmail() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_EMAIL_ADDRESS
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.ADDRESS, p.fieldClass)
        assertEquals(SuggestMode.ENGLISH_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture) // D-034 Q7
        assertFalse(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertFalse(p.autoCap)
        assertTrue(p.autoSpace) // Email: yes autoSpace
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.EMAIL, p.commaVariant)
    }

    @Test
    fun addressWebEmail() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_WEB_EMAIL_ADDRESS
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.ADDRESS, p.fieldClass)
        assertEquals(SuggestMode.ENGLISH_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture) // D-034 Q7
        assertFalse(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertFalse(p.autoCap)
        assertTrue(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.EMAIL, p.commaVariant)
    }

    // --- Incognito Tests ---
    @Test
    fun incognitoFlagNormalText() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.INCOGNITO, p.fieldClass)
        assertEquals(SuggestMode.DICTIONARY_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
        assertTrue(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertTrue(p.autoCap)
        assertTrue(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.NORMAL, p.commaVariant)
    }

    // --- No Suggestions Tests ---
    @Test
    fun noSuggestionsFlag() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NO_SUGGEST, p.fieldClass)
        assertEquals(SuggestMode.TYPED_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertTrue(p.clipboardCapture)
        assertTrue(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertTrue(p.autoCap)
        assertTrue(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.NORMAL, p.commaVariant)
    }

    // --- Normal Text Tests ---
    @Test
    fun normalText() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.ON, p.suggestions)
        assertTrue(p.learning)
        assertTrue(p.clipboardCapture)
        assertTrue(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.composing)
        assertTrue(p.autoCap)
        assertTrue(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.NORMAL, p.commaVariant)
    }

    // --- Precedence Tests ---
    @Test
    fun precedencePasswordOverNumeric() {
        // Number password has class NUMBER and password variation
        val inputType = FieldPolicy.TYPE_CLASS_NUMBER or FieldPolicy.NUMBER_VARIATION_PASSWORD
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
    }

    @Test
    fun precedencePasswordOverAddress() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD or FieldPolicy.TEXT_VARIATION_URI
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
    }

    @Test
    fun precedencePasswordOverIncognito() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD
        val p = FieldPolicy.classify(inputType, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
        assertFalse(p.learning)
    }

    @Test
    fun precedencePasswordOverNoSuggest() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.PASSWORD, p.fieldClass)
    }

    @Test
    fun precedenceNumericOverIncognito() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_NUMBER, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.NUMERIC, p.fieldClass)
        assertFalse("Learning must remain false", p.learning)
        assertEquals(LayoutKind.NUMBER, p.layout)
    }

    @Test
    fun precedenceAddressOverIncognito() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_URI
        val p = FieldPolicy.classify(inputType, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.ADDRESS, p.fieldClass)
        assertEquals(SuggestMode.ENGLISH_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertEquals(CommaVariant.URL, p.commaVariant)
    }

    @Test
    fun precedenceIncognitoOverNoSuggest() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val p = FieldPolicy.classify(inputType, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.INCOGNITO, p.fieldClass)
        assertEquals(SuggestMode.DICTIONARY_ONLY, p.suggestions)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
    }

    // --- Type Null Edge Case (§7) ---
    @Test
    fun typeNullDefaultsToNormalWithComposingAndSuggestionsOff() {
        // §7 edge case: inputType = 0 (TYPE_NULL) -> NORMAL policy but composing=false, suggestions=OFF
        val p = FieldPolicy.classify(0, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.composing)
        assertTrue(p.learning)
        assertTrue(p.clipboardCapture)
        assertTrue(p.amharicLookup)
        assertTrue(p.voiceKey)
        assertTrue(p.autoCap)
        assertTrue(p.autoSpace)
        assertEquals(LayoutKind.QWERTY, p.layout)
        assertEquals(CommaVariant.NORMAL, p.commaVariant)
    }

    @Test
    fun typeNullWithIncognitoFlagDisablesLearning() {
        val p = FieldPolicy.classify(0, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(FieldClass.INCOGNITO, p.fieldClass)
        assertEquals(SuggestMode.OFF, p.suggestions)
        assertFalse(p.composing)
        assertFalse(p.learning)
        assertFalse(p.clipboardCapture)
    }

    // --- EnterAction Tests ---
    @Test
    fun enterActionGo() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 2)
        assertEquals(EnterAction.GO, p.enterAction)
    }

    @Test
    fun enterActionSearch() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 3)
        assertEquals(EnterAction.SEARCH, p.enterAction)
    }

    @Test
    fun enterActionSend() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 4)
        assertEquals(EnterAction.SEND, p.enterAction)
    }

    @Test
    fun enterActionNext() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 5)
        assertEquals(EnterAction.NEXT, p.enterAction)
    }

    @Test
    fun enterActionDone() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 6)
        assertEquals(EnterAction.DONE, p.enterAction)
    }

    @Test
    fun enterActionPrevious() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 7)
        assertEquals(EnterAction.PREVIOUS, p.enterAction)
    }

    @Test
    fun enterActionUnspecifiedDefaultsToNewline() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 0)
        assertEquals(EnterAction.NEWLINE, p.enterAction)
    }

    @Test
    fun enterActionNoneDefaultsToNewline() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 1)
        assertEquals(EnterAction.NEWLINE, p.enterAction)
    }

    @Test
    fun enterActionUnknownActionDefaultsToNewline() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 99)
        assertEquals(EnterAction.NEWLINE, p.enterAction)
    }

    @Test
    fun enterActionFlagNoEnterActionForcesNewline() {
        // Even when action is SEND (4), IME_FLAG_NO_ENTER_ACTION (0x40000000) forces NEWLINE
        val imeOptions = 4 or FieldPolicy.IME_FLAG_NO_ENTER_ACTION
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, imeOptions)
        assertEquals(EnterAction.NEWLINE, p.enterAction)
    }

    @Test
    fun enterActionFlagNoEnterActionWithSearchForcesNewline() {
        val imeOptions = 3 or FieldPolicy.IME_FLAG_NO_ENTER_ACTION
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, imeOptions)
        assertEquals(EnterAction.NEWLINE, p.enterAction)
    }

    @Test
    fun enterActionPreservedWithHighBits() {
        val imeOptions = 5 or 0x08000000 // action NEXT with custom flags
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, imeOptions)
        assertEquals(EnterAction.NEXT, p.enterAction)
    }

    // --- Unknown Variation Bits (§7) ---
    @Test
    fun unknownVariationTreatedAsPlainText() {
        // §7: Unknown variation bits treated as plain text of its class
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or 0x3F0
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.ON, p.suggestions)
        assertTrue(p.learning)
        assertEquals(LayoutKind.QWERTY, p.layout)
    }

    @Test
    fun multilineTextFlagTreatedAsPlainText() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or 0x20000
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.ON, p.suggestions)
    }

    @Test
    fun autoCompleteFlagTreatedAsPlainText() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or 0x10000
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.ON, p.suggestions)
    }

    @Test
    fun personNameVariationTreatedAsPlainText() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or 0x60
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
        assertEquals(SuggestMode.ON, p.suggestions)
    }

    @Test
    fun postalAddressVariationTreatedAsPlainText() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or 0x70
        val p = FieldPolicy.classify(inputType, 0)
        assertEquals(FieldClass.NORMAL, p.fieldClass)
    }

    // --- Learning Flag Enforcement Across All Field Classes ---
    @Test
    fun noPersonalizedLearningEnforcedOnAddress() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_EMAIL_ADDRESS
        val p = FieldPolicy.classify(inputType, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(p.learning)
    }

    @Test
    fun noPersonalizedLearningEnforcedOnNoSuggest() {
        val inputType = FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val p = FieldPolicy.classify(inputType, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(p.learning)
    }

    @Test
    fun noPersonalizedLearningEnforcedOnNumeric() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_PHONE, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(p.learning)
    }

    @Test
    fun noPersonalizedLearningEnforcedOnNormal() {
        val p = FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertFalse(p.learning)
    }

    // --- Clipboard Capture Safety Matrix ---
    @Test
    fun clipboardCaptureDeniedForPassword() {
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD, 0).clipboardCapture)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_NUMBER or FieldPolicy.NUMBER_VARIATION_PASSWORD, 0).clipboardCapture)
    }

    @Test
    fun clipboardCaptureDeniedForNumeric() {
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_NUMBER, 0).clipboardCapture)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_PHONE, 0).clipboardCapture)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_DATETIME, 0).clipboardCapture)
    }

    @Test
    fun clipboardCaptureDeniedForAddress() {
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_URI, 0).clipboardCapture)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_EMAIL_ADDRESS, 0).clipboardCapture)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_WEB_EMAIL_ADDRESS, 0).clipboardCapture)
    }

    @Test
    fun clipboardCaptureDeniedForIncognito() {
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING).clipboardCapture)
    }

    @Test
    fun clipboardCaptureAllowedForNormalAndNoSuggest() {
        assertTrue(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 0).clipboardCapture)
        assertTrue(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 0).clipboardCapture)
    }

    // --- Amharic Lookup Safety Matrix ---
    @Test
    fun amharicLookupDeniedForPasswordNumericAddress() {
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_PASSWORD, 0).amharicLookup)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_NUMBER, 0).amharicLookup)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_PHONE, 0).amharicLookup)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_URI, 0).amharicLookup)
        assertFalse(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TEXT_VARIATION_EMAIL_ADDRESS, 0).amharicLookup)
    }

    @Test
    fun amharicLookupAllowedForIncognitoNoSuggestNormal() {
        assertTrue(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, FieldPolicy.IME_FLAG_NO_PERSONALIZED_LEARNING).amharicLookup)
        assertTrue(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT or FieldPolicy.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 0).amharicLookup)
        assertTrue(FieldPolicy.classify(FieldPolicy.TYPE_CLASS_TEXT, 0).amharicLookup)
    }
}
