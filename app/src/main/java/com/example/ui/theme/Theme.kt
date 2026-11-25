package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BubbleBlitzColorScheme = darkColorScheme(
    primary = VividViolet,
    onPrimary = Color.White,
    primaryContainer = RoyalPurple,
    onPrimaryContainer = GoldYellow,
    secondary = SkyBlue,
    onSecondary = Color.White,
    secondaryContainer = DeepCobalt,
    onSecondaryContainer = FrostCyan,
    tertiary = GoldYellow,
    onTertiary = Color(0xFF4A1A00),
    background = DeepIndigo,
    onBackground = Color.White,
    surface = RoyalPurple,
    onSurface = Color.White,
    surfaceVariant = ElectricViolet,
    onSurfaceVariant = SoftLavender,
    error = CoralRed,
    onError = Color.White,
    outline = SoftLavender
)

private val BubbleBlitzShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BubbleBlitzTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BubbleBlitzColorScheme,
        typography = BubbleBlitzTypography,
        shapes = BubbleBlitzShapes,
        content = content
    )
}
