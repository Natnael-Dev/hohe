package app.hohe.keyboard.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hohe.keyboard.ime.Icon
import app.hohe.keyboard.ime.Icons

/** App-shell tokens (color-palette.md §2). */
private val Light =
    lightColorScheme(
        primary = Color(0xFF0B6B4F),
        onPrimary = Color.White,
        secondary = Color(0xFF0B6B4F),
        onSecondary = Color.White,
        tertiary = Color(0xFF0B6B4F),
        primaryContainer = Color(0xFFD5EDE3),
        onPrimaryContainer = Color(0xFF05392A),
        secondaryContainer = Color(0xFFD5EDE3),
        onSecondaryContainer = Color(0xFF05392A),
        background = Color(0xFFF3F5F4),
        onBackground = Color(0xFF121816),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF121816),
        surfaceVariant = Color(0xFFF3F5F4),
        onSurfaceVariant = Color(0xFF505C58),
        surfaceContainer = Color(0xFFFFFFFF),
        surfaceContainerHigh = Color(0xFFFFFFFF),
        outline = Color(0xFF8A9692),
        outlineVariant = Color(0xFFDCE3E0),
        error = Color(0xFFB3261E),
    )

private val Dark =
    darkColorScheme(
        primary = Color(0xFF74D3B0),
        onPrimary = Color(0xFF06231A),
        secondary = Color(0xFF74D3B0),
        onSecondary = Color(0xFF06231A),
        tertiary = Color(0xFF74D3B0),
        primaryContainer = Color(0xFF1C3A30),
        onPrimaryContainer = Color(0xFFBDF0DC),
        secondaryContainer = Color(0xFF1C3A30),
        onSecondaryContainer = Color(0xFFBDF0DC),
        background = Color(0xFF0D1311),
        onBackground = Color(0xFFEDF2F0),
        surface = Color(0xFF161E1B),
        onSurface = Color(0xFFEDF2F0),
        surfaceVariant = Color(0xFF0D1311),
        onSurfaceVariant = Color(0xFFA7B4AF),
        surfaceContainer = Color(0xFF161E1B),
        surfaceContainerHigh = Color(0xFF161E1B),
        outline = Color(0xFF6C7A75),
        outlineVariant = Color(0xFF26302C),
        error = Color(0xFFFFB4AB),
    )

@Composable
fun HoheTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) Dark else Light
    MaterialTheme(colorScheme = scheme) {
        // Plain Text outside a Surface would otherwise draw black on the dark page.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides scheme.onBackground,
            content = content,
        )
    }
}

/** Renders one of the keyboard's canvas icons in Compose. */
@Composable
fun HoheIcon(
    icon: Icon,
    tint: Color,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier.size(size)) {
        paint.color = tint.toArgb()
        drawIntoCanvas { Icons.draw(it.nativeCanvas, icon, this.size.width / 2, this.size.height / 2, this.size.minDimension, paint) }
    }
}
