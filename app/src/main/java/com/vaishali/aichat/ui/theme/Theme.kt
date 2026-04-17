package com.vaishali.aichat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}

private val AiColorScheme = darkColorScheme(
    primary = AiPrimary,
    secondary = AiSecondary,
    tertiary = AiAccent,
    background = AiDeepBlue,
    surface = AiSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AiTextPrimary,
    onSurface = AiTextPrimary,
    primaryContainer = AiUserBubble,
    onPrimaryContainer = Color.White,
    secondaryContainer = AiBotBubble,
    onSecondaryContainer = Color.White,
    surfaceVariant = AiSurface,
    onSurfaceVariant = AiTextSecondary
)

private val LightAiColorScheme = lightColorScheme(
    primary = AiPrimary,
    secondary = AiSecondary,
    tertiary = AiAccent,
    background = AiLightBackground,
    surface = AiLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = AiLightTextPrimary,
    onSurface = AiLightTextPrimary,
    primaryContainer = AiLightUserBubble,
    onPrimaryContainer = AiLightTextPrimary,
    secondaryContainer = AiLightBotBubble,
    onSecondaryContainer = AiLightTextPrimary,
    surfaceVariant = AiLightBotBubble,
    onSurfaceVariant = AiLightTextSecondary
)

@Composable
fun AiChatTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) AiColorScheme else LightAiColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}