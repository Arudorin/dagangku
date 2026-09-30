package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = DagangBluePrimary,
    onPrimary = Color.White,
    primaryContainer = DagangBlueContainer,
    onPrimaryContainer = DagangOnBlueContainer,
    secondary = DagangBlueLight,
    onSecondary = Color.White,
    tertiary = DagangIncomeGreen,
    onTertiary = Color.White,
    background = NeutralBackground,
    onBackground = NeutralTextPrimary,
    surface = NeutralSurface,
    onSurface = NeutralTextPrimary,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralTextSecondary,
    outline = NeutralOutline,
    error = DagangDebtRed,
    errorContainer = DagangDebtRedContainer,
    onError = Color.White,
    onErrorContainer = DagangOnDebtRed
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkBluePrimary,
    onPrimary = Color(0xFF002D6C),
    primaryContainer = DarkBlueContainer,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = DagangBlueLight,
    onSecondary = Color.White,
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF052E16),
    background = DarkBackground,
    onBackground = Color(0xFFF1F5F9),
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = Color(0xFFFCA5A5),
    errorContainer = Color(0xFF7F1D1D),
    onError = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun DagangKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep DagangKu #1E4FA3 signature brand by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
