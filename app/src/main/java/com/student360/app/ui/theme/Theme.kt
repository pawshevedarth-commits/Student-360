package com.student360.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode(val title: String) {
    SYSTEM_DEFAULT("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

data class AppColors(
    val bg: Color,
    val surface: Color,
    val card: Color,
    val elevatedCard: Color,
    val border: Color,
    val activePill: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val isDark: Boolean
)

val DarkAppColors = AppColors(
    bg = Color(0xFF0D0E17),
    surface = Color(0xFF1A1C2B),
    card = Color(0xFF1A1C2B),
    elevatedCard = Color(0xFF202236),
    border = Color(0xFF282A3E),
    activePill = Color(0xFF51496F),
    textPrimary = Color(0xFFF2F2F7),
    textSecondary = Color(0xFF9294A8),
    textMuted = Color(0xFF686A7E),
    accent = Color(0xFFA78BFA),
    success = Color(0xFF62D9A3),
    danger = Color(0xFFF05C67),
    warning = Color(0xFFF2C45C),
    isDark = true
)

val LightAppColors = AppColors(
    bg = Color(0xFFF4F5FB),
    surface = Color(0xFFFFFFFF),
    card = Color(0xFFFFFFFF),
    elevatedCard = Color(0xFFF0F2FA),
    border = Color(0xFFE2E4ED),
    activePill = Color(0xFFE4DEFC),
    textPrimary = Color(0xFF171926),
    textSecondary = Color(0xFF646882),
    textMuted = Color(0xFF9498AB),
    accent = Color(0xFF5B4FB5),
    success = Color(0xFF0EAD69),
    danger = Color(0xFFE63946),
    warning = Color(0xFFEAA220),
    isDark = false
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

object ThemeManager {
    private const val PREFS_NAME = "student360_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_USE_SYSTEM_COLORS = "use_system_colors"

    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM_DEFAULT)
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _useSystemColors = MutableStateFlow(true)
    val useSystemColors: StateFlow<Boolean> = _useSystemColors.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM_DEFAULT.name)
        val mode = try {
            AppThemeMode.valueOf(modeStr ?: AppThemeMode.SYSTEM_DEFAULT.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM_DEFAULT
        }
        _themeMode.value = mode
        _useSystemColors.value = prefs.getBoolean(KEY_USE_SYSTEM_COLORS, true)
    }

    fun setTheme(context: Context, mode: AppThemeMode, useSystem: Boolean) {
        _themeMode.value = mode
        _useSystemColors.value = useSystem
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .putBoolean(KEY_USE_SYSTEM_COLORS, useSystem)
            .apply()
    }
}

private val DarkMaterialColorScheme = darkColorScheme(
    primary = Color(0xFF51496F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1C2B),
    onPrimaryContainer = Color(0xFFA78BFA),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF0D0E17),
    secondaryContainer = Color(0xFF202236),
    onSecondaryContainer = Color(0xFFF2F2F7),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color.White,
    background = Color(0xFF0D0E17),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1A1C2B),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF1A1C2B),
    onSurfaceVariant = Color(0xFF9294A8),
    outline = Color(0xFF282A3E),
    outlineVariant = Color(0xFF282A3E),
    error = Color(0xFFF05C67),
    onError = Color.White
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = Color(0xFF5B4FB5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DEFC),
    onPrimaryContainer = Color(0xFF171926),
    secondary = Color(0xFF5B4FB5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F2FA),
    onSecondaryContainer = Color(0xFF171926),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color.White,
    background = Color(0xFFF4F5FB),
    onBackground = Color(0xFF171926),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171926),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF646882),
    outline = Color(0xFFE2E4ED),
    outlineVariant = Color(0xFFE2E4ED),
    error = Color(0xFFE63946),
    onError = Color.White
)

@Composable
fun Student360Theme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ThemeManager.init(context)
    }

    val themeMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM_DEFAULT -> isSystemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val appColors = if (isDark) DarkAppColors else LightAppColors
    val materialColorScheme = if (isDark) DarkMaterialColorScheme else LightMaterialColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = appColors.bg.toArgb()
            window.navigationBarColor = appColors.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = Typography,
            content = content
        )
    }
}
