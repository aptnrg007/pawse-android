package com.pawse.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PawseColorScheme = lightColorScheme(
    primary = ButtonDark,
    onPrimary = OnButtonDark,
    secondaryContainer = Lavender,
    onSecondaryContainer = TextPrimary,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface,
    onSurfaceVariant = TextSecondary,
    error = Danger,
)

private val PawseShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Pawse's look: soft pastel cards, rounded corners, dark pill buttons instead of default
 * Material purple. Light-only — no dark theme for this pass.
 */
@Composable
fun PawseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PawseColorScheme,
        shapes = PawseShapes,
        content = content,
    )
}
