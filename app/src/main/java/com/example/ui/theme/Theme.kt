package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KryptDarkColorScheme = darkColorScheme(
    primary = KryptWhite,
    onPrimary = KryptBlack,
    primaryContainer = KryptCharcoalElevated,
    onPrimaryContainer = KryptPlatinum,
    secondary = KryptSilver,
    onSecondary = KryptBlack,
    secondaryContainer = KryptCharcoal,
    onSecondaryContainer = KryptWhite,
    tertiary = KryptAmber,
    onTertiary = KryptBlack,
    tertiaryContainer = KryptAmberSurface,
    onTertiaryContainer = KryptAmber,
    background = KryptBlack,
    onBackground = KryptWhite,
    surface = KryptCharcoalDeep,
    onSurface = KryptWhite,
    surfaceVariant = KryptCharcoal,
    onSurfaceVariant = KryptSilver,
    surfaceContainerLowest = KryptBlack,
    surfaceContainerLow = KryptCharcoalDeep,
    surfaceContainer = KryptCharcoal,
    surfaceContainerHigh = KryptCharcoalElevated,
    outline = KryptBorder,
    outlineVariant = KryptBorderBright,
    error = KryptCrimson,
    onError = KryptWhite,
    errorContainer = KryptCrimsonSurface,
    onErrorContainer = KryptCrimson
)

val KryptShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

@Composable
fun KryptTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KryptDarkColorScheme,
        typography = KryptTypography,
        shapes = KryptShapes,
        content = content
    )
}
