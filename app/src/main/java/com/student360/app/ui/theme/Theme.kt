@file:Suppress("UNUSED_PARAMETER")

package com.student360.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = CardDark,
    onPrimaryContainer = LightPurple,
    secondary = LightPurple,
    onSecondary = BgDark,
    secondaryContainer = ElevatedCardDark,
    onSecondaryContainer = PrimaryText,
    tertiary = ExamPurple,
    onTertiary = Color.White,
    background = BgDark,
    onBackground = PrimaryText,
    surface = SurfaceDark,
    onSurface = PrimaryText,
    surfaceVariant = CardDark,
    onSurfaceVariant = SecondaryText,
    outline = BorderDark,
    outlineVariant = BorderDark.copy(alpha = 0.5f),
    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFF321A1D),
    onErrorContainer = Color(0xFFFFB4AB)
)

@Composable
fun Student360Theme(
    darkTheme: Boolean = true, // Dark mode first
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDark.toArgb()
            window.navigationBarColor = BgDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
