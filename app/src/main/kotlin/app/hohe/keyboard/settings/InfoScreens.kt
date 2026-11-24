package app.hohe.keyboard.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hohe.keyboard.BuildConfig
import app.hohe.keyboard.R

/** Offline FAQ (REQ-086): no web form, nothing collected. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val faq =
        listOf(
            R.string.faq_q1 to R.string.faq_a1,
            R.string.faq_q2 to R.string.faq_a2,
            R.string.faq_q3 to R.string.faq_a3,
            R.string.faq_q4 to R.string.faq_a4,
            R.string.faq_q5 to R.string.faq_a5,
            R.string.faq_q6 to R.string.faq_a6,
        )
    ScreenScaffold(stringResource(R.string.help_title), onBack) {
        Card {
            faq.forEachIndexed { i, (q, a) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(q), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(a), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** About: version, open-source notices, privacy policy (REQ-087). */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") navigate: (Screen) -> Unit,
) {
    ScreenScaffold(stringResource(R.string.about_title), onBack) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Hohe ሆሄ", style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.titleMedium)
        Card { Text(stringResource(R.string.privacy_body), modifier = Modifier.padding(16.dp)) }
        Text(stringResource(R.string.licences_title), style = MaterialTheme.typography.titleMedium)
        Card { Text(stringResource(R.string.licences_body), modifier = Modifier.padding(16.dp)) }
    }
}

/** Voice typing explainer and microphone permission (REQ-051). */
@Composable
fun VoiceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    ScreenScaffold(stringResource(R.string.style_voice), onBack) {
        Text(stringResource(R.string.voice_explainer))
        if (granted) {
            Text(stringResource(R.string.voice_ready), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            TryKeyboardField()
        } else {
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.voice_allow))
            }
        }
    }
}

/** Handwriting is not in this build; the screen says so plainly (REQ-050 deferred). */
@Composable
fun HandwritingScreen(onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.style_handwriting), onBack) {
        Text(stringResource(R.string.handwriting_later))
    }
}
