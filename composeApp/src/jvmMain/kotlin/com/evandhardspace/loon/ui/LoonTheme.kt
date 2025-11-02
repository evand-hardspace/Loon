package com.evandhardspace.loon.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// IDE-inspired color palette
private val DarkBackground = Color(0xFF1E1F22)
private val DarkSurface = Color(0xFF2B2D30)
private val DarkSurfaceVariant = Color(0xFF313335)
private val EditorBackground = Color(0xFF2B2D30)
private val SelectionBlue = Color(0xFF2675BF)
private val AccentBlue = Color(0xFF2A6882)
private val AccentGreen = Color(0xFF499C54)
private val AccentOrange = Color(0xFFCF8E6D)
private val AccentPurple = Color(0xFFB07EC3)
private val TextPrimary = Color(0xFFBCBEC4)
private val TextSecondary = Color(0xFF868A91)
private val BorderColor = Color(0xFF3C3F41)
private val ErrorRed = Color(0xFFE85757)

// Light theme colors (optional)
private val LightBackground = Color(0xFFF7F8FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEBECF0)
private val LightTextPrimary = Color(0xFF2B2D30)
private val LightTextSecondary = Color(0xFF6C707E)
private val LightBorder = Color(0xFFDFE1E5)

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = SelectionBlue,
    onPrimaryContainer = Color.White,

    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2D4032),
    onSecondaryContainer = AccentGreen,

    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3D2F42),
    onTertiaryContainer = AccentPurple,

    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF4A2626),
    onErrorContainer = ErrorRed,

    background = DarkBackground,
    onBackground = TextPrimary,

    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,

    surfaceTint = AccentBlue,
    inverseSurface = Color(0xFFE3E3E3),
    inverseOnSurface = Color(0xFF2B2D30),

    outline = BorderColor,
    outlineVariant = Color(0xFF2D2F31),

    scrim = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = SelectionBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAFF),
    onPrimaryContainer = Color(0xFF0D3C61),

    secondary = AccentGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F0D9),
    onSecondaryContainer = Color(0xFF1E4620),

    tertiary = AccentPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E4F5),
    onTertiaryContainer = Color(0xFF3D2645),

    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    outlineVariant = Color(0xFFE8E9ED),
)

@Composable
fun LoonTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// Example usage with common IDE accent colors
object IDEColors {
    val CodeKeyword = AccentOrange
    val CodeString = AccentGreen
    val CodeNumber = AccentPurple
    val CodeComment = TextSecondary
    val CodeFunction = Color(0xFF56A8F5)
    val CodeClass = Color(0xFF8DC149)
    val WarningYellow = Color(0xFFEDA200)
    val InfoBlue = AccentBlue
}