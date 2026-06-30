package com.controlpro.banklistener.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BankBlue,
    onPrimary = Color(0xFF04152C),
    primaryContainer = Color(0xFF162240),
    onPrimaryContainer = TextDark,
    secondary = BankGreen,
    onSecondary = Color(0xFF062114),
    error = BankRed,
    background = BgDark,
    onBackground = TextDark,
    surface = SurfaceDark,
    onSurface = TextDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = MutedDark,
    outline = StrokeDark,
    outlineVariant = Color(0x0DFFFFFF)
)

@Composable
fun BankListenerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
