package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

var isLightThemeMode by mutableStateOf(false)

val ThreadsOled: Color get() = if (isLightThemeMode) Color(0xFFFFFFFF) else Color(0xFF000000)
val ThreadsCard: Color get() = if (isLightThemeMode) Color(0xFFF5F5F5) else Color(0xFF101010)
val ThreadsSuccessGreen: Color get() = Color(0xFF00D27B)
val ThreadsWhite: Color get() = if (isLightThemeMode) Color(0xFF000000) else Color(0xFFFFFFFF)
val ThreadsGray: Color get() = if (isLightThemeMode) Color(0xFFEBEBEB) else Color(0xFF1A1A1A)
val ThreadsSubtext: Color get() = if (isLightThemeMode) Color(0xFF808080) else Color(0xFF777777)
val ThreadsBorder: Color get() = if (isLightThemeMode) Color(0xFFD2D2D2) else Color(0xFF262626)
val ThreadsBlack: Color get() = if (isLightThemeMode) Color(0xFFFAFAFA) else Color(0xFF0D0D0D)
val ThreadsErrorRed: Color get() = Color(0xFFFF3B30)

private val DarkColorScheme = darkColorScheme(
    primary = ThreadsWhite,
    secondary = ThreadsGray,
    background = ThreadsOled,
    surface = ThreadsCard,
    onPrimary = ThreadsOled,
    onSecondary = ThreadsWhite,
    onBackground = ThreadsWhite,
    onSurface = ThreadsWhite
)

@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isLightThemeMode) {
        lightColorScheme(
            primary = Color(0xFF000000),
            secondary = Color(0xFFEBEBEB),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5),
            onPrimary = Color(0xFFFFFFFF),
            onSecondary = Color(0xFF000000),
            onBackground = Color(0xFF000000),
            onSurface = Color(0xFF000000)
        )
    } else {
        DarkColorScheme
    }
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

