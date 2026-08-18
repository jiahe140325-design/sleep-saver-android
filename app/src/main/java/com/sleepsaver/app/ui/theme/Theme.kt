package com.sleepsaver.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFFFF4D6)
val Ink = Color(0xFF3D3654)
val Blush = Color(0xFFFFB6C1)
val Sage = Color(0xFF7FA99B)
val Lavender = Color(0xFFC9B6FF)
val Paper = Color(0xFFFFFCF3)

private val SleepSaverColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDEBE5),
    onPrimaryContainer = Ink,
    secondary = Lavender,
    onSecondary = Ink,
    tertiary = Blush,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF4E9CF),
    onSurfaceVariant = Color(0xFF665E70),
    outline = Color(0xFFC7BDA8)
)

@Composable
fun SleepSaverTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SleepSaverColors,
        typography = Typography(),
        content = content
    )
}

