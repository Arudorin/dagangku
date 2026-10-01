package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// DagangKu Brand Colors
val DagangBluePrimary = Color(0xFF3B82F6)
val DagangBlueDark = Color(0xFF1E4FA3)
val DagangBlueLight = Color(0xFF60A5FA)

// Dark-Safe Status Colors (as requested)
val StatusGreenContainer = Color(0xFF14532D)
val StatusGreenText = Color(0xFF86EFAC)

val StatusRedContainer = Color(0xFF7F1D1D)
val StatusRedText = Color(0xFFFCA5A5)

val StatusAmberContainer = Color(0xFF78350F)
val StatusAmberText = Color(0xFFFCD34D)

val StatusPurpleContainer = Color(0xFF3B0764)
val StatusPurpleText = Color(0xFFD8B4FE)

// Backward compatible aliases mapped to dark-safe colors
val DagangIncomeGreen = StatusGreenText
val DagangIncomeGreenContainer = StatusGreenContainer
val DagangOnIncomeGreen = StatusGreenText

val DagangDebtRed = StatusRedText
val DagangDebtRedContainer = StatusRedContainer
val DagangOnDebtRed = StatusRedText

val DagangWarningAmber = StatusAmberText
val DagangWarningAmberContainer = StatusAmberContainer
val DagangOnWarningAmber = StatusAmberText

val StatusPurple = StatusPurpleText

// Dark Theme base surfaces
val DarkBackground = Color(0xFF0F172A)
val DarkSurface = Color(0xFF1E293B)
val DarkSurfaceVariant = Color(0xFF334155)
val DarkOutline = Color(0xFF475569)
val DarkOnSurface = Color(0xFFF1F5F9)
val DarkOnSurfaceVariant = Color(0xFF94A3B8)
val DarkPrimary = Color(0xFF60A5FA)
val DarkPrimaryContainer = Color(0xFF1E3A8A)
val DarkOnPrimary = Color(0xFF0F172A)
val DarkOnPrimaryContainer = Color(0xFFDBEAFE)

// Keep Neutral* backward compatible pointing to Dark theme equivalents
val NeutralBackground = DarkBackground
val NeutralSurface = DarkSurface
val NeutralSurfaceVariant = DarkSurfaceVariant
val NeutralOutline = DarkOutline
val NeutralTextPrimary = DarkOnSurface
val NeutralTextSecondary = DarkOnSurfaceVariant
val DagangBlueContainer = DarkPrimaryContainer
val DagangOnBlueContainer = DarkOnPrimaryContainer
