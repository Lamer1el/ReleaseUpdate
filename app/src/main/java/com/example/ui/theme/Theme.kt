package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

fun getCyberColorScheme(
    darkTheme: Boolean,
    accent: String
): androidx.compose.material3.ColorScheme {
    val (primary, secondary, container) = when (accent) {
        "MATRIX_GREEN" -> Triple(MatrixGreenPrimary, MatrixGreenSecondary, MatrixGreenContainer)
        "SOLAR_AMBER" -> Triple(SolarAmberPrimary, SolarAmberSecondary, SolarAmberContainer)
        "SYNTHWAVE" -> Triple(SynthwavePrimary, SynthwaveSecondary, SynthwaveContainer)
        "CRIMSON" -> Triple(CrimsonPrimary, CrimsonSecondary, CrimsonContainer)
        else -> Triple(CyberNeonPrimary, CyberNeonSecondary, CyberNeonContainer) // CYBER_NEON default
    }

    return if (darkTheme) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF050B14),
            primaryContainer = container,
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.25f),
            onSecondaryContainer = Color.White,
            background = CyberBackgroundDark,
            onBackground = Color(0xFFE2E8F0),
            surface = CyberSurfaceDark,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = CyberSurfaceVariantDark,
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = CyberOutlineDark
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = secondary,
            background = CyberBackgroundLight,
            onBackground = Color(0xFF0F172A),
            surface = CyberSurfaceLight,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = CyberSurfaceVariantLight,
            onSurfaceVariant = Color(0xFF475569),
            outline = CyberOutlineLight
        )
    }
}

@Composable
fun SkitefiTheme(
    themeMode: String = "DARK",
    accentColor: String = "CYBER_NEON",
    cardRadiusDp: Int = 14,
    content: @Composable () -> Unit
) {
    // Default to dark cyber HUD for extreme hardware benchmark aesthetics
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    // Explicitly non-Material-You: pure high-tech gaming HUD design
    val colorScheme = getCyberColorScheme(darkTheme, accentColor)

    val shapes = Shapes(
        extraSmall = RoundedCornerShape((cardRadiusDp / 4).coerceAtLeast(4).dp),
        small = RoundedCornerShape((cardRadiusDp / 2).coerceAtLeast(6).dp),
        medium = RoundedCornerShape(cardRadiusDp.dp),
        large = RoundedCornerShape((cardRadiusDp * 1.5).toInt().dp),
        extraLarge = RoundedCornerShape((cardRadiusDp * 2).dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = shapes,
        content = content
    )
}
