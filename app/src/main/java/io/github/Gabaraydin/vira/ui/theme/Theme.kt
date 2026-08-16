package io.github.Gabaraydin.vira.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ViraDarkColorScheme = darkColorScheme(
    primary = ViraAmber,
    secondary = ViraAmberDark,
    background = ViraBackgroundDark,
    surface = ViraSurfaceDark,
    onBackground = ViraOnSurfaceDark,
    onSurface = ViraOnSurfaceDark,
)

private val ViraLightColorScheme = lightColorScheme(
    primary = ViraAmber,
    secondary = ViraAmberDark,
    background = ViraBackgroundLight,
    surface = ViraSurfaceLight,
    onBackground = ViraOnSurfaceLight,
    onSurface = ViraOnSurfaceLight,
)

// Defaults to dark; useDarkTheme lets the settings screen (issue #22) override once ThemeMode is wired up.
@Composable
fun ViraTheme(
    useDarkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (useDarkTheme) ViraDarkColorScheme else ViraLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ViraTypography,
        content = content,
    )
}
