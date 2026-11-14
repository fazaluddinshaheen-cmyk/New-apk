package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val DarkWealthColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = EmeraldObsidian,
    primaryContainer = ChampagneGoldContainer,
    onPrimaryContainer = ChampagneGoldLight,
    secondary = EmeraldPrimary,
    onSecondary = EmeraldObsidian,
    secondaryContainer = EmeraldElevated,
    onSecondaryContainer = MintProfit,
    tertiary = ChampagneGoldLight,
    onTertiary = EmeraldObsidian,
    background = EmeraldObsidian,
    onBackground = IvoryText,
    surface = EmeraldSurface,
    onSurface = IvoryText,
    surfaceVariant = EmeraldCard,
    onSurfaceVariant = SageMuted,
    outline = BorderSubtleDark,
    error = CoralAlert,
    onError = Color(0xFF3A0A0A)
)

private val LightWealthColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.White,
    primaryContainer = SoftMintSurface,
    onPrimaryContainer = ForestPrimary,
    secondary = GoldAccentLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDF3D7),
    onSecondaryContainer = Color(0xFF473300),
    tertiary = EmeraldPrimaryDeep,
    onTertiary = Color.White,
    background = AlabasterBg,
    onBackground = DeepSlateText,
    surface = PureCardLight,
    onSurface = DeepSlateText,
    surfaceVariant = SoftMintSurface,
    onSurfaceVariant = MutedSlateText,
    outline = BorderSubtleLight,
    error = Color(0xFFB91C1C),
    onError = Color.White
)

val WealthShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CrescentWealthTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkWealthColorScheme else LightWealthColorScheme
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = WealthShapes,
            content = content
        )
    }
}
