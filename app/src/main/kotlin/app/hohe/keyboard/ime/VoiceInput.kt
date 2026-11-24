package app.hohe.keyboard.ime

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import app.hohe.keyboard.R
import app.hohe.keyboard.core.settings.TypingLanguage

/**
 * Voice typing through Android's speech recogniser (REQ-051). The microphone permission is
 * requested by the app (an IME cannot show the dialog), so [start] returns false when it is missing.
 * Only the recognised text is committed; no audio or text is stored.
 */
class VoiceInput(
    private val context: Context,
    private val onResult: (String) -> Unit,
) {
    private var recognizer: SpeechRecognizer? = null

    var available: Boolean = false
        private set

    fun refreshAvailability() {
        available = SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun hasPermission(): Boolean = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Starts listening; false if the permission is missing (caller opens the app to ask). */
    fun start(language: TypingLanguage): Boolean {
        if (!hasPermission()) return false
        if (!available) {
            Toast.makeText(context, R.string.voice_unavailable, Toast.LENGTH_SHORT).show()
            return true
        }
        cancel()
        val r = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(listener)
        val tag = if (language == TypingLanguage.AMHARIC) "am-ET" else "en-US"
        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        r.startListening(intent)
        Toast.makeText(context, R.string.voice_listening, Toast.LENGTH_SHORT).show()
        return true
    }

    fun cancel() {
        recognizer?.let {
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    private val listener =
        object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) onResult(text)
                cancel()
            }

            override fun onError(error: Int) {
                val msg =
                    if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                        R.string.voice_no_match
                    } else {
                        R.string.voice_error
                    }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                cancel()
            }

            override fun onReadyForSpeech(params: Bundle?) = Unit

            override fun onBeginningOfSpeech() = Unit

            override fun onRmsChanged(rmsdB: Float) = Unit

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() = Unit

            override fun onPartialResults(partialResults: Bundle?) = Unit

            override fun onEvent(
                eventType: Int,
                params: Bundle?,
            ) = Unit
        }
}
