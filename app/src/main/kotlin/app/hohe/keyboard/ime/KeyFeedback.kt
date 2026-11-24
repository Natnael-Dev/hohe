package app.hohe.keyboard.ime

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import app.hohe.keyboard.core.layout.KeyCodes
import app.hohe.keyboard.core.settings.KeyboardSettings

/** Key-press sound and vibration (REQ-012). Runs on the main thread in well under 1 ms. */
class KeyFeedback(
    context: Context,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun onKeyDown(
        view: View,
        code: Int,
        settings: KeyboardSettings,
    ) {
        if (settings.vibrate) vibrate(view, settings.vibrationMs)
        if (settings.sound && audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val fx =
                when (code) {
                    KeyCodes.DELETE -> AudioManager.FX_KEYPRESS_DELETE
                    KeyCodes.SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
                    KeyCodes.ENTER -> AudioManager.FX_KEYPRESS_RETURN
                    else -> AudioManager.FX_KEYPRESS_STANDARD
                }
            audio.playSoundEffect(fx, settings.soundLevel / 100f)
        }
    }

    private fun vibrate(
        view: View,
        ms: Int,
    ) {
        if (ms < 0) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        if (ms == 0) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
