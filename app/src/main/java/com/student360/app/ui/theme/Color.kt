package com.student360.app.ui.theme

import androidx.compose.ui.graphics.Color

// Reference Dark Theme Palette (Source of Truth)
val BgDark = Color(0xFF0D0E17)            // #0D0E17 / very dark navy
val SurfaceDark = Color(0xFF1A1C2B)       // #1A1C2B / surface card
val CardDark = Color(0xFF1A1C2B)          // #1A1C2B / surface card
val ElevatedCardDark = Color(0xFF202236)  // #202236 / elevated container
val BorderDark = Color(0xFF282A3E)        // #282A3E / subtle low-contrast border

// Active Navigation & Accent Tokens
val ActivePillPurple = Color(0xFF51496F)  // #51496F / muted violet active pill
val LightPurple = Color(0xFFA78BFA)       // #A78BFA / lavender highlight
val BrandPurple = Color(0xFF8B5CF6)       // #8B5CF6 / brand purple
val SoftPurple = Color(0xFFC084FC)        // #C084FC / soft purple

// Brand Primary & Accents
val BrandBlue = Color(0xFF3B82F6)
val BrandCyan = Color(0xFF06B6D4)
val BrandSky = Color(0xFF38BDF8)
val BrandNavy = Color(0xFF0D0E17)

val PrimaryBlue = BrandBlue
val AccentCyan = BrandCyan
val PrimaryPurple = ActivePillPurple

// Typography Tokens
val PrimaryText = Color(0xFFF2F2F7)       // #F2F2F7 / crisp primary text
val SecondaryText = Color(0xFF9294A8)     // #9294A8 / muted secondary text
val MutedText = Color(0xFF686A7E)         // #686A7E / subtle tertiary text

// Semantic State Indicators
val SuccessGreen = Color(0xFF62D9A3)      // #62D9A3 / good positive attendance
val SafeGreen = SuccessGreen
val SafeGreenLight = Color(0xFF133B29)

val DangerRed = Color(0xFFF05C67)         // #F05C67 / missed low attendance
val CriticalRed = DangerRed
val CriticalRedLight = Color(0xFF3F191E)

val WarningOrange = Color(0xFFF2C45C)     // #F2C45C / warning or off status
val WarningYellow = WarningOrange
val WarningYellowLight = Color(0xFF3D3216)

val MixedPurple = Color(0xFFC084FC)
val NeutralGray = Color(0xFF9294A8)

val HolidayGrey = Color(0xFF475569)
val HolidayGreyLight = Color(0xFF1E293B)
val ExamPurple = Color(0xFF8B5CF6)
val ExamPurpleLight = Color(0xFF2E1065)

val Purple80 = LightPurple
val PurpleGrey80 = SecondaryText
val Pink80 = AccentCyan
val Purple40 = ActivePillPurple
val PurpleGrey40 = SurfaceDark
val Pink40 = ActivePillPurple

val NavyBg = BgDark
val NavySurface = SurfaceDark
val NavyCard = CardDark
val NavyElevatedCard = ElevatedCardDark
val NavyBorder = BorderDark
