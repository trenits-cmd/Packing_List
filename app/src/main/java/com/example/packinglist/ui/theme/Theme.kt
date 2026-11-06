package com.example.packinglist.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class StatusColors(
    val open: Color,
    val openContainer: Color,
    val onOpenContainer: Color,
    val packed: Color,
    val packedContainer: Color,
    val onPackedContainer: Color,
    val warning: Color
)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(
        open = OpenRed,
        openContainer = OpenRedContainer,
        onOpenContainer = OnOpenRedContainer,
        packed = PackedGreen,
        packedContainer = PackedGreenContainer,
        onPackedContainer = OnPackedGreenContainer,
        warning = CounterYellow
    )
}

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2DFDB),
    onPrimaryContainer = Color(0xFF00251F),
    secondary = Sand,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = Color(0xFF4A635E),
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF191C1B),
    surface = BackgroundLight,
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDCE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7976),
    outlineVariant = Color(0xFFBEC9C5),
    error = OpenRed,
    onError = Color.White,
    errorContainer = OpenRedContainer,
    onErrorContainer = OnOpenRedContainer
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF003730),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFFB2DFDB),
    secondary = SandLight,
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3D00),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = Color(0xFFB1CCC6),
    onTertiary = Color(0xFF1C3531),
    background = BackgroundDark,
    onBackground = Color(0xFFE0E3E1),
    surface = BackgroundDark,
    onSurface = Color(0xFFE0E3E1),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF89938F),
    outlineVariant = Color(0xFF3F4946),
    error = OpenRedDark,
    onError = Color(0xFF690005),
    errorContainer = OpenRedContainerDark,
    onErrorContainer = OnOpenRedContainerDark
)

private val LightStatus = StatusColors(
    open = OpenRed,
    openContainer = OpenRedContainer,
    onOpenContainer = OnOpenRedContainer,
    packed = PackedGreen,
    packedContainer = PackedGreenContainer,
    onPackedContainer = OnPackedGreenContainer,
    warning = CounterYellow
)

private val DarkStatus = StatusColors(
    open = OpenRedDark,
    openContainer = OpenRedContainerDark,
    onOpenContainer = OnOpenRedContainerDark,
    packed = PackedGreenDark,
    packedContainer = PackedGreenContainerDark,
    onPackedContainer = OnPackedGreenContainerDark,
    warning = CounterYellowDark
)

@Composable
fun PackingListTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val statusColors = if (darkTheme) DarkStatus else LightStatus

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
